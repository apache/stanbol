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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.stanbol.rules.base.api.RuleStore;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will add new recipe to the KReSRuleStore used as input.<br/>
 * The KReSRuleStore object used as input is not changed and to get the new modified KReSRuleStore there is the method getStore().<br/>
 * If a recipe with a same name or IRI is already inside the KReSRuleStore an error is lunched and the process stopped.<br/>
 * 
 */
public class AddRecipe {

   private OWLOntology owlmodel;
   private OWLOntologyManager owlmanager;
   private OWLDataFactory factory;
   private String owlIDrmi;
   private String owlID;
   private RuleStore storeaux;
   
   private Logger log = LoggerFactory.getLogger(getClass());

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
    * N.B. To get the new KReSRuleStore object there is the method getStore();<br/>
    * @param store {The KReSRuleStore where to add the recipe.}
    */
   public AddRecipe(RuleStore store){
       this.storeaux = store;
       //cloneOntology(storeaux.getOntology());
       this.owlmanager = OWLManager.createOWLOntologyManager();
       this.owlmodel = storeaux.getOntology();
       this.factory = owlmanager.getOWLDataFactory();
       this.owlIDrmi="http://kres.iks-project.eu/ontology/meta/rmi.owl#";
       this.owlID = owlmodel.getOntologyID().getOntologyIRI().toString()+"#";
   }

   /**
    * Constructor, the input is a KReSRuleStore object and a string contains the base iri of the resource.<br/>
    * N.B. To get the new KReSRuleStore object there is the method getStore();<br/>
    * @param store {The KReSRuleStore where to add the recipe.}
    * @param owlid {The base iri of resource}
    */
   public AddRecipe(RuleStore store, String owlid){
       this.storeaux = store;
       cloneOntology(storeaux.getOntology());
       this.factory = owlmanager.getOWLDataFactory();
       this.owlIDrmi="http://kres.iks-project.eu/ontology/meta/rmi.owl#";
       this.owlID = owlid;
   }

