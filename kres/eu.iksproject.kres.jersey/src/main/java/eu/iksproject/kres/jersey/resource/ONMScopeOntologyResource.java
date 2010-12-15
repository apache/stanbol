package eu.iksproject.kres.jersey.resource;

import static javax.ws.rs.core.Response.Status.*;

import java.net.URI;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;

import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceModificationException;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.jersey.util.OntologyRenderUtils;
import eu.iksproject.kres.manager.io.RootOntologySource;

/**
 * This resource represents ontologies loaded within a scope.
 * 
 * @author alessandro
 * 
 */
@Path("/ontology/{scopeid}/{uri:.+}")
public class ONMScopeOntologyResource extends NavigationMixin {

	/*
	 * Placeholder for the KReSONManager to be fetched from the servlet context.
	 */
	protected KReSONManager onm;

	protected ServletContext servletContext;

	public ONMScopeOntologyResource(@Context ServletContext servletContext) {
		this.servletContext = servletContext;
		onm = (KReSONManager) this.servletContext
				.getAttribute(KReSONManager.class.getName());
	}

	/**
	 * Returns an RDF/XML representation of the ontology identified by logical
	 * IRI <code>ontologyid</code>, if it is loaded within the scope
	 * <code>[baseUri]/scopeid</code>.
	 * 
	 * @param scopeid
	 * @param ontologyid
	 * @param uriInfo
	 * @return, or a status 404 if either the scope is not registered or the
	 *          ontology is not loaded within that scope.
	 */
	@GET
	@Produces(value = { KReSFormat.RDF_XML, KReSFormat.OWL_XML,
			KReSFormat.TURTLE, KReSFormat.FUNCTIONAL_OWL,
			KReSFormat.MANCHESTER_OWL, KReSFormat.RDF_JSON })
	public Response getScopeOntology(@PathParam("scopeid") String scopeid,
			@PathParam("uri") String ontologyid, @Context UriInfo uriInfo) {

		String absur = uriInfo.getAbsolutePath().toString();
		URI uri = URI.create(absur.substring(0,
				absur.lastIndexOf(ontologyid) - 1));

		IRI sciri = IRI.create(uri);
		IRI ontiri = IRI.create(ontologyid);
		
		// TODO: hack (ma anche no)
		if (!ontiri.isAbsolute())
			ontiri = IRI.create(absur);
			
		ScopeRegistry reg = onm.getScopeRegistry();
		OntologyScope scope = reg.getScope(sciri);
		if (scope == null)
			return Response.status(NOT_FOUND).build();

		/* BEGIN debug code, uncomment only for local testing */
		// OWLOntology test = null, top = null;
		// test = scope.getCustomSpace().getOntology(ontiri);
		// System.out.println("Ontology " + ontiri);
		// for (OWLImportsDeclaration imp : test.getImportsDeclarations())
		// System.out.println("\timports " + imp.getIRI());
		// top = scope.getCoreSpace().getTopOntology();
		// System.out.println("Core root for scope " + scopeid);
		// for (OWLImportsDeclaration imp : top.getImportsDeclarations())
		// System.out.println("\timports " + imp.getIRI());
		/* END debug code */

		OWLOntology ont = null;
		// By default, always try retrieving the ontology from the custom space
		// first.
		OntologySpace space = scope.getCustomSpace();
		if (space == null)
			space = scope.getCoreSpace();
		if (space != null)
			ont = space.getOntology(ontiri);

		if (ont == null) {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			final Set<OWLOntology> ontologies = scope.getSessionSpace(ontiri)
					.getOntologies();

			OWLOntologySetProvider provider = new OWLOntologySetProvider() {

				@Override
				public Set<OWLOntology> getOntologies() {
					// System.out.println("ID SPACE : " + ontologies);
					return ontologies;
				}
			};
			OWLOntologyMerger merger = new OWLOntologyMerger(provider);

			/*
			 * Set<OntologySpace> spaces = scope.getSessionSpaces();
			 * for(OntologySpace space : spaces){
			 * System.out.println("ID SPACE : "+space.getID()); }
			 */

			try {
				ont = merger.createMergedOntology(man, ontiri);
			} catch (OWLOntologyCreationException e) {
				throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
			}

		}
		if (ont == null) {
			return Response.status(NOT_FOUND).build();
		}
		String res = null;
		try {
			res = OntologyRenderUtils.renderOntology(ont,
					new RDFXMLOntologyFormat(), sciri.toString(), onm);
		} catch (OWLOntologyStorageException e) {
			throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
		}
		return Response.ok(res).build();

	}

	/**
	 * Unloads an ontology from an ontology scope.
	 * 
	 * @param scopeId
	 * @param ontologyid
	 * @param uriInfo
	 * @param headers
	 */
	@DELETE
	public void unloadOntology(@PathParam("scopeid") String scopeId,
			@PathParam("uri") String ontologyid, @Context UriInfo uriInfo,
			@Context HttpHeaders headers) {

		if (ontologyid != null && !ontologyid.equals("")) {
			String scopeURI = uriInfo.getAbsolutePath().toString().replace(
					ontologyid, "");
			System.out.println("Received DELETE request for ontology "
					+ ontologyid + " in scope " + scopeURI);
			IRI scopeIri = IRI.create(uriInfo.getBaseUri() + "ontology/"
					+ scopeId);
			System.out.println("SCOPE IRI : " + scopeIri);
			IRI ontIri = IRI.create(ontologyid);
			ScopeRegistry reg = onm.getScopeRegistry();
			OntologyScope scope = reg.getScope(scopeIri);
			OntologySpace cs = scope.getCustomSpace();
			if (cs.hasOntology(ontIri)) {
				try {
					reg.setScopeActive(scopeIri, false);
					cs.removeOntology(new RootOntologySource(cs
							.getOntology(ontIri)));
					reg.setScopeActive(scopeIri, true);
				} catch (OntologySpaceModificationException e) {
					reg.setScopeActive(scopeIri, true);
					throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
				}
			}
		}
	}

}
