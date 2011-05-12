package org.apache.stanbol.rules.web.resources;

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

/**
 * This is the root resource of Apache Stanbol Rules.<br>
 * It contains an explaination about the HTTP services provided by Stanbol Rules.
 * 
 * @author andrea.nuzzolese
 *
 */

@Path("/rules")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class RulesRootResource extends BaseStanbolResource {

	public RulesRootResource(@Context ServletContext servletContext) {
        

    }
	
	@GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }
	
}
