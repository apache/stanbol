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
import java.util.Set;
import java.util.Vector;

import org.apache.stanbol.rules.base.api.RuleStore;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author elvio
 */
public class GetRecipe {
    
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
    public GetRecipe(RuleStore store){
        this.owlmodel = store.getOntology();
        this.owlIDrmi = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";
        this.owlID = owlmodel.getOntologyID().toString().replace("<","").replace(">","")+"#";
        this.owlmanager = owlmodel.getOWLOntologyManager();
        this.factory = owlmanager.getOWLDataFactory();
    }

   /**
     * This method returns the IRI of the named recipe with its sequence string
     *
     * @param recipename {It is the string name of the recipe}
     * @return {Return an HashMap with the IRI as a key and the sequence of rules as value.}
     */
    public HashMap<IRI,String> getRecipe(String recipename){

        HashMap<IRI,String> recipe = new HashMap();
        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"Recipe"));
        OWLNamedIndividual indrecipe = factory.getOWLNamedIndividual(IRI.create(owlID+recipename));

        if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, indrecipe))){
          
            OWLDataProperty prop = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasSequence"));
            Set<OWLLiteral> value = indrecipe.getDataPropertyValues(prop, owlmodel);
            if(!value.isEmpty()){
               
                    recipe.put(indrecipe.getIRI(),value.iterator().next().getLiteral());
            }else{
                recipe.put(indrecipe.getIRI(),"");
            }

        }else{
            log.error("The recipe with name "+recipename+" doesn't exist.");
            return(null);
        }

        return recipe;
    }

    /**
     * This method returns the IRI of the named recipe with its sequence string
     *
     * @param recipename {It is the IRI name of the recipe}
     * @return {Return an HashMap with the IRI as a key and the sequence of rules as value.}
     */
    public HashMap<IRI,String> getRecipe(IRI recipename){

        HashMap<IRI,String> recipe = new HashMap();
        OWLClass ontocls = factory.getOWLClass(IRI.create(owlIDrmi+"Recipe"));
        OWLNamedIndividual indrecipe = factory.getOWLNamedIndividual(recipename);

        if(owlmodel.containsAxiom(factory.getOWLClassAssertionAxiom(ontocls, indrecipe))){
            OWLDataProperty prop = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasSequence"));
            Set<OWLLiteral> value = indrecipe.getDataPropertyValues(prop, owlmodel);
            if(!value.isEmpty()){
                    recipe.put(indrecipe.getIRI(),value.iterator().next().getLiteral());
            }else{
                recipe.put(indrecipe.getIRI(),"");
            }

        }else{
           log.error("The recipe with name "+recipename+" doesn't exist.");
            return(null);
        }

        return recipe;
    }

   /**
     * This methods returns all the recipes with their sequence stored inside the ontology
     *
     * @return {Return an HashMap with the IRI as a key and the sequence of rules value.}
     */
    public HashMap<IRI,String> getAllRecipes(){
        HashMap<IRI,String> recipe = new HashMap();
        OWLDataProperty prop = factory.getOWLDataProperty(IRI.create(owlIDrmi + "hasSequence"));
        Iterator<OWLNamedIndividual> indaxiom = owlmodel.getIndividualsInSignature().iterator();

        while(indaxiom.hasNext()){
            OWLNamedIndividual ax = indaxiom.next();
            Set<OWLLiteral> value = ax.getDataPropertyValues(prop, owlmodel);
            if(!value.isEmpty())
                recipe.put(ax.getIRI(),value.iterator().next().getLiteral());
        }

        return(recipe);
    }

    /**
     * This methods returns all the recipes with their sequence stored inside the ontology
     *
     * @return {Return an HashMap with the IRI as a key and the sequence of rules value.}
     */
    public Vector<IRI> getGeneralRecipes(){
        Vector<IRI> recipe = new Vector();
        OWLClass recipeclas = factory.getOWLClass(IRI.create(owlIDrmi + "Recipe"));
        Iterator<OWLIndividual> indaxiom = recipeclas.getIndividuals(owlmodel).iterator();
        while(indaxiom.hasNext()){
            OWLIndividual axind = indaxiom.next();
                recipe.add(IRI.create(axind.toStringID()));
        }

        return(recipe);
    }

    /**
     * This methods returns a map contains the count of the seuqnce compound of two rule
     *
     * @return {Return an HashMap with the IRI as a key and the sequence of rules value.}
     */
    public HashMap<String,Integer> getBinSequenceRecipeCount(){
        HashMap<String,Integer> recipe = new HashMap();
        HashMap<IRI, String> map = getAllRecipes();
        Iterator<IRI> keys = map.keySet().iterator();

        while(keys.hasNext()){
            IRI iri = keys.next();
            String[] sequence = map.get(iri).split(",");
            String bin ="";
            for(int i = 0; i<sequence.length-1;i++){
                bin = sequence[i].replace(" ","").trim()+" precedes "+ sequence[i+1].replace(" ","").trim();
                if(recipe.containsKey(bin)){
                    recipe.put(bin,recipe.get(bin)+1);
                }else{
                    recipe.put(bin,1);
                }
            }
        }

        return(recipe);
    }

    /**
     * To get the description of a recipe
     * @param recipeName {A IRI contains the full recipe name}
     * @return {A string contains the description}
     */
    public String getDescription(IRI recipeName){
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(recipeName);
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
     * To get the description of a recipe
     * @param recipeName {A string contains the recipe name}
     * @return {A string contains the description}
     */
    public String getDescription(String recipeName){
        OWLNamedIndividual ontoind = factory.getOWLNamedIndividual(IRI.create(owlID+recipeName));
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
