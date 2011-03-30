package org.apache.stanbol.commons.web.home.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

import com.sun.jersey.api.view.Viewable;

/**
 * Root JAX-RS resource. The HTML view is implicitly rendered by a freemarker template to be found in the
 * META-INF/templates folder.
 */
@Path("/")
public class StanbolRootResource extends BaseStanbolResource {

    @GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

}
