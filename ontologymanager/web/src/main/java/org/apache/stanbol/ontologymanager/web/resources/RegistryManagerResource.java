package org.apache.stanbol.ontologymanager.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

@Path("/ontonet/registry")
public class RegistryManagerResource extends BaseStanbolResource {

    protected RegistryManager regMgr;

    private final Logger log = LoggerFactory.getLogger(getClass());

    // bind the registry manager by looking it up from the servlet request context
    public RegistryManagerResource(@Context ServletContext context, @Context UriInfo uriInfo) {
        super();
        regMgr = ContextHelper.getServiceFromContext(RegistryManager.class, context);
        this.uriInfo = uriInfo;
    }
    
    public String getPath() {
        return uriInfo.getPath().replaceAll("[\\/]*$", "");
    }

    @GET
    @Produces(value = MediaType.TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok();
        enableCORS(servletContext, rb, headers);
        return rb.build();
    }

    public List<Library> getLibraries() {
        if (regMgr != null) {
            log.debug("There are {} ontology libraries registered.", regMgr.getLibraries().size());
            return new ArrayList<Library>(regMgr.getLibraries());
        } else {
            log.debug("There are no ontology libraries registered.");
            return Collections.emptyList();
        }
    }

}
