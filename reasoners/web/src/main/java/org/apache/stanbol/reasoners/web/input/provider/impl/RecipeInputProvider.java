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
package org.apache.stanbol.reasoners.web.input.provider.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputProvider;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RecipeConstructionException;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdapterManager;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.semanticweb.owlapi.model.SWRLRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.reasoner.rulesys.Rule;
/**
 * Input provider which binds the reasoners input to the Rule module.
 * 
 * TODO Waiting for STANBOL-186, this provider only supports OWLApi reasoning services.
 * 
 * @author enridaga
 *
 */
public class RecipeInputProvider implements ReasoningServiceInputProvider {
    private final Logger log = LoggerFactory.getLogger(RecipeInputProvider.class);

    private RuleStore store;
    private RuleAdapterManager adapterManager;
    private String recipeId;

    /**
     * Constructor
     * 
     * @param store
     * @param recipeId
     */
    public RecipeInputProvider(RuleStore store, RuleAdapterManager adapterManager, String recipeId){
        this.store = store;
        this.adapterManager = adapterManager;
        this.recipeId = recipeId;
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public <T> Iterator<T> getInput(Class<T> type) throws IOException {
    	
    	ReasoningProvider reasoningProvider = null;
    	
    	if(type.isAssignableFrom(SWRLRule.class)){
    		reasoningProvider = ReasoningProvider.OWL2;
    	}
    	else if(type.isAssignableFrom(Rule.class)){
    		reasoningProvider = ReasoningProvider.Jena;
    	}
    	else{
            log.error("Cannot adapt to this type {}", type.getCanonicalName());
            throw new UnsupportedOperationException("Cannot adapt to " + type.getCanonicalName());
        }
    	
    	switch (reasoningProvider) {
		case OWL2:
			List<SWRLRule> rules = null;
	        if (recipeId != null) {
	            long start = System.currentTimeMillis();
	            log.info("[start] Prepare rules for OWLApi ");

	         // If recipe exists, return it as a list of SWRL rules
	            rules = new ArrayList<SWRLRule>();
	            try {
	                Recipe recipe = null;
	                synchronized (store) {
	                    try {
							recipe = store.getRecipe(new IRI(recipeId));
						} catch (RecipeConstructionException e) {
							log.error("An error occurred while generating the recipe.", e);
						}                    
	                }
	                log.debug("Recipe is: {}", recipe);
	                
	                /*
	                 * We ask to the adapter manager to get the right adapter in order to transform
	                 * recipes into SWRLRule objects.
	                 */
	                RuleAdapter adapter;
					try {
						adapter = adapterManager.getAdapter(recipe, SWRLRule.class);
						rules = (List<SWRLRule>) adapter.adaptTo(recipe, SWRLRule.class);
					} catch (UnavailableRuleObjectException e) {
						log.error(e.getMessage(), e);
					} catch (RuleAtomCallExeption e) {
						log.error(e.getMessage(), e);
					} catch (UnsupportedTypeForExportException e) {
						log.error(e.getMessage(), e);
					}
	                
	                
	                /*
	                RuleList ruleList = recipe.getRuleList();
	                log.debug("RuleList is: {}",ruleList);
	                for(org.apache.stanbol.rules.base.api.Rule r : ruleList ){
	                    SWRLRule swrl = r.toSWRL(OWLManager.getOWLDataFactory());
	                    log.debug("Prepared rule: {}",swrl);
	                    rules.add(swrl);
	                }*/
	            } catch (NoSuchRecipeException e) {
	                log.error("Recipe {} does not exists", recipeId);
	                throw new IOException(e);
	            }

	            long end = System.currentTimeMillis();
	            log.info("[end] Prepared {} rules for OWLApi in {} ms.", rules.size(), (end - start));
	            
	        }
	        if(rules == null){
	            log.error("No rules have been loaded");
	            throw new IOException("No rules loaded");
	        }
	        final Iterator<SWRLRule> iterator = Collections.unmodifiableList(rules).iterator();
	        return new Iterator<T>(){

	            @Override
	            public boolean hasNext() {
	                return iterator.hasNext();
	            }

	            @Override
	            public T next() {
	                return (T) iterator.next();
	            }

	            @Override
	            public void remove() {
	                log.error("Cannot remove items from this iterator. This may be cused by an error in the program");
	                throw new UnsupportedOperationException("Cannot remove items from this iterator");
	            }
	            
	        };
		case Jena:
			List<Rule> jenaRules = null;
	        if (recipeId != null) {
	            long start = System.currentTimeMillis();
	            log.info("[start] Prepare rules for Jena ");

	            try {
	                Recipe recipe = null;
	                synchronized (store) {
	                    try {
							recipe = store.getRecipe(new IRI(recipeId));
						} catch (RecipeConstructionException e) {
							log.error("An error occurred while generating the recipe.", e);
						}                    
	                }
	                
	                
	                if(recipe != null){
		                log.debug("Recipe is: {}", recipe);
		                
		                /*
		                 * We ask to the adapter manager to get the right adapter in order to transform
		                 * recipes into Jena Rule objects.
		                 */
		                RuleAdapter adapter;
						try {
							adapter = adapterManager.getAdapter(recipe, Rule.class);
							jenaRules = (List<Rule>) adapter.adaptTo(recipe, Rule.class);
						} catch (UnavailableRuleObjectException e) {
							log.error(e.getMessage(), e);
						} catch (RuleAtomCallExeption e) {
							log.error(e.getMessage(), e);
						} catch (UnsupportedTypeForExportException e) {
							log.error(e.getMessage(), e);
						}
		                
	                }
	                
	                //jenaRules = recipe.toJenaRules();
	            } catch (NoSuchRecipeException e) {
	                log.error("Recipe {} does not exists", recipeId);
	                throw new IOException(e);
	            }
	            
	            
	            long end = System.currentTimeMillis();
	            log.info("[end] Prepared {} rules for Jena in {} ms.", jenaRules.size(), (end - start));
	            
	        }
	        if(jenaRules == null){
	            log.error("No rules have been loaded");
	            throw new IOException("No rules loaded");
	        }
	        final Iterator<Rule> jRiterator = Collections.unmodifiableList(jenaRules).iterator();
	        return new Iterator<T>(){

	            @Override
	            public boolean hasNext() {
	                return jRiterator.hasNext();
	            }

	            @Override
	            public T next() {
	                return (T) jRiterator.next();
	            }

	            @Override
	            public void remove() {
	                log.error("Cannot remove items from this iterator. This may be cused by an error in the program");
	                throw new UnsupportedOperationException("Cannot remove items from this iterator");
	            }
	            
	        };
		default:
			
			return null;
			
		}
    	
    	   
    }

    @Override
    public <T> boolean adaptTo(Class<T> type) {
        if(type.isAssignableFrom(SWRLRule.class)){
        	return true;
        }
        else if(type.isAssignableFrom(Rule.class)){
        	return true;
        }
        return false;
    }

}
