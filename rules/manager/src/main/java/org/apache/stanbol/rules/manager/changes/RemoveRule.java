/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.manager.changes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.stanbol.rules.base.api.RuleStore;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.slf4j.LoggerFactory;

/**
 * This class will remove a rule from the KReSRuleStore used as input.<br/>
 * The KReSRuleStore object used as input is not changed and to get the new modified KReSRuleStore there is the method getStore().<br/>
 * If the rule name or IRI is not already inside the KReSRuleStore an error is lunched and the process stopped.
 *
 */
public class RemoveRule {
   private OWLOntology owlmodel;
   private OWLOntologyManager owlmanager;
   private OWLDataFactory factory;
   private String owlIDrmi;
   private String owlID;
   private RuleStore storeaux;

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
            LoggerFactory.getLogger(RemoveRule.class).error("Problem to clone ontology",ex);
        }

    }

  /**
    * Constructor, the input is a KReSRuleStore object.<br/>
    * N.B. To get the new KReSRuleStore object there is the method getStore();
    * @param store {The KReSRuleStore where there are the added rules and recipes.}
    */
   public RemoveRule(RuleStore store){
       this.storeaux = store;
       cloneOntology(storeaux.getOntology());
       this.factory = owlmanager.getOWLDataFactory();
       this.owlIDrmi ="http://kres.iks-project.eu/ontology/meta/rmi.owl#";
       this.owlID = owlmodel.getOntologyID().getOntologyIRI()+"#";
   }

   /**
    * Constructor, the input is a KReSRuleStore object.<br/>
    * N.B. To get the new KReSRuleStore object there is the method getStore();
    * @param store {The KReSRuleStore where there are the added rules and recipes.}
    */
   public RemoveRule(RuleStore store,String owlid){
       this.storeaux = store;
       cloneOntology(storeaux.getOntology());
       this.factory = owlmanager.getOWLDataFactory();
       this.owlID = owlid;
       this.owlIDrmi = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";
   }

   /**
    * To remove a rule with a given name.
    *
    * @param ruleName {The rule string name.}
    * @return {Return true is the process finished without errors.}
    */
   public boolean removeRule(String ruleName){
       boolean ok = false;
       OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
       OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID+ruleName));
       OWLObjectProperty hasrule = factory.getOWLObjectProperty(IRI.create(owlIDrmi+"hasRule"));
       OWLEntityRemover remover = new OWLEntityRemover(owlmanager, Collections.singleton(owlmodel));

       if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){
           
            ontoind.accept(remover);
            List<OWLOntologyChange> list = remover.getChanges();
            Iterator<OWLOntologyChange> iter = list.iterator();
            Vector<String> usage = new Vector();

            while(iter.hasNext()){
                OWLAxiom ax =iter.next().getAxiom();
                if(ax.getObjectPropertiesInSignature().contains(hasrule)){
                    usage.add(Arrays.toString(ax.getIndividualsInSignature().toArray()));
              
                }
            }

            if(usage.isEmpty()){
                        ok = true;
                        owlmanager.applyChanges(remover.getChanges());
                        remover.reset();
                    }else{
                         LoggerFactory.getLogger(RemoveRule.class).error("The rule cannot be deleted because is used by some recipes. Pleas check the following recipes:\n"+usage.toString());
                         ok = false;
                         return(ok);
                 }

           if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){
               LoggerFactory.getLogger(RemoveRule.class).error("Some error occurs during deletion.");
               
               ok = false;
               return(ok);
           }else{
                ok = true;
           }

       }else{
           LoggerFactory.getLogger(RemoveRule.class).error("The rule with name "+ruleName+" is not inside the ontology. Pleas check the name.");
           ok =false;
           return(ok);
       }

       if(ok){
            this.storeaux.setStore(owlmodel);
       }

       return ok;
   }

   /**
    * To remove a rule with a given IRI.
    *
    * @param ruleName {The rule complete IRI.}
    * @return {Return true is the process finished without errors.}
    */
   public boolean removeRule(IRI ruleName){
       boolean ok = false;
       OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
       OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(ruleName);
       OWLObjectProperty hasrule = factory.getOWLObjectProperty(IRI.create(owlIDrmi+"hasRule"));
       OWLEntityRemover remover = new OWLEntityRemover(owlmanager, Collections.singleton(owlmodel));

       if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){

            ontoind.accept(remover);
            List<OWLOntologyChange> list = remover.getChanges();
            Iterator<OWLOntologyChange> iter = list.iterator();
            Vector<String> usage = new Vector();

            while(iter.hasNext()){
                OWLAxiom ax =iter.next().getAxiom();
                if(ax.getObjectPropertiesInSignature().contains(hasrule)){
                    usage.add(Arrays.toString(ax.getIndividualsInSignature().toArray()));

                }
            }

            if(usage.isEmpty()){
                        ok = true;
                        owlmanager.applyChanges(remover.getChanges());
                        remover.reset();
                    }else{
                         LoggerFactory.getLogger(RemoveRule.class).error("The rule cannot be deleted because is used by some recipes. Pleas check the following recipes:\n"+usage.toString());
                         ok = false;
                         return(ok);
                 }

           if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){
               LoggerFactory.getLogger(RemoveRule.class).error("Some error occurs during deletion.");
               ok = false;
               return(ok);
           }else{
                ok = true;
           }

       }else{
           LoggerFactory.getLogger(RemoveRule.class).error("The rule with name "+ruleName+" is not inside the ontology. Pleas check the name.");
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

   /**
      * Delete a single rule from a recipe
      * @param ruleName {The IRI of the rule}
      * @param recipeName {The IRI of the recipe}
      * @return {True if the operation works.}
      */
     public boolean removeRuleFromRecipe(IRI ruleName, IRI recipeName){
         boolean ok = false;
         OWLClass ontoclsrule = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
         OWLClass ontoclsrecipe = factory.getOWLClass(IRI.create(owlIDrmi+"Recipe"));
         OWLNamedIndividual rule = factory.getOWLNamedIndividual(ruleName);
         OWLNamedIndividual recipe = factory.getOWLNamedIndividual(recipeName);

         if(!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontoclsrule, rule))){
              LoggerFactory.getLogger(RemoveRule.class).error("The rule with name "+ruleName+" is not inside the ontology. Pleas check the name.");
              if(!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontoclsrecipe, recipe))){
                LoggerFactory.getLogger(RemoveRule.class).error("The rule with name "+recipeName+" is not inside the ontology. Pleas check the name.");
                return false;
              }
              return false;
         }

        //Get the recipe
        GetRecipe getrecipe = new GetRecipe(storeaux);
        HashMap<IRI, String> map = getrecipe.getRecipe(recipeName);

        String[] sequence = map.get(recipeName).split(",");
        Vector<IRI> ruleseq = new Vector();
        for(String seq : sequence)
            ruleseq.add(IRI.create(seq.replace(" ","").trim()));

        String desc = getrecipe.getDescription(recipeName);
        if(desc.isEmpty()){
            LoggerFactory.getLogger(RemoveRule.class).error("Description for "+recipeName+" not found");
                return false;
        }

        if(ruleseq.contains(ruleName))
            ruleseq.remove(ruleseq.indexOf(ruleName));
        else{
            LoggerFactory.getLogger(RemoveRule.class).error("The rule with name "+ruleName+" is not inside the ontology. Pleas check the name.");
            return false;
        }

            //Remove the old recipe
            RemoveRecipe remove;
            try {
                remove = new RemoveRecipe(storeaux);
                ok = remove.removeRecipe(recipeName);
            } catch (Exception ex) {
                LoggerFactory.getLogger(RemoveRule.class).error(null,ex);
            }
            
            if(!ok){
                LoggerFactory.getLogger(RemoveRule.class).error("Some errors occured when delete "+ruleName+" in recipe "+recipeName);
                return false;
            }

            //Add the recipe with without the specified rule
            AddRecipe newadd = new AddRecipe(storeaux);
            if(ruleseq.isEmpty())
                ok = newadd.addSimpleRecipe(recipeName, desc);
            else
                ok = newadd.addRecipe(recipeName, ruleseq, desc);
            if(ok){
                return true;
            }else{
                return false;
            }
     }


}
