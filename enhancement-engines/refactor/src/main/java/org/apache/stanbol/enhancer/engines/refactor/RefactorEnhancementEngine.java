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
package org.apache.stanbol.enhancer.engines.refactor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.enhancer.engines.refactor.dereferencer.Dereferencer;
import org.apache.stanbol.enhancer.engines.refactor.dereferencer.DereferencerImpl;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.impl.AbstractEnhancementEngine;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.ontologymanager.servicesapi.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.apache.stanbol.ontologymanager.sources.clerezza.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.sources.clerezza.GraphSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.apache.stanbol.rules.base.api.AlreadyExistingRecipeException;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RecipeConstructionException;
import org.apache.stanbol.rules.base.api.RecipeEliminationException;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This an engine to post-process the enhancements. Its main goal is to refactor the RDF produced by the
 * enhancement applying some vocabulary related to a specific task.
 * 
 * To do that, exploit a Refactor recipe and an ontology scope of OntoNet.
 * 
 * The first implementation is targeted to SEO use case. * It retrieves data by dereferencing the entities, *
 * includes the DBpedia ontology * refactor the data using the google rich snippets vocabulary.
 * 
 * @author anuzzolese, alberto.musetti
 * 
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, specVersion = "1.1", metatype = true, immediate = true, inherit = true)
@Service
@Properties(value = {@Property(name = EnhancementEngine.PROPERTY_NAME, value = "seo_refactoring")

})
public class RefactorEnhancementEngine extends AbstractEnhancementEngine<RuntimeException,RuntimeException>
        implements EnhancementEngine, ServiceProperties {

    /**
     * A special input source that allows to bind a physical IRI with an ontology parsed from an input stream.
     * Due to its unconventional nature it is kept private.
     * 
     * @author alexdma
     * 
     */
    private class GraphContentSourceWithPhysicalIRI extends GraphContentInputSource {

        public GraphContentSourceWithPhysicalIRI(InputStream content, org.semanticweb.owlapi.model.IRI physicalIri) {
            super(content);
            bindPhysicalOrigin(Origin.create(physicalIri));
        }

    }

    @Property(boolValue = true)
    public static final String APPEND_OTHER_ENHANCEMENT_GRAPHS = RefactorEnhancementEngineConf.APPEND_OTHER_ENHANCEMENT_GRAPHS;

    @Property(value = "google_rich_snippet_rules")
    public static final String RECIPE_ID = RefactorEnhancementEngineConf.RECIPE_ID;

    @Property(value = "")
    public static final String RECIPE_LOCATION = RefactorEnhancementEngineConf.RECIPE_LOCATION;

    @Property(value = "seo")
    public static final String SCOPE = RefactorEnhancementEngineConf.SCOPE;

    @Property(cardinality = 1000, value = {"http://ontologydesignpatterns.org/ont/iks/kres/dbpedia_demo.owl"})
    public static final String SCOPE_CORE_ONTOLOGY = RefactorEnhancementEngineConf.SCOPE_CORE_ONTOLOGY;

    @Property(boolValue = true)
    public static final String USE_ENTITY_HUB = RefactorEnhancementEngineConf.USE_ENTITY_HUB;

    private ComponentContext context;

    @Reference
    Dereferencer dereferencer;

    private RefactorEnhancementEngineConf engineConfiguration;

    private final Object lock = new Object();

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    ScopeManager onManager;

    @Reference
    OntologyProvider<TcProvider> ontologyProvider;

    private ComponentInstance refactorEngineComponentInstance;

    @Reference
    Refactorer refactorer;

    @Reference
    SiteManager referencedSiteManager;

    @Reference
    RuleStore ruleStore;

    private Scope scope;

    @Reference
    SessionManager sessionManager;

    /**
     * Activating the component
     * 
     * @param context
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(final ComponentContext context) throws ConfigurationException {
        log.info("in " + RefactorEnhancementEngine.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        super.activate(context);
        this.context = context;

        Map<String,Object> config = new HashMap<String,Object>();
        Dictionary<String,Object> properties = (Dictionary<String,Object>) context.getProperties();
        // copy the properties to a map
        for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
            String key = e.nextElement();
            config.put(key, properties.get(key));
            log.debug("Configuration property: " + key + " :- " + properties.get(key));
        }

        // Initialize engine-specific features.
        engineConfiguration = new DefaultRefactorEnhancementEngineConf(properties);
        initEngine(engineConfiguration);

        log.debug(RefactorEnhancementEngine.class + " activated.");
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        /*
         * Being a post-processing engine, the Refactor can enhance only content items that are previously
         * enhanced by other enhancement engines.
         */
        return ci.getMetadata() == null ? CANNOT_ENHANCE : ENHANCE_SYNCHRONOUS;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {

        // Prepare the OntoNet environment. First we create the OntoNet session in which run the whole
        final Session session;
        try {
            session = sessionManager.createSession();
        } catch (SessionLimitException e1) {
            throw new EngineException(
                    "OntoNet session quota reached. The Refactor Engine requires its own new session to execute.");
        }
        if (session == null) throw new EngineException(
                "Failed to create OntoNet session. The Refactor Engine requires its own new session to execute.");

        log.debug("Refactor enhancement job will run in session '{}'.", session.getID());

        // Retrieve and filter the metadata graph for entities recognized by the engines.
        final Graph metadataGraph = ci.getMetadata(), signaturesGraph = new IndexedGraph();
        // FIXME the Stanbol Enhancer vocabulary should be retrieved from somewhere in the enhancer API.
        final IRI ENHANCER_ENTITY_REFERENCE = new IRI(
                "http://fise.iks-project.eu/ontology/entity-reference");
        Iterator<Triple> tripleIt = metadataGraph.filter(null, ENHANCER_ENTITY_REFERENCE, null);
        while (tripleIt.hasNext()) {
            // Get the entity URI
            RDFTerm obj = tripleIt.next().getObject();
            if (!(obj instanceof IRI)) {
                log.warn("Invalid IRI for entity reference {}. Skipping.", obj);
                continue;
            }
            final String entityReference = ((IRI) obj).getUnicodeString();
            log.debug("Trying to resolve entity {}", entityReference);

            // Populate the entity signatures graph, by querying either the Entity Hub or the dereferencer.
            if (engineConfiguration.isEntityHubUsed()) {
                Graph result = populateWithEntity(entityReference, signaturesGraph);
                if (result != signaturesGraph && result != null) {
                    log.warn("Entity Hub query added triples to a new graph instead of populating the supplied one!"
                             + " New signatures will be discarded.");
                }
            } else try {
                OntologyInputSource<Graph> source = new GraphContentSourceWithPhysicalIRI(
                        dereferencer.resolve(entityReference), org.semanticweb.owlapi.model.IRI.create(entityReference));
                signaturesGraph.addAll(source.getRootOntology());
            } catch (FileNotFoundException e) {
                log.error("Failed to dereference entity " + entityReference + ". Skipping.", e);
                continue;
            }
        }

        try {
            /*
             * The dedicated session for this job will store the following: (1) all the (merged) signatures
             * for all detected entities; (2) the original content metadata graph returned earlier in the
             * chain.
             * 
             * There is no chance that (2) could be null, as it was previously controlled by the JobManager
             * through the canEnhance() method and the computeEnhancement is always called iff the former
             * returns true.
             */
            session.addOntology(new GraphSource(signaturesGraph));
            session.addOntology(new GraphSource(metadataGraph));
        } catch (UnmodifiableOntologyCollectorException e1) {
            throw new EngineException("Cannot add enhancement graph to OntoNet session for refactoring", e1);
        }

        try {
            /*
             * Export the entire session (incl. entities and enhancement graph) as a single merged ontology.
             * 
             * TODO the refactorer should have methods to accommodate an OntologyCollector directly instead.
             */
            OWLOntology ontology = session.export(OWLOntology.class, true);
            log.debug("Refactoring recipe IRI is : " + engineConfiguration.getRecipeId());

            /*
             * We pass the ontology and the recipe IRI to the Refactor that returns the refactored graph
             * expressed by using the given vocabulary.
             * 
             * To perform the refactoring of the ontology to a given vocabulary we use the Stanbol Refactor.
             */
            Recipe recipe = ruleStore.getRecipe(new IRI(engineConfiguration.getRecipeId()));

            log.debug("Recipe {} contains {} rules.", recipe, recipe.getRuleList().size());
            log.debug("The ontology to be refactor is {}", ontology);

            Graph tc = refactorer.graphRefactoring(
                OWLAPIToClerezzaConverter.owlOntologyToClerezzaGraph(ontology), recipe);

            /*
             * ontology = refactorer .ontologyRefactoring(ontology,
             * org.semanticweb.owlapi.model.IRI.create(engineConfiguration.getRecipeId()));
             */
            /*
             * The newly generated ontology is converted to Clarezza format and then added os substitued to
             * the old mGraph.
             */
            if (engineConfiguration.isInGraphAppendMode()) {
                log.debug("Metadata of the content will replace old ones.", this);
            } else {
                metadataGraph.clear();
                log.debug("Content metadata will be appended to the existing ones.", this);
            }
            metadataGraph.addAll(tc);

        } catch (RefactoringException e) {
            String msg = "Refactor engine execution failed on content item " + ci + ".";
            log.error(msg, e);
            throw new EngineException(msg, e);
        } catch (NoSuchRecipeException e) {
            String msg = "Refactor engine could not find recipe " + engineConfiguration.getRecipeId()
                         + " to refactor content item " + ci + ".";
            log.error(msg, e);
            throw new EngineException(msg, e);
        } catch (Exception e) {
            throw new EngineException("Refactor Engine has failed.", e);
        } finally {
            /*
             * The session needs to be destroyed anyhow.
             * 
             * Clear contents before destroying (FIXME only do this until this is implemented in the
             * destroySession() method).
             */
            for (OWLOntologyID id : session.listManagedOntologies()) {
                try {
                    String key = ontologyProvider.getKey(id.getOntologyIRI());
                    ontologyProvider.getStore().deleteGraph(new IRI(key));
                } catch (Exception ex) {
                    log.error("Failed to delete triple collection " + id, ex);
                    continue;
                }
            }
            sessionManager.destroySession(session.getID());
        }

    }

    @SuppressWarnings("unchecked")
    protected void createRefactorEngineComponent(ComponentFactory factory) {
        // both create*** methods sync on the searcherAndDereferencerLock to avoid
        // multiple component instances because of concurrent calls
        synchronized (this.lock) {
            if (refactorEngineComponentInstance == null) {
                this.refactorEngineComponentInstance = factory.newInstance(OsgiUtils.copyConfig(context
                        .getProperties()));
            }
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        // Deactivation clears all the rules and releases OntoNet resources.

        IRI recipeId = new IRI(engineConfiguration.getRecipeId());
        try {
            // step 1: get all the rules
            log.debug("Recipe {} and its associated rules will be removed from the rule store.", recipeId);
            Recipe recipe = null;
            try {
                recipe = ruleStore.getRecipe(recipeId);
            } catch (RecipeConstructionException e) {
                log.error(e.getMessage());
            }
            if (recipe != null) {

                // step 2: remove the recipe
                try {
                    if (ruleStore.removeRecipe(recipeId)) {
                        log.debug(
                            "Recipe {} has been removed correctly. Note that its rules will be removed separately.",
                            recipeId);
                    } else log.error("Recipe {} cannot be removed.", recipeId);
                } catch (RecipeEliminationException e) {
                    log.error(e.getMessage());
                }
            }

        } catch (NoSuchRecipeException ex) {
            log.error("The recipe " + engineConfiguration.getRecipeId() + " doesn't exist", ex);
        }

        // step 3: clear OntoNet resources
        scope.getCoreSpace().tearDown();
        scope.tearDown();
        onManager.deregisterScope(scope);
        log.debug("OntoNet resources released : scope {}", scope);

        log.info("in " + RefactorEnhancementEngine.class + " deactivate with context " + context);

    }

    /**
     * Fetch the OWLOntology containing the graph associated to an entity from Linked Data. It uses the Entity
     * Hub for accessing LOD and fetching entities.
     * 
     * @param entityURI
     *            {@link String}
     * @return the {@link OWLOntology} of the entity
     */
    private Graph populateWithEntity(String entityURI, Graph target) {
        log.debug("Requesting signature of entity {}", entityURI);
        Graph graph = target != null ? target : new IndexedGraph();
        // Query the Entity Hub
        Entity signature = referencedSiteManager.getEntity(entityURI);
        if (signature != null) {
            RdfRepresentation rdfSignature = RdfValueFactory.getInstance().toRdfRepresentation(
                signature.getRepresentation());
            graph.addAll(rdfSignature.getRdfGraph());
        }
        return graph;
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
            ServiceProperties.ENHANCEMENT_ENGINE_ORDERING,
            (Object) ServiceProperties.ORDERING_POST_PROCESSING));
    }

    /**
     * Method for adding ontologies to the scope core ontology.
     * <ol>
     * <li>Get all the ontologies from the property.</li>
     * <li>Create a base scope.</li>
     * <li>Retrieve the ontology space from the scope.</li>
     * <li>Add the ontologies to the scope via ontology space.</li>
     * </ol>
     */
    private void initEngine(RefactorEnhancementEngineConf engineConfiguration) {

        // IRI dulcifierScopeIRI = org.semanticweb.owlapi.model.IRI.create((String) context.getProperties().get(SCOPE));
        String scopeId = engineConfiguration.getScope();

        // Create or get the scope with the configured ID
        try {
            scope = onManager.createOntologyScope(scopeId);
            // No need to deactivate a newly created scope.
        } catch (DuplicateIDException e) {
            scope = onManager.getScope(scopeId);
            onManager.setScopeActive(scopeId, false);
        }
        // All resolvable ontologies stated in the configuration are loaded into the core space.
        OntologySpace ontologySpace = scope.getCoreSpace();
        ontologySpace.tearDown();
        String[] coreScopeOntologySet = engineConfiguration.getScopeCoreOntologies();
        List<String> success = new ArrayList<String>(), failed = new ArrayList<String>();
        try {
            log.info("Will now load requested ontology into the core space of scope '{}'.", scopeId);
            OWLOntologyManager sharedManager = OWLManager.createOWLOntologyManager();
            org.semanticweb.owlapi.model.IRI physicalIRI = null;
            for (int o = 0; o < coreScopeOntologySet.length; o++) {
                String url = coreScopeOntologySet[o];
                try {
                    physicalIRI = org.semanticweb.owlapi.model.IRI.create(url);
                } catch (Exception e) {
                    failed.add(url);
                }
                try {
                    // TODO replace with a Clerezza equivalent
                    ontologySpace.addOntology(new RootOntologySource(physicalIRI, sharedManager));
                    success.add(url);
                } catch (OWLOntologyCreationException e) {
                    log.error("Failed to load ontology from physical location " + physicalIRI
                              + " Continuing with next...", e);
                    failed.add(url);
                }
            }
        } catch (UnmodifiableOntologyCollectorException ex) {
            log.error("Ontology space {} was found locked for modification. Cannot populate.", ontologySpace);
        }
        for (String s : success)
            log.info(" >> {} : SUCCESS", s);
        for (String s : failed)
            log.info(" >> {} : FAILED", s);
        ontologySpace.setUp();
        // if (!onManager.containsScope(scopeId)) onManager.registerScope(scope);
        onManager.setScopeActive(scopeId, true);

        /*
         * The first thing to do is to create a recipe in the rule store that can be used by the engine to
         * refactor the enhancement graphs.
         */
        String recipeId = engineConfiguration.getRecipeId();
        Recipe recipe = null;
        try {
            recipe = ruleStore.createRecipe(new IRI(recipeId), null);
        } catch (AlreadyExistingRecipeException e1) {
            log.error("A recipe with ID {} already exists in the store.", recipeId);
        }

        if (recipe != null) {
            log.debug("Initialised blank recipe with ID {}", recipeId);

            /*
             * The set of rule to put in the recipe can be provided by the user. A default set of rules is
             * provided in /META-INF/default/seo_rules.sem. Use the property engine.refactor in the felix
             * console to pass to the engine your set of rules.
             */
            String recipeLocation = engineConfiguration.getRecipeLocation();

            InputStream recipeStream = null;
            String recipeString = null;

            if (recipeLocation != null && !recipeLocation.isEmpty()) {
                Dereferencer dereferencer = new DereferencerImpl();
                try {
                    recipeStream = dereferencer.resolve(recipeLocation);
                    log.debug("Loaded recipe from external source {}", recipeLocation);
                } catch (FileNotFoundException e) {
                    log.error("Recipe Stream is null.", e);
                }
            } else {
                // TODO remove this part (or manage it better in the @Activate method).
                String loc = "/META-INF/default/seo_rules.sem";
                recipeStream = getClass().getResourceAsStream(loc);
                log.debug("Loaded default recipe in {}.", loc);
            }

            if (recipeStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(recipeStream));
                recipeString = "";
                String line = null;
                try {
                    while ((line = reader.readLine()) != null)
                        recipeString += line;
                } catch (IOException e) {
                    log.error("Failed to load Refactor Engine recipe from stream. Aborting read. ", e);
                    recipeString = null;
                }
            }
            log.debug("Recipe content follows :\n{}", recipeString);
            if (recipeString != null) {
                ruleStore.addRulesToRecipe(recipe, recipeString, null);
                log.debug("Added rules to recipe {}", recipeId);
            }
        }
    }

}
