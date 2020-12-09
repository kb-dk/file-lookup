/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package dk.kb.lookup.api;

import org.apache.cxf.jaxrs.ext.MessageContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * All the OpenAPI-defined web services in one interface.
 */
public interface MergedApi extends LookupApi, ControlApi, StatusApi {
    // We override this to avoid the problem of inheriting multiple versions of the default implementation
    /**
    * This method just redirects gets to WEBAPP/api to the swagger UI /WEBAPP/api/api-docs?url=WEBAPP/api/openapi.yaml
    */
    @GET
    @Path("/")
    default public Response redirect(@Context MessageContext request){
        String path = request.get("org.apache.cxf.message.Message.PATH_INFO").toString();
        if (path != null && !path.endsWith("/")){
            path = path + "/";
        }
        return Response.temporaryRedirect(URI.create("api-docs?url=" + path + "openapi.yaml")).build();
    }
}
