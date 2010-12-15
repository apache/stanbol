package eu.iksproject.kres.jersey.resource;

import static javax.ws.rs.core.Response.Status.*;

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

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.io.OntologyRegistryIRISource;
import eu.iksproject.kres.manager.io.RootOntologyIRISource;

@Path("/ontology/{scopeid}")
public class ONMScopeResource extends NavigationMixin {

	/*
	 * Placeholder for the KReSONManager to be fetched from the servlet context.
	 */
	protected KReSONManager onm;

	protected ServletContext servletContext;

	public ONMScopeResource(@Context ServletContext servletContext) {
		this.servletContext = servletContext;
		onm = (KReSONManager) this.servletContext
				.getAttribute(KReSONManager.class.getName());

		if (onm == null) {
			System.err
					.println("[KReS] :: No KReS Ontology Network Manager provided by Servlet Context. Instantiating now...");
			onm = new ONManager();
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
			coreSrc = new OntologyRegistryIRISource(IRI.create(coreRegistry));
		} catch (Exception e1) {
			// Bad or not supplied core registry, try the ontology.
			try {
				coreSrc = new RootOntologyIRISource(IRI.create(coreOntology));
			} catch (Exception e2) {
				// If this fails too, throw a bad request.
				System.out.println("1.1");
				e2.printStackTrace();
				throw new WebApplicationException(e2, BAD_REQUEST);
			}
		}

		// Don't bother if no custom was supplied at all...
		if (customOntology != null || customRegistry != null) {
			// ...but if it was, be prepared to throw exceptions.
			try {
				custSrc = new OntologyRegistryIRISource(IRI
						.create(customRegistry));
			} catch (Exception e1) {
				// Bad or not supplied custom registry, try the ontology.
				try {
					custSrc = new RootOntologyIRISource(IRI
							.create(customOntology));
				} catch (Exception e2) {
					// If this fails too, throw a bad request.
					System.out.println("1.2");
					e2.printStackTrace();
					throw new WebApplicationException(e2, BAD_REQUEST);
				}
			}
		}
		// If we weren't able to build core source, throw bad request.
		if (coreSrc == null) {
			System.out.println("1.3");
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
