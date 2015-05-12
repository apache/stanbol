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

package org.apache.stanbol.rules.adapters.sparql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.rules.adapters.AbstractRuleAdapter;
import org.apache.stanbol.rules.adapters.AdaptableAtom;
import org.apache.stanbol.rules.base.api.Adaptable;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdaptersFactory;
import org.apache.stanbol.rules.base.api.RuleAtom;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SPARQL adapter.<br/>
 * This class implements the interface {@link RuleAdapter} and allows to adapt Stanbol recipes to object of
 * the {@link SPARQLObject}. This class returns the string serialization of the SPARQL query throw the methos
 * {@link SPARQLObject#getObject()}.<br/>
 * 
 * @author anuzzolese
 * 
 */

@Component(immediate = true)
@Service(RuleAdapter.class)
public class SPARQLAdapter extends AbstractRuleAdapter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String ARTIFACT = "org.apache.stanbol.rules.adapters.sparql.atoms";

    @Reference
    RuleStore ruleStore;

    @Reference
    RuleAdaptersFactory ruleAdaptersFactory;
    
    private ComponentContext componentContext;

    @SuppressWarnings("unchecked")
    protected <T> T adaptRecipeTo(Recipe recipe, Class<T> type) throws UnsupportedTypeForExportException,
                                                               UnavailableRuleObjectException {

        List<SPARQLObject> sparqlObjects = null;

        if (type == SPARQLObject.class) {

            RuleList ruleList = recipe.getRuleList();
            Iterator<Rule> ruleIterator = ruleList.iterator();

            sparqlObjects = new ArrayList<SPARQLObject>();
            for (int i = 0; ruleIterator.hasNext(); i++) {
                sparqlObjects.add((SPARQLObject) adaptRuleTo(ruleIterator.next(), type));
            }

        } else {
            throw new UnsupportedTypeForExportException(
                    "The SPARQL Export Provider does not support the selected serialization : "
                            + type.getCanonicalName());
        }

        return (T) sparqlObjects;
    }

    /*
     * public <T> T exportRecipe(String recipeID, Class<T> type) throws UnsupportedTypeForExportException,
     * UnavailableRuleObjectException, NoSuchRecipeException {
     * 
     * 
     * Recipe recipe; try { recipe = ruleStore.getRecipe(IRI.create(recipeID));
     * 
     * return exportRecipe(recipe, type); } catch (NoSuchRecipeException e) { throw e; }
     * 
     * 
     * }
     */

    @SuppressWarnings("unchecked")
    protected <T> T adaptRuleTo(Rule rule, Class<T> type) throws UnsupportedTypeForExportException,
                                                         UnavailableRuleObjectException {

        String sparql = "CONSTRUCT {";

        boolean firstIte = true;

        for (RuleAtom ruleAtom : rule.getHead()) {
            if (!firstIte) {
                sparql += " . ";
            }
            firstIte = false;
            sparql += ((SPARQLObject) adaptRuleAtomTo(ruleAtom, type)).getObject();
        }

        sparql += "} ";
        sparql += "WHERE {";

        firstIte = true;
        ArrayList<SPARQLObject> sparqlObjects = new ArrayList<SPARQLObject>();
        for (RuleAtom ruleAtom : rule.getBody()) {

            SPARQLObject tmp = ((SPARQLObject) adaptRuleAtomTo(ruleAtom, type));
            if (tmp instanceof SPARQLNot) {
                sparqlObjects.add(tmp);
            } else if (tmp instanceof SPARQLComparison) {
                sparqlObjects.add(tmp);
            } else {
                if (!firstIte) {
                    sparql += " . ";
                } else {
                    firstIte = false;
                }
                sparql += tmp.getObject();
            }
        }

        firstIte = true;

        String optional = "";
        String filter = "";
        for (SPARQLObject sparqlObj : sparqlObjects) {
            if (sparqlObj instanceof SPARQLNot) {
                SPARQLNot sparqlNot = (SPARQLNot) sparqlObj;
                if (!firstIte) {
                    optional += " . ";
                } else {
                    firstIte = false;
                }

                optional += sparqlNot.getObject();

                String[] filters = sparqlNot.getFilters();
                for (String theFilter : filters) {
                    if (!filter.isEmpty()) {
                        filter += " && ";
                    }
                    filter += theFilter;
                }
            } else if (sparqlObj instanceof SPARQLComparison) {
                SPARQLComparison sparqlDifferent = (SPARQLComparison) sparqlObj;

                String theFilter = sparqlDifferent.getObject();

                if (!filter.isEmpty()) {
                    filter += " && ";
                }

                filter += theFilter;
            }
        }

        if (!optional.isEmpty()) {
            sparql += " . OPTIONAL { " + optional + " } ";
        }
        if (!filter.isEmpty()) {
            sparql += " . FILTER ( " + filter + " ) ";
        }

        sparql += "}";

        return (T) new SPARQLQuery(sparql);
    }

    @SuppressWarnings("unchecked")
    protected <T> T adaptRuleAtomTo(RuleAtom ruleAtom, Class<T> type) throws UnsupportedTypeForExportException,
                                                                     UnavailableRuleObjectException {

        if (type == SPARQLObject.class) {

            String className = ruleAtom.getClass().getSimpleName();

            String canonicalName = ARTIFACT + "." + className;

            try {
                
                Class<AdaptableAtom> sparqlAtomClass = (Class<AdaptableAtom>) Class.forName(canonicalName);
                
                try {
                    AdaptableAtom sparqlAtom = sparqlAtomClass.newInstance();
                    sparqlAtom.setRuleAdapter(this);

                    return (T) sparqlAtom.adapt(ruleAtom);

                } catch (InstantiationException e) {
                    log.error(e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage(), e);
                } catch (RuleAtomCallExeption e) {
                    log.error(e.getMessage(), e);
                }

            } catch (ClassNotFoundException e) {
                log.error(e.getMessage(), e);
            } catch (SecurityException e) {
                log.error(e.getMessage(), e);
            }

        }

        return null;

    }

    /*
     * @Override public <T> boolean canExportRecipe(Class<T> type) { // TODO Auto-generated method stub return
     * false; }
     */

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {

        // context.getBundleContext().addServiceListener((RuleAdaptersFactoryImpl<SPARQLObject>)ruleAdaptersFactory);

        log.info("in " + SPARQLAdapter.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        
        componentContext = context;
        
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Should be called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        log.info("SPARQL Export Provider for Stanbol Rules  is active", this);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + SPARQLAdapter.class + " deactivate with context " + context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> getExportClass() {
        return (Class<T>) SPARQLObject.class;
    }

    @Override
    public <T> boolean canAdaptTo(Adaptable adaptable, Class<T> type) {
        if(type == SPARQLObject.class){
            return true;
        }
        else{
            return false;
        }
    }
}
