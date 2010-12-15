/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.jersey.manager;

import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.jersey.resource.NavigationMixin;
import eu.iksproject.kres.rules.manager.KReSAddRecipe;
import eu.iksproject.kres.rules.manager.KReSGetRecipe;
import eu.iksproject.kres.rules.manager.KReSRemoveRecipe;
import eu.iksproject.kres.rules.manager.KReSRuleStore;
import eu.iksproject.kres.api.format.KReSFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.sun.jersey.api.view.ImplicitProduces;

/**
 *
 * @author elvio
 */
@Path("/recipe")///{uri:.+}")
//@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class Recipe extends NavigationMixin{

    private RuleStore kresRuleStore;

   /**
     * To get the KReSRuleStore where are stored the rules and the recipes
     *
     * @param servletContext {To get the context where the REST service is running.}
     */
    public Recipe(@Context ServletContext servletContext){
       this.kresRuleStore = (RuleStore) servletContext.getAttribute(RuleStore.class.getName());
       if (kresRuleStore == null) {
           System.err.println("WARNING: KReSRuleStore with stored rules and recipes is missing in ServletContext. A new instance has been created.");
           this.kresRuleStore = new KReSRuleStore("");
           System.err.println("PATH TO OWL FILE LOADED: "+kresRuleStore.getFilePath());
            /*throw new IllegalStateException(
                    "KReSRuleStore with stored rules and recipes is missing in ServletContext");*/
        }
    }

   /**
     * Get a recipe with its rules from the rule base (that is the ontology that contains the rules and the recipe).
     *
     * @param uri {A string contains the IRI full name of the recipe.}
     * @return Return: <br/>
     *       200 The recipe is retrieved (import declarations point to KReS Services) <br/>
     *       404 The recipe does not exists in the manager <br/>
     *       500 Some error occurred 
     *
     */
    @GET
    @Path("/{uri:.+}")
    @Produces(value={KReSFormat.RDF_XML,
    				 KReSFormat.TURTLE,
    				 KReSFormat.OWL_XML, 
    				 KReSFormat.FUNCTIONAL_OWL, 
    				 KReSFormat.MANCHESTER_OWL,
    				 KReSFormat.RDF_JSON})
    public Response getRecipe(@PathParam("uri") String uri){
      try{

       KReSGetRecipe rule = new KReSGetRecipe(kresRuleStore);

       //String ID = kresRuleStore.getOntology().getOntologyID().toString().replace(">","").replace("<","")+"#";
       
       if(uri.equals("all")){

           Vector<IRI> recipe = rule.getGeneralRecipes();
           
        if(recipe==null){
            //The recipe does not exists in the manager
            return Response.status(Status.NOT_FOUND).build();
        }else{
            
            //The recipe is retrieved (import declarations point to KReS Services)
            OWLOntology onto = kresRuleStore.getOntology();
            OWLOntology newmodel = OWLManager.createOWLOntologyManager().createOntology(onto.getOntologyID());
            OWLDataFactory factory = onto.getOWLOntologyManager().getOWLDataFactory();

            Iterator<OWLOntology> importedonto = onto.getDirectImports().iterator();
            List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
            OWLDataFactory auxfactory = onto.getOWLOntologyManager().getOWLDataFactory();

            while(importedonto.hasNext()){
                OWLOntology auxonto = importedonto.next();
                additions.add(new AddImport(newmodel,auxfactory.getOWLImportsDeclaration(auxonto.getOWLOntologyManager().getOntologyDocumentIRI(auxonto))));
            }

            if(!additions.isEmpty())
                newmodel.getOWLOntologyManager().applyChanges(additions);

            for(int i = 0; i<recipe.size(); i++){
                OWLNamedIndividual ind = factory.getOWLNamedIndividual(recipe.get(i));
                Set<OWLIndividualAxiom> ax = onto.getAxioms(ind);
                newmodel.getOWLOntologyManager().addAxioms(newmodel,ax);

            }
            
            try {
            	OWLManager.createOWLOntologyManager().saveOntology(newmodel, newmodel.getOWLOntologyManager().getOntologyFormat(newmodel), System.out);
    		} catch (OWLOntologyStorageException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
            
            return Response.ok(newmodel).build();
        }

       }else{

        HashMap<IRI, String> recipe = rule.getRecipe(IRI.create(uri));
     
        if(recipe==null){
            //The recipe deos not exists in the manager
            return Response.status(Status.NOT_FOUND).build();
        }else{
            //The recipe is retrieved (import declarations point to KReS Services)
            OWLOntology onto = kresRuleStore.getOntology();

            OWLDataFactory factory = onto.getOWLOntologyManager().getOWLDataFactory();
            OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create("http://kres.iks-project.eu/ontology/meta/rmi.owl#hasRule"));
            OWLNamedIndividual ind = factory.getOWLNamedIndividual(IRI.create(uri));
            Set<OWLIndividual> value = ind.getObjectPropertyValues(prop, onto);
            Set<OWLIndividualAxiom> ax = onto.getAxioms(ind);
            
            Iterator<OWLIndividual> iter = value.iterator();

            OWLOntology newmodel = OWLManager.createOWLOntologyManager().createOntology(onto.getOntologyID());

            Iterator<OWLOntology> importedonto = onto.getDirectImports().iterator();
            List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
            OWLDataFactory auxfactory = onto.getOWLOntologyManager().getOWLDataFactory();

            while(importedonto.hasNext()){
                OWLOntology auxonto = importedonto.next();
                additions.add(new AddImport(newmodel,auxfactory.getOWLImportsDeclaration(auxonto.getOWLOntologyManager().getOntologyDocumentIRI(auxonto))));
            }

            if(!additions.isEmpty())
                newmodel.getOWLOntologyManager().applyChanges(additions);

            newmodel.getOWLOntologyManager().addAxioms(newmodel,ax);

            while(iter.hasNext()){
        
                ind = (OWLNamedIndividual) iter.next();
                ax = onto.getAxioms(ind);
              
                newmodel.getOWLOntologyManager().addAxioms(newmodel,ax);
            }

            try {
            	OWLManager.createOWLOntologyManager().saveOntology(newmodel, newmodel.getOWLOntologyManager().getOntologyFormat(newmodel), System.out);
    		} catch (OWLOntologyStorageException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

            return Response.ok(newmodel).build();
        }
       }
      }catch (Exception e){
          //Some error occurred
         throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
      }

    }

   /**
     * To add a recipe without rules.
     * @param recipe {A string contains the IRI of the recipe to be added}
     * @param description {A string contains a description of the rule}
     * @return Return: <br/>
     *      200 The recipe has been added<br/>
     *      409 The recipe has not been added<br/>
     *      500 Some error occurred
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(value={KReSFormat.RDF_XML,
    				 KReSFormat.TURTLE,
    				 KReSFormat.OWL_XML,
    				 KReSFormat.FUNCTIONAL_OWL,
    				 KReSFormat.MANCHESTER_OWL,
    				 KReSFormat.RDF_JSON})
    public Response addRecipe(@FormParam(value="recipe") String recipe,@FormParam(value="description") String description){

        try{

            KReSAddRecipe instance = new KReSAddRecipe(kresRuleStore);

            //String ID = kresRuleStore.getOntology().getOntologyID().toString().replace(">","").replace("<","")+"#";

            boolean ok = instance.addSimpleRecipe(IRI.create(recipe), description);
                
                if(!ok){

                   return Response.status(Status.CONFLICT).build();
                  
                }else{
                   kresRuleStore.saveOntology();
                   return Response.status(Status.OK).build();
                }
   
        }catch (Exception e){
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

   /**
     * To delete a recipe
     * @param recipe {A tring contains an IRI of the recipe}
     * @return
     *      200 The recipe has been deleted<br/>
     *      409 The recipe has not been deleted<br/>
     *      500 Some error occurred
     */
    @DELETE
    //@Consumes(MediaType.TEXT_PLAIN)
    @Produces("text/plain")
    public Response removeRecipe(@QueryParam(value="recipe") String recipe){

        try{
            
            KReSRemoveRecipe instance = new KReSRemoveRecipe(kresRuleStore);

            //String ID = kresRuleStore.getOntology().getOntologyID().toString().replace(">","").replace("<","")+"#";

            boolean ok = instance.removeRecipe(IRI.create(recipe));
            
                if(!ok){
                   return Response.status(Status.CONFLICT).build();
                }else{
                   kresRuleStore.saveOntology();
                   return Response.ok().build();
                }

        }catch(Exception e){
           throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

}
