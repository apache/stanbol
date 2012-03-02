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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.impl.SimpleGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.transformation.JenaToClerezzaConverter;
import org.apache.stanbol.commons.owl.transformation.JenaToOwlConvert;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.arqextention.CreatePropertyURIStringFromLabel;
import org.apache.stanbol.rules.manager.arqextention.CreateStandardLabel;
import org.apache.stanbol.rules.manager.arqextention.CreateURI;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.apache.stanbol.rules.refactor.api.util.URIGenerator;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.sparql.function.FunctionRegistry;
import com.hp.hpl.jena.sparql.pfunction.PropertyFunctionRegistry;
import com.hp.hpl.jena.update.UpdateAction;

class ForwardChainingRefactoringGraph {

    private MGraph inputGraph;
    private Graph outputGraph;

    public ForwardChainingRefactoringGraph(MGraph inputGraph, Graph outputGraph) {
        this.inputGraph = inputGraph;
        this.outputGraph = outputGraph;
    }

    public MGraph getInputGraph() {
        return inputGraph;
    }

    public Graph getOutputGraph() {
        return outputGraph;
    }

}

/**
 * The RefactorerImpl is the concrete implementation of the Refactorer interface defined in the KReS APIs. A
 * SemionRefacter is able to perform ontology refactorings and mappings.
 * 
 * @author andrea.nuzzolese
 * 
 */

@Component(immediate = true, metatype = true)
@Service(Refactorer.class)
public class RefactorerImpl implements Refactorer {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Reference
    protected RuleStore ruleStore;

    
    @Reference
    protected Serializer serializer;

    @Reference
    protected TcManager tcManager;

    @Reference
    protected WeightedTcProvider weightedTcProvider;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the RefactorerImpl instances do need to be configured! YOU
     * NEED TO USE
     * {@link #RefactorerImpl(WeightedTcProvider, Serializer, TcManager, ONManager, SemionManager, RuleStore, Reasoner, Dictionary)}
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
     * @param onManager
     * @param semionManager
     * @param ruleStore
     * @param kReSReasoner
     * @param configuration
     */
    public RefactorerImpl(WeightedTcProvider weightedTcProvider,
                          Serializer serializer,
                          TcManager tcManager,
                          RuleStore ruleStore,
                          Dictionary<String,Object> configuration) {
        this();
        this.weightedTcProvider = weightedTcProvider;
        this.serializer = serializer;
        this.tcManager = tcManager;
        this.ruleStore = ruleStore;
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
        this.serializer = null;
        this.tcManager = null;
        this.ruleStore = null;
    }

    private ForwardChainingRefactoringGraph forwardChainingOperation(String query, MGraph mGraph) {

        Graph graph = kReSCoreOperation(query, mGraph);

        mGraph.addAll(graph);

        return new ForwardChainingRefactoringGraph(mGraph, graph);
    }

    @Override
    public MGraph getRefactoredDataSet(UriRef uriRef) {

        return weightedTcProvider.getMGraph(uriRef);
    }

    private Graph kReSCoreOperation(String query, MGraph mGraph) {

        /*
         * 
         * Graph constructedGraph = null; try { ConstructQuery constructQuery = (ConstructQuery)
         * QueryParser.getInstance() .parse(query); constructedGraph = tcManager.executeSparqlQuery(
         * constructQuery, mGraph);
         * 
         * } catch (ParseException e) { log.error(e.getMessage()); } catch (NoQueryEngineException e) {
         * log.error(e.getMessage()); }
         * 
         * return constructedGraph;
         */

        Model model = JenaToClerezzaConverter.clerezzaMGraphToJenaModel(mGraph);

        Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);
        QueryExecution qexec = QueryExecutionFactory.create(sparqlQuery, model);

        return JenaToClerezzaConverter.jenaModelToClerezzaMGraph(qexec.execConstruct()).getGraph();

    }

    /**
     * Method borrowed from the old ontonet ClerezzaStorage
     * 
     * @param sparql
     * @param datasetURI
     * @return
     */
    private OWLOntology sparqlConstruct(String sparql, String datasetURI) {

        org.apache.clerezza.rdf.core.sparql.query.Query query;
        MGraph mGraph = new SimpleMGraph();
        try {
            query = QueryParser.getInstance().parse(sparql);
            UriRef datasetUriRef = new UriRef(datasetURI);
            MGraph dataset = weightedTcProvider.getMGraph(datasetUriRef);
            mGraph.addAll((SimpleGraph) tcManager.executeSparqlQuery(query, dataset));
        } catch (ParseException e) {
            log.error("Unable to execute SPARQL. ", e);
        }

        Model om = JenaToClerezzaConverter.clerezzaMGraphToJenaModel(mGraph);
        JenaToOwlConvert converter = new JenaToOwlConvert();

        return converter.ModelJenaToOwlConvert(om, "RDF/XML");
    }

