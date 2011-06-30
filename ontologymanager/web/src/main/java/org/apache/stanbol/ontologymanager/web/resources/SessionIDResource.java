package org.apache.stanbol.ontologymanager.web.resources;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.renderers.SessionRenderer;
import org.semanticweb.owlapi.model.IRI;

@Path("/ontonet/session/{id:.+}")
public class SessionIDResource extends BaseStanbolResource {

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;

    protected ServletContext servletContext;

    public SessionIDResource(@Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.onm = (ONManager) ContextHelper.getServiceFromContext(ONManager.class, servletContext);
    }

    @GET
    @Produces("application/rdf+xml")
    public Response getSessionMetadataOntology(@PathParam("id") String sessionId,
                                               @Context UriInfo uriInfo,
                                               @Context HttpHeaders headers) {

        Session ses = null;
        SessionManager mgr = onm.getSessionManager();
        ses = mgr.getSession(IRI.create(sessionId));
        if (ses == null) return Response.status(Status.NO_CONTENT).build();

        return Response.ok(SessionRenderer.getSessionMetadataRDFasOntology(ses)).build();

    }

}
