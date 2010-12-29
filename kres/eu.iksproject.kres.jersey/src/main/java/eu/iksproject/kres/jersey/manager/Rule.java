/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.jersey.manager;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Hashtable;
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
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.format.KReSFormat;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.api.storage.OntologyStoreProvider;
import eu.iksproject.kres.jersey.resource.NavigationMixin;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.rules.manager.KReSAddRecipe;
import eu.iksproject.kres.rules.manager.KReSAddRule;
import eu.iksproject.kres.rules.manager.KReSGetRecipe;
import eu.iksproject.kres.rules.manager.KReSGetRule;
import eu.iksproject.kres.rules.manager.KReSRemoveRecipe;
import eu.iksproject.kres.rules.manager.KReSRemoveRule;
import eu.iksproject.kres.rules.manager.KReSRuleStore;
import eu.iksproject.kres.storage.provider.OntologyStorageProviderImpl;

/**
 *
 * @author elvio
 * @author andrea.nuzzolese
 * 
 */
@Path("/rule")
public class Rule extends NavigationMixin{

	protected KReSONManager onm;
	protected OntologyStoreProvider storeProvider;

	private Logger log = LoggerFactory.getLogger(getClass());

	private RuleStore kresRuleStore;
    private HashMap<IRI, String> map;
    private String desc;

   /**
     * To get the KReSRuleStore where are stored the rules and the recipes
     *
	 * @param servletContext
	 *            {To get the context where the REST service is running.}
     */
    public Rule(@Context ServletContext servletContext){
		this.kresRuleStore = (RuleStore) servletContext
				.getAttribute(RuleStore.class.getName());
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
       if (kresRuleStore == null) {
			log
					.warn("No KReSRuleStore with stored rules and recipes found in servlet context. Instantiating manually with default values...");
			this.kresRuleStore = new KReSRuleStore(onm,
					new Hashtable<String, Object>(), "");
			log
					.debug("PATH TO OWL FILE LOADED: "
							+ kresRuleStore.getFilePath());
        }
    }