    @Override
    public void ontologyRefactoring(IRI refactoredOntologyIRI, IRI datasetURI, IRI recipeIRI) throws RefactoringException,
                                                                                             NoSuchRecipeException {

        OWLOntology refactoredOntology = null;

        // ClerezzaOntologyStorage ontologyStorage = onManager.getOntologyStore();

        Recipe recipe;
        try {
            recipe = ruleStore.getRecipe(recipeIRI);

            RuleList kReSRuleList = recipe.getkReSRuleList();

            OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

            String fingerPrint = "";
            for (Rule kReSRule : kReSRuleList) {
                String sparql = kReSRule.toSPARQL();
                OWLOntology refactoredDataSet = /* ontologyStorage */this.sparqlConstruct(sparql,
                    datasetURI.toString());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    ontologyManager.saveOntology(refactoredDataSet, new RDFXMLOntologyFormat(), out);
                    if (refactoredOntologyIRI == null) {
                        ByteArrayOutputStream fpOut = new ByteArrayOutputStream();
                        fingerPrint += URIGenerator.createID("", fpOut.toByteArray());
                    }

                } catch (OWLOntologyStorageException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

                try {
                    ontologyManager.loadOntologyFromOntologyDocument(in);
                } catch (OWLOntologyCreationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            if (refactoredOntologyIRI == null) {
                refactoredOntologyIRI = IRI.create(URIGenerator.createID("urn://", fingerPrint.getBytes()));
            }
            OWLOntologyMerger merger = new OWLOntologyMerger(ontologyManager);

            try {

                refactoredOntology = merger.createMergedOntology(ontologyManager, refactoredOntologyIRI);

                /* ontologyStorage. */store(refactoredOntology);

            } catch (OWLOntologyCreationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } catch (NoSuchRecipeException e1) {
            log.error("SemionRefactorer : No Such recipe in the KReS Rule Store", e1);
            throw e1;
        }

        if (refactoredOntology == null) {
            throw new RefactoringException();
        }
    }

    /**
     * Method borrowed from the old ontonet ClerezzaStorage
     * 
     * @param o
     */
    private void store(OWLOntology o) {
        // // Why was it using two converters earlier?
        // JenaToOwlConvert converter = new JenaToOwlConvert();
        // OntModel om = converter.ModelOwlToJenaConvert(o, "RDF/XML");
        // MGraph mg = JenaToClerezzaConverter.jenaModelToClerezzaMGraph(om);
        TripleCollection mg = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(o);
        MGraph mg2 = null;
        IRI iri = OWLUtils.guessOntologyIdentifier(o);
        UriRef ref = new UriRef(iri.toString());
        try {
            mg2 = tcManager.createMGraph(ref);
        } catch (EntityAlreadyExistsException ex) {
            log.info("Entity " + ref + " already exists in store. Replacing...");
            mg2 = tcManager.getMGraph(ref);
        }

        mg2.addAll(mg);
    }

    @Override
    public OWLOntology ontologyRefactoring(OWLOntology inputOntology, IRI recipeIRI) throws RefactoringException,
                                                                                    NoSuchRecipeException {
        OWLOntology refactoredOntology = null;

        // JenaToOwlConvert jenaToOwlConvert = new JenaToOwlConvert();

        // OntModel ontModel =
        // jenaToOwlConvert.ModelOwlToJenaConvert(inputOntology, "RDF/XML");

        Recipe recipe;
        try {
            recipe = ruleStore.getRecipe(recipeIRI);

            RuleList kReSRuleList = recipe.getkReSRuleList();
            log.info("RULE LIST SIZE : " + kReSRuleList.size());

            MGraph unionMGraph = new SimpleMGraph();

            TripleCollection mGraph = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(inputOntology);

            for (Rule kReSRule : kReSRuleList) {
                String sparql = kReSRule.toSPARQL();
                log.info("SPARQL : " + sparql);

                Graph constructedGraph = null;

                switch (kReSRule.getExpressiveness()) {
                    case KReSCore:
                        if (mGraph instanceof MGraph) constructedGraph = kReSCoreOperation(sparql,
                            (MGraph) mGraph);
                        break;
                    case ForwardChaining:
                        if (mGraph instanceof MGraph) {
                            ForwardChainingRefactoringGraph forwardChainingRefactoringGraph = forwardChainingOperation(
                                sparql, (MGraph) mGraph);
                            constructedGraph = forwardChainingRefactoringGraph.getOutputGraph();
                            mGraph = forwardChainingRefactoringGraph.getInputGraph();
                        }
                        break;
                    case Reflexive:
                        constructedGraph = kReSCoreOperation(sparql, unionMGraph);
                        break;
                    case SPARQLConstruct:
                        if (mGraph instanceof MGraph) constructedGraph = kReSCoreOperation(sparql,
                            (MGraph) mGraph);
                        break;
                    case SPARQLDelete:
                        constructedGraph = sparqlUpdateOperation(sparql, unionMGraph);
                        break;
                    case SPARQLDeleteData:
                        constructedGraph = sparqlUpdateOperation(sparql, unionMGraph);
                        break;
                    default:
                        break;
                }

                if (constructedGraph != null) {
                    unionMGraph.addAll(constructedGraph);
                }

            }

            refactoredOntology = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(unionMGraph);

        } catch (NoSuchRecipeException e1) {
            e1.printStackTrace();
            log.error("SemionRefactorer : No Such recipe in the KReS Rule Store", e1);
            throw e1;
        }

        if (refactoredOntology == null) {
            throw new RefactoringException();
        } else {
            return refactoredOntology;
        }
    }

    @Override
    public OWLOntology ontologyRefactoring(OWLOntology inputOntology, Recipe recipe) throws RefactoringException {
        OWLOntology refactoredOntology = null;

        // JenaToOwlConvert jenaToOwlConvert = new JenaToOwlConvert();

        // OntModel ontModel =
        // jenaToOwlConvert.ModelOwlToJenaConvert(inputOntology, "RDF/XML");

        // OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        RuleList ruleList = recipe.getkReSRuleList();
        log.info("RULE LIST SIZE : " + ruleList.size());

        // OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        // OWLOntologyManager ontologyManager2 = OWLManager.createOWLOntologyManager();

        MGraph unionMGraph = new SimpleMGraph();

        TripleCollection mGraph = OWLAPIToClerezzaConverter.owlOntologyToClerezzaMGraph(inputOntology);

        for (Rule kReSRule : ruleList) {
            String sparql = kReSRule.toSPARQL();
            log.info("SPARQL : " + sparql);

            Graph constructedGraph = null;

            switch (kReSRule.getExpressiveness()) {
                case KReSCore:
                    if (mGraph instanceof MGraph) constructedGraph = kReSCoreOperation(sparql,
                        (MGraph) mGraph);
                    break;
                case ForwardChaining:
                    if (mGraph instanceof MGraph) {
                        ForwardChainingRefactoringGraph forwardChainingRefactoringGraph = forwardChainingOperation(
                            sparql, (MGraph) mGraph);
                        constructedGraph = forwardChainingRefactoringGraph.getOutputGraph();
                        mGraph = forwardChainingRefactoringGraph.getInputGraph();
                    }
                    break;
                case Reflexive:
                    constructedGraph = kReSCoreOperation(sparql, unionMGraph);
                    break;
                case SPARQLConstruct:
                    if (mGraph instanceof MGraph) constructedGraph = kReSCoreOperation(sparql,
                        (MGraph) mGraph);
                    break;
                case SPARQLDelete:
                    constructedGraph = sparqlUpdateOperation(sparql, unionMGraph);
                    break;
                case SPARQLDeleteData:
                    constructedGraph = sparqlUpdateOperation(sparql, unionMGraph);
                    break;
                default:
                    break;
            }

            if (constructedGraph != null) {
                unionMGraph.addAll(constructedGraph);
            }

        }

        refactoredOntology = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(unionMGraph);

        if (refactoredOntology == null) {
            throw new RefactoringException();
        } else {
            return refactoredOntology;
        }
    }

    private Graph sparqlUpdateOperation(String query, MGraph mGraph) {
        Model model = JenaToClerezzaConverter.clerezzaMGraphToJenaModel(mGraph);
        UpdateAction.parseExecute(query, model);
        return JenaToClerezzaConverter.jenaModelToClerezzaMGraph(model).getGraph();
    }

}
