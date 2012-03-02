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
package org.apache.stanbol.explanation.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.clerezza.rdf.core.sparql.SolutionMapping;
import org.apache.clerezza.rdf.core.sparql.query.Query;
import org.apache.clerezza.rdf.core.sparql.query.Variable;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.explanation.api.Configuration;
import org.apache.stanbol.explanation.api.Explainable;
import org.apache.stanbol.explanation.api.Explanation;
import org.apache.stanbol.explanation.api.ExplanationGenerator;
import org.apache.stanbol.explanation.api.ExplanationTypes;
import org.apache.stanbol.explanation.api.KnowledgeItem;
import org.apache.stanbol.explanation.impl.clerezza.PathConstructor;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.BlankOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO Update the merged ontology as a background job.
 * 
 * @author alessandro
 * 
 */
@Component(immediate = true, metatype = true)
@Service(ExplanationGenerator.class)
public class ExplanationGeneratorImpl implements ExplanationGenerator {

    private static final String _EXPLANATION_SCHEMA = "http://ontologydesignpatterns.org/schemas/explanationschema.owl";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private Configuration config;

    @Reference
    private ONManager onm;

    @Reference
    private SessionManager sesMgr;

    @Reference
    private DataFileProvider dataFileProvider;

    @Reference
    private QueryParser queryParser;

    @Reference
    private TcManager tcManager;

    private OntologyScope scope;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ExplanationGeneratorImpl instances do need to be
     * configured! YOU NEED TO USE {@link #ExplanationGeneratorImpl(ONManager, DataFileProvider, Dictionary)}
     * or its overloads, to parse the configuration and then initialise the rule store if running outside an
     * OSGI environment.
     */
    public ExplanationGeneratorImpl() {
        super();
    }

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param onManager
     * @param config
     * @param dataFileProvider
     * @param configuration
     */
    public ExplanationGeneratorImpl(Configuration config,
                                    DataFileProvider dataFileProvider,
                                    Dictionary<String,Object> configuration) {
        this();
        this.config = config;
        this.onm = config.getOntologyNetworkManager();
        this.dataFileProvider = dataFileProvider;
        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + ExplanationGeneratorImpl.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        // OntoNetSimulator sim = new OntoNetSimulator();
        // Create and register the scope for explanations.
        String scopeid = config.getScopeID();
        try {
            OntologyInputSource<?,?> coreSrc;
            try {
                coreSrc = new RootOntologyIRISource(IRI.create(_EXPLANATION_SCHEMA));
            } catch (OWLOntologyCreationException e) {
                coreSrc = new BlankOntologySource();
            }
            scope = onm.getOntologyScopeFactory().createOntologyScope(scopeid, coreSrc);
            onm.getScopeRegistry().registerScope(scope, true);
        } catch (DuplicateIDException e) {
            log.warn("Cannot create scope {}. A scope with this ID is already registered.", scopeid);
            scope = onm.getScopeRegistry().getScope(scopeid);
        }
        if (scope != null) {
            try {
                OntologySpace spc = scope.getCustomSpace();
                spc.addOntology(new RootOntologyIRISource(IRI
                        .create("http://xmlns.com/foaf/spec/20100809.rdf")));
                spc.addOntology(new RootOntologyIRISource(IRI.create("http://vocab.org/review/terms.rdf")));
            } catch (UnmodifiableOntologyCollectorException e) {
                log.warn("Failed to load Revyu dataset into locked scope {}", scope);
            } catch (OWLOntologyCreationException e) {
                log.warn("Failed to load FOAF into locked scope {}", scope);
            }
        }

        log.debug("Explanation Generator activated.");

    }

    @Override
    public Explanation createExplanation(Explainable<?> item,
                                         ExplanationTypes type,
                                         Set<? extends OWLAxiom> grounds) {

        Explanation result = new ExplanationImpl(item, type);

        switch (type) {
            case KNOWLEDGE_OBJECT_SYNOPSIS:
                if (!(item instanceof KnowledgeItem)) break;
                KnowledgeItem ki = (KnowledgeItem) item;
                break;
            case UI_FEEDBACK_JUSTIFICATION:
                break;
        }

        return result;
    }

    /**
     * Deactivation of the ONManagerImpl resets all its resources.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + ExplanationGeneratorImpl.class + " deactivate with context " + context);
    }

    @Override
    public Set<URI> getRelations(URI from, URI to, int length, String sessionId) {

        Session ses = sesMgr.getSession(sessionId);

        if (ses == null) {
            log.warn("No session with id {}. Will get relations on scope only.");
        } else {
            System.out.println("Attaching " + scope + " to session " + ses);
            ses.attachScope(scope);
            System.out.println(ses + " has " + ses.getAttachedScopes().size() + " attached scopes.");

            // Load the stuff to
            String lookfor = "revyu_sparql-20k.rdf";
            boolean hasRevyu = dataFileProvider.isAvailable(null, lookfor, null);
            log.info("{} available? {}", lookfor, hasRevyu);

            InputStream is;
            try {
                is = dataFileProvider.getInputStream(null, lookfor, null);
                ses.addOntology(new GraphContentInputSource(is, tcManager));
            } catch (IOException e) {
                log.error("Failed to load Revyu dataset", e);
            } catch (UnmodifiableOntologyCollectorException e) {
                log.error("Failed to load Revyu dataset into unmodifiable session {}", ses);
            }
        }

        // Graph g = (ses != null) ? ses.export(Graph.class, false) : scope.export(Graph.class, false);

        Query q;
        try {
            // Using a StringBuffer because it will be dynamic one day
            StringBuffer sparql = new StringBuffer("SELECT");
            sparql.append(" ?x ?r ?y");
            sparql.append(" WHERE");
            sparql.append(" {");
            sparql.append(" ?x ?r ?y .");
            sparql.append(" }");
            q = queryParser.parse(sparql.toString());

            Set<TripleCollection> graphs = new HashSet<TripleCollection>();
            if (ses != null) graphs.addAll(ses.getManagedOntologies(Graph.class, false));
            // for (String id : ses.getAttachedScopes())
            // graphs.add(onm.getScopeRegistry().getScope(id).export(Graph.class, true));

//            for (TripleCollection g : graphs) {
//                Object o = tcManager.executeSparqlQuery(q, g);
//                if (o instanceof ResultSet) {
//                    ResultSet result = (ResultSet) o;
//                    while (result.hasNext()) {
//                        SolutionMapping sm = result.next();
//                        for (Variable key : sm.keySet())
//                            System.out.println("##### " + key.getName() + " **** " + sm.get(key));
//                    }
//
//                }
//            }

            PathConstructor.computePathCompatibility(new UriRef(from.toString()), to == null ? null
                    : new UriRef(to.toString()), graphs, 3);

        } catch (ParseException e) {
            log.error("Could not execute SPARQL query.", e);
        }

        return null;
    }

}
