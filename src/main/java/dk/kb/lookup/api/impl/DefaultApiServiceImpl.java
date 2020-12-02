package dk.kb.lookup.api.impl;

import dk.kb.lookup.api.*;
import dk.kb.lookup.model.ErrorDto;
import java.util.List;
import java.util.Map;
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
     * Get the status for the service
     *
     */
    @Override
    public StatusReplyDto getstatus() {
        // TODO: Implement...
        StatusReplyDto response = new StatusReplyDto();
        response.setGeneral("m8D31");
        return response;
    }

    /**
     * Ping the server to check if the server is reachable
     *
     */
    @Override
    public String ping() {
        // TODO: Implement...
        String response = "aMmlQ31eYeoO3q75";
        return response;
    }

}
