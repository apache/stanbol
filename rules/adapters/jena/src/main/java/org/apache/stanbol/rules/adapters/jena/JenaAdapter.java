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

package org.apache.stanbol.rules.adapters.jena;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.rules.adapters.AbstractRuleAdapter;
import org.apache.stanbol.rules.adapters.AdaptableAtom;
import org.apache.stanbol.rules.adapters.jena.atoms.VariableAtom;
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
import org.apache.stanbol.rules.base.api.util.AtomList;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.RecipeImpl;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.JenaRuntime;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import com.hp.hpl.jena.reasoner.rulesys.OWLFBRuleReasonerFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;
import com.ibm.icu.util.Output;

/**
 * The adapter for Jena rules. <br/>
 * The output object of this adapter is a {@link com.hp.hpl.jena.reasoner.rulesys.Rule} instance.<br/>
 * For that reason the adaptTo method works only if the second argument is
 * <code>com.hp.hpl.jena.reasoner.rulesys.Rule.class</code>
 * 
 * @author anuzzolese
 * 
 */

@Component(immediate = true)
@Service(RuleAdapter.class)
public class JenaAdapter extends AbstractRuleAdapter {

    public static final String ARTIFACT = "org.apache.stanbol.rules.adapters.jena.atoms";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    RuleStore ruleStore;

    @Reference
    RuleAdaptersFactory ruleAdaptersFactory;
    
    private ComponentContext componentContext;
    
