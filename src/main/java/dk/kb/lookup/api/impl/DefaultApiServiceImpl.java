package dk.kb.lookup.api.impl;

import dk.kb.lookup.api.*;
import java.util.ArrayList;
import dk.kb.lookup.model.EntryReplyDto;
import dk.kb.lookup.model.ErrorDto;
import java.util.List;
import java.util.Map;
import dk.kb.lookup.model.RootsReplyDto;
import dk.kb.lookup.model.StatusReplyDto;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.io.File;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;

import org.apache.cxf.jaxrs.ext.multipart.*;

import io.swagger.annotations.Api;

/**
 * file-lookup
 *
 * <p>This pom can be inherited by projects wishing to integrate to the SBForge development platform. 
 *
 */
public class DefaultApiServiceImpl implements DefaultApi {
    /**
     * Get the entries (path, filename and lastSeen) for multiple filenames filenames
     *
     */
    @Override
    public List<EntryReplyDto> getEntriesFromFilename(List<String> filename) {
        // TODO: Implement...
        List<EntryReplyDto> response = new ArrayList<>();
        EntryReplyDto item = new EntryReplyDto();
        item.setPath("KC5lU");
        item.setFilename("rE1RW");
        item.setLastSeen("b5PaH4");
        response.add(item);
        return response;
    }

    /**
     * Get the entries (path, filename and lastSeen) for a given regexp. Note that this is a heavy request
     *
     */
    @Override
    public List<EntryReplyDto> getEntriesFromRegexp(String regexp, Integer max) {
        // TODO: Implement...
        List<EntryReplyDto> response = new ArrayList<>();
        EntryReplyDto item = new EntryReplyDto();
        item.setPath("X9E5j");
        item.setFilename("rvfkpMz");
        item.setLastSeen("Psr2U");
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
        response.setPath("UTF2s");
        response.setFilename("SiG2Hg1WQ");
        response.setLastSeen("wMaNJCZ8k");
        return response;
    }

    /**
     * Get the number of files registered
     *
     */
    @Override
    public Integer getFilecount() {
        // TODO: Implement...
        Integer response = -758350544;
        return response;
    }

    /**
     * Get the file paths that are tracked
     *
     */
    @Override
    public RootsReplyDto getRoots() {
        // TODO: Implement...
        RootsReplyDto response = new RootsReplyDto();
        List<String> roots = new ArrayList<>();
        roots.add("Ppffh5AnmGQ");
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
        response.setGeneral("JpUjSU");
        return response;
    }

    /**
     * Ping the server to check if the server is reachable
     *
     */
    @Override
    public String ping() {
        // TODO: Implement...
        String response = "jEsjV";
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
        roots.add("gWOYLye");
        response.setRoots(roots);
        return response;
    }

}
