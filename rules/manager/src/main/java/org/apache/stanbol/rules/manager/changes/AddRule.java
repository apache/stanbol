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
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * This class will add new rule to the KReSRuleStore used as input.<br/>
 * The KReSRuleStore object used as input is not changed and to get the new modified KReSRuleStore there is the method getStore().<br/>
 * If a rule with a same name or IRI is already inside the KReSRuleStore an error is lunched and the process stopped.
 *
 */
public class AddRule {
    
   private Logger log = LoggerFactory.getLogger(getClass());

   private OWLOntology owlmodel;
   private OWLOntologyManager owlmanager;
   private OWLDataFactory factory;
   private String owlID;
   private String owlIDrmi;
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
            ex.printStackTrace();
        }

    }

   /**
    * Constructor, the input is a KReSRuleStore object.<br/>
    * N.B. To get the new KReSRuleStore object there is the method getStore().
    * @param store {The KReSRuleStore where to add the rule.}
    */
   public AddRule(RuleStore store){
       this.storeaux = store;
       cloneOntology(storeaux.getOntology());
       this.factory = owlmanager.getOWLDataFactory();
       this.owlIDrmi ="http://kres.iks-project.eu/ontology/meta/rmi.owl#";
       this.owlID = owlmodel.getOntologyID().getOntologyIRI().toString()+"#";
   }

   /**
    * Constructor, the input is a KReSRuleStore object and a string contains the base iri of the resource.<br/>
    * N.B. To get the new KReSRuleStore object there is the method getStore().
    * @param owlid {The base iri of resource}
    * @param store {The KReSRuleStore where to add the rule.}
    */
   public AddRule(RuleStore store, String owlid){
       this.storeaux = store;
       //cloneOntology(storeaux.getOntology());
       this.owlmanager = OWLManager.createOWLOntologyManager();
       this.owlmodel = storeaux.getOntology();
       this.factory = owlmanager.getOWLDataFactory();
       this.owlIDrmi ="http://kres.iks-project.eu/ontology/meta/rmi.owl#";
       this.owlID = owlid;
   }

   /**
    * Method to add a Rule. The inputs are: a rule name string that doesn't exist in the ontology, a rule body->head string and eventualy a description of the rule
    *
    * @param ruleName {A string variable contains a name}
    * @param ruleBodyHead {A string variable contains the body and head of the rule}
    * @param ruleDescription {A briefly description of the rule}
    * @return {A boolean that is true if the operation is ok}
    */
   public boolean addRule(String ruleName, String ruleBodyHead, String ruleDescription){
       boolean ok = false;

       OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
       OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID+ruleName));
       OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasDescription"));
       OWLDataProperty bodyhead = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasBodyAndHead"));

       if(((ruleName!=null)||!ruleName.isEmpty())&&((ruleBodyHead!=null)||!ruleBodyHead.isEmpty())){
    	   if(!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){
       
		        //Add the rule istance
		        OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls,ontoind);
		        owlmanager.addAxiom(owlmodel, classAssertion);
		        //Add the rule Body and Head
		        OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(bodyhead, ontoind, ruleBodyHead);
		        owlmanager.addAxiom(owlmodel, dataPropAssertion);
		
		        if((ruleDescription!=null))
		            if(!ruleDescription.isEmpty()){
		            //Add the rule description
		            dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, ruleDescription);
		            owlmanager.addAxiom(owlmodel, dataPropAssertion);
		        }

		        ok = true;
    	   }
    	   else{
	           log.error("The rule with name "+ruleName+" already exists. Pleas check the name.");
	           ok = false;
	           return(ok);
    	   }

       }
       else{
	       log.error("The rule with name and the body-head string cannot be empity or null.");
	       ok=false;
	       return(ok);
       }
       if(ok)
    	   this.storeaux.setStore(owlmodel);
       
       return(ok);
   }

   /**
    * Method to add a Rule. The inputs are: a rule name IRI that doesn't exist in the ontology, a rule body->head string and eventualy a description of the rule
    *
    * @param ruleName {An IRI variable contains the complete name}
    * @param ruleBodyHead {A string variable contains the body and head of the rule}
    * @param ruleDescription {A briefly description of the rule}
    * @return {A boolean that is true if the operation is ok}
    */
   public boolean addRule(IRI ruleName, String ruleBodyHead, String ruleDescription){
       boolean ok = false;

       OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
       OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(ruleName);
       OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasDescription"));
       OWLDataProperty bodyhead = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasBodyAndHead"));
       
       if(((ruleName!=null)&&!ruleName.toString().isEmpty())&&((ruleBodyHead!=null)&&!ruleBodyHead.isEmpty())){
    	   if(!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){
	     
	            //Add the rule istance
	            OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls,ontoind);
	            owlmanager.addAxiom(owlmodel, classAssertion);
	            //Add the rule Body and Head
	            OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(bodyhead, ontoind, ruleBodyHead);
	            owlmanager.addAxiom(owlmodel, dataPropAssertion);
	            
	            if(ruleDescription!=null)
	            if(!ruleDescription.isEmpty()){
	                //Add the rule description
	                dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, ruleDescription);
	                owlmanager.addAxiom(owlmodel, dataPropAssertion);
	            }

            ok = true;
	       }
	       else{
	           log.error("The rule with name "+ruleName+" already exists. Pleas check the name.");
	           ok = false;
	           return(ok);
	       }

       }
       else{
	       log.error("The rule with name and the body-head string cannot be empity or null.");
	       ok=false;
	       return(ok);
       }

       if(ok){
    	   this.storeaux.setStore(owlmodel);
       }

       return(ok);
   }
   
   /**
    * Method to add a Rule. The inputs are two HashMap with the key the rule name and the value are the Body -> Head string a rule description.
    *
    * @param ruleBodyHeadMap {An HashMap variable contains string rule name as key and the body and head as value}
    * @param ruleDescriptionMap {An HashMap variable contains string rule name as key and the rule's description as value}
    * @return {A boolean that is true if the operation is ok}
    */
   public boolean addRuleMap(HashMap<String,String> ruleBodyHeadMap, HashMap<String,String> ruleDescriptionMap){
       boolean ok = false;

    OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
    OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasDescription"));
    OWLDataProperty bodyhead = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasBodyAndHead"));
    Object[] keys = ruleBodyHeadMap.keySet().toArray();
   
    String ruleDescription = "";

    for(int k = 0; k<keys.length;k++){
    String ruleName = (String) keys[k];
    String ruleBodyHead = ruleBodyHeadMap.get(ruleName);
    
    if((ruleDescriptionMap!=null)&&!ruleDescriptionMap.isEmpty()){
        ruleDescription = ruleDescriptionMap.get(ruleName);
    }else{
        ruleDescription ="";
    }

    OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID+ruleName));
    if(((ruleName!=null)||!ruleName.isEmpty())&&((ruleBodyHead!=null)||!ruleBodyHead.isEmpty())){
       if(!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){
       
            //Add the rule istance
            OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls,ontoind);
            owlmanager.addAxiom(owlmodel, classAssertion);
            //Add the rule Body and Head
            OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(bodyhead, ontoind, ruleBodyHead);
            owlmanager.addAxiom(owlmodel, dataPropAssertion);

            if((ruleDescription!=null))
                if(!ruleDescription.isEmpty()){
                //Add the rule description
                dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, ruleDescription);
                owlmanager.addAxiom(owlmodel, dataPropAssertion);
            }

           ok = true;
       }else{
           log.error("The rule with name "+ruleName+" already exists. Pleas check the name.");
           ok = false;
           return(ok);
       }

    }else{
       log.error("The rule with name and the body-head string cannot be empity or null.");
       ok=false;
       return(ok);
    }

    }
       if(ok)
        this.storeaux.setStore(owlmodel);

    return(ok);
   }

   /**
    * Method to add a Rule. The inputs are two HashMap with the key the rule name and the value are the Body -> Head string a rule description.
    *
    * @param ruleBodyHeadMap {An HashMap variable contains string rule name as key and the body and head as value}
    * @param ruleDescriptionMap {An HashMap variable contains string rule name as key and the rule's description as value}
    * @return {A boolean that is true if the operation is ok}
    */

   public boolean addRuleMapIRI(HashMap<IRI,String> ruleBodyHeadMap, HashMap<IRI,String> ruleDescriptionMap){

    boolean ok = false;

    OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
    OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasDescription"));
    OWLDataProperty bodyhead = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasBodyAndHead"));
    Iterator<IRI> keys = ruleBodyHeadMap.keySet().iterator();
    String ruleDescription = "";

    while(keys.hasNext()){
    IRI ruleName = keys.next();
    String ruleBodyHead = ruleBodyHeadMap.get(ruleName);

    if((ruleDescriptionMap!=null)&&!ruleDescriptionMap.isEmpty()){
        ruleDescription = ruleDescriptionMap.get(ruleName);
    }else{
        ruleDescription ="";
    }

    OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(ruleName);
    if(((ruleName!=null)&&!ruleName.toString().isEmpty())&&((ruleBodyHead!=null)&&!ruleBodyHead.isEmpty())){
       if(!owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){

            //Add the rule istance
            OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(ontocls,ontoind);
            owlmanager.addAxiom(owlmodel, classAssertion);
            //Add the rule Body and Head
            OWLDataPropertyAssertionAxiom dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(bodyhead, ontoind, ruleBodyHead);
            owlmanager.addAxiom(owlmodel, dataPropAssertion);

            if((ruleDescription!=null))
                if(!ruleDescription.isEmpty()){
                //Add the rule description
                dataPropAssertion = factory.getOWLDataPropertyAssertionAxiom(description, ontoind, ruleDescription);
                owlmanager.addAxiom(owlmodel, dataPropAssertion);
            }

            ok= true;
       }else{
           log.error("The rule with name "+ruleName+" already exists. Pleas check the name.");
           ok = false;
           return(ok);
       }

    }else{
       log.error("The rule with name and the body-head string cannot be empity or null.");
       ok=false;
       return(ok);
    }

    }

      if(ok)
        this.storeaux.setStore(owlmodel);

       return(ok);
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
