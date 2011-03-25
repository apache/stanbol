package org.apache.stanbol.kres.jersey.resource;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.KReSSessionManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.renderers.SessionRenderer;
import org.semanticweb.owlapi.model.IRI;

@Path("/session/{id:.+}")
public class KReSSessionIDResource extends NavigationMixin {

	/*
	 * Placeholder for the ONManager to be fetched from the servlet context.
	 */
	protected ONManager onm;

	protected ServletContext servletContext;

	public KReSSessionIDResource(@Context ServletContext servletContext) {
		this.servletContext = servletContext;
		onm = (ONManager) this.servletContext
				.getAttribute(ONManager.class.getName());
		if (onm == null) {
			System.err
					.println("[KReS] :: No KReS Ontology Network Manager provided by Servlet Context. Instantiating now...");
			onm = new ONManagerImpl();
		}
	}

	@GET
	@Produces("application/rdf+xml")
	public Response getSessionMetadataOntology(
			@PathParam("id") String sessionId, @Context UriInfo uriInfo,
			@Context HttpHeaders headers) {

		Session ses = null;
		KReSSessionManager mgr = onm.getSessionManager();
		ses = mgr.getSession(IRI.create(sessionId));
		if (ses == null)
			return Response.status(Status.NO_CONTENT).build();

		return Response.ok(SessionRenderer.getSessionMetadataRDF(ses)).build();

	}

}
