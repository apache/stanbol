package org.apache.stanbol.factstore.web.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

import com.sun.jersey.api.view.Viewable;

@Path("/factstore")
public class FactStoreRootResource extends BaseStanbolResource {
    
    @GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }
    
}
