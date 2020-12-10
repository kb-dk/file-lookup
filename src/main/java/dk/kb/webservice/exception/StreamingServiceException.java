package dk.kb.webservice.exception;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/*
 * Custom web-exception class (200)
 *
 * An on-purpose Exception (aka a hack) for streaming a response.
 */
public class StreamingServiceException extends ServiceException {

    //Constant fields for the OpenApi
    public static final String description = "StreamingServiceException";
    public static final String responseCode = "200";


    private static final long serialVersionUID = 27182825L;
    private static final Response.Status responseStatus = Response.Status.OK; // 200

    /**
     * @param stream the content to deliver. The MIME type will be 'application/octet-stream'.
     */
    public StreamingServiceException(InputStream stream) {
        super("application/octet-stream", stream, responseStatus);
    }

    /**
     * @param mimeType the MIME type to send with the response.
     * @param stream the content to deliver.
     */
    public StreamingServiceException(String mimeType, InputStream stream) {
        super(mimeType, stream, responseStatus);
    }

}


