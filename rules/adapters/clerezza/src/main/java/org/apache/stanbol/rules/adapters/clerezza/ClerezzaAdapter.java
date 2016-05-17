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

package org.apache.stanbol.rules.adapters.clerezza;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.Expression;
import org.apache.clerezza.rdf.core.sparql.query.TriplePattern;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleConstructQuery;
import org.apache.clerezza.rdf.core.sparql.query.impl.SimpleGroupGraphPattern;
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
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.RecipeImpl;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

/**
 * 
 * The Rule Adapter for Clerezza.<br/>
 * This adapter allows to convert Stanbol Rules to Clerezza {@link ConstructQuery} objects.
 * 
 * @author anuzzolese
 * 
 */

@Component(immediate = true)
@Service(RuleAdapter.class)
public class ClerezzaAdapter extends AbstractRuleAdapter {

    public static final String ARTIFACT = "org.apache.stanbol.rules.adapters.clerezza.atoms";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    RuleStore ruleStore;

    @Reference
    RuleAdaptersFactory ruleAdaptersFactory;
    
    ComponentContext componentContext;
    
    /**
     * For OSGi environments.
     */
    public ClerezzaAdapter() {
        
    }

    /**
     * For non-OSGi environments.
     */
    public ClerezzaAdapter(Dictionary<String,Object> configuration,
                           RuleStore ruleStore,
                           RuleAdaptersFactory ruleAdaptersFactory) {
        this.ruleStore = ruleStore;
        this.ruleAdaptersFactory = ruleAdaptersFactory;

        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Failed to load configuration");
        }

