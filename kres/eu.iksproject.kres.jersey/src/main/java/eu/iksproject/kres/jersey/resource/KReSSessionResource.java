package eu.iksproject.kres.jersey.resource;

import static javax.ws.rs.core.Response.Status.*;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.io.RootOntologyIRISource;
import eu.iksproject.kres.api.manager.io.RootOntologySource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceModificationException;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.api.manager.session.KReSSession;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.jersey.format.KReSFormat;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.renderers.SessionRenderer;

@Path("/session")
public class KReSSessionResource extends NavigationMixin {

    /*
     * Placeholder for the KReSONManager to be fetched from the servlet context.
     */
    protected KReSONManager onm;

    protected ServletContext servletContext;

    public KReSSessionResource(@Context ServletContext servletContext) {
        this.servletContext = servletContext;
        onm = (KReSONManager) this.servletContext.getAttribute(KReSONManager.class.getName());
        if (onm == null) {
            System.err
                    .println("[KReS] :: No KReS Ontology Network Manager provided by Servlet Context. Instantiating now...");
            onm = new ONManager();
        }
    }

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addOntology(@QueryParam("scope") String scope,
                                @QueryParam("import") InputStream importOntology,
                                @QueryParam("session") String session,
                                @Context UriInfo uriInfo,
                                @Context HttpHeaders headers,
                                @Context ServletContext servletContext) {

        IRI scopeIRI = IRI.create(scope);
        IRI sessionIRI = IRI.create(session);

        OWLOntology ontology;
        try {
            ontology = onm.getOwlCacheManager().loadOntologyFromOntologyDocument(importOntology);

            ScopeRegistry scopeRegistry = onm.getScopeRegistry();

            OntologyScope ontologyScope = scopeRegistry.getScope(scopeIRI);
            SessionOntologySpace sos = ontologyScope.getSessionSpace(sessionIRI);
            try {
                sos.addOntology(new RootOntologySource(ontology));
                return Response.ok().build();
            } catch (UnmodifiableOntologySpaceException e) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            }
        } catch (OWLOntologyCreationException e1) {
            return Response.status(NOT_FOUND).build();
        }

    }

    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addOntology(@QueryParam("scope") String scope,
                                @QueryParam("session") String session,
                                @QueryParam("location") String location,
                                @Context UriInfo uriInfo,
                                @Context HttpHeaders headers,
                                @Context ServletContext servletContext) {

        IRI scopeIRI = IRI.create(scope);
        IRI sessionIRI = IRI.create(session);
        IRI ontologyIRI = IRI.create(location);
        ScopeRegistry scopeRegistry = onm.getScopeRegistry();

        OntologyScope ontologyScope = scopeRegistry.getScope(scopeIRI);
        SessionOntologySpace sos = ontologyScope.getSessionSpace(sessionIRI);
        try {
            sos.addOntology(new RootOntologyIRISource(ontologyIRI));
            return Response.ok().build();
        } catch (UnmodifiableOntologySpaceException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        } catch (OWLOntologyCreationException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }

    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(value = {KReSFormat.RDF_XML, KReSFormat.OWL_XML, KReSFormat.TURTLE, KReSFormat.FUNCTIONAL_OWL,
                       KReSFormat.MANCHESTER_OWL, KReSFormat.RDF_JSON})
    public Response createSession(@FormParam("scope") String scope,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {

        KReSSession ses = null;
        KReSSessionManager mgr = onm.getSessionManager();

        /*
         * Create the KReS session to associate to the scope.
         */
        ses = mgr.createSession();

        /*
         * First get the scope registry.
         */
        ScopeRegistry scopeRegistry = onm.getScopeRegistry();

        /*
         * Then retrieve the ontology scope.
         */
        IRI scopeIRI = IRI.create(scope);
        OntologyScope ontologyScope = scopeRegistry.getScope(scopeIRI);

        /*
         * Finally associate the KReS session to the scope.
         */
        OntologySpaceFactory ontologySpaceFactory = onm.getOntologySpaceFactory();
        SessionOntologySpace sessionOntologySpace = ontologySpaceFactory.createSessionOntologySpace(scopeIRI);
        ontologyScope.addSessionSpace(sessionOntologySpace, ses.getID());

        return Response.ok(SessionRenderer.getSessionMetadataRDF(ses)).build();

    }

    /**
     * FIXME what are these path params anyway?
     * 
     * @param scope
     * @param session
     * @param deleteOntology
     * @param uriInfo
     * @param headers
     * @return
     */
    @DELETE
    public Response deleteSession(@PathParam("scope") String scope,
                                  @PathParam("session") String session,
                                  @PathParam("delete") String deleteOntology,
                                  @Context UriInfo uriInfo,
                                  @Context HttpHeaders headers) {

        IRI scopeID = IRI.create(scope);
        IRI sessionID = IRI.create(session);

        if (deleteOntology != null) {
            IRI ontologyIRI = IRI.create(deleteOntology);

            ScopeRegistry scopeRegistry = onm.getScopeRegistry();

            OntologyScope ontologyScope = scopeRegistry.getScope(scopeID);
            SessionOntologySpace sos = ontologyScope.getSessionSpace(sessionID);

            try {
                /*
                 * TODO : previous implementation reloaded the whole ontology before deleting it, thus
                 * treating this as a physical IRI. See if it still works this way
                 */
                OWLOntology o = sos.getOntology(ontologyIRI);
                if (o != null) sos.removeOntology(new RootOntologySource(o));
                return Response.ok().build();
            } catch (OntologySpaceModificationException e) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            }
        } else {
            onm.getSessionManager().destroySession(sessionID);
            return Response.ok().build();
        }

    }

}
