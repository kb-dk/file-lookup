package dk.kb.lookup.webservice.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/*
 * Catches {@link ServiceException}s and adjusts the response accordingly.
 */
@Provider
public class ServiceExceptionMapper implements ExceptionMapper<ServiceException> {

    @Override
    public Response toResponse(ServiceException exception) {
    
        Response.Status responseStatus = exception.getResponseStatus();
        Object entity = exception.getEntity();

        return entity != null ?
                Response.status(responseStatus)
                        .entity(entity)
                        .type(exception.getMimeType())
                        .build() :
                Response.status(responseStatus).build();
    }
}
