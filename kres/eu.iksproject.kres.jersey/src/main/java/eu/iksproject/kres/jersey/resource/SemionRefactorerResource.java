package eu.iksproject.kres.jersey.resource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.DescribeQuery;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.sun.jersey.api.view.ImplicitProduces;


import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.reasoners.InconcistencyException;
import eu.iksproject.kres.api.rules.NoSuchRecipeException;
import eu.iksproject.kres.api.rules.Recipe;
import eu.iksproject.kres.api.semion.SemionRefactorer;
import eu.iksproject.kres.api.semion.SemionManager;
import eu.iksproject.kres.api.semion.SemionRefactoringException;
import eu.iksproject.kres.semion.refactorer.SemionRefactorerImpl;

/**
 * 
 * @author andrea.nuzzolese
 *
 */

@Path("/refactorer")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class SemionRefactorerResource extends NavigationMixin {

	
	protected KReSONManager onManager;
	protected SemionManager semionManager;
	protected TcManager tcManager;
	
	public SemionRefactorerResource(@Context ServletContext servletContext) {
		semionManager = (SemionManager) (servletContext.getAttribute(SemionManager.class.getName()));
		
		onManager = (KReSONManager) (servletContext.getAttribute(KReSONManager.class.getName()));
		
		tcManager = (TcManager) (servletContext.getAttribute(TcManager.class.getName()));
        if (semionManager == null) {
            throw new IllegalStateException(
                    "SemionManager missing in ServletContext");
        }
        
        
    }
	
	
	@GET
	@Path("/lazy")
	public Response performRefactoringLazyCreateGraph(
			@QueryParam("recipe") String recipe,
			@QueryParam("input-graph") String inputGraph,			
			@QueryParam("output-graph") String outputGraph
			){
		
		
		System.out.println("recipe: "+recipe);
		System.out.println("input-graph: "+inputGraph);
		System.out.println("output-graph: "+outputGraph);
		IRI recipeIRI = IRI.create(recipe);
	    IRI inputGraphIRI = IRI.create(inputGraph);
	    IRI outputGraphIRI = IRI.create(outputGraph);
        
	    SemionRefactorer semionRefactorer = semionManager.getRegisteredRefactorer();
	    
	    try {
	    	semionRefactorer.ontologyRefactoring(outputGraphIRI, inputGraphIRI, recipeIRI);
			return Response.ok().build();
		} catch (SemionRefactoringException e) {
			return Response.status(500).build();
		} catch (NoSuchRecipeException e) {
			return Response.status(204).build();
		}
	    
		
	}
	
	
	@POST
	@Path("/lazy")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(value = {KReSFormat.TURTLE, 
				KReSFormat.FUNCTIONAL_OWL, 
				KReSFormat.MANCHESTER_OWL, 
				KReSFormat.RDF_XML,
				KReSFormat.OWL_XML,
				KReSFormat.RDF_JSON})
	public Response performRefactoring(
			@FormParam("recipe") String recipe,
			@FormParam("input") InputStream input){
		
		SemionRefactorer semionRefactorer = semionManager.getRegisteredRefactorer();
		
	    IRI recipeIRI = IRI.create(recipe);
	    
	    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	    OWLOntology inputOntology;
		try {
			inputOntology = manager.loadOntologyFromOntologyDocument(input);
			
			OWLOntology outputOntology;
			try {
				outputOntology = semionRefactorer.ontologyRefactoring(inputOntology, recipeIRI);
			} catch (SemionRefactoringException e) {
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
	public Response performConsistentRefactoringCreateGraph(
			@QueryParam("recipe") String recipe,
			@QueryParam("input-graph") String inputGraph,
			@QueryParam("output-graph") String outputGraph){
		
		IRI recipeIRI = IRI.create(recipe);
	    IRI inputGraphIRI = IRI.create(inputGraph);
	    IRI outputGraphIRI = IRI.create(outputGraph);
        
	    SemionRefactorer semionRefactorer = semionManager.getRegisteredRefactorer();
	    
	    try {
			semionRefactorer.consistentOntologyRefactoring(outputGraphIRI, inputGraphIRI, recipeIRI);
			return Response.ok().build();
		} catch (SemionRefactoringException e) {
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
	@Produces({KReSFormat.TURTLE, 
				KReSFormat.FUNCTIONAL_OWL, 
				KReSFormat.MANCHESTER_OWL, 
				KReSFormat.RDF_XML,
				KReSFormat.OWL_XML,
				KReSFormat.RDF_JSON})
	public Response consistentRefactoringOfNewGraph(
			@FormParam("recipe") String recipe,
			@FormParam("input") InputStream input){
		
		IRI recipeIRI = IRI.create(recipe);
		
		SemionRefactorer semionRefactorer = semionManager.getRegisteredRefactorer();
		
	    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	    OWLOntology inputOntology;
		try {
			inputOntology = manager.loadOntologyFromOntologyDocument(input);
			
			OWLOntology outputOntology;
			try {
				outputOntology = semionRefactorer.consistentOntologyRefactoring(inputOntology, recipeIRI);
			} catch (SemionRefactoringException e) {
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
	
	public String getNamespace(){
		return onManager.getKReSNamespace();
	}
	
}
