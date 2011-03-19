package eu.iksproject.kres.jersey.resource;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.OntologyRegistryIRISource;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.OntologyStorage;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.jersey.format.KReSFormat;

@Path("/ontology/{scopeid}")
public class ONMScopeResource extends NavigationMixin {

	private Logger log = LoggerFactory.getLogger(getClass());

	/*
	 * Placeholder for the KReSONManager to be fetched from the servlet context.
	 */
	protected KReSONManager onm;
	protected OntologyStorage storage;

	protected ServletContext servletContext;

	public ONMScopeResource(@Context ServletContext servletContext) {
		this.servletContext = servletContext;
		this.onm = (KReSONManager) servletContext
				.getAttribute(KReSONManager.class.getName());
		this.storage = (OntologyStorage) servletContext
				.getAttribute(OntologyStorage.class.getName());
//      this.storage = (OntologyStorage) servletContext
//      .getAttribute(OntologyStorage.class.getName());
// Contingency code for missing components follows.
/*
 * FIXME! The following code is required only for the tests. This should
 * be removed and the test should work without this code.
 */
if (onm == null) {
    log
            .warn("No KReSONManager in servlet context. Instantiating manually...");
    onm = new ONManager(new TcManager(), null,
            new Hashtable<String, Object>());
}
this.storage = onm.getOntologyStore();
if (storage == null) {
    log.warn("No OntologyStorage in servlet context. Instantiating manually...");
    storage = new OntologyStorage(new TcManager(),null);
}
	}

	@DELETE
	public void deregisterScope(@PathParam("scopeid") String scopeid,
			@Context UriInfo uriInfo, @Context HttpHeaders headers,
			@Context ServletContext servletContext) {

		ScopeRegistry reg = onm.getScopeRegistry();

		OntologyScope scope = reg.getScope(IRI
				.create(uriInfo.getAbsolutePath()));
		if (scope == null)
			return;
		reg.deregisterScope(scope);
	}

	@GET
	@Produces(value = { KReSFormat.RDF_XML, KReSFormat.OWL_XML,
			KReSFormat.TURTLE, KReSFormat.FUNCTIONAL_OWL,
			KReSFormat.MANCHESTER_OWL, KReSFormat.RDF_JSON })
	public Response getTopOntology(@Context UriInfo uriInfo,
			@Context HttpHeaders headers, @Context ServletContext servletContext) {

		ScopeRegistry reg = onm.getScopeRegistry();

		OntologyScope scope = reg.getScope(IRI
				.create(uriInfo.getAbsolutePath()));
		if (scope == null)
			return Response.status(404).build();

		OntologySpace cs = scope.getCustomSpace();
		OWLOntology ont = null;
		if (cs != null)
			ont = scope.getCustomSpace().getTopOntology();
		if (ont == null)
			ont = scope.getCoreSpace().getTopOntology();

		return Response.ok(ont).build();

	}

	@POST
	// @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces("text/plain")
	public Response loadCustomOntology(@PathParam("scopeid") String scopeid,
			@FormParam("location") String physIri,
			@FormParam("registry") boolean asRegistry,
			@Context UriInfo uriInfo, @Context HttpHeaders headers,
			@Context ServletContext servletContext) {

		ScopeRegistry reg = onm.getScopeRegistry();

		String res = "";
		IRI scopeiri = null;
		IRI ontoiri = null;
		try {
			scopeiri = IRI.create(uriInfo.getAbsolutePath());
			ontoiri = IRI.create(physIri);
		} catch (Exception ex) {
			// Malformed IRI, throw bad request.
			throw new WebApplicationException(ex, BAD_REQUEST);
		}
		if (reg.containsScope(scopeiri)) {
			res = "Ok, scope is there";
			OntologyScope scope = reg.getScope(scopeiri);
			try {
				OntologyInputSource src = new RootOntologyIRISource(ontoiri);
				OntologySpace space = scope.getCustomSpace();
				if (space == null) {
					space = onm.getOntologySpaceFactory()
							.createCustomOntologySpace(scopeiri, src);

					scope.setCustomSpace(space);
					// space.setUp();
				} else
					space.addOntology(src);
			} catch (OWLOntologyCreationException e) {
				throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
			} catch (UnmodifiableOntologySpaceException e) {
				throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
			}
		} else
			throw new WebApplicationException(404);
		return Response.ok(res).build();
	}

