package dk.kb.lookup.api.impl;

import dk.kb.lookup.CallbackInputStream;
import dk.kb.lookup.FileEntry;
import dk.kb.lookup.ScanBot;
import dk.kb.lookup.api.MergedApi;
import dk.kb.lookup.config.ServiceConfig;
import dk.kb.lookup.model.EntryReplyDto;
import dk.kb.lookup.model.RootsReplyDto;
import dk.kb.lookup.model.StatusReplyDto;
import dk.kb.webservice.exception.InternalServiceException;
import dk.kb.webservice.exception.InvalidArgumentServiceException;
import dk.kb.webservice.exception.NoContentServiceException;
import dk.kb.webservice.exception.ServiceException;
import dk.kb.webservice.exception.StreamingServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * H2-backed persistent file-lookup implementation.
 */
public class H2Impl implements MergedApi {
    private static final Logger log = LoggerFactory.getLogger(H2Impl.class);

    public static final int REPLY_STREAM_ACTIVATION = 1000; // When more than this is requested, streaming is used
    public static final int REPLY_SORT_LIMIT = 100_000; // Only accept sort requests up to this size

    // Must be final as MemoryImpl are instantiated anew for each call
    private final static List<String> roots = ServiceConfig.getConfig().getList(".lookup.roots");
    private final static Map<String, FileEntry> filenameMap = new HashMap<>();
    private final static ReadWriteLock locks = new ReentrantReadWriteLock();

    /**
     * Get the entries (path, filename and lastSeen) based on a multiple optional constraints. All returned entries fulfills all given constraints. Note that this is potentially a heavy request
     *
     * @param regexp : The regexp which will be matched against the full path + filename
     *
     * @param glob : Glob-style matcher, which will be matched against the full path + filename. See https://docs.oracle.com/javase/7/docs/api/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String) for syntax
     *
     * @param since : Only entries newer than this will be returned
     *
     * @param sinceEpochMS : Only entries newer than this will be returned
     *
     * @param max : The maximum number of entries to return, -1 if there is no limit
     *
     * @param ordered : If true, the entries are returned ordered by their timestamp. Setting this to true with max&#x3D;-1 or max&gt;100000 will fail
     *
     * @return <ul>
      *   <li>code = 200, message = "A list with the path, filename and lastSeen timestamps for the matches, sorted oldest to newest. The list can be empty", response = EntryReplyDto.class, responseContainer = "List"</li>
      *   <li>code = 500, message = "Internal Error", response = ErrorDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public Response getEntries(
            String regexp, String glob, String since, Long sinceEpochMS, Integer max, Boolean ordered)
            throws ServiceException {
        long sinceEpoch = sinceEpochMS == null ? 0 : sinceEpochMS;
        if (since != null) {
            sinceEpoch = Math.max(sinceEpoch, toEpoch(since));
        }

        final long finalSince = sinceEpoch;
        final int limit = max == -1 ? Integer.MAX_VALUE : max;
        Pattern pattern = regexp == null || regexp.isEmpty() ? null :
                Pattern.compile(regexp);
        PathMatcher globMatcher = glob == null  || glob.isEmpty() ? null :
                FileSystems.getDefault().getPathMatcher("glob:" + glob);

        ordered = ordered != null && ordered;
        if (ordered && limit > REPLY_SORT_LIMIT) {
            throw new InvalidArgumentServiceException(
                    "Sorted response was requested but max=" + max + " exceeds the sort limit of " + REPLY_SORT_LIMIT);
        }

        try {
            locks.readLock().lock();

            // Create a stream with the entries
            Stream<FileEntry> entries = filenameMap.values().stream().
                    filter(entry -> entry.lastSeen >= finalSince).
                    filter(entry -> pattern == null || pattern.matcher(entry.getFullpath()).matches()).
                    filter(entry -> globMatcher == null || globMatcher.matches(Paths.get(entry.getFullpath()))).
                    limit(limit);

            // Sort if needed
            entries = ordered ? entries.sorted(Comparator.comparingLong(e -> e.lastSeen)) : entries;

            // If the max is low enough, collect the results immediately and return them
            if (limit > -1 && limit <= REPLY_STREAM_ACTIVATION) { // Return directly
                return Response.accepted(entries.
                        map(this::toReplyEntry).
                        collect(Collectors.toList())).build();
            }

            // It is potentially a very large result, so stream it
            throw streamReplies(entries);
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            locks.readLock().unlock();
        }
    }

    /**
     * Creates an "Exception" that signals HTTP 200 and delivers the FileEntries as a valid JSON stream.
     * @param entries a stream of entries to deliver.
     */
    private StreamingServiceException streamReplies(Stream<FileEntry> entries) {
        locks.readLock().lock();
        final Iterator<FileEntry> iterator = entries.iterator();
        final AtomicBoolean first = new AtomicBoolean(true);
        return new StreamingServiceException("application/json", new CallbackInputStream(
                () -> { // Producer
                    try {
                        if (first.get()) {
                            first.set(false);
                            return "{\n" + (iterator.hasNext() ? "" : "}");
                        }
                        if (!iterator.hasNext()) { // Depleted
                            return "";
                        }
                        return iterator.next().toJSON() + (iterator.hasNext() ? ",\n" : "\n}");
                    } catch (Exception e) {

                        locks.readLock().unlock();
                        throw new RuntimeException("Exception while writing output", e);
                    }
                },
                depleted -> { // Finalizer
                    // Manual check with breakpoints shows that InputStream.close is called if the client disconnects
                    // This causes the depleted-callback to fire and thus release the lock
                    locks.readLock().unlock();
                }
        ));
    }

