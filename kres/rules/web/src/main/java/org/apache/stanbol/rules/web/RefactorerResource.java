package org.apache.stanbol.rules.web;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.reasoners.base.api.InconcistencyException;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.sun.jersey.api.view.ImplicitProduces;

import org.apache.stanbol.kres.jersey.format.KReSFormat;
import org.apache.stanbol.kres.jersey.resource.NavigationMixin;

/**
 * 
 * @author andrea.nuzzolese
 * 
 */

@Path("/refactorer")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class RefactorerResource extends NavigationMixin {

    protected ONManager onManager;
    // protected SemionManager semionManager;
    protected TcManager tcManager;
    protected Refactorer semionRefactorer;

    public RefactorerResource(@Context ServletContext servletContext) {
        semionRefactorer = (Refactorer) (servletContext.getAttribute(Refactorer.class.getName()));

        onManager = (ONManager) (servletContext.getAttribute(ONManager.class.getName()));

        tcManager = (TcManager) (servletContext.getAttribute(TcManager.class.getName()));
        if (semionRefactorer == null) {
            throw new IllegalStateException("SemionRefactorer missing in ServletContext");
        }

    }

    @GET
    @Path("/lazy")
    public Response performRefactoringLazyCreateGraph(@QueryParam("recipe") String recipe,
                                                      @QueryParam("input-graph") String inputGraph,
                                                      @QueryParam("output-graph") String outputGraph) {

        System.out.println("recipe: " + recipe);
        System.out.println("input-graph: " + inputGraph);
        System.out.println("output-graph: " + outputGraph);
        IRI recipeIRI = IRI.create(recipe);
        IRI inputGraphIRI = IRI.create(inputGraph);
        IRI outputGraphIRI = IRI.create(outputGraph);

        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();

        try {
            semionRefactorer.ontologyRefactoring(outputGraphIRI, inputGraphIRI, recipeIRI);
            return Response.ok().build();
        } catch (RefactoringException e) {
            return Response.status(500).build();
        } catch (NoSuchRecipeException e) {
            return Response.status(204).build();
        }

    }

    @POST
    @Path("/lazy")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(value = {KReSFormat.TURTLE, KReSFormat.FUNCTIONAL_OWL, KReSFormat.MANCHESTER_OWL,
                       KReSFormat.RDF_XML, KReSFormat.OWL_XML, KReSFormat.RDF_JSON})
    public Response performRefactoring(@FormParam("recipe") String recipe,
                                       @FormParam("input") InputStream input) {

        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();

        IRI recipeIRI = IRI.create(recipe);

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology inputOntology;
        try {
            inputOntology = manager.loadOntologyFromOntologyDocument(input);

            OWLOntology outputOntology;
            try {
                outputOntology = semionRefactorer.ontologyRefactoring(inputOntology, recipeIRI);
            } catch (RefactoringException e) {
                e.printStackTrace();
                return Response.status(500).build();
            } catch (NoSuchRecipeException e) {
                return Response.status(204).build();
            }

            return Response.ok(outputOntology).build();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return Response.status(404).build();
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
            semionRefactorer.consistentOntologyRefactoring(outputGraphIRI, inputGraphIRI, recipeIRI);
            return Response.ok().build();
        } catch (RefactoringException e) {
            return Response.status(500).build();
        } catch (NoSuchRecipeException e) {
            return Response.status(204).build();
        } catch (InconcistencyException e) {
            return Response.status(415).build();
        }

    }

    @POST
    @Path("/consistent")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces( {KReSFormat.TURTLE, KReSFormat.FUNCTIONAL_OWL, KReSFormat.MANCHESTER_OWL, KReSFormat.RDF_XML,
                KReSFormat.OWL_XML, KReSFormat.RDF_JSON})
    public Response consistentRefactoringOfNewGraph(@FormParam("recipe") String recipe,
                                                    @FormParam("input") InputStream input) {

        IRI recipeIRI = IRI.create(recipe);

        // Refactorer semionRefactorer = semionManager.getRegisteredRefactorer();

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology inputOntology;
        try {
            inputOntology = manager.loadOntologyFromOntologyDocument(input);

            OWLOntology outputOntology;
            try {
                outputOntology = semionRefactorer.consistentOntologyRefactoring(inputOntology, recipeIRI);
            } catch (RefactoringException e) {
                return Response.status(500).build();
            } catch (NoSuchRecipeException e) {
                return Response.status(204).build();
            } catch (InconcistencyException e) {
                return Response.status(415).build();
            }

            return Response.ok(outputOntology).build();
        } catch (OWLOntologyCreationException e) {
            return Response.status(404).build();
        }

    }

    public String getNamespace() {
        return onManager.getKReSNamespace();
    }

}
