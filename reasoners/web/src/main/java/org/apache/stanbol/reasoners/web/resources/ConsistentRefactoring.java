package org.apache.stanbol.reasoners.web.resources;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.apache.stanbol.commons.web.base.format.KRFormat.*;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.reasoners.base.api.ConsistentRefactorer;
import org.apache.stanbol.reasoners.base.api.InconcistencyException;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.multipart.FormDataParam;

/**
 * Special refactoring services that employ a DL reasoner for ensuring/checking consistency.
 * 
 * @author alessandro
 * 
 */
@Path("/reasoners/refactor")
public class ConsistentRefactoring extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    protected ConsistentRefactorer refactorer;

    public ConsistentRefactoring(@Context ServletContext servletContext) {
        refactorer = (ConsistentRefactorer) (servletContext
                .getAttribute(ConsistentRefactorer.class.getName()));
        if (refactorer == null) {
            throw new IllegalStateException("SemionRefactorer missing in ServletContext");
        }

    }

    @GET
    @Path("/consistent")
    public Response performConsistentRefactoringCreateGraph(@QueryParam("recipe") String recipe,
                                                            @QueryParam("input-graph") String inputGraph,
                                                            @QueryParam("output-graph") String outputGraph) {

        IRI recipeIRI = IRI.create(recipe);
        IRI inputGraphIRI = IRI.create(inputGraph);
        IRI outputGraphIRI = IRI.create(outputGraph);

        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();

        try {
            refactorer.consistentOntologyRefactoring(outputGraphIRI, inputGraphIRI, recipeIRI);
            return Response.ok().build();
        } catch (RefactoringException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        } catch (NoSuchRecipeException e) {
            return Response.status(NO_CONTENT).build();
        } catch (InconcistencyException e) {
            log.error("Cannot classify ionconsistent graph " + inputGraph, e);
            return Response.status(PRECONDITION_FAILED).build();
        }

    }

    @POST
    @Path("/consistent")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces({TURTLE, FUNCTIONAL_OWL, MANCHESTER_OWL, RDF_XML,
               OWL_XML, RDF_JSON})
    public Response consistentRefactoringOfNewGraph(@FormDataParam("recipe") String recipe,
                                                    @FormDataParam("input") InputStream input) {

        IRI recipeIRI = IRI.create(recipe);

        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology inputOntology;
        try {
            inputOntology = manager.loadOntologyFromOntologyDocument(input);

            OWLOntology outputOntology;
            try {
                outputOntology = refactorer.consistentOntologyRefactoring(inputOntology, recipeIRI);
            } catch (RefactoringException e) {
                return Response.status(INTERNAL_SERVER_ERROR).build();
            } catch (NoSuchRecipeException e) {
                return Response.status(NO_CONTENT).build();
            } catch (InconcistencyException e) {
                log.error("Cannot classify ionconsistent graph " + inputOntology, e);
                return Response.status(PRECONDITION_FAILED).build();
            }

            return Response.ok(outputOntology).build();
        } catch (OWLOntologyCreationException e) {
            return Response.status(NOT_FOUND).build();
        }

    }

    @GET
    @Produces(TEXT_HTML)
    public Response getView() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

}