   /**
    * Adds a Recipe.
    * The inputs are: a recipe name string that doesn't exist in the ontology,
    * a string vector with the IRI of each rule and eventualy a description of the recipe.
    *
    * @param recipeName {A string variable contains a name}
    * @param rules {A string vector variable contains the IRI of each rule}
    * @param recipeDescription {A briefly description of the rule}
    * @return {A boolean that is true if the operation is ok}
    */
   public boolean addRecipe(String recipeName, Vector<IRI> rules, String recipeDescription) {
       boolean ok = false;

       OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
       OWLClass kresrule = factory.getOWLClass(IRI.create(owlIDrmi + "KReSRule"));
       OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID + recipeName));
       OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasDescription"));
       OWLDataProperty sequence = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasSequence"));
       OWLObjectProperty hasrule = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "hasRule"));
       OWLObjectProperty start = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "startWith"));
       OWLObjectProperty end = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "endWith"));
       OWLObjectProperty precedes = factory.getOWLObjectProperty(IRI.create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));
       OWLObjectPropertyAssertionAxiom objectPropAssertion;

       if (((recipeName != null) || !recipeName.toString().isEmpty()) && ((rules != null) || !rules.isEmpty())) {
           if (!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))) {

               //Add the rule istance
               OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
               owlmanager.addAxiom(owlmodel, classAssertion);

               //start and end
               OWLNamedIndividual ind = factory.getOWLNamedIndividual(rules.firstElement());
               if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                   objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(start, ontoind, ind);
                   owlmanager.addAxiom(owlmodel, objectPropAssertion);
                   ok = true;
               } else {
                   log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                   ok = false;
                   return (ok);
               }

               ind = factory.getOWLNamedIndividual(rules.lastElement());
               if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                   objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(end, ontoind, ind);
                   owlmanager.addAxiom(owlmodel, objectPropAssertion);
                   ok = true;
               } else {
                   log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                   ok = false;
                   return (ok);

               }

               //Add the sequence string
               OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(sequence, ontoind, rules.toString().replace("[", "").replace("]", ""));
               owlmanager.addAxiom(owlmodel, dataPropAssertion);

               //Add description
               if ((recipeDescription != null) || !recipeDescription.isEmpty()) {
                   //Add the rule description
                   dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, recipeDescription);
                   owlmanager.addAxiom(owlmodel, dataPropAssertion);
                   ok = true;
               }

               //Add single rule
               for (int r = 0; r < rules.size() - 1; r++) {
                   ind = factory.getOWLNamedIndividual(rules.get(r));
                   if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                       //Add the rule to the recipes
                       objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(hasrule, ontoind, ind);
                       owlmanager.addAxiom(owlmodel, objectPropAssertion);
                       ok = true;
                       //Add precedes
                       OWLNamedIndividual indf = factory.getOWLNamedIndividual(rules.get(r + 1));
                       if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, indf))) {
                           objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(precedes, ind, indf);
                           owlmanager.addAxiom(owlmodel, objectPropAssertion);
                           ok = true;
                       } else {
                           log.error("The rule with IRI " + indf.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                           ok = false;
                           return (ok);
                       }
                   } else {
                       log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                       ok = false;
                       return (ok);
                   }

               }
               //Add last element
               ind = factory.getOWLNamedIndividual(rules.lastElement());
               if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                   //Add the rule to the recipes
                   objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(hasrule, ontoind, ind);
                   owlmanager.addAxiom(owlmodel, objectPropAssertion);
                   ok = true;
               } else {
                   log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                   ok = false;
                   return (ok);
               }

           } else {
               log.error("The recipe with name " + recipeName + " already exists. Pleas check the name.");
               ok = false;
               return (ok);
           }

       } else {
           log.error("The recipe with name and the set of rules cannot be empity or null.");
           ok = false;
           return (ok);
       }

       if (ok) {
           storeaux.setStore(owlmodel);
       }

       return (ok);
   }


    /**
     * Adds a simple Recipe without rules.
     * The inputs are: a recipe name string that doesn't exist in the ontology
     * and eventualy a description of the recipe.
     *
     * @param recipeName {A string variable contains a name}
     * @param recipeDescription {A briefly description of the rule}
     *
     * @return {A boolean that is true if the operation is ok}
     */
    public boolean addSimpleRecipe(String recipeName, String recipeDescription) {
        boolean ok = false;

        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID + recipeName));
        OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasDescription"));
        OWLDataPropertyAssertionAxiom dataPropAssertion;


        if ((recipeName != null || !recipeName.isEmpty())) {
            if (!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))) {

                //Add the rule istance
                OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
                owlmanager.addAxiom(owlmodel, classAssertion);

                //Add description
                if ((recipeDescription != null) || !recipeDescription.isEmpty()) {
                    //Add the rule description
                    dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, recipeDescription);
                    owlmanager.addAxiom(owlmodel, dataPropAssertion);
                }

                if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind)))
                    ok = true;

            } else {
                log.error("The recipe with name " + recipeName + " already exists. Pleas check the name.");
                ok = false;
                return (ok);
            }

        } else {
            log.error("The recipe with name and the set of rules cannot be empity or null.");
            ok = false;
            return (ok);
        }

        if (ok) {
            storeaux.setStore(owlmodel);
        }

        return ok;
    }

    /**
     * Add a simple Recipe without rules.
     * The inputs are: a recipe name string that doesn't exist in the ontology and eventualy a description of the recipe.
     *
     * @param recipeIRI {An IRI contains the full recipe name}
     * @param recipeDescription {A briefly description of the rule}
     *
     * @return {A boolean that is true if the operation is ok}
     */
    public boolean addSimpleRecipe(IRI recipeIRI, String recipeDescription) {
        boolean ok = false;

        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipeIRI);
        OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasDescription"));
        OWLDataPropertyAssertionAxiom dataPropAssertion;

        if ((recipeIRI != null)) {
            if (!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))) {

                //Add the rule istance
                OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
                owlmanager.addAxiom(owlmodel, classAssertion);

                //Add description
                if (recipeDescription != null && !recipeDescription.isEmpty()) {
                    //Add the rule description
                    dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, recipeDescription);
                    owlmanager.addAxiom(owlmodel, dataPropAssertion);
                }
                
                ok = true;

            } else {
                log.error("The recipe with name " + recipeIRI + " already exists. Pleas check the name.");
                ok = false;
                return (ok);
            }

        } else {
            log.error("The recipe with name and the set of rules cannot be empity or null.");
            ok = false;
            return (ok);
        }

        if (ok) {
            storeaux.setStore(owlmodel);
        }

        return (ok);
    }

   /**
    * Adds a Recipe.
    * The inputs are: a recipe name string that doesn't exist in the ontology, a string vector with the IRI of each rule and eventualy a description of the recipe.
    *
    * @param recipeName {An IRI variable contains the complete recipe name}
    * @param rules {A string vector variable contains the IRI of each rule}
    * @param recipeDescription {A briefly description of the rule}
    * @return {A boolean that is true if the operation is ok}
    */
   public boolean addRecipe(IRI recipeName, Vector<IRI> rules, String recipeDescription) {
       boolean ok = false;

       OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
       OWLClass kresrule = factory.getOWLClass(IRI.create(owlIDrmi + "KReSRule"));
       OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipeName);
       OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasDescription"));
       OWLDataProperty sequence = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasSequence"));
       OWLObjectProperty hasrule = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "hasRule"));
       OWLObjectProperty start = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "startWith"));
       OWLObjectProperty end = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "endWith"));
       OWLObjectProperty precedes = factory.getOWLObjectProperty(IRI.create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));
       OWLObjectPropertyAssertionAxiom objectPropAssertion;

       if (((recipeName != null) || !recipeName.toString().isEmpty()) && ((rules != null) || !rules.isEmpty())) {
           if (!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))) {

               //Add the rule istance
               OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
               owlmanager.addAxiom(owlmodel, classAssertion);

               //start and end
               OWLNamedIndividual ind = factory.getOWLNamedIndividual(rules.firstElement());
               if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                   objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(start, ontoind, ind);
                   owlmanager.addAxiom(owlmodel, objectPropAssertion);
                   ok = true;
               } else {
                   log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                   ok = false;
                   return (ok);
               }

               ind = factory.getOWLNamedIndividual(rules.lastElement());
               if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                   objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(end, ontoind, ind);
                   owlmanager.addAxiom(owlmodel, objectPropAssertion);
                   ok = true;
               } else {
                   log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                   ok = false;
                   return (ok);
               }

               //Add the sequence string
               OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(sequence, ontoind, rules.toString().replace("[", "").replace("]", ""));
               owlmanager.addAxiom(owlmodel, dataPropAssertion);

               //Add description
               if ((recipeDescription != null) || !recipeDescription.isEmpty()) {
                   //Add the rule description
                   dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, recipeDescription);
                   owlmanager.addAxiom(owlmodel, dataPropAssertion);
                   ok = true;
               }

               //Add single rule

               /*
               * BUGFIX - previously the check was done on rules.size()-1.
               * The right code is rules.size(). Moreover is need also a control "if(r+1>(rules.size()-1)) break;" because the last rule has not successive rules.
               *
               */
               for (int r = 0; r < rules.size(); r++) {
                   ind = factory.getOWLNamedIndividual(rules.get(r));
                   if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                       //Add the rule to the recipes
                       objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(hasrule, ontoind, ind);
                       owlmanager.addAxiom(owlmodel, objectPropAssertion);
                       ok = true;
                       //Add precedes
                       if (r + 1 > (rules.size() - 1)) {
                           break;
                       }
                       OWLNamedIndividual indf = factory.getOWLNamedIndividual(rules.get(r + 1));
                       if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, indf))) {
                           objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(precedes, ind, indf);
                           owlmanager.addAxiom(owlmodel, objectPropAssertion);
                           ok = true;
                       } else {
                           log.error("The rule with IRI " + indf.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                           ok = false;
                           return (ok);
                       }
                   } else {
                       log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                       ok = false;
                       return (ok);
                   }

               }
               //Add last element
               ind = factory.getOWLNamedIndividual(rules.lastElement());
               if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                   //Add the rule to the recipes
                   objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(hasrule, ontoind, ind);
                   owlmanager.addAxiom(owlmodel, objectPropAssertion);
                   ok = true;
               } else {
                   log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                   ok = false;
                   return (ok);
               }

           } else {
               log.error("The recipe with name " + recipeName + " already exists. Pleas check the name.");
               ok = false;
               return (ok);
           }

       } else {
           log.error("The recipe with name and the set of rules cannot be empity or null.");
           ok = false;
           return (ok);
       }

       if (ok) {
           this.storeaux.setStore(owlmodel);
       }

       return ok;
   }

    /**
     * Adds a Recipe.
     * The inputs are two HashMap with the key the recipe name and the value is a vector of IRI contains the rule's sequence; the second map contains the description.
     *
     * @param recipeMap {An HashMap variable contains string recipe name as key and an IRI vector contains the rules of the sequence as value}
     * @param recipeDescriptionMap {An HashMap variable contains string recipe name as key and the recipe's description as value}
     *
     * @return {A boolean that is true if the operation is ok}
     */
    public boolean addRecipeMap(HashMap<String, Vector<IRI>> recipeMap, HashMap<String, String> recipeDescriptionMap) {
        boolean ok = false;

        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
        OWLClass kresrule = factory.getOWLClass(IRI.create(owlIDrmi + "KReSRule"));
        OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasDescription"));
        OWLDataProperty sequence = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasSequence"));
        OWLObjectProperty hasrule = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "hasRule"));
        OWLObjectProperty start = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "startWith"));
        OWLObjectProperty end = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "endWith"));
        OWLObjectProperty precedes = factory.getOWLObjectProperty(IRI.create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));

        Object[] keys = recipeMap.keySet().toArray();

        String recipeDescription = "";
        OWLObjectPropertyAssertionAxiom objectPropAssertion;

        for (int k = 0; k < keys.length; k++) {
            String recipeName = (String) keys[k];

            OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID + recipeName));
            Vector<IRI> rules = recipeMap.get(recipeName);

            if ((recipeDescriptionMap != null))
                if (!recipeDescriptionMap.isEmpty()) {
                    recipeDescription = recipeDescriptionMap.get(recipeName);
                } else {
                    recipeDescription = "";
                }

            if (((recipeName != null) || !recipeName.toString().isEmpty()) && ((rules != null) || !rules.isEmpty())) {
                if (!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))) {

                    //Add the rule istance
                    OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
                    owlmanager.addAxiom(owlmodel, classAssertion);

                    //start and end
                    OWLNamedIndividual ind = factory.getOWLNamedIndividual(rules.firstElement());
                    if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(start, ontoind, ind);
                        owlmanager.addAxiom(owlmodel, objectPropAssertion);
                        ok = true;
                    } else {
                        log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                        ok = false;
                        return (ok);
                    }

                    ind = factory.getOWLNamedIndividual(rules.lastElement());
                    if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(end, ontoind, ind);
                        owlmanager.addAxiom(owlmodel, objectPropAssertion);
                        ok = true;
                    } else {
                        log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                        ok = false;
                        return (ok);
                    }

                    //Add the sequence string
                    OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(sequence, ontoind, rules.toString().replace("[", "").replace("]", ""));
                    owlmanager.addAxiom(owlmodel, dataPropAssertion);

                    //Add description
                    if ((recipeDescription != null))
                        if (!recipeDescription.isEmpty()) {
                            //Add the rule description
                            dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, recipeDescription);
                            owlmanager.addAxiom(owlmodel, dataPropAssertion);
                            ok = true;
                        }

                    //Add single rule
                    for (int r = 0; r < rules.size() - 1; r++) {
                        ind = factory.getOWLNamedIndividual(rules.get(r));
                        if (owlmodel.containsIndividualInSignature(ind.getIRI())) {
                            //Add the rule to the recipes
                            objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(hasrule, ontoind, ind);
                            owlmanager.addAxiom(owlmodel, objectPropAssertion);
                            ok = true;
                            //Add precedes
                            OWLNamedIndividual indf = factory.getOWLNamedIndividual(rules.get(r + 1));
                            if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, indf))) {
                                objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(precedes, ind, indf);
                                owlmanager.addAxiom(owlmodel, objectPropAssertion);
                                ok = true;
                            } else {
                                log.error("The rule with IRI " + indf.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                                ok = false;
                                return (ok);
                            }
                        } else {
                            log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                            ok = false;
                            return (ok);
                        }

                    }

                    //Add last element
                    ind = factory.getOWLNamedIndividual(rules.lastElement());
                    if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                        //Add the rule to the recipes
                        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(hasrule, ontoind, ind);
                        owlmanager.addAxiom(owlmodel, objectPropAssertion);
                        ok = true;
                    } else {
                        log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                        ok = false;
                        return (ok);
                    }

                } else {
                    log.error("The recipe with name " + recipeName + " already exists. Pleas check the name.");
                    ok = false;
                    return (ok);
                }

            } else {
                log.error("The recipe with name and the set of rules cannot be empity or null.");
                ok = false;
                return (ok);
            }
        }

        if (ok)
            this.storeaux.setStore(owlmodel);

        return (ok);
    }

    /**
     * Methods to add a Recipe.
     * The inputs are two HashMap with the key the recipe IRI name and the value is a vector IRI contains the rule's sequence; the second map contains the description.
     *
     * @param recipeMap {An HashMap variable contains the recipe IRI name as key and an IRI vector contains the rules of the sequence as value}
     * @param recipeDescriptionMap {An HashMap variable contains the recipe IRI name as key and the recipe's description as value}
     *
     * @return {A boolean that is true if the operation is ok}
     */
    public boolean addRecipeMapIRI(HashMap<IRI, Vector<IRI>> recipeMap, HashMap<IRI, String> recipeDescriptionMap) {
        boolean ok = false;

        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
        OWLClass kresrule = factory.getOWLClass(IRI.create(owlIDrmi + "KReSRule"));
        OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasDescription"));
        OWLDataProperty sequence = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasSequence"));
        OWLObjectProperty hasrule = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "hasRule"));
        OWLObjectProperty start = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "startWith"));
        OWLObjectProperty end = factory.getOWLObjectProperty(IRI.create(owlIDrmi + "endWith"));
        OWLObjectProperty precedes = factory.getOWLObjectProperty(IRI.create("http://www.ontologydesignpatterns.org/cp/owl/sequence.owl#directlyPrecedes"));

        Object[] keys = recipeMap.keySet().toArray();

        String recipeDescription = "";
        OWLObjectPropertyAssertionAxiom objectPropAssertion;


        for (int k = 0; k < keys.length; k++) {
            IRI recipeName = (IRI) keys[k];

            OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipeName);
            Vector<IRI> rules = recipeMap.get(recipeName);

            if ((recipeDescriptionMap != null))
                if (!recipeDescriptionMap.isEmpty()) {
                    recipeDescription = recipeDescriptionMap.get(recipeName);
                } else {
                    recipeDescription = "";
                }

            if (((recipeName != null) || !recipeName.toString().isEmpty()) && ((rules != null) || !rules.isEmpty())) {
                if (!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))) {

                    //Add the rule istance
                    OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls, ontoind);
                    owlmanager.addAxiom(owlmodel, classAssertion);

                    //start and end
                    OWLNamedIndividual ind = factory.getOWLNamedIndividual(rules.firstElement());
                    if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(start, ontoind, ind);
                        owlmanager.addAxiom(owlmodel, objectPropAssertion);
                        ok = true;
                    } else {
                        log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                        ok = false;
                        return (ok);
                    }

                    ind = factory.getOWLNamedIndividual(rules.lastElement());
                    if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(end, ontoind, ind);
                        owlmanager.addAxiom(owlmodel, objectPropAssertion);
                        ok = true;
                    } else {
                        log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                        ok = false;
                        return (ok);
                    }

                    //Add the sequence string
                    OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(sequence, ontoind, rules.toString().replace("[", "").replace("]", ""));
                    owlmanager.addAxiom(owlmodel, dataPropAssertion);

                    //Add description
                    if ((recipeDescription != null))
                        if (!recipeDescription.isEmpty()) {
                            //Add the rule description
                            dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, recipeDescription);
                            owlmanager.addAxiom(owlmodel, dataPropAssertion);
                            ok = true;
                        }

                    //Add single rule
                    for (int r = 0; r < rules.size() - 1; r++) {
                        ind = factory.getOWLNamedIndividual(rules.get(r));
                        if (owlmodel.containsIndividualInSignature(ind.getIRI())) {
                            //Add the rule to the recipes
                            objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(hasrule, ontoind, ind);
                            owlmanager.addAxiom(owlmodel, objectPropAssertion);

                            //Add precedes
                            OWLNamedIndividual indf = factory.getOWLNamedIndividual(rules.get(r + 1));
                            if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, indf))) {
                                objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(precedes, ind, indf);
                                owlmanager.addAxiom(owlmodel, objectPropAssertion);
                                ok = true;
                            } else {
                                log.error("The rule with IRI " + indf.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                                ok = false;
                                return (ok);
                            }
                        } else {
                            log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                            ok = false;
                            return (ok);
                        }

                    }
                    //Add last element
                    ind = factory.getOWLNamedIndividual(rules.lastElement());
                    if (owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(kresrule, ind))) {
                        //Add the rule to the recipes
                        objectPropAssertion = factory.getOWLObjectPropertyAssertionAxiom(hasrule, ontoind, ind);
                        owlmanager.addAxiom(owlmodel, objectPropAssertion);
                        ok = true;
                    } else {
                        log.error("The rule with IRI " + ind.getIRI() + " is not inside the ontology. Pleas check its IRI.");
                        ok = false;
                        return (ok);
                    }

                } else {
                    log.error("The recipe with name " + recipeName + " already exists. Pleas check the name.");
                    ok = false;
                    return (ok);
                }

            } else {
                log.error("The recipe with name and the set of rules cannot be empity or null.");
                ok = false;
                return (ok);
            }
        }
        if (ok)
            this.storeaux.setStore(owlmodel);
        return ok;
    }

    /**
     * Gets the KReSRuleStore filled with rules and recipes.
     *
     * @return {A KReSRuleStore object with the stored rules and recipes.}
     */
    public RuleStore getStore() {
        return this.storeaux;
    }

}
