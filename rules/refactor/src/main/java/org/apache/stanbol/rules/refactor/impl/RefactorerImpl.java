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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.sparql.QueryEngine;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.clerezza.rdf.jena.sparql.JenaSparqlEngine;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.transformation.JenaToClerezzaConverter;

import org.apache.stanbol.rules.adapters.clerezza.ClerezzaAdapter;
import org.apache.stanbol.rules.adapters.impl.RuleAdaptersFactoryImpl;
import org.apache.stanbol.rules.adapters.impl.RuleAdaptersManagerImpl;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RecipeConstructionException;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdapterManager;
import org.apache.stanbol.rules.base.api.RuleAdaptersFactory;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.ClerezzaRuleStore;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.RecipeImpl;
import org.apache.stanbol.rules.manager.arqextention.CreatePropertyURIStringFromLabel;
import org.apache.stanbol.rules.manager.arqextention.CreateStandardLabel;
import org.apache.stanbol.rules.manager.arqextention.CreateURI;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.sparql.function.FunctionRegistry;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * The RefactorerImpl is the concrete implementation of the Refactorer interface defined in the rule APIs of
 * Stanbol. A Refacter is able to perform RDF graph refactorings and mappings.
 * 
 * @author anuzzolese
 * 
 */

@Component(immediate = true, metatype = true)
@Service(Refactorer.class)
public class RefactorerImpl implements Refactorer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    protected RuleStore ruleStore;

    @Reference
    protected TcManager tcManager;

    @Reference
    protected WeightedTcProvider weightedTcProvider;

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
     * @param weightedTcProvider
     * @param serializer
     * @param tcManager
     * @param semionManager
     * @param ruleStore
     * @param kReSReasoner
     * @param configuration
     */
    public RefactorerImpl(WeightedTcProvider weightedTcProvider,
                          TcManager tcManager,
                          RuleStore ruleStore,
                          RuleAdapterManager ruleAdapterManager,
                          Dictionary<String,Object> configuration) {
        this();
        this.weightedTcProvider = weightedTcProvider;
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

        this.weightedTcProvider = null;
        this.tcManager = null;
        this.ruleStore = null;
    }

    @Override
    public MGraph getRefactoredDataSet(UriRef uriRef) {

        return weightedTcProvider.getMGraph(uriRef);
    }

    /**
     * Execute a sparql construct on Clerezza.
     * 
     * @param sparql
     * @param datasetID
     * @return
     */
    private Graph sparqlConstruct(ConstructQuery constructQuery, UriRef datasetID) {

        MGraph graph = weightedTcProvider.getMGraph(datasetID);
        return sparqlConstruct(constructQuery, graph);

    }

    private Graph sparqlConstruct(ConstructQuery constructQuery, TripleCollection tripleCollection) {

        return tcManager.executeSparqlQuery(constructQuery, tripleCollection);

    }

    @SuppressWarnings("unchecked")
    @Override
    public void graphRefactoring(UriRef refactoredOntologyID, UriRef datasetID, UriRef recipeID) throws RefactoringException,
                                                                                                NoSuchRecipeException {

        Recipe recipe;
        try {
            try {
                recipe = ruleStore.getRecipe(recipeID);

                RuleAdapter ruleAdapter = ruleAdapterManager.getAdapter(recipe, ConstructQuery.class);
                List<ConstructQuery> constructQueries = (List<ConstructQuery>) ruleAdapter.adaptTo(recipe,
                    ConstructQuery.class);

                MGraph mGraph = tcManager.createMGraph(refactoredOntologyID);
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
    public TripleCollection graphRefactoring(UriRef graphID, UriRef recipeID) throws RefactoringException,
                                                                             NoSuchRecipeException {
        MGraph unionMGraph = null;

        // JenaToOwlConvert jenaToOwlConvert = new JenaToOwlConvert();

        // OntModel ontModel =
        // jenaToOwlConvert.ModelOwlToJenaConvert(inputOntology, "RDF/XML");

        Recipe recipe;
        try {
            recipe = ruleStore.getRecipe(recipeID);

            RuleAdapter ruleAdapter = ruleAdapterManager.getAdapter(recipe, ConstructQuery.class);

            List<ConstructQuery> constructQueries = (List<ConstructQuery>) ruleAdapter.adaptTo(recipe,
                ConstructQuery.class);

            unionMGraph = new SimpleMGraph();

            for (ConstructQuery constructQuery : constructQueries) {
                unionMGraph.addAll(this.sparqlConstruct(constructQuery, graphID));
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

        return unionMGraph.getGraph();

    }

    @SuppressWarnings("unchecked")
    @Override
    public TripleCollection graphRefactoring(TripleCollection inputGraph, Recipe recipe) throws RefactoringException {

        RuleAdapter ruleAdapter;
        try {
            ruleAdapter = ruleAdapterManager.getAdapter(recipe, ConstructQuery.class);
            List<ConstructQuery> constructQueries = (List<ConstructQuery>) ruleAdapter.adaptTo(recipe,
                ConstructQuery.class);

            for(ConstructQuery constructQuery : constructQueries){
                System.out.println(constructQuery.toString());
            }

            MGraph unionMGraph = new SimpleMGraph();
            for (ConstructQuery constructQuery : constructQueries) {
                unionMGraph.addAll(sparqlConstruct(constructQuery, inputGraph));
            }

            return unionMGraph;
        } catch (UnavailableRuleObjectException e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        } catch (UnsupportedTypeForExportException e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        } catch (RuleAtomCallExeption e) {
            throw new RefactoringException("The cause of the refactoring excpetion is: " + e.getMessage(), e);
        }

    }
    
    
    public static void main(String[] args){
        String graph =  "<http://revyu.com/things/eswc-2008-paper-exposing-large-sitemaps> <http://purl.org/stuff/rev#hasReview> <http://revyu.com/reviews/bbecea3192d3c3bc5473ca8d9ab38cb143314a8e> ." +
                        "<http://revyu.com/reviews/bbecea3192d3c3bc5473ca8d9ab38cb143314a8e> <http://purl.org/stuff/rev#reviewer> <http://revyu.com/people/ChrisBizer> . " +
                        "<http://people.apache.org/~alexdma> <http://xmlns.com/foaf/0.1/knows> <http://revyu.com/people/ChrisBizer> . " +
                        "<http://revyu.com/reviews/bbecea3192d3c3bc5473ca8d9ab38cb143314a8e> <http://purl.org/stuff/rev#rating> \"4\"^^<" + XSD.integer.getURI() + "> . ";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(graph.getBytes());
        
        Model model = ModelFactory.createDefaultModel();
        model.read(inputStream, null, "N3");
        MGraph deserializedGraph = JenaToClerezzaConverter.jenaModelToClerezzaMGraph(model);
        
        String recipeString =   "foaf = <http://xmlns.com/foaf/0.1/> . " +
                                "revyu = <http://purl.org/stuff/rev#> . " +
                                "myvoc = <http://www.semanticweb.org/voc/my/> . " +
                                "trustedContent[ " +
                                "has(revyu:hasReview, ?ci, ?review) . " +
                                "has(revyu:reviewer, ?review, ?y) . has(foaf:knows, ?x, ?y) . " +
                                "values(revyu:rating, ?review, ?rating) . gt(str(?rating), 3) " +
                                "-> has(myvoc:trustsContent, ?x, ?ci) " +
                                "]";
        
        KB kb = RuleParserImpl.parse("http://test.org/recipe", recipeString);
        
        Recipe recipe = new RecipeImpl(new UriRef("recipe"), "A recipe", kb.getRuleList());
        
        class SpecialTcManager extends TcManager {
            public SpecialTcManager(QueryEngine qe, WeightedTcProvider wtcp) {
                super();
                bindQueryEngine(qe);
                bindWeightedTcProvider(wtcp);
            }
        }

        QueryEngine qe = new JenaSparqlEngine();
        WeightedTcProvider wtcp = new SimpleTcProvider();
        TcManager tcm = new SpecialTcManager(qe, wtcp);

        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        RuleStore store = new ClerezzaRuleStore(configuration, tcm);

        Dictionary<String,Object> configuration2 = new Hashtable<String,Object>();

        RuleAdaptersFactory ruleAdaptersFactory = new RuleAdaptersFactoryImpl();

        Dictionary<String,Object> configuration3 = new Hashtable<String,Object>();
        new ClerezzaAdapter(configuration3, store, ruleAdaptersFactory);

        RuleAdapterManager ruleAdapterManager = new RuleAdaptersManagerImpl(configuration2,
                ruleAdaptersFactory);

        Dictionary<String,Object> configuration4 = new Hashtable<String,Object>();

        RefactorerImpl refactorer = new RefactorerImpl(wtcp, tcm, store, ruleAdapterManager, configuration4);
        try {
            MGraph mGraph = new SimpleMGraph();
            mGraph.addAll(refactorer.graphRefactoring(deserializedGraph, recipe));
            JenaToClerezzaConverter.clerezzaMGraphToJenaModel(mGraph).write(System.out);
        } catch (RefactoringException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

}
