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
     * Inform the service of an added files. If a file is already known, its timestamp is updated
     *
     */
    @Override
    public List<String> addFiles(List<String> file, Boolean validate) {
        // TODO: Implement...
        List<String> response = new ArrayList<>();
        response.add("qvNn0fq3V");
        return response;
    }

    /**
     * Get the entries (path, filename and lastSeen) based on a given regexp or start time. Note that this is potentially a heavy request
     *
     */
    @Override
    public List<EntryReplyDto> getEntries(EntriesRequestDto entriesRequest, Integer max) {
        // TODO: Implement...
        List<EntryReplyDto> response = new ArrayList<>();
        EntryReplyDto item = new EntryReplyDto();
        item.setPath("ZmOP0e");
        item.setFilename("AtwV7");
        item.setLastSeen("jwaw5");
        item.setLastSeenEpochMS(7593289901101535232L);
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
        item.setPath("Eeqpt");
        item.setFilename("Q5ptm");
        item.setLastSeen("rhvVoW5Xtc");
        item.setLastSeenEpochMS(6307370278029373440L);
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
        response.setPath("njqBW");
        response.setFilename("kCQoat1");
        response.setLastSeen("Uqh0w");
        response.setLastSeenEpochMS(2989570608208822272L);
        return response;
    }

    /**
     * Get the number of files registered
     *
     */
    @Override
    public Integer getFilecount() {
        // TODO: Implement...
        Integer response = 1491044217;
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
        roots.add("D1DKNx");
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
        response.setGeneral("Gh281");
        List<String> roots = new ArrayList<>();
        roots.add("fCUDRK");
        response.setRoots(roots);
        response.setFiles(368767240);
        response.setState(StatusReplyDto.StateEnum.IDLE);
        response.setCurrentScanFolder("b8V4t");
        return response;
    }

    /**
     * Ping the server to check if the server is reachable
     *
     */
    @Override
    public String ping() {
        // TODO: Implement...
        String response = "Hn373M";
        return response;
    }

    /**
     * Inform the service of removed files
     *
     */
    @Override
    public List<String> removeFiles(List<String> file, Boolean validate) {
        // TODO: Implement...
        List<String> response = new ArrayList<>();
        response.add("J9ywU3");
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
        roots.add("Qg9eT7baL");
        response.setRoots(roots);
        return response;
    }

}
