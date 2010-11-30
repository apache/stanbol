package eu.iksproject.kres.jersey.resource;

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

import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.session.KReSSession;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.renderers.SessionRenderer;

@Path("/session/{id:.+}")
public class KReSSessionIDResource extends NavigationMixin {

	/*
	 * Placeholder for the KReSONManager to be fetched from the servlet context.
	 */
	protected KReSONManager onm;

	protected ServletContext servletContext;

	public KReSSessionIDResource(@Context ServletContext servletContext) {
		this.servletContext = servletContext;
		onm = (KReSONManager) this.servletContext
				.getAttribute(KReSONManager.class.getName());
		if (onm == null) {
			System.err
					.println("[KReS] :: No KReS Ontology Network Manager provided by Servlet Context. Instantiating now...");
			onm = new ONManager();
		}
	}

	@GET
	@Produces("application/rdf+xml")
	public Response getSessionMetadataOntology(
			@PathParam("id") String sessionId, @Context UriInfo uriInfo,
			@Context HttpHeaders headers) {

		KReSSession ses = null;
		KReSSessionManager mgr = onm.getSessionManager();
		ses = mgr.getSession(IRI.create(sessionId));
		if (ses == null)
			return Response.status(Status.NO_CONTENT).build();

		return Response.ok(SessionRenderer.getSessionMetadataRDF(ses)).build();

	}

}
