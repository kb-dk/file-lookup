package dk.kb.lookup.api.impl;

import dk.kb.lookup.api.DefaultApi;
import dk.kb.lookup.config.LookupServiceConfig;
import dk.kb.lookup.model.EntryReplyDto;
import dk.kb.lookup.model.RootsReplyDto;
import dk.kb.lookup.model.StatusReplyDto;

import java.util.ArrayList;
import java.util.List;

/**
 * file-lookup
 *
 * <p>This pom can be inherited by projects wishing to integrate to the SBForge development platform. 
 *
 */
public class MemoryImpl implements DefaultApi {

    private List<String> roots = LookupServiceConfig.getConfig().getList(".config.roots");

    /**
     * Get the entries (path, filename and lastSeen) for a given regexp
     *
     */
    @Override
    public List<EntryReplyDto> getEntriesFromRegexp(String regexp) {
        // TODO: Implement...
        List<EntryReplyDto> response = new ArrayList<>();
        EntryReplyDto item = new EntryReplyDto();
        item.setPath("yn430g");
        item.setFilename("smHFEK");
        item.setLastSeen("W8UKj5");
        response.add(item);
        return response;
    }

    /**
     * Get the entry (path, filename and lastSeen) for a given filename
     *
     */
    @Override
    public EntryReplyDto getEntryFromFilename(String filename) {
        // TODO: Implement...
        EntryReplyDto response = new EntryReplyDto();
        response.setPath("oJKLY");
        response.setFilename("H2zGL");
        response.setLastSeen("B3hjM");
        return response;
    }

    /**
     * Get the number of files registered
     *
     */
    @Override
    public Integer getFilecount() {
        // TODO: Implement...
        Integer response = -917886229;
        return response;
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
        // TODO: Implement...
        StatusReplyDto response = new StatusReplyDto();
        response.setGeneral("MQ3h45z1gBlJC");
        return response;
    }

    /**
     * Ping the server to check if the server is reachable
     *
     */
    @Override
    public String ping() {
        // TODO: Implement...
        String response = "W49VwBr";
        return response;
    }

    /**
     * Start a scan of all or some of the roots. If a scan is already running a new one will not be started
     *
     */
    @Override
    public RootsReplyDto startScan(String rootPattern) {
        // TODO: Implement...
        RootsReplyDto response = new RootsReplyDto();
        List<String> roots = new ArrayList<>();
        roots.add("L8U47na");
        response.setRoots(roots);
        return response;
    }

}