   /**
	 * Get a rule from the rule base (that is the ontology that contains the
	 * rules and the recipe). curl -v -X GET
	 * http://localhost:8080/kres/rule/http
	 * ://kres.iks-project.eu/ontology/meta/rmi.owl#ProvaParentRule
	 * 
	 * @param uri
	 *            {A string contains the IRI full name of the rule.}
     * @return Return: <br/>
	 *         200 The rule is retrieved (import declarations point to KReS
	 *         Services) <br/>
     *       404 The rule does not exists in the manager <br/>
     *       500 Some error occurred
     *
     */
    @GET
    @Path("/{uri:.+}")
	@Produces(value = { KReSFormat.RDF_XML, KReSFormat.TURTLE,
			KReSFormat.OWL_XML })
    public Response getRule(@PathParam("uri") String uri){
 
      try{

       KReSGetRule recipe = new KReSGetRule(kresRuleStore);
       if(uri.equals("all")){

           HashMap<IRI, String> rule = recipe.getAllRules();
           Iterator<IRI> keys = rule.keySet().iterator();
          
        if(rule==null){
            return Response.status(Status.NOT_FOUND).build();
        }else{

            OWLOntology onto = kresRuleStore.getOntology();
					OWLOntology newmodel = OWLManager
							.createOWLOntologyManager().createOntology(
									onto.getOntologyID());
					OWLDataFactory factory = onto.getOWLOntologyManager()
							.getOWLDataFactory();

					Iterator<OWLOntology> importedonto = onto
							.getDirectImports().iterator();
            List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
					OWLDataFactory auxfactory = onto.getOWLOntologyManager()
							.getOWLDataFactory();

            while(importedonto.hasNext()){
                OWLOntology auxonto = importedonto.next();
						additions.add(new AddImport(newmodel, auxfactory
								.getOWLImportsDeclaration(auxonto
										.getOWLOntologyManager()
										.getOntologyDocumentIRI(auxonto))));
            }

            if(!additions.isEmpty())
						newmodel.getOWLOntologyManager()
								.applyChanges(additions);

            while(keys.hasNext()){
						OWLNamedIndividual ind = factory
								.getOWLNamedIndividual(keys.next());
                Set<OWLIndividualAxiom> ax = onto.getAxioms(ind);
						newmodel.getOWLOntologyManager()
								.addAxioms(newmodel, ax);
            }

            try {
						OWLManager.createOWLOntologyManager().saveOntology(
								newmodel,
								newmodel.getOWLOntologyManager()
										.getOntologyFormat(newmodel),
								System.out);
    		} catch (OWLOntologyStorageException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

            return Response.ok(newmodel).build();
        }

       }else{

        HashMap<IRI, String> rule = recipe.getRule(IRI.create(uri));
       
        if(rule==null){
            return Response.status(Status.NOT_FOUND).build();
        }else{
            OWLOntology onto = kresRuleStore.getOntology();

					OWLDataFactory factory = onto.getOWLOntologyManager()
							.getOWLDataFactory();
					OWLNamedIndividual ind = factory.getOWLNamedIndividual(IRI
							.create(uri));
            Set<OWLIndividualAxiom> ax = onto.getAxioms(ind);
					OWLOntology newmodel = OWLManager
							.createOWLOntologyManager().createOntology(
									onto.getOntologyID());

					Iterator<OWLOntology> importedonto = onto
							.getDirectImports().iterator();
            List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
					OWLDataFactory auxfactory = onto.getOWLOntologyManager()
							.getOWLDataFactory();

            while(importedonto.hasNext()){
                OWLOntology auxonto = importedonto.next();
						additions.add(new AddImport(newmodel, auxfactory
								.getOWLImportsDeclaration(auxonto
										.getOWLOntologyManager()
										.getOntologyDocumentIRI(auxonto))));
            }

            if(!additions.isEmpty())
						newmodel.getOWLOntologyManager()
								.applyChanges(additions);

            newmodel.getOWLOntologyManager().addAxioms(newmodel,ax);

            try {
						OWLManager.createOWLOntologyManager().saveOntology(
								newmodel,
								newmodel.getOWLOntologyManager()
										.getOntologyFormat(newmodel),
								System.out);
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
    
    @GET
    @Path("/of-recipe/{uri:.+}")
	@Produces(value = { KReSFormat.RDF_XML, KReSFormat.RDF_JSON })
    public Response getRulesOfRecipe(@PathParam("uri") String recipeURI){
    	
    	KReSGetRule kReSGetRule = new KReSGetRule(kresRuleStore);
    	String recipeURIEnc;
		try {
			recipeURIEnc = URLEncoder
					.encode(
							"http://kres.iks-project.eu/ontology/meta/rmi_config.owl#MyRecipeA",
							"UTF-8");
			System.out.println("RECIPE : "+recipeURIEnc);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	System.out.println("RECIPE IRI : "+IRI.create(recipeURI).toString());
		OWLOntology ontology = kReSGetRule.getAllRulesOfARecipe(IRI
				.create(recipeURI));
		
    	return Response.ok(ontology).build();
			
    }

   /**
	 * To add a rule to a recipe at the end of the sequence. curl -v -X POST -F
	 * "recipe=http://kres.iks-project.eu/ontology/meta/rmi.owl%23ProvaParentRecipe"
	 * -F "rule=http://kres.iks-project.eu/ontology/meta/rmi.owl%23ProvaRuleNEW"
	 * -F "kres-syntax=body -> head" -F "description=prova di aggiunta regola"
	 * http://localhost:8080/kres/rule
	 * 
	 * @param recipe
	 *            {A string contains the IRI of the recipe where to add the
	 *            rule}
	 * @param rule
	 *            {A string contains the IRI of the rule to be added at the
	 *            recipe}
	 * @param kres_syntax
	 *            {A string contains the body and the head of the kres rule. If
	 *            not specified the rule is search in the Ontology otherwise is
	 *            added as new.}
	 * @param description
	 *            {A string contains a description of the rule}
     * @return Return: <br/>
     *      200 The rule has been added <br/>
     *      204 The rule has not been added <br/>
     *      400 The rule and recipe are not specified<br/>
     *      404 Recipe or rule not found<br/>
     *      409 The rule has not been added<br/>
     *      500 Some error occurred
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response addRuleToRecipe(@FormParam(value = "recipe") String recipe,
			@FormParam(value = "rule") String rule,
			@FormParam(value = "kres-syntax") String kres_syntax,
			@FormParam(value = "description") String description) {
    
//        System.err.println("recipe "+recipe);
//        System.err.println("rule " + rule);
//        System.err.println("kres-syntax "+kres_syntax);
//        System.err.println("description "+description);
//
//        return Response.ok().build();
           
        try{

         if((recipe==null)&&(rule==null)){
                return Response.status(Status.BAD_REQUEST).build();
         }

         recipe = recipe.replace(" ","").trim();
         rule = rule.replace(" ","").trim();

         //The rule is already inside the rule store
         if((kres_syntax==null)){
            //Get the rule
            KReSGetRule inrule = new KReSGetRule(kresRuleStore);
            this.map = inrule.getRule(IRI.create(rule));
            
            if(map==null){
                return Response.status(Status.NOT_FOUND).build();
            }

            //Get the recipe
            KReSGetRecipe getrecipe = new KReSGetRecipe(kresRuleStore);
            this.map = getrecipe.getRecipe(IRI.create(recipe));
            if(map!=null){
                this.desc = getrecipe.getDescription(IRI.create(recipe));
                if(desc==null)
                    Response.status(Status.NOT_FOUND).build();
            }else{
                return Response.status(Status.NOT_FOUND).build();
            }

            String[] sequence = map.get(IRI.create(recipe)).split(",");
            Vector<IRI> ruleseq = new Vector();
            if(!sequence[0].isEmpty())
            for(String seq : sequence)
                ruleseq.add(IRI.create(seq.replace(" ","").trim()));

            //Add the new rule to the end
            ruleseq.add(IRI.create(rule));
            //Remove the old recipe
            KReSRemoveRecipe remove = new KReSRemoveRecipe(kresRuleStore);
            boolean ok = remove.removeRecipe(IRI.create(recipe));
            
            if(!ok)
                return Response.status(Status.CONFLICT).build();

            //Add the recipe with the new rule
            KReSAddRecipe newadd = new KReSAddRecipe(kresRuleStore);
            ok = newadd.addRecipe(IRI.create(recipe), ruleseq, desc);
            
            if(ok){
                    kresRuleStore.saveOntology();
                    return Response.ok().build();
            }else{
                    return Response.status(Status.NO_CONTENT).build();
            }
        }

        //The rule is added to the store and to the recipe
         if((kres_syntax!=null)&(description!=null)){
            //Get the rule
            KReSAddRule inrule = new KReSAddRule(kresRuleStore);
				boolean ok = inrule.addRule(IRI.create(rule), kres_syntax,
						description);
            if(!ok){
                System.err.println("PROBLEM TO ADD: "+rule);
                return Response.status(Status.CONFLICT).build();
            }
            
            //Get the recipe
            KReSGetRecipe getrecipe = new KReSGetRecipe(kresRuleStore);
            this.map = getrecipe.getRecipe(IRI.create(recipe));
            System.out.println("RECIPE FOR RULE: "+recipe);
            if(map!=null){
                this.desc = getrecipe.getDescription(IRI.create(recipe));
               
                if(desc==null)
                   return Response.status(Status.NOT_FOUND).build();
            }else{
                return Response.status(Status.NOT_FOUND).build();
            }

            String[] sequence = map.get(IRI.create(recipe)).split(",");
            Vector<IRI> ruleseq = new Vector();
            if(!sequence[0].isEmpty())
            for(String seq : sequence)
                ruleseq.add(IRI.create(seq.replace(" ","").trim()));

            //Add the new rule to the end          
            ruleseq.add(IRI.create(rule));
            //Remove the old recipe
            KReSRemoveRecipe remove = new KReSRemoveRecipe(kresRuleStore);
            ok = remove.removeRecipe(IRI.create(recipe));
            if(!ok){
                System.err.println("ERROR TO REMOVE OLD RECIPE: "+recipe);
                return Response.status(Status.CONFLICT).build();
            }

            //Add the recipe with the new rule
            KReSAddRecipe newadd = new KReSAddRecipe(kresRuleStore);
            ok = newadd.addRecipe(IRI.create(recipe), ruleseq, desc);
            if(ok){
                    kresRuleStore.saveOntology();
                    return Response.ok().build();
            }else{
                    return Response.status(Status.NO_CONTENT).build();
            }
        }else{
             return Response.status(Status.BAD_REQUEST).build();
        }

        }catch (Exception e){
            throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

   /**
	 * To delete a rule from a recipe or from the ontology. If the recipe is not
	 * specified the rule is deleted from the ontology. curl -v -X DELETE -G \
	 * -d recipe=
	 * "http://kres.iks-project.eu/ontology/meta/rmi.owl#ProvaParentRecipe" \ -d
	 * rule
	 * ="http://kres.iks-project.eu/ontology/meta/rmi.owl#ProvaParentNewRule" \
     * http://localhost:port/kres/rule
    *
     * @Param rule {A string contains an IRI of the rule to be removed}
	 * @param recipe
	 *            {A string contains an IRI of the recipe where remove the rule}
     * @return Return: <br/>
     *      200 The rule has been deleted<br/>
     *      204 The rule has not been deleted<br/>
     *      404 Recipe or rule not found<br/>
     *      409 The recipe has not been deleted<br/>
     *      500 Some error occurred
     */
    @DELETE
    //@Consumes(MediaType.TEXT_PLAIN)
    @Produces(KReSFormat.TEXT_PLAIN)
	public Response removeRule(@QueryParam(value = "rule") String rule,
			@QueryParam(value = "recipe") String recipe) {

        boolean ok;

        try{
         
         //Delete from the recipe
         if((recipe!=null)&&(rule!=null)){
             recipe = recipe.replace(" ","").trim();
             rule = rule.replace(" ","").trim();
            //Get the rule
            KReSGetRule getrule = new KReSGetRule(kresRuleStore);
            this.map = getrule.getRule(IRI.create(rule));
            if(map==null){
                return Response.status(Status.NOT_FOUND).build();
            }

            //Get the recipe
            KReSGetRecipe getrecipe = new KReSGetRecipe(kresRuleStore);
            this.map = getrecipe.getRecipe(IRI.create(recipe));
            if(map!=null){
                this.desc = getrecipe.getDescription(IRI.create(recipe));
                if(desc.isEmpty())
                    return Response.status(Status.NOT_FOUND).build();
            }else{
                return Response.status(Status.NOT_FOUND).build();
            }
            
            KReSRemoveRule remove = new KReSRemoveRule(kresRuleStore);
				ok = remove.removeRuleFromRecipe(IRI.create(rule), IRI
						.create(recipe));
            if(ok){
                kresRuleStore.saveOntology();
                return Response.status(Status.OK).build();
            }else{
                return Response.status(Status.NO_CONTENT).build();
            }
         }

         //Delete from the ontology
         if((recipe==null)&&(rule!=null)){
             rule = rule.replace(" ","").trim();
            //Get the rule
            KReSGetRule getrule = new KReSGetRule(kresRuleStore);
            this.map = getrule.getRule(IRI.create(rule));
            if(map==null){
                return Response.status(Status.NOT_FOUND).build();
            }

            //Remove the old recipe
            KReSRemoveRule remove = new KReSRemoveRule(kresRuleStore);
            ok = remove.removeRule(IRI.create(rule));

            if(ok){
                kresRuleStore.saveOntology();
                return Response.ok().build();
            }else{
                return Response.status(Status.NO_CONTENT).build();
            }
         }else{
             return Response.status(Status.BAD_REQUEST).build();
         }
            
        }catch(Exception e){
           throw new WebApplicationException(e, Status.INTERNAL_SERVER_ERROR);
        }
    }

}
