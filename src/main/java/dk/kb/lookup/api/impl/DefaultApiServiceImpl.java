package dk.kb.lookup.api.impl;

import dk.kb.lookup.api.*;
import java.util.ArrayList;

import dk.kb.lookup.model.EntriesRequestDto;
import dk.kb.lookup.model.EntryReplyDto;
import dk.kb.lookup.model.ErrorDto;
import java.util.List;
import java.util.Map;
import dk.kb.lookup.model.ParamDto;
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
    public List<EntryReplyDto> getEntries(EntriesRequestDto param, Integer max) {
        // TODO: Implement...
        List<EntryReplyDto> response = new ArrayList<>();
        EntryReplyDto item = new EntryReplyDto();
        item.setPath("oOPCdo0J");
        item.setFilename("T5T7v");
        item.setLastSeen("DWC22");
        item.setLastSeenEpochMS(2267730128045207552L);
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
        item.setPath("Ejcyezm0BvZ3M5");
        item.setFilename("V1NXTQ1M");
        item.setLastSeen("I9evB8");
        item.setLastSeenEpochMS(1963752492612741120L);
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
        response.setPath("y3JDM");
        response.setFilename("Se05y");
        response.setLastSeen("tL7sS4");
        response.setLastSeenEpochMS(1217444224118642688L);
        return response;
    }

    /**
     * Get the number of files registered
     *
     */
    @Override
    public Integer getFilecount() {
        // TODO: Implement...
        Integer response = -1984007099;
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
        roots.add("wfdl73");
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
        response.setGeneral("IT6E2");
        return response;
    }

    /**
     * Ping the server to check if the server is reachable
     *
     */
    @Override
    public String ping() {
        // TODO: Implement...
        String response = "urTOJ";
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
        roots.add("vOzC71");
        response.setRoots(roots);
        return response;
    }

}
