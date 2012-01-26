package org.apache.stanbol.contenthub.web.resources;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

@Path("/contenthub")
public class RootResource extends BaseStanbolResource {

    @GET
    public Response getView() throws URISyntaxException {
        return Response.seeOther(new URI(uriInfo.getBaseUri() + "contenthub/store/")).build();
    }
}
