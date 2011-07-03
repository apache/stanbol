package org.apache.stanbol.demos.integritycheck.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.ScriptResource;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.api.view.Viewable;

/**
 * 
 * @author enridaga
 */
@Path("/integritycheck")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class IntegrityCheckResource extends BaseStanbolResource{
	@GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }
}