    public Map<String,Integer> variableMap = new HashMap<String,Integer>();
    
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRecipeTo(Recipe recipe, Class<T> type) throws RuleAtomCallExeption,
                                                               UnsupportedTypeForExportException,
                                                               UnavailableRuleObjectException {

        List<com.hp.hpl.jena.reasoner.rulesys.Rule> jenaRules = null;

        if (type == com.hp.hpl.jena.reasoner.rulesys.Rule.class) {

            RuleList ruleList = recipe.getRuleList();
            Iterator<Rule> ruleIterator = ruleList.iterator();

            jenaRules = new ArrayList<com.hp.hpl.jena.reasoner.rulesys.Rule>();
            for (int i = 0; ruleIterator.hasNext(); i++) {
                jenaRules.add((com.hp.hpl.jena.reasoner.rulesys.Rule) adaptRuleTo(ruleIterator.next(), type));
            }

        } else {
            throw new UnsupportedTypeForExportException(
                    "The Jena Adapter does not support the selected serialization : "
                            + type.getCanonicalName());
        }

        return (T) jenaRules;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRuleTo(Rule rule, Class<T> type) throws RuleAtomCallExeption,
                                                         UnsupportedTypeForExportException,
                                                         UnavailableRuleObjectException {

        if (type == com.hp.hpl.jena.reasoner.rulesys.Rule.class) {
            AtomList bodyAtomList = rule.getBody();
            AtomList headAtomList = rule.getHead();

            List<ClauseEntry> headClauseEntries = new ArrayList<ClauseEntry>();
            List<ClauseEntry> bodyClauseEntries = new ArrayList<ClauseEntry>();

            
            variableMap = new HashMap<String,Integer>();
            
            Iterator<RuleAtom> it = headAtomList.iterator();
            while (it.hasNext()) {
                RuleAtom atom = it.next();
                ClauseEntry clauseEntry = adaptRuleAtomTo(atom, com.hp.hpl.jena.reasoner.rulesys.Rule.class);

                if (clauseEntry instanceof HigherOrderClauseEntry) {
                    List<ClauseEntry> clauseEntries = ((HigherOrderClauseEntry) clauseEntry)
                            .getClauseEntries();

                    for (ClauseEntry ce : clauseEntries) {
                        headClauseEntries.add(ce);
                    }
                } else {
                    headClauseEntries.add(clauseEntry);
                }
            }

            it = bodyAtomList.iterator();
            while (it.hasNext()) {
                RuleAtom atom = it.next();
                ClauseEntry clauseEntry = adaptRuleAtomTo(atom, com.hp.hpl.jena.reasoner.rulesys.Rule.class);

                if (clauseEntry instanceof HigherOrderClauseEntry) {
                    List<ClauseEntry> clauseEntries = ((HigherOrderClauseEntry) clauseEntry)
                            .getClauseEntries();

                    for (ClauseEntry ce : clauseEntries) {
                        bodyClauseEntries.add(ce);
                    }
                } else {
                    bodyClauseEntries.add(clauseEntry);
                }
            }

            return (T) new com.hp.hpl.jena.reasoner.rulesys.Rule(rule.getRuleName(), headClauseEntries,
                    bodyClauseEntries);
        } else {
            throw new UnsupportedTypeForExportException("The adapter " + getClass()
                                                        + " does not support type : "
                                                        + type.getCanonicalName());
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    protected <T> T adaptRuleAtomTo(RuleAtom ruleAtom, Class<T> type) throws RuleAtomCallExeption,
                                                                     UnsupportedTypeForExportException,
                                                                     UnavailableRuleObjectException {

        if (type == com.hp.hpl.jena.reasoner.rulesys.Rule.class) {

            String className = ruleAtom.getClass().getSimpleName();

            String canonicalName = ARTIFACT + "." + className;
            
            try {
                
                
                Class<AdaptableAtom> jenaAtomClass = (Class<AdaptableAtom>) Class.forName(canonicalName);
                
                try 
                
                {
                    
                    
                    AdaptableAtom jenaAtom = jenaAtomClass.newInstance();
                    
                    if(jenaAtom instanceof VariableAtom){
                        System.out.println("Class equals");
                    }

                    jenaAtom.setRuleAdapter(this);

                    return (T) jenaAtom.adapt(ruleAtom);

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

        } else {
            throw new UnsupportedTypeForExportException("The adapter " + getClass()
                                                        + " does not support type : "
                                                        + type.getCanonicalName());
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

        log.info("in " + JenaAdapter.class + " activate with context " + context);
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

        log.info("The Jena Adapter for Stanbol Rules  is active", this);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + JenaAdapter.class + " deactivate with context " + context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> getExportClass() {
        return (Class<T>) com.hp.hpl.jena.reasoner.rulesys.Rule.class;
    }

    @Override
    public <T> boolean canAdaptTo(Adaptable adaptable, Class<T> type) {
        if(type == com.hp.hpl.jena.reasoner.rulesys.Rule.class){
            return true;
        }
        else{
            return false;
        }
    }
    
    public static void main(String[] args){
        RuleAdapter ruleAdapter = new JenaAdapter();
        try {
            KB kb = RuleParserImpl.parse("http://sssw.org/2012/rules/", new FileInputStream("/Users/mac/Documents/CNR/SSSW2012/rules/exercise1"));
            System.out.println("Rules: " + kb.getRuleList().size());
            Recipe recipe = new RecipeImpl(new IRI("http://sssw.org/2012/rules/"), "Recipe", kb.getRuleList());
            
            List<com.hp.hpl.jena.reasoner.rulesys.Rule> jenaRules = (List<com.hp.hpl.jena.reasoner.rulesys.Rule>) ruleAdapter.adaptTo(recipe, com.hp.hpl.jena.reasoner.rulesys.Rule.class);
            
            String rules = "[ Exercise1: (http://dbpedia.org/resource/Madrid http://dbpedia.org/ontology/locationOf ?location) (?location rdf:type http://dbpedia.org/ontology/Museum) (?location http://dbpedia.org/ontology/numberOfVisitors ?visitors) greaterThan(?visitors '2000000'^^http://www.w3.org/2001/XMLSchema#integer) -> (?location rdf:type http://www.mytravels.com/Itinerary/MadridItinerary) ]";
            
            //List<com.hp.hpl.jena.reasoner.rulesys.Rule> jenaRules = com.hp.hpl.jena.reasoner.rulesys.Rule.parseRules(rules);
            for(com.hp.hpl.jena.reasoner.rulesys.Rule jenaRule : jenaRules){
                System.out.println(jenaRule.toString());
            }
            
            Model m = ModelFactory.createDefaultModel();
            
            Resource configuration =  m.createResource();
            configuration.addProperty(ReasonerVocabulary.PROPruleMode, "hybrid");
            
            //Model model = FileManager.get().loadModel("/Users/mac/Documents/workspaceMyStanbol/sssw2012/events.rdf");
            Model model = FileManager.get().loadModel("/Users/mac/Documents/CNR/SSSW2012/datasets_new/Exercise1.rdf");
            //GenericRuleReasoner reasoner = new GenericRuleReasoner(jenaRules);
            
            //GenericRuleReasoner reasoner = new GenericRuleReasoner(com.hp.hpl.jena.reasoner.rulesys.Rule.parseRules(rules));
            GenericRuleReasoner reasoner = new GenericRuleReasoner(jenaRules);
            
            reasoner.setOWLTranslation(true);               // not needed in RDFS case
            reasoner.setTransitiveClosureCaching(true);
            
            
            InfModel infModel = ModelFactory.createInfModel(reasoner, model);
            
            infModel.prepare();
            infModel.getDeductionsModel().write(System.out);
            //String sparql = "select * where {?s a <http://www.mytravels.com/Itinerary/MovieCityMuseums> }";
            //String sparql = "select * where {?s a <http://www.mytravels.com/Itinerary/CityEventItinerary> }";
            String sparql = "select * where {?s a <http://www.mytravels.com/Itinerary/MadridItinerary> }";
            //String sparql = "select * where {?s a <http://linkedevents.org/ontology/cazzo> }"; 
            //String sparql = "select * where {?s a <http://www.mytravels.com/Itinerary/MovieCityItinerary> }";
            
            Query query = QueryFactory.create(sparql, Syntax.syntaxARQ);
            QueryExecution queryExecution = QueryExecutionFactory.create(query, infModel);
            
            com.hp.hpl.jena.query.ResultSet resultSet = queryExecution.execSelect();
            
            ResultSetFormatter.out(System.out, resultSet);
            
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RuleAtomCallExeption e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnavailableRuleObjectException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedTypeForExportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }
    
    public Map<String,Integer> getVariableMap() {
        return variableMap;
    }
}
