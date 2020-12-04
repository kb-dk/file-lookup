package dk.kb.lookup.api.impl;

import dk.kb.lookup.FileEntry;
import dk.kb.lookup.ScanBot;
import dk.kb.lookup.api.DefaultApi;
import dk.kb.lookup.config.LookupServiceConfig;
import dk.kb.lookup.model.EntryReplyDto;
import dk.kb.lookup.model.RootsReplyDto;
import dk.kb.lookup.model.StatusReplyDto;
import dk.kb.lookup.webservice.exception.NoContentServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * file-lookup
 *
 * <p>This pom can be inherited by projects wishing to integrate to the SBForge development platform. 
 *
 */
public class MemoryImpl implements DefaultApi {
    private static final Logger log = LoggerFactory.getLogger(MemoryImpl.class);

    private static List<String> roots = LookupServiceConfig.getConfig().getList(".config.roots");
    private final static Map<String, FileEntry> filenameMap = new HashMap<>();
    private final static ReadWriteLock locks = new ReentrantReadWriteLock();

    /**
     * Get the entries (path, filename and lastSeen) for a given regexp
     *
     */
    @Override
    public List<EntryReplyDto> getEntriesFromRegexp(String regexp, Integer max) {
        Pattern pattern = Pattern.compile(regexp);
        final int limit = max == -1 ? Integer.MAX_VALUE : max;

        try {
            locks.readLock().lock();
            return filenameMap.values().stream().
                    filter(entry -> pattern.matcher(entry.getFullpath()).matches()).
                    limit(limit).
                    map(this::toReplyEntry).
                    collect(Collectors.toList());
        } finally {
            locks.readLock().unlock();
        }
    }

    /**
     * Get the entry (path, filename and lastSeen) for a given filename
     *
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
        } finally {
            locks.readLock().unlock();
        }
    }

    /**
     * Get the entries (path, filename and lastSeen) for multiple filenames filenames
     *
     */
    @Override
    public List<EntryReplyDto> getEntriesFromFilename(List<String> filenames) {
        try {
            locks.readLock().lock();
            return filenames.stream().
                    map(filenameMap::get).
                    filter(Objects::nonNull).
                    map(this::toReplyEntry).
                    collect(Collectors.toList());
        } finally {
            locks.readLock().unlock();
        }
    }

    /**
     * Get the number of files registered
     *
     */
    @Override
    public Integer getFilecount() {
        log.debug("Returning filecount " + filenameMap.size()); // TODO: Delete
        return filenameMap.size();
    }

    /**
     * Get the file paths that are tracked
     *
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
     */
    @Override
    public StatusReplyDto getStatus() {
        StatusReplyDto response = new StatusReplyDto();
        response.setGeneral(String.format(Locale.ENGLISH, "%d roots, %d files", roots.size(), filenameMap.size()));
        return response;
    }

    /**
     * Ping the server to check if the server is reachable
     *
     */
    @Override
    public String ping() {
        return "file-lookup is alive";
    }

    /**
     * Start a scan of all or some of the roots. If a scan is already running a new one will not be started
     *
     */
    @Override
    public RootsReplyDto startScan(String rootPattern) {
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
        } finally {
            locks.writeLock().unlock();
        }
        log.debug("Filecount after accept=" + filenameMap.size());
    }

    /* ----------------------------------------------------------------------------------- */

    private EntryReplyDto toReplyEntry(FileEntry fileEntry) {
        EntryReplyDto item = new EntryReplyDto();
        item.setPath(fileEntry.path);
        item.setFilename(fileEntry.filename);
        item.setLastSeen(fileEntry.getLastSeenAsISO8601());
        return item;
    }
}
