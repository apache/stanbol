/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.rules.manager;

import eu.iksproject.kres.api.rules.RuleStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLEntityRemover;

/**
 * This class will remove a recipe from the KReSRuleStore used as input.<br/>
 * The KReSRuleStore object used as input is not changed and to get the new modified KReSRuleStore there is the method getStore().<br/>
 * If the recipe name or IRI is not already inside the KReSRuleStore an error is lunched and the process stopped.
 *
 */
public class KReSRemoveRecipe {


   private OWLOntology owlmodel;
   private OWLOntologyManager owlmanager;
   private OWLDataFactory factory;
   private String owlIDrmi;
   private String owlID;
   public RuleStore storeaux;

   /**
     * To create a list of imported ontlogy to be added as import declarations
     *
     * @param inowl {Input ontology where to get the import declarations}
     * @return {A list of declarations}
     */
	private List<OWLOntologyChange> createImportList(OWLOntology inowl,OWLOntology toadd){
		
        Iterator<OWLOntology> importedonto = inowl.getDirectImports().iterator();
        List<OWLOntologyChange> additions = new LinkedList<OWLOntologyChange>();
        OWLDataFactory auxfactory = inowl.getOWLOntologyManager().getOWLDataFactory();
		
        while(importedonto.hasNext()){
            OWLOntology auxonto = importedonto.next();
            additions.add(new AddImport(toadd,auxfactory.getOWLImportsDeclaration(auxonto.getOWLOntologyManager().getOntologyDocumentIRI(auxonto))));
        }
		
        if(additions.size()==0){
            Iterator<OWLImportsDeclaration> importedontob = inowl.getImportsDeclarations().iterator();
            additions = new LinkedList<OWLOntologyChange>();
            auxfactory = inowl.getOWLOntologyManager().getOWLDataFactory();
			
            while(importedontob.hasNext()){
                OWLImportsDeclaration  auxontob = importedontob.next();
                additions.add(new AddImport(toadd,auxontob));
            }
        }
		
        return additions;
    }

   /**
     * To clone ontology with all its axioms and imports declaration
     *
     * @param inowl {The onotlogy to be cloned}
     * @return {An ontology with the same characteristics}
     */
    private void cloneOntology(OWLOntology inowl){

        //Clone the targetontology
        try {
            this.owlmodel = OWLManager.createOWLOntologyManager().createOntology(inowl.getOntologyID().getOntologyIRI());
            this.owlmanager = owlmodel.getOWLOntologyManager();
            //Add axioms
            owlmanager.addAxioms(owlmodel,inowl.getAxioms());
            //Add import declaration
            List<OWLOntologyChange> additions = createImportList(inowl,owlmodel);
            if(additions.size()>0)
                owlmanager.applyChanges(additions);
           
        } catch (OWLOntologyCreationException ex) {
            ex.printStackTrace();
        }

    }

   /**
    * Constructor, the input is a KReSRuleStore object.<br/>
    * N.B. To get the new KReSRuleStore object there is the method getStore().
    * @param store {The KReSRuleStore where there are the added rules and recipes.}
    */
   public KReSRemoveRecipe(RuleStore store){
       this.storeaux = store;
       cloneOntology(store.getOntology());
       this.factory = owlmanager.getOWLDataFactory();
       this.owlIDrmi="http://kres.iks-project.eu/ontology/meta/rmi.owl#";
       this.owlID = owlmodel.getOntologyID().getOntologyIRI().toString()+"#";
   }

    /**
    * Constructor, the input is a KReSRuleStore object and a string contains the base iri of the resource.<br/>
    *
    * @param store {The KReSRuleStore where there are the added rules and recipes.}
    * @param owlid {The base iri of resource}
    */
   public KReSRemoveRecipe(RuleStore store, String owlid){
       this.storeaux = store;
       cloneOntology(storeaux.getOntology());
       this.factory = owlmanager.getOWLDataFactory();
       this.owlIDrmi="http://kres.iks-project.eu/ontology/meta/rmi.owl#";
       this.owlID = owlid;
   }