	/**
	 * At least one between corereg and coreont must be present. Registry iris
	 * supersede ontology iris.
	 * 
	 * @param scopeid
	 * @param coreRegistry
	 *            a. If it is a well-formed IRI it supersedes
	 *            <code>coreOntology</code>.
	 * @param coreOntology
	 * @param customRegistry
	 *            a. If it is a well-formed IRI it supersedes
	 *            <code>customOntology</code>.
	 * @param customOntology
	 * @param activate
	 *            if true, the new scope will be activated upon creation.
	 * @param uriInfo
	 * @param headers
	 * @return
	 */
	@PUT
	@Consumes(MediaType.WILDCARD)
	public Response registerScope(@PathParam("scopeid") String scopeid,
			@QueryParam("corereg") String coreRegistry,
			@QueryParam("coreont") String coreOntology,
			@QueryParam("customreg") String customRegistry,
			@QueryParam("customont") String customOntology,
			@DefaultValue("false") @QueryParam("activate") String activate,
			@Context UriInfo uriInfo, @Context HttpHeaders headers,
			@Context ServletContext servletContext) {

		ScopeRegistry reg = onm.getScopeRegistry();
		OntologyScopeFactory f = onm.getOntologyScopeFactory();

		System.out.println("GOT PUT");

		OntologyScope scope;
		OntologyInputSource coreSrc = null, custSrc = null;

		// First thing, check the core source.
		try {
			coreSrc = new OntologyRegistryIRISource(IRI.create(coreRegistry),
					onm.getOwlCacheManager(), onm.getRegistryLoader());
		} catch (Exception e1) {
			// Bad or not supplied core registry, try the ontology.
			try {
				coreSrc = new RootOntologyIRISource(IRI.create(coreOntology));
			} catch (Exception e2) {
				// If this fails too, throw a bad request.
				// System.out.println("1.1");
				e2.printStackTrace();
				throw new WebApplicationException(e2, BAD_REQUEST);
			}
		}

		// Don't bother if no custom was supplied at all...
		if (customOntology != null || customRegistry != null) {
			// ...but if it was, be prepared to throw exceptions.
			try {
				custSrc = new OntologyRegistryIRISource(IRI
						.create(customRegistry), onm.getOwlCacheManager(), onm
						.getRegistryLoader());
			} catch (Exception e1) {
				// Bad or not supplied custom registry, try the ontology.
				try {
					custSrc = new RootOntologyIRISource(IRI
							.create(customOntology));
				} catch (Exception e2) {
					// If this fails too, throw a bad request.
					// System.out.println("1.2");
					e2.printStackTrace();
					throw new WebApplicationException(e2, BAD_REQUEST);
				}
			}
		}
		// If we weren't able to build core source, throw bad request.
		if (coreSrc == null) {
			// System.out.println("1.3");
			throw new WebApplicationException(BAD_REQUEST);
		}

		// Now the creation.
		try {
			IRI scopeId = IRI.create(uriInfo.getAbsolutePath());
			// Invoke the appropriate factory method depending on the
			// availability of a custom source.
			scope = (custSrc != null) ? f.createOntologyScope(scopeId, coreSrc,
					custSrc) : f.createOntologyScope(scopeId, coreSrc);
			// Setup and register the scope. If no custom space was set, it will
			// still be open for modification.
			scope.setUp();
			reg.registerScope(scope);
			boolean activateBool = true;
			if (activate != null && !activate.equals("")) {
				activateBool = Boolean.valueOf(activate);
			}
			reg.setScopeActive(scopeId, activateBool);
		} catch (DuplicateIDException e) {
			throw new WebApplicationException(e, CONFLICT);
		}

		return Response.ok().build();
	}

}