        try {
            this.ruleAdaptersFactory.addRuleAdapter(this);
        } catch (UnavailableRuleObjectException e) {
            log.error("Failed to add the adapter to the registry.", e);
        }
        
        
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRecipeTo(Recipe recipe, Class<T> type) throws RuleAtomCallExeption,
                                                               UnsupportedTypeForExportException,
                                                               UnavailableRuleObjectException {

        List<ConstructQuery> constructQueries = null;

        if (type == ConstructQuery.class) {

            constructQueries = new ArrayList<ConstructQuery>();

            RuleList ruleList = recipe.getRuleList();
            Iterator<Rule> ruleIterator = ruleList.iterator();

            for (int i = 0; ruleIterator.hasNext(); i++) {
                constructQueries.add((ConstructQuery) adaptRuleTo(ruleIterator.next(), type));
            }

        } else {
            throw new UnsupportedTypeForExportException(
                    "The adapter for Clerezza does not support the selected serialization : "
                            + type.getCanonicalName());
        }

        return (T) constructQueries;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRuleTo(Rule rule, Class<T> type) throws RuleAtomCallExeption,
                                                         UnsupportedTypeForExportException,
                                                         UnavailableRuleObjectException {

        Set<TriplePattern> triplePatterns = new HashSet<TriplePattern>();

        List<Expression> expressions = new ArrayList<Expression>();

        Iterator<RuleAtom> it = rule.getBody().iterator();
        while (it.hasNext()) {
            RuleAtom ruleAtom = it.next();
            ClerezzaSparqlObject clerezzaSparqlObject = null;

            log.debug("Type to adapt {}", type);
            clerezzaSparqlObject = (ClerezzaSparqlObject) adaptRuleAtomTo(ruleAtom, type);

            Object clerezzaObj = clerezzaSparqlObject.getClerezzaObject();
            if (clerezzaObj instanceof TriplePattern) {
                triplePatterns.add((TriplePattern) clerezzaObj);
            } else if (clerezzaObj instanceof Expression) {
                expressions.add((Expression) clerezzaObj);
            }
        }

        SimpleGroupGraphPattern groupGraphPattern = new SimpleGroupGraphPattern();

        groupGraphPattern.addTriplePatterns(triplePatterns);

        for (Expression expression : expressions) {
            groupGraphPattern.addConstraint(expression);
        }

        triplePatterns = new HashSet<TriplePattern>();
        it = rule.getHead().iterator();
        while (it.hasNext()) {

            RuleAtom ruleAtom = it.next();

            ClerezzaSparqlObject clerezzaSparqlObject = (ClerezzaSparqlObject) adaptRuleAtomTo(ruleAtom, type);
            triplePatterns.add((TriplePattern) clerezzaSparqlObject.getClerezzaObject());

        }

        SimpleConstructQuery constructQuery = new SimpleConstructQuery(triplePatterns);
        constructQuery.setQueryPattern(groupGraphPattern);

        return (T) constructQuery;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRuleAtomTo(RuleAtom ruleAtom, Class<T> type) throws RuleAtomCallExeption,
                                                                     UnsupportedTypeForExportException,
                                                                     UnavailableRuleObjectException {

        if (type == ConstructQuery.class) {

            
            //ClassLoader loader = Thread.currentThread().getContextClassLoader();
            
            //log.info("loader : " + loader);
           
            
           

            String className = ruleAtom.getClass().getSimpleName();

            String canonicalName = ARTIFACT + "." + className;

            try {
                Class<AdaptableAtom> clerezzaAtomClass = (Class<AdaptableAtom>) Class.forName(canonicalName);

                try {
                    AdaptableAtom clerezzaAtom = clerezzaAtomClass.newInstance();

                    clerezzaAtom.setRuleAdapter(this);

                    return (T) clerezzaAtom.adapt(ruleAtom);

                } catch (InstantiationException e) {
                    log.error(e.getMessage(), e);
                } catch (IllegalAccessException e) {
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

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {

        log.info("in " + ClerezzaAdapter.class + " activate with context " + context);
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

        log.info("The Clerezza adapter for Stanbol Rules  is active", this);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + ClerezzaAdapter.class + " deactivate with context " + context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> getExportClass() {
        return (Class<T>) ConstructQuery.class;
    }

    @Override
    public <T> boolean canAdaptTo(Adaptable adaptable, Class<T> type) {
        if(type == ConstructQuery.class){
            return true;
        }
        else{
            return false;
        }
    }
    
    
    public static void main(String[] args){
        RuleAdapter ruleAdapter = new ClerezzaAdapter();
        try {
            KB kb = RuleParserImpl.parse("http://sssw.org/2012/rules/", new FileInputStream("/Users/mac/Documents/CNR/SSSW2012/construct/exercise3"));
            System.out.println("Rules: " + kb.getRuleList().size());
            Recipe recipe = new RecipeImpl(new IRI("http://sssw.org/2012/rules/"), "Recipe", kb.getRuleList());
            
            //List<ConstructQuery> jenaRules = (List<ConstructQuery>) ruleAdapter.adaptTo(recipe, ConstructQuery.class);
            
            String rules = "[ Exercise1: (http://dbpedia.org/resource/Madrid http://dbpedia.org/ontology/locationOf ?location) (?location rdf:type http://dbpedia.org/ontology/Museum) (?location http://dbpedia.org/ontology/numberOfVisitors ?visitors) greaterThan(?visitors '2000000'^^http://www.w3.org/2001/XMLSchema#integer) -> (?location rdf:type http://www.mytravels.com/Itinerary/MadridItinerary) ]";
            
            //List<com.hp.hpl.jena.reasoner.rulesys.Rule> jenaRules = com.hp.hpl.jena.reasoner.rulesys.Rule.parseRules(rules);
            
            
            String spqral = "CONSTRUCT " +
            "{ ?city a <http://www.mytravels.com/Itinerary/MovieCityItinerary> . " +
            "   ?city <http://www.w3.org/2000/01/rdf-schema#label> ?cLabel . " +
            "   ?event a <http://linkedevents.org/ontology/Event> . " +
            "   ?event <http://linkedevents.org/ontology/atPlace> ?location . " +
            "   ?location <http://www.w3.org/2000/01/rdf-schema#label> ?lLabel . " +
            "   ?location <http://www.w3.org/2002/07/owl#sameAs> ?city" +
            "} " +
            "WHERE " +
            "{ " +
            "   ?city a <http://www.mytravels.com/Itinerary/MovieCityItinerary> . " +
            "   ?city <http://www.w3.org/2000/01/rdf-schema#label> ?cLabel . " +
            "   ?event a <http://linkedevents.org/ontology/Event> . " +
            "   ?event <http://linkedevents.org/ontology/atPlace> ?location . " +
            "   ?location <http://www.w3.org/2000/01/rdf-schema#label> ?lLabel . " +
            "   FILTER(?lLabel = ?cLabel) " +            
            "}";
            Model m = ModelFactory.createDefaultModel();
            Model model = FileManager.get().loadModel("/Users/mac/Documents/CNR/SSSW2012/datasets_new/Exercise5_tmp.rdf");
            //for(ConstructQuery constructQuery : jenaRules){
                //Query query = QueryFactory.create(constructQuery.toString(), Syntax.syntaxARQ);
                Query query = QueryFactory.create(spqral, Syntax.syntaxARQ);
                QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
                
                //System.out.println(constructQuery.toString());
                m.add(queryExecution.execConstruct());
           //}
            
            FileOutputStream max = new FileOutputStream("/Users/mac/Documents/CNR/SSSW2012/datasets_new/example5.rdf");
            m.write(max);
            
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } /*catch (RuleAtomCallExeption e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnavailableRuleObjectException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedTypeForExportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
        
        
    }
    
    
}
