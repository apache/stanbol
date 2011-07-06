package org.apache.stanbol.reasoners.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.api.view.Viewable;

@Path("/reasoners")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class ReasonersResource extends BaseStanbolResource {

    public ReasonersResource(@Context ServletContext servletContext) {}

    @GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }
}
