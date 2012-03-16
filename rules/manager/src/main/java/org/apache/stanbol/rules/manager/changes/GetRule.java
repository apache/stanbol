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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.stanbol.rules.base.api.RuleStore;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author elvio
 */
public class GetRule {
    
    private Logger log = LoggerFactory.getLogger(getClass());

    private OWLOntology owlmodel;
    private String owlID;
    private String owlIDrmi;
    private OWLOntologyManager owlmanager;
    private OWLDataFactory factory;

  /**
    * Constructor, the input is a KReSRuleStore object.
    *
    * @param store {The KReSRuleStore where there are the added rules and recipes.}
    */
    public GetRule(RuleStore store){
        this.owlmodel = store.getOntology();
        this.owlIDrmi = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";
        this.owlID = owlmodel.getOntologyID().toString().replace("<","").replace(">","")+"#";
        this.owlmanager = owlmodel.getOWLOntologyManager();
        this.factory = owlmanager.getOWLDataFactory();
    }

   /**
     * To return the IRI of the named rule with its string Body -> Head
     *
     * @param rulename {It is the string name of the rule}
     * @return {Return an HashMap with the IRI as a key and the rule string as value.}
     */
    public HashMap<IRI,String> getRule(String rulename){

        HashMap<IRI,String> rule = new HashMap();
        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
        OWLNamedIndividual indrule = factory.getOWLNamedIndividual(IRI.create(owlID+rulename));

        if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, indrule))){
            
            OWLDataProperty prop = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasBodyAndHead"));
            Set<OWLLiteral> value = indrule.getDataPropertyValues(prop, owlmodel);

            rule.put(indrule.getIRI(),value.iterator().next().getLiteral());
    
        }else{
            log.error("The rule with name "+rulename+" doesn't exist.");
            return(null);
        }

        return rule;
    }

    /**
     * To return the IRI of the named rule with its string Body -> Head
     *
     * @param rulename {It is the IRI name of the rule}
     * @return {Return an HashMap with the IRI as a key and the rule string as value.}
     */
    public HashMap<IRI,String> getRule(IRI rulename){

        HashMap<IRI,String> rule = new HashMap();
        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
        OWLNamedIndividual indrule = factory.getOWLNamedIndividual(rulename);

        if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, indrule))){
            OWLDataProperty prop = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasBodyAndHead"));
            Set<OWLLiteral> value = indrule.getDataPropertyValues(prop, owlmodel);

            rule.put(indrule.getIRI(),value.iterator().next().getLiteral());

        }else{
            log.error("The rule with name "+rulename+" doesn't exist.");
            return(null);
        }

        return rule;
    }

   /**
     * Return all the rules stored inside the ontology
     *
     * @return {Return an HashMap with the IRI as a key and the rule string as value.}
     */
    public HashMap<IRI,String> getAllRules(){
        HashMap<IRI,String> rule = new HashMap();
        OWLDataProperty prop = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasBodyAndHead"));
        Iterator<OWLNamedIndividual> indaxiom = owlmodel.getIndividualsInSignature().iterator();

        while(indaxiom.hasNext()){
            OWLNamedIndividual ax = indaxiom.next();
            Set<OWLLiteral> value = ax.getDataPropertyValues(prop, owlmodel);
            if(!value.isEmpty())
                rule.put(ax.getIRI(),value.iterator().next().getLiteral());
        }

        return(rule);
    }

     /**
     * Return all the rules of a recipe stored inside the ontology
     *
     * @return {Return an HashMap with the IRI as a key and the rule string as value.}
     */
    public OWLOntology getAllRulesOfARecipe(IRI recipeIRI){
        ArrayList<IRI> ruleIRIs = new ArrayList<IRI>();
        OWLObjectProperty prop = factory.getOWLObjectProperty(IRI.create(owlIDrmi+"hasRule"));
        Iterator<OWLNamedIndividual> indaxiom = owlmodel.getIndividualsInSignature().iterator();

        OWLOntology ontlogy = null;
		try {
			ontlogy = OWLManager.createOWLOntologyManager().createOntology();
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		log.debug("RECIPE IRI : "+recipeIRI.toString());

		if(ontlogy != null){
	        OWLIndividual ind = factory.getOWLNamedIndividual(recipeIRI);
	        Set<OWLIndividual> rules = ind.getObjectPropertyValues(prop, owlmodel);

	        log.debug("Rules length : "+rules.size());
	        for(OWLIndividual rule : rules){
	        	OWLAxiom axiom = factory.getOWLObjectPropertyAssertionAxiom(prop, ind, rule);
	        	owlmanager.addAxiom(ontlogy, axiom);
	        }
		}

        /*while(indaxiom.hasNext()){
            OWLNamedIndividual ax = indaxiom.next();
            Set<OWLIndividual> value = ax.getObjectPropertyValues(prop, owlmodel);
            if(value != null){
            	for(OWLIndividual ind : value){
            		ruleIRIs.add(IRI.create(ind.toStringID()));
            	}
            }

        }*/

        return ontlogy;
    }



   /**
     * To get the Recipes where the rule is used.
     *
     * @param ruleName {The IRI of the rule}
     * @return {A Vector with the IRI of the recipes.}
     */
    public Vector<IRI> getRuleUsage(IRI ruleName){
        Vector<IRI> ruleusage = new Vector();
        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"KReSRule"));
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(ruleName);
        OWLEntityRemover remover = new OWLEntityRemover(owlmanager, Collections.singleton(owlmodel));
        OWLObjectProperty hasrule = factory.getOWLObjectProperty(IRI.create(owlIDrmi+"hasRule"));


        if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, ontoind))){

            ontoind.accept(remover);
            List<OWLOntologyChange> list = remover.getChanges();
            Iterator<OWLOntologyChange> iter = list.iterator();

            while(iter.hasNext()){
                OWLAxiom ax =iter.next().getAxiom();
                if(ax.getObjectPropertiesInSignature().contains(hasrule)){
                    String[] data = Arrays.toString(ax.getIndividualsInSignature().toArray()).split(",");
                    for(int s = 0; s<data.length;s++){
                        if(!data[s].contains(ontoind.getIRI().toString())){
                            String string = data[s].replace("<","").replace(">","").replace("[","").replace("]","").replace(" ","");
                            if(!string.startsWith("["))
                                ruleusage.add(IRI.create(string));
                        }
                    }  
                }
            }

            remover.reset();

            return(ruleusage);
        }else{
           log.error("The rule with name "+ruleName+" is not inside the ontology. Pleas check the name.");
           return(null);
        }
    }
    
   /**
     * To get the description of a rule 
     * @param ruleName {A IRI contains the full rule name}
     * @return {A string contains the description}
     */
    public String getDescription(IRI ruleName){
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(ruleName);
        OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasDescription"));
        Set<OWLLiteral> lit = ontoind.getDataPropertyValues(description, owlmodel);
        try{
           String string = lit.iterator().next().getLiteral();
           if(string!=null)
               return string;
           else
               return null;
        }catch(Exception e){
               return null;
        }
            
    }

    /**
     * To get the description of a rule
     * @param ruleName {A string contains the rule name}
     * @return {A string contains the description}
     */
    public String getDescription(String ruleName){
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID+ruleName));
        OWLDataProperty description = factory.getOWLDataProperty(IRI.create(owlIDrmi+"hasDescription"));
        Set<OWLLiteral> lit = ontoind.getDataPropertyValues(description, owlmodel);
        try{
           String string = lit.iterator().next().getLiteral();
           if(string!=null)
               return string;
           else
               return null;
        }catch(Exception e){
               return null;
        }

    }

}
