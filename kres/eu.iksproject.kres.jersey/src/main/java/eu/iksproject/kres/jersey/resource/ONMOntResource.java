package eu.iksproject.kres.jersey.resource;

import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyIndex;
import eu.iksproject.kres.api.storage.OntologyStoreProvider;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.storage.provider.OntologyStorageProviderImpl;

@Path("/ontology/get")
public class ONMOntResource extends NavigationMixin {

	private Logger log = LoggerFactory.getLogger(getClass());

	/*
	 * Placeholder for the KReSONManager to be fetched from the servlet context.
	 */
	protected KReSONManager onm;
	protected OntologyStoreProvider storeProvider;

	protected ServletContext servletContext;

	protected Serializer serializer;

	public ONMOntResource(@Context ServletContext servletContext) {
		this.servletContext = servletContext;
		this.onm = (KReSONManager) servletContext
				.getAttribute(KReSONManager.class.getName());
this.storeProvider = (OntologyStoreProvider) servletContext
		.getAttribute(OntologyStoreProvider.class.getName());
// Contingency code for missing components follows.
/*
 * FIXME! The following code is required only for the tests. This should
 * be removed and the test should work without this code.
 */
if (storeProvider == null) {
	log
			.warn("No OntologyStoreProvider in servlet context. Instantiating manually...");
	storeProvider = new OntologyStorageProviderImpl();
}
if (onm == null) {
	log
			.warn("No KReSONManager in servlet context. Instantiating manually...");
	onm = new ONManager(storeProvider.getActiveOntologyStorage(),
			new Hashtable<String, Object>());
}
		serializer = (Serializer) this.servletContext
				.getAttribute(Serializer.class.getName());
	}

	@GET
	@Produces("application/rdf+xml")
	public Response getOntology(@QueryParam("iri") String ontologyIri) {

		IRI iri = null;
		try {
			iri = IRI.create(ontologyIri);
		} catch (Exception ex) {
			throw new WebApplicationException(404);
		}
		OntologyIndex index = onm.getOntologyIndex();
		if (!index.isOntologyLoaded(iri))
			// No such ontology registered, so return 404.
			return Response.status(404).build();

		OWLOntology ont = index.getOntology(iri);
		OWLOntologyManager tmpmgr = OWLManager.createOWLOntologyManager();
		StringDocumentTarget tgt = new StringDocumentTarget();
		try {
			tmpmgr.saveOntology(ont, new RDFXMLOntologyFormat(), tgt);
		} catch (OWLOntologyStorageException e) {
			throw new WebApplicationException(500);
		}
		return Response.ok(tgt.toString()).build();
	}

	@GET
	@Produces("text/turtle")
	public Response getOntologyT(@QueryParam("iri") String ontologyIri) {

		IRI iri = null;
		try {
			iri = IRI.create(ontologyIri);
		} catch (Exception ex) {
			throw new WebApplicationException(404);
		}
		OntologyIndex index = onm.getOntologyIndex();
		if (!index.isOntologyLoaded(iri))
			// No such ontology registered, so return 404.
			return Response.status(404).build();

		OWLOntology ont = index.getOntology(iri);
		OWLOntologyManager tmpmgr = OWLManager.createOWLOntologyManager();
		StringDocumentTarget tgt = new StringDocumentTarget();
		try {
			tmpmgr.saveOntology(ont, new TurtleOntologyFormat(), tgt);
		} catch (OWLOntologyStorageException e) {
			throw new WebApplicationException(500);
		}
		return Response.ok(tgt.toString()).build();
	}

}
