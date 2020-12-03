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

import java.util.*;
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

    /**
     * Get the entries (path, filename and lastSeen) for a given regexp
     *
     */
    @Override
    public List<EntryReplyDto> getEntriesFromRegexp(String regexp, Integer max) {
        Pattern pattern = Pattern.compile(regexp);
        final int limit = max == -1 ? Integer.MAX_VALUE : max;

        return filenameMap.values().stream().
                filter(entry -> pattern.matcher(entry.getFullpath()).matches()).
                limit(limit).
                map(this::toReplyEntry).
                collect(Collectors.toList());
    }

    /**
     * Get the entry (path, filename and lastSeen) for a given filename
     *
     */
    @Override
    public EntryReplyDto getEntryFromFilename(String filename) {
        FileEntry entry = filenameMap.get(filename);
        if (entry != null) {
            return toReplyEntry(entry);
        }
        throw new NoContentServiceException("Unable to locate an entry for '" + filename + "'");
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

        // TODO: Better return message
        if (scanRoots.isEmpty() ||
            !ScanBot.instance().isReady() ||
            !ScanBot.instance().startScan(scanRoots, this::acceptFolder)) {
            RootsReplyDto response = new RootsReplyDto();
            response.setRoots(Collections.emptyList());
            return response;
        }

        RootsReplyDto response = new RootsReplyDto();
        response.setRoots(scanRoots);
        return response;
    }

    private void acceptFolder(ScanBot.Folder folder) {

        log.debug("acceptFolder(" + folder + ") called");
        // TODO: Delete all folder content from MemoryImpl before adding new
        folder.forEach(entry -> filenameMap.put(entry.filename, entry));
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
