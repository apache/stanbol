package eu.iksproject.kres.jersey.resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.semanticweb.owlapi.model.OWLOntology;

import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.renderers.ScopeSetRenderer;

/**
 * The main Web resource of the KReS ontology manager. All the scopes, sessions
 * and ontologies are accessible as subresources of ONMRootResource.<br>
 * <br>
 * This resource allows a GET method for obtaining an RDF representation of the
 * set of registered scopes and a DELETE method for clearing the scope set and
 * ontology store accordingly.
 * 
 * @author alessandro
 * 
 */
@Path("/ontology")
public class ONMRootResource extends NavigationMixin {

	/*
	 * Placeholder for the KReSONManager to be fetched from the servlet context.
	 */
	protected KReSONManager onm;

	protected ServletContext servletContext;

	public ONMRootResource(@Context ServletContext servletContext) {
		this.servletContext = servletContext;
		onm = (KReSONManager) this.servletContext
				.getAttribute(KReSONManager.class.getName());
		
		if (onm == null) {
			System.err
					.println("[KReS] :: No KReS Ontology Network Manager provided by Servlet Context. Instantiating now...");
			onm = new ONManager();
		}
	}

	/**
	 * Default GET method for obtaining the set of (both active and, optionally,
	 * inactive) ontology scopes currently registered with this instance of
	 * KReS.
	 * 
	 * @param inactive
	 *            if true, both active and inactive scopes will be included.
	 *            Default is false.
	 * @param headers
	 *            the HTTP headers, supplied by the REST call.
	 * @param servletContext
	 *            the servlet context, supplied by the REST call.
	 * @return a string representation of the requested scope set, in a format
	 *         acceptable by the client.
	 */
	@GET
	@Produces( { KReSFormat.FUNCTIONAL_OWL, 
				 KReSFormat.MANCHERSTER_OWL, 
				 KReSFormat.OWL_XML,
				 KReSFormat.RDF_XML, 
				 KReSFormat.TURTLE,
				 KReSFormat.RDF_JSON})
	public Response getScopes(
			@DefaultValue("false") @QueryParam("with-inactive") boolean inactive,
			@Context HttpHeaders headers, @Context ServletContext servletContext) {

		ScopeRegistry reg = onm.getScopeRegistry();
		
		Set<OntologyScope> scopes = inactive ? reg.getRegisteredScopes() : reg
				.getActiveScopes();
		
		OWLOntology ontology = ScopeSetRenderer.getScopes(scopes);

		return Response.ok(ontology).build();
	}
	
	/**
	 * RESTful DELETE method that clears the entire scope registry and managed
	 * ontology store.
	 */
	@DELETE
	public void clearOntologies() {
		// First clear the registry...
		ScopeRegistry reg = onm.getScopeRegistry();
		for (OntologyScope scope : reg.getRegisteredScopes())
			reg.deregisterScope(scope);
		// ...then clear the store.
		// TODO : the other way around?
		onm.getOntologyStore().clear();
	}

}