    public synchronized long toEpoch(String iso) {
        try {
            return iso8601.parse(iso).getTime();
        } catch (ParseException e) {
            throw new InvalidArgumentServiceException(
                    "The timestamp '" + iso + "' could not be parsed using pattern '" + iso8601.toPattern() + "'", e);
        }
    }
    final static SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);


    /**
     * Get the entry (path, filename and lastSeen) for a given filename
     *
     * @param filename: The filename to locate
     *
     * @return <ul>
      *   <li>code = 200, message = "A JSON structure containing the path, filename and lastSeen timestamp for the given filename", response = EntryReplyDto.class</li>
      *   <li>code = 204, message = "If an entry for the given filename could not be located", response = String.class</li>
      *   <li>code = 500, message = "Internal Error", response = ErrorDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public EntryReplyDto getEntryFromFilename(String filename) {
        FileEntry entry;
        try {
            locks.readLock().lock();
            entry = filenameMap.get(filename);
            if (entry != null) {
                return toReplyEntry(entry);
            }
            throw new NoContentServiceException("Unable to locate an entry for '" + filename + "'");
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            locks.readLock().unlock();
        }
    }

    /**
     * Get the entries (path, filename and lastSeen) for multiple filenames
     *
     * @param filenames: The filenames to locate
     *
     * @return <ul>
      *   <li>code = 200, message = "A list with the path, filename and lastSeen timestamps for the filenames. The list can be empty", response = EntryReplyDto.class, responseContainer = "List"</li>
      *   <li>code = 500, message = "Internal Error", response = ErrorDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public List<EntryReplyDto> getEntriesFromFilenames(List<String> filenames) {
        try {
            locks.readLock().lock();
            return filenames.stream().
                    map(filenameMap::get).
                    filter(Objects::nonNull).
                    map(this::toReplyEntry).
                    collect(Collectors.toList());
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            locks.readLock().unlock();
        }
    }

    /**
     * Get the number of files registered
     *
     * @return <ul>
      *   <li>code = 200, message = "An integer stating the number of registered files", response = Integer.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public Integer getFilecount() {
        log.debug("Returning filecount " + filenameMap.size()); // TODO: Delete
        return filenameMap.size();
    }

    /**
     * Get the file paths that are tracked
     *
     * @return <ul>
      *   <li>code = 200, message = "The roots (file paths) that are tracked by the service", response = RootsReplyDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public RootsReplyDto getRoots() {
        RootsReplyDto response = new RootsReplyDto();
        response.setRoots(roots);
        return response;
    }

    /**
     * Get the status for the service
     *
     * @return <ul>
      *   <li>code = 200, message = "A structure containing the status of the service (number of files etc.)", response = StatusReplyDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public StatusReplyDto getStatus() {
        StatusReplyDto response = new StatusReplyDto();
        response.setGeneral(String.format(Locale.ENGLISH, "%d roots, %d files", roots.size(), filenameMap.size()));
        response.setRoots(roots);
        response.setFiles(filenameMap.size());
        response.setState(ScanBot.instance().getState() == ScanBot.STATE.idle ?
                                  StatusReplyDto.StateEnum.IDLE :
                                  StatusReplyDto.StateEnum.SCANNING);
        response.setCurrentScanFolder(ScanBot.instance().getActivePath());
        return response;
    }

    /**
     * Ping the server to check if the server is reachable
     *
     * @return <ul>
      *   <li>code = 200, message = "OK", response = String.class</li>
      *   <li>code = 406, message = "Not Acceptable", response = ErrorDto.class</li>
      *   <li>code = 500, message = "Internal Error", response = ErrorDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public String ping() {
        return "file-lookup is alive";
    }

    /**
     * Start a scan of all or some of the roots. If a scan is already running a new one will not be started
     *
     * @param rootPattern: A pattern for the roots to scan
     *
     * @return <ul>
      *   <li>code = 200, message = "A list of the roots for the started scan or the empty list if the pattern did not match any roots or a scan was already running", response = RootsReplyDto.class</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public RootsReplyDto startScan(String rootPattern) {
        log.debug("startScan(rootPattern=" + rootPattern + ") called");
        Pattern pattern = Pattern.compile(rootPattern);
        List<String> scanRoots = roots.stream().
                filter(root -> pattern.matcher(root).matches()).
                collect(Collectors.toList());

        final long startTime = System.currentTimeMillis();
        if (scanRoots.isEmpty() ||
            !ScanBot.instance().isReady() ||
            !ScanBot.instance().startScan(scanRoots, this::acceptFolder, new Purger(scanRoots, startTime))) {
            // TODO: Better return message
            RootsReplyDto response = new RootsReplyDto();
            response.setRoots(Collections.emptyList());
            return response;
        }

        RootsReplyDto response = new RootsReplyDto();
        response.setRoots(scanRoots);
        return response;
    }

    /**
     * Inform the service of an added files. If a file is already known, its timestamp is updated
     *
     * @param files: Paths and filenames of the files
     *
     * @param validate: Whether or not the files existence should be validated before adding
     *
     * @return <ul>
      *   <li>code = 200, message = "An list of the added files. This will always be equal to the input if validate is false", response = String.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public List<String> addFiles(List<String> files, Boolean validate) {
        log.debug("addFiles(#" + files.size() + " files, validate=" + validate + ") called");
        List<String> feedback = new ArrayList<>(files.size());
        List<FileEntry> keep = new ArrayList<>(files.size());
        for (String fileStr: files) {
            File file = new File(fileStr);
            if (validate && !file.isFile()) {
                continue;
            }
            feedback.add(fileStr);
            keep.add(new FileEntry(file.getPath(), file.getName()));
        }

        log.debug("addFiles adding " + keep.size() + "/" + files.size() + " files");
        try {
            locks.writeLock().lock();
            keep.forEach(entry -> filenameMap.put(entry.filename, entry));
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            locks.writeLock().unlock();
        }

        return feedback;
    }

    /**
     * Inform the service of removed files
     *
     * @param files: Paths and filenames of the files
     *
     * @param validate: Whether or not the files existence should be validated before removing. If it still exists it will not be removed
     *
     * @return <ul>
      *   <li>code = 200, message = "An list of the removed files. This will always be equal to the input if validate is false", response = String.class, responseContainer = "List"</li>
      *   </ul>
      * @throws ServiceException when other http codes should be returned
      *
      * @implNote return will always produce a HTTP 200 code. Throw ServiceException if you need to return other codes
     */
    @Override
    public List<String> removeFiles(List<String> files, Boolean validate) {
        log.debug("removeFiles(#" + files.size() + " files, validate=" + validate + ") called");
        List<String> feedback = new ArrayList<>(files.size());
        List<FileEntry> remove = new ArrayList<>(files.size());
        for (String fileStr: files) {
            File file = new File(fileStr);
            if (validate && file.isFile()) {
                continue;
            }
            feedback.add(fileStr);
            remove.add(new FileEntry(file.getPath(), file.getName()));
        }

        log.debug("removeFiles removing " + remove.size() + "/" + files.size() + " files");
        try {
            locks.writeLock().lock();
            remove.forEach(entry -> filenameMap.remove(entry.filename));
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            locks.writeLock().unlock();
        }

        return feedback;
    }

    /* ----------------------------------------------------------------------------------- */


    /**
     * Removes entries under the given roots that are older than minTime,
     */
    private static class Purger implements Runnable {
        final List<String> roots;
        final long minTime;

        /**
         * @param roots the roots to consider when removing entries.
         * @param minTime the minimum time for an entry under the roots to be preserved.
         */
        public Purger(List<String> roots, long minTime) {
            this.roots = roots;
            this.minTime = minTime;
        }

        @Override
        public void run() {
            long purgeCount = 0;
            try {
                locks.writeLock().lock();
                Iterator<Map.Entry<String, FileEntry>> entries = filenameMap.entrySet().iterator();
                while (entries.hasNext()) {
                    FileEntry entry = entries.next().getValue();
                    if (entry.lastSeen < minTime) { // Old entry. Check if it was under one of the scanned roots
                        for (String root : roots) {
                            if (entry.path.startsWith(root)) { // Old and under one of the roots: Purge it
                                entries.remove();
                                purgeCount++;
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn(String.format(Locale.ENGLISH, "Unhandled Exception during purge with roots=%s, minTime=%d",
                                       roots, minTime), e);
            } finally {
                locks.writeLock().unlock();
            }
            log.debug("Purged " + purgeCount + " entries for deleted files for roots '" + roots);
        }
    }


    private void acceptFolder(ScanBot.Folder folder) {
        log.debug("acceptFolder(" + folder + ") called");
        try {
            locks.writeLock().lock();
            folder.forEach(entry -> filenameMap.put(entry.filename, entry));
        } catch (Exception e) {
            throw handleException(e);
        } finally {
            locks.writeLock().unlock();
        }
        //log.debug("File count after accept=" + filenameMap.size());
    }

    private EntryReplyDto toReplyEntry(FileEntry fileEntry) {
        EntryReplyDto item = new EntryReplyDto();
        item.setPath(fileEntry.path);
        item.setFilename(fileEntry.filename);
        item.setLastSeenEpochMS(fileEntry.lastSeen);
        item.setLastSeen(fileEntry.getLastSeenAsISO8601());
        return item;
    }

    /**
    * This method simply converts any Exception into a Service exception
    * @param e: Any kind of exception
    * @return A ServiceException
    * @see dk.kb.webservice.ServiceExceptionMapper
    */
    private ServiceException handleException(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e; // Do nothing - this is a declared ServiceException from within module.
        } else {// Unforseen exception (should not happen). Wrap in internal service exception
            log.error("ServiceException(HTTP 500):", e); //You probably want to log this.
            return new InternalServiceException(e.getMessage());
        }
    }
}
