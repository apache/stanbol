package org.apache.stanbol.commons.web.vie.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

import com.sun.jersey.api.view.Viewable;

/**
 * Adapter to add a VIE based interface for the Stanbol Enhancer
 * @author Szaby Gruenwald
 */
@Path("/enhancervie")
public class EnhancerVieRootResource extends BaseStanbolResource {


    @GET
    @Produces(TEXT_HTML + ";qs=2")
    public Viewable getView() {
        return new Viewable("index", this);
    }

}