   /**
    * To remove a recipe with a given name.
    *
    * @param recipeName {The recipe string name.}
    * @return {Return true is the process finished without errors.}
    */
   public boolean removeRecipe(String recipeName){
       boolean ok = false;
       OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"Recipe"));
       OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID+recipeName));
       OWLEntityRemover remover = new OWLEntityRemover(owlmanager, Collections.singleton(owlmodel));
       OWLObjectProperty precedes = factory.getOWLObjectProperty(IRI.create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));
       OWLObjectPropertyAssertionAxiom objectPropAssertion;

        KReSGetRecipe getrecipe = new KReSGetRecipe(storeaux);
        HashMap<IRI, String> map = getrecipe.getRecipe(recipeName);
        
        String[] sequence = map.get(IRI.create(owlID+recipeName)).split(",");
        Vector<IRI> ruleseq = new Vector();
        for(String seq : sequence){
            if(!seq.replace(" ","").trim().isEmpty())
                ruleseq.add(IRI.create(seq.replace(" ","").trim()));
        }
        HashMap<String, Integer> count = getrecipe.getBinSequenceRecipeCount();
        Vector<String> binseq = new Vector();
        String bs="";
        for(int i = 0; i<ruleseq.size()-1;i++){
            bs=ruleseq.get(i).toString()+" precedes "+ruleseq.get(i+1).toString();
            if(count.containsKey(bs)){
                if(count.get(bs)==1){
                    binseq.add(bs);
                }
            }
        }

       if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){

            ontoind.accept(remover);
            owlmanager.applyChanges(remover.getChanges());
            remover.reset();

           if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){
               System.err.println("Some error occurs during deletion.");
               ok = false;
               return(ok);
           }else{

                for(int i = 0; i<binseq.size(); i++){
                    String[] iris = binseq.get(i).split(" precedes ");
                    OWLNamedIndividual ontoindA = factory.getOWLNamedIndividual(IRI.create(iris[0].replace(" ","").trim()));
                    OWLNamedIndividual ontoindB = factory.getOWLNamedIndividual(IRI.create(iris[1].replace(" ","").trim()));
                    objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(precedes,ontoindA, ontoindB);
                    if(owlmodel.containsAxiom(objectPropAssertion)){
                        owlmanager.removeAxiom(owlmodel, objectPropAssertion);
                        if(!owlmodel.containsAxiom(objectPropAssertion)){
                            ok = true;
                        }else{
                            System.err.println("Some error occurs during deletion.");
                            ok = false;
                            return(ok);
                        }
                    }
                }
           }

       }else{
           System.err.println("The rule with name "+recipeName+" is not inside the ontology. Pleas check the name.");
           ok =false;
           return(ok);
       }

       if(ok)
           this.storeaux.setStore(owlmodel);
       
       return ok;
   }

   /**
    * To remove a recipe with a given IRI.
    *
    * @param recipeName {The complete recipe IRI.}
    * @return {Return true is the process finished without errors.}
    */
   public boolean removeRecipe(IRI recipeName){
       boolean ok = false;
       OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"Recipe"));
       OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipeName);
       OWLEntityRemover remover = new OWLEntityRemover(owlmanager, Collections.singleton(owlmodel));
       OWLObjectProperty precedes = factory.getOWLObjectProperty(IRI.create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));
       OWLObjectPropertyAssertionAxiom objectPropAssertion;

        KReSGetRecipe getrecipe = new KReSGetRecipe(storeaux);
        HashMap<IRI, String> map = getrecipe.getRecipe(recipeName);
        
        String[] sequence = map.get(recipeName).split(",");
        Vector<IRI> ruleseq = new Vector();
        for(String seq : sequence){
            if(!seq.replace(" ","").trim().isEmpty())
                ruleseq.add(IRI.create(seq.replace(" ","").trim()));
        }
        HashMap<String, Integer> count = getrecipe.getBinSequenceRecipeCount();
       
        Vector<String> binseq = new Vector();
        String bs="";
        for(int i = 0; i<ruleseq.size()-1;i++){
            bs=ruleseq.get(i).toString()+" precedes "+ruleseq.get(i+1).toString();
            if(count.containsKey(bs)){
                if(count.get(bs)==1){
                    binseq.add(bs);
                }
            }
        }
   
       if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){

            ontoind.accept(remover);
            owlmanager.applyChanges(remover.getChanges());
            remover.reset();

           if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){
               System.err.println("Some error occurs during deletion.");
               ok = false;
               return(ok);
           }else{
                if(binseq.size()>0){
                for(int i = 0; i<binseq.size(); i++){
                    String[] iris = binseq.get(i).split(" precedes ");
                    OWLNamedIndividual ontoindA = factory.getOWLNamedIndividual(IRI.create(iris[0].replace(" ","").trim()));
                    OWLNamedIndividual ontoindB = factory.getOWLNamedIndividual(IRI.create(iris[1].replace(" ","").trim()));
                    objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(precedes,ontoindA, ontoindB);
                    if(owlmodel.containsAxiom(objectPropAssertion)){
                        owlmanager.removeAxiom(owlmodel, objectPropAssertion);
                        if(!owlmodel.containsAxiom(objectPropAssertion)){
                            ok = true;
                        }else{
                            System.err.println("Some error occurs during deletion.");
                            ok = false;
                            return(ok);
                        }
                    }
                }
                }else{
                    ok = true;
                }
           }

       }else{
           System.err.println("The rule with name "+recipeName+" is not inside the ontology. Pleas check the name.");
           ok =false;
           return(ok);
       }

       if(ok){
           this.storeaux.setStore(owlmodel);
       }
       return ok;
   }

   /**
     * Get the KReSRuleStore filled with rules and recipes
    *
     * @return {A KReSRuleStore object with the stored rules and recipes.}
     */
     public RuleStore getStore(){
         return this.storeaux;
     }

}
