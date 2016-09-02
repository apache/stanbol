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
package org.apache.stanbol.rules.refactor.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RecipeConstructionException;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdapterManager;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.arqextention.CreatePropertyURIStringFromLabel;
import org.apache.stanbol.rules.manager.arqextention.CreateStandardLabel;
import org.apache.stanbol.rules.manager.arqextention.CreateURI;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.sparql.function.FunctionRegistry;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;

/**
 * The RefactorerImpl is the concrete implementation of the Refactorer interface defined in the rule APIs of
 * Stanbol. A Refacter is able to perform RDF graph refactorings and mappings.
 * 
 * @author anuzzolese
 * 
 */

@Component(immediate = true)
@Service(Refactorer.class)
public class RefactorerImpl implements Refactorer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    protected RuleStore ruleStore;

    @Reference
    protected TcManager tcManager;

    @Reference
    protected RuleAdapterManager ruleAdapterManager;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the RefactorerImpl instances do need to be configured! YOU
     * NEED TO USE
     * {@link #RefactorerImpl(WeightedTcProvider, TcManager, RuleStore, RuleAdapterManager, Dictionary)}
     * or its overloads, to parse the configuration and then initialise the rule store if running outside a
     * OSGI environment.
     */
    public RefactorerImpl() {

    }

    /**
     * Basic constructor to be used if outside of an OSGi environment. Invokes default constructor.
     * 
     * @param serializer
     * @param tcManager
     * @param semionManager
     * @param ruleStore
     * @param kReSReasoner
     * @param configuration
     */
    public RefactorerImpl(TcManager tcManager,
                          RuleStore ruleStore,
                          RuleAdapterManager ruleAdapterManager,
                          Dictionary<String,Object> configuration) {
        this();
        this.tcManager = tcManager;
        this.ruleStore = ruleStore;
        this.ruleAdapterManager = ruleAdapterManager;
        activate(configuration);
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + getClass() + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    protected void activate(Dictionary<String,Object> configuration) {
        PropertyFunctionRegistry.get().put("http://www.stlab.istc.cnr.it/semion/function#createURI",
            CreateURI.class);
        FunctionRegistry.get().put("http://www.stlab.istc.cnr.it/semion/function#createLabel",
            CreateStandardLabel.class);
        FunctionRegistry.get().put("http://www.stlab.istc.cnr.it/semion/function#propString",
            CreatePropertyURIStringFromLabel.class);

        log.debug(Refactorer.class + "activated.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + getClass() + " deactivate with context " + context);

        this.tcManager = null;
        this.ruleStore = null;
    }

    @Override
    public Graph getRefactoredDataSet(IRI uriRef) {

        return tcManager.getGraph(uriRef);
    }

    /**
     * Execute a sparql construct on Clerezza.
     * 
     * @param sparql
     * @param datasetID
     * @return
     */
    private ImmutableGraph sparqlConstruct(ConstructQuery constructQuery, IRI datasetID) {

        Graph graph = tcManager.getGraph(datasetID);
        return sparqlConstruct(constructQuery, graph);

    }

    private ImmutableGraph sparqlConstruct(ConstructQuery constructQuery, Graph tripleCollection) {

        return tcManager.executeSparqlQuery(constructQuery, tripleCollection);

    }

    @SuppressWarnings("unchecked")
    @Override
    public void graphRefactoring(IRI refactoredOntologyID, IRI datasetID, IRI recipeID) throws RefactoringException,
                                                                                                NoSuchRecipeException {

        Recipe recipe;
        try {
            try {
                recipe = ruleStore.getRecipe(recipeID);

                RuleAdapter ruleAdapter = ruleAdapterManager.getAdapter(recipe, ConstructQuery.class);
                List<ConstructQuery> constructQueries = (List<ConstructQuery>) ruleAdapter.adaptTo(recipe,
                    ConstructQuery.class);

                Graph mGraph = tcManager.createGraph(refactoredOntologyID);
                for (ConstructQuery constructQuery : constructQueries) {
                    mGraph.addAll(this.sparqlConstruct(constructQuery, datasetID));
                }
            } catch (RecipeConstructionException e) {
                throw new RefactoringException(
                        "The cause of the refactoring excpetion is: " + e.getMessage(), e);
            } catch (UnavailableRuleObjectException e) {
                throw new RefactoringException(
                        "The cause of the refactoring excpetion is: " + e.getMessage(), e);
            } catch (UnsupportedTypeForExportException e) {
                throw new RefactoringException(
                        "The cause of the refactoring excpetion is: " + e.getMessage(), e);
            } catch (RuleAtomCallExeption e) {
                throw new RefactoringException(
                        "The cause of the refactoring excpetion is: " + e.getMessage(), e);
            }

        } catch (NoSuchRecipeException e1) {
            log.error("No Such recipe in the Rule Store", e1);
            throw e1;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Graph graphRefactoring(IRI graphID, IRI recipeID) throws RefactoringException,
                                                                             NoSuchRecipeException {
        Graph unionGraph = null;

        // JenaToOwlConvert jenaToOwlConvert = new JenaToOwlConvert();

        // OntModel ontModel =
        // jenaToOwlConvert.ModelOwlToJenaConvert(inputOntology, "RDF/XML");

        Recipe recipe;
        try {
            recipe = ruleStore.getRecipe(recipeID);

            RuleAdapter ruleAdapter = ruleAdapterManager.getAdapter(recipe, ConstructQuery.class);

            List<ConstructQuery> constructQueries = (List<ConstructQuery>) ruleAdapter.adaptTo(recipe,
                ConstructQuery.class);

            unionGraph = new SimpleGraph();

            for (ConstructQuery constructQuery : constructQueries) {
                unionGraph.addAll(this.sparqlConstruct(constructQuery, graphID));
            }

        } catch (NoSuchRecipeException e1) {
            log.error("Refactor : No Such recipe in the Rule Store", e1);
            throw e1;
        } catch (RecipeConstructionException e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        } catch (UnavailableRuleObjectException e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        } catch (UnsupportedTypeForExportException e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        } catch (RuleAtomCallExeption e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        }

        return unionGraph.getImmutableGraph();

    }

    @SuppressWarnings("unchecked")
    @Override
    public Graph graphRefactoring(Graph inputGraph, Recipe recipe) throws RefactoringException {

        RuleAdapter ruleAdapter;
        try {
            ruleAdapter = ruleAdapterManager.getAdapter(recipe, ConstructQuery.class);
            List<ConstructQuery> constructQueries = (List<ConstructQuery>) ruleAdapter.adaptTo(recipe,
                ConstructQuery.class);

            for(ConstructQuery constructQuery : constructQueries){
                System.out.println(constructQuery.toString());
            }

            Graph unionGraph = new SimpleGraph();
            for (ConstructQuery constructQuery : constructQueries) {
                unionGraph.addAll(sparqlConstruct(constructQuery, inputGraph));
            }

            return unionGraph;
        } catch (UnavailableRuleObjectException e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        } catch (UnsupportedTypeForExportException e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        } catch (RuleAtomCallExeption e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        }

    }


}
