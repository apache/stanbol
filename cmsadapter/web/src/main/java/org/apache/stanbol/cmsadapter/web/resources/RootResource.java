package org.apache.stanbol.cmsadapter.web.resources;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

@Path("/cmsadapter")
public class RootResource extends BaseStanbolResource {

    /**
     * Simply redirects user to CMS Adapter's wiki page at IKS Wiki. 
     * @return  
     */
    @GET
    public Response notifyChange() {
        try {
            return Response.seeOther(new URI("http://wiki.iks-project.eu/index.php/CMSAdapterRest")).build();
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e, Response.status(Status.BAD_REQUEST).build());
        }
    }
}
