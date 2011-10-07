package org.apache.stanbol.reasoners.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServicesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

@Path("/reasoners")
public class ReasoningServicesResource extends BaseStanbolResource {
    private Logger log = LoggerFactory.getLogger(getClass());
    private ServletContext context;
    private UriInfo uriInfo;

    public ReasoningServicesResource(@Context ServletContext servletContext, @Context UriInfo uriInfo) {
        super();
        this.context = servletContext;
        this.uriInfo = uriInfo;
    }

    public String getCurrentPath() {
        return uriInfo.getPath().replaceAll("[\\/]*$", "");
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getDocumentation() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

    private ReasoningServicesManager getServicesManager() {
        log.debug("(getServicesManager()) ");
        return (ReasoningServicesManager) ContextHelper.getServiceFromContext(ReasoningServicesManager.class,
            this.context);
    }

    public Set<ReasoningService<?,?,?>> getActiveServices() {
        log.debug("(getActiveServices()) There are {} reasoning services", getServicesManager().size());
        return getServicesManager().asUnmodifiableSet();
    }

}
