package org.apache.stanbol.ontologymanager.web.resource;

import java.util.Hashtable;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyIndex;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
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

@Path("/ontology/get")
public class ONMOntResource extends NavigationMixin {

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;
    protected ClerezzaOntologyStorage storage;

    protected ServletContext servletContext;

    protected Serializer serializer;

    public ONMOntResource(@Context ServletContext servletContext) {
        this.servletContext = servletContext;
        this.onm = (ONManager) servletContext.getAttribute(ONManager.class.getName());
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
    onm = new ONManagerImpl(new TcManager(), null,
            new Hashtable<String, Object>());
}
this.storage = onm.getOntologyStore();
if (storage == null) {
    log.warn("No OntologyStorage in servlet context. Instantiating manually...");
    storage = new ClerezzaOntologyStorage(new TcManager(),null);
}
        serializer = (Serializer) this.servletContext.getAttribute(Serializer.class.getName());
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
