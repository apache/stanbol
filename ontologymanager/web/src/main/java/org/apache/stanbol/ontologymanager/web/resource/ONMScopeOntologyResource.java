package org.apache.stanbol.ontologymanager.web.resource;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.net.URI;
import java.util.Hashtable;
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

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.web.util.OntologyRenderUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.stanbol.commons.web.base.format.KRFormat;

/**
 * This resource represents ontologies loaded within a scope.
 * 
 * @author alessandro
 * 
 */
@Path("/ontology/{scopeid}/{uri:.+}")
public class ONMScopeOntologyResource extends NavigationMixin {

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Placeholder for the ONManager to be fetched from the servlet context.
     */
    protected ONManager onm;
    protected ClerezzaOntologyStorage storage;

    protected ServletContext servletContext;

    public ONMScopeOntologyResource(@Context ServletContext servletContext) {
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
    }

    /**
     * Returns an RDF/XML representation of the ontology identified by logical IRI <code>ontologyid</code>, if
     * it is loaded within the scope <code>[baseUri]/scopeid</code>.
     * 
     * @param scopeid
     * @param ontologyid
     * @param uriInfo
     * @return, or a status 404 if either the scope is not registered or the ontology is not loaded within
     *          that scope.
     */
    @GET
    @Produces(value = {KRFormat.RDF_XML, KRFormat.OWL_XML, KRFormat.TURTLE, KRFormat.FUNCTIONAL_OWL,
                       KRFormat.MANCHESTER_OWL, KRFormat.RDF_JSON})
    public Response getScopeOntology(@PathParam("scopeid") String scopeid,
                                     @PathParam("uri") String ontologyid,
                                     @Context UriInfo uriInfo) {

        String absur = uriInfo.getAbsolutePath().toString();
        URI uri = URI.create(absur.substring(0, absur.lastIndexOf(ontologyid) - 1));

        IRI sciri = IRI.create(uri);
        IRI ontiri = IRI.create(ontologyid);

        // TODO: hack (ma anche no)
        if (!ontiri.isAbsolute()) ontiri = IRI.create(absur);

        ScopeRegistry reg = onm.getScopeRegistry();
        OntologyScope scope = reg.getScope(sciri);
        if (scope == null) return Response.status(NOT_FOUND).build();

        /* BEGIN debug code, uncomment only for local testing */
        OWLOntology test = null, top = null;
        test = scope.getCustomSpace().getOntology(ontiri);
        System.out.println("Ontology " + ontiri);
        for (OWLImportsDeclaration imp : test.getImportsDeclarations())
            System.out.println("\timports " + imp.getIRI());
        top = scope.getCoreSpace().getTopOntology();
        System.out.println("Core root for scope " + scopeid);
        for (OWLImportsDeclaration imp : top.getImportsDeclarations())
            System.out.println("\timports " + imp.getIRI());
        /* END debug code */

        OWLOntology ont = null;
        // By default, always try retrieving the ontology from the custom space
        // first.
        OntologySpace space = scope.getCustomSpace();
        if (space == null) space = scope.getCoreSpace();
        if (space != null) ont = space.getOntology(ontiri);

        if (ont == null) {
            OWLOntologyManager man = OWLManager.createOWLOntologyManager();
            final Set<OWLOntology> ontologies = scope.getSessionSpace(ontiri).getOntologies();

            OWLOntologySetProvider provider = new OWLOntologySetProvider() {

                @Override
                public Set<OWLOntology> getOntologies() {
                    // System.out.println("ID SPACE : " + ontologies);
                    return ontologies;
                }
            };
            OWLOntologyMerger merger = new OWLOntologyMerger(provider);

            /*
             * Set<OntologySpace> spaces = scope.getSessionSpaces(); for(OntologySpace space : spaces){
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
            res = OntologyRenderUtils.renderOntology(ont, new RDFXMLOntologyFormat(), sciri.toString(), onm);
        } catch (OWLOntologyStorageException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        return Response.ok(/* ont */res).build();

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
                               @PathParam("uri") String ontologyid,
                               @Context UriInfo uriInfo,
                               @Context HttpHeaders headers) {

        if (ontologyid != null && !ontologyid.equals("")) {
            String scopeURI = uriInfo.getAbsolutePath().toString().replace(ontologyid, "");
            System.out
                    .println("Received DELETE request for ontology " + ontologyid + " in scope " + scopeURI);
            IRI scopeIri = IRI.create(uriInfo.getBaseUri() + "ontology/" + scopeId);
            System.out.println("SCOPE IRI : " + scopeIri);
            IRI ontIri = IRI.create(ontologyid);
            ScopeRegistry reg = onm.getScopeRegistry();
            OntologyScope scope = reg.getScope(scopeIri);
            OntologySpace cs = scope.getCustomSpace();
            if (cs.hasOntology(ontIri)) {
                try {
                    reg.setScopeActive(scopeIri, false);
                    cs.removeOntology(new RootOntologySource(cs.getOntology(ontIri)));
                    reg.setScopeActive(scopeIri, true);
                } catch (OntologySpaceModificationException e) {
                    reg.setScopeActive(scopeIri, true);
                    throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

}
