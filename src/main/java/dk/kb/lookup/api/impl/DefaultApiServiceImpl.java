package dk.kb.lookup.api.impl;

import dk.kb.lookup.api.*;
import java.util.ArrayList;
import dk.kb.lookup.model.EntriesRequestDto;
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
     * Get the entries (path, filename and lastSeen) based on a given regexp or start time. Note that this is potentially a heavy request
     *
     */
    @Override
    public List<EntryReplyDto> getEntries(EntriesRequestDto entriesRequest, Integer max) {
        // TODO: Implement...
        List<EntryReplyDto> response = new ArrayList<>();
        EntryReplyDto item = new EntryReplyDto();
        item.setPath("rjh7EJ4o");
        item.setFilename("pu498VK");
        item.setLastSeen("ExHbM1");
        item.setLastSeenEpochMS(-4581060747177314304L);
        response.add(item);
        return response;
    }

    /**
     * Get the entries (path, filename and lastSeen) for multiple filenames
     *
     */
    @Override
    public List<EntryReplyDto> getEntriesFromFilenames(List<String> filename) {
        // TODO: Implement...
        List<EntryReplyDto> response = new ArrayList<>();
        EntryReplyDto item = new EntryReplyDto();
        item.setPath("KEbPS");
        item.setFilename("wAhvs9i");
        item.setLastSeen("D2UgVR2");
        item.setLastSeenEpochMS(6379308956885094400L);
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
        response.setPath("uynk8x");
        response.setFilename("ghmpsK");
        response.setLastSeen("DcnOk5Q");
        response.setLastSeenEpochMS(7880842345871437824L);
        return response;
    }

    /**
     * Get the number of files registered
     *
     */
    @Override
    public Integer getFilecount() {
        // TODO: Implement...
        Integer response = -1528319574;
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
        roots.add("cHD9dr44x");
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
        response.setGeneral("Cn4SAKWA751");
        List<String> roots = new ArrayList<>();
        roots.add("uQ9fPSTv");
        response.setRoots(roots);
        response.setFiles(-2009464021);
        response.setState(StatusReplyDto.StateEnum.SCANNING);
        response.setCurrentScanFolder("M55t1");
        return response;
    }

    /**
     * Ping the server to check if the server is reachable
     *
     */
    @Override
    public String ping() {
        // TODO: Implement...
        String response = "olt3B4";
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
        roots.add("OW5cP9W315L4");
        response.setRoots(roots);
        return response;
    }

}
