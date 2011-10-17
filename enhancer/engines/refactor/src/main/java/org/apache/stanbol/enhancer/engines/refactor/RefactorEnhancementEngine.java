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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.refactor.dereferencer.Dereferencer;
import org.apache.stanbol.enhancer.engines.refactor.dereferencer.IDereferencer;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologySetProvider;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
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
 * @author andrea.nuzzolese
 * 
 */

@Component(immediate = true, metatype = true)
@Service(EnhancementEngine.class)
public class RefactorEnhancementEngine implements EnhancementEngine, ServiceProperties {

    /*
     * TODO This are the scope and recipe IDs to be used by this implementation In future implementation this
     * will be configurable
     */

    @Property(value = "enhancer/engines/refactor")
    public static final String SCOPE = "engine.refactor.scope";

    @Property(value = "")
    public static final String RECIPE_URI = "engine.refactor.recipe";

    @Property(value = {"http://ontologydesignpatterns.org/ont/iks/kres/dbpedia_demo.owl", ""}, cardinality = 1000, description = "To fix a set of resolvable ontology URIs for the scope's ontologies.")
    public static final String SCOPE_CORE_ONTOLOGY = "engine.refactor.scope.core.ontology";

    @Property(boolValue = true, description = "If true: the previously generated RDF is deleted and substituted with the new one. If false: the new one is appended to the old RDF. Possible value: true or false.")
    public static final String APPEND_OTHER_ENHANCEMENT_GRAPHS = "engine.refactor.append.graphs";

    @Property(boolValue = true, description = "If true: entities are fetched via the EntityHub. If false: entities are fetched on-line. Possible value: true or false.")
    public static final String USAGE_OF_ENTITY_HUB = "engine.refactor.entityhub";

    @Reference
    ONManager onManager;

    @Reference
    ReferencedSiteManager referencedSiteManager;

    @Reference
    RuleStore ruleStore;

    @Reference
    Refactorer refactorer;

    private OntologyScope scope;
    private IRI recipeIRI;
    private boolean graph_append;
    private boolean useEntityHub;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        /*
         * Dulcifier can enhance only content items that are previously enhanced by other enhancement engines,
         * as it must be the last engine in the chain.
         * 
         * Works only if some enhancement has been produced.
         */
        MGraph mGraph = ci.getMetadata();
        if (mGraph != null) {
            return ENHANCE_SYNCHRONOUS;
        } else {
            return CANNOT_ENHANCE;
        }
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        /*
         * Retrieve the graph
         */
        final MGraph mGraph = ci.getMetadata();

        /*
         * We filter the entities recognized by the engines
         */
        UriRef fiseEntityReference = new UriRef("http://fise.iks-project.eu/ontology/entity-reference");
        Iterator<Triple> tripleIt = mGraph.filter(null, fiseEntityReference, null);

        /*
         * Now we prepare the OntoNet environment. First we create the OntoNet session in which run the whole
         */
        final String sessionIRI = createAndAddSessionSpaceToScope();

        /*
         * We retrieve the session space
         */
        OntologySpace sessionSpace = scope.getSessionSpace(sessionIRI);

        while (tripleIt.hasNext()) {
            Triple triple = tripleIt.next();
            Resource entityReference = triple.getObject();
            /*
             * the entity uri
             */
            final String entityReferenceString = entityReference.toString().replace("<", "").replace(">", "");
            log.debug("Trying to resolve entity " + entityReferenceString);
            /**
             * We fetch the entity in the OntologyInputSource object
             */
            try {

                final IRI fetchedIri = IRI.create(entityReferenceString);

                /*
                 * The RDF graph of an entity is fetched via the EntityHub. The getEntityOntology is a method
                 * the do the job of asking the entity to the EntityHub and wrap the RDF graph into an
                 * OWLOntology.
                 */
                OWLOntology fetched = null;

                if (useEntityHub) {
                    fetched = getEntityOntology(entityReferenceString);
                } else {
                    Dereferencer dereferencer = new Dereferencer();
                    try {
                        fetched = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(
                            dereferencer.resolve(entityReferenceString));
                    } catch (OWLOntologyCreationException e) {
                        log.error("An error occurred while trying to create the ontology related to the entity "
                                  + entityReferenceString);
                    } catch (FileNotFoundException e) {
                        log.error("The entity " + entityReferenceString + " does not exist or is unreachable");
                    }
                }

                if (fetched != null) {
                    final OWLOntology fetchedFinal = fetched;
                    OntologyInputSource ontologySource = new OntologyInputSource() {

                        @Override
                        public boolean hasRootOntology() {
                            return (fetchedFinal != null);
                        }

                        @Override
                        public boolean hasPhysicalIRI() {
                            return true;
                        }

                        @Override
                        public OWLOntology getRootOntology() {
                            return fetchedFinal;
                        }

                        @Override
                        public IRI getPhysicalIRI() {
                            return fetchedIri;
                        }

                        @Override
                        public Set<OWLOntology> getImports(boolean direct) {
                            // TODO Auto-generated method stub
                            return null;
                        }

                    };
                    sessionSpace.addOntology(ontologySource);
                }

                log.debug("Added " + entityReferenceString + " to the session space of scope "
                          + scope.getID().toString(), this);

            } catch (UnmodifiableOntologyCollectorException e) {
                log.error("Cannot load the entity", e);
            }

        }

        /*
         * Now we merge the RDF from the T-box - the ontologies - and the A-box - the RDF data fetched
         */

        final OWLOntologyManager man = OWLManager.createOWLOntologyManager();

        OWLOntologySetProvider provider = new OWLOntologySetProvider() {

            @Override
            public Set<OWLOntology> getOntologies() {

                Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
                OntologySpace sessionSpace = scope.getSessionSpace(sessionIRI);
                ontologies.addAll(sessionSpace.getOntologies(true));

                /*
                 * We add to the set the graph containing the metadata generated by previous enhancement
                 * engines. It is important becaus we want to menage during the refactoring also some
                 * information fron that graph. As the graph is provided as a Clerezza MGraph, we first need
                 * to convert it to an OWLAPI OWLOntology. There is no chance that the mGraph could be null as
                 * it was previously controlled by the JobManager through the canEnhance method and the
                 * computeEnhancement is always called iff the former returns true.
                 */
                OWLOntology fiseMetadataOntology = OWLAPIToClerezzaConverter
                        .clerezzaGraphToOWLOntology(mGraph);
                ontologies.add(fiseMetadataOntology);
                return ontologies;
            }
        };

        /*
         * We merge all the ontologies from the session space of the scope into a single ontology that will be
         * used for the refactoring.
         */
        OWLOntologyMerger merger = new OWLOntologyMerger(provider);

        OWLOntology ontology;
        try {
            ontology = merger.createMergedOntology(man,
                IRI.create("http://fise.iks-project.eu/dulcifier/integrity-check"));

            /*
             * To perform the refactoring of the ontology to a given vocabulary we use the Stanbol Refactor.
             */

            log.debug("Refactoring recipe IRI is : " + recipeIRI);

            /*
             * We pass the ontology and the recipe IRI to the Refactor that returns the refactored graph
             * expressed by using the given vocabulary.
             */
            try {

                Recipe recipe = ruleStore.getRecipe(recipeIRI);

                log.debug("Rules in the recipe are : " + recipe.getkReSRuleList().size(), this);

                log.debug("The ontology to be refactor is : " + ontology, this);

                ontology = refactorer.ontologyRefactoring(ontology, recipeIRI);

            } catch (RefactoringException e) {
                log.error("The refactoring engine failed the execution.", e);
            } catch (NoSuchRecipeException e) {
                log.error("The recipe with ID " + recipeIRI + " does not exists", e);
            }

            log.debug("Merged ontologies in " + ontology);

            /*
             * The new generated ontology is converted to Clarezza format and than added os substitued to the
             * old mGraph.
             */
            if (graph_append) {
                mGraph.addAll(OWLAPIToClerezzaConverter.owlOntologyToClerezzaTriples(ontology));
                log.debug("Metadata of the content passd have been substituted", this);
            } else {
                mGraph.removeAll(mGraph);
                mGraph.addAll(OWLAPIToClerezzaConverter.owlOntologyToClerezzaTriples(ontology));
                log.debug("Metadata of the content is appended to the existent one", this);
            }

            /*
             * The session needs to be destroyed, as it is no more useful.
             */
            onManager.getSessionManager().destroySession(sessionIRI.toString());

        } catch (OWLOntologyCreationException e) {
            log.error("Cannot create the ontology for the refactoring", e);
        }
    }

    /**
     * Setup the KReS session
     * 
     * @return
     */
    private String createAndAddSessionSpaceToScope() {
        /*
         * Retrieve the session manager
         */
        SessionManager sessionManager = onManager.getSessionManager();
        log.debug("Starting create session for the dulcifier");

        /*
         * Create and setup the session. TODO FIXME This is an operation that should be made easier for
         * developers to do through the API
         */
        Session session = sessionManager.createSession();
        // OntologySpaceFactory ontologySpaceFactory = onManager.getOntologySpaceFactory();
        // OntologySpace sessionSpace = ontologySpaceFactory.createSessionOntologySpace(scope.getID());
        // try {
        // scope.addSessionSpace(sessionSpace, session.getID());
        // } catch (UnmodifiableOntologySpaceException e) {
        // log.error("Failed to add session space to unmodifiable scope " + scope, e);
        // }

        /*
         * Finally, we return the session ID to be used by the caller
         */
        log.debug("Session " + session.getID() + " created", this);
        return session.getID();
    }

    /**
     * To create the input source necesary to load the ontology inside the scope.
     * 
     * @param uri
     *            -- A resolvable string uri.
     * @return An OntologyInputSource
     */
    private OntologyInputSource oisForScope(final String uri) {
        /*
         * The scope factory needs an OntologyInputSource as input for the core ontology space. We want to use
         * the dbpedia ontology as core ontology of our scope.
         */
        OntologyInputSource ois = new OntologyInputSource() {

            @Override
            public boolean hasRootOntology() {
                return true;
            }

            @Override
            public boolean hasPhysicalIRI() {
                return false;
            }

            @Override
            public OWLOntology getRootOntology() {
                try {
                    /*
                     * The input stream for the dbpedia ontology is obtained through the dereferencer
                     * component.
                     */
                    // inputStream = dereferencer.resolve(uri);
                    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                    return manager.loadOntologyFromOntologyDocument(IRI.create(uri));
                    // return getEntityOntology(uri);
                } catch (Exception e) {
                    log.error("Cannot load the ontology " + uri, e);
                }
                /** If some errors occur **/
                return null;
            }

            @Override
            public IRI getPhysicalIRI() {
                return null;
            }

            @Override
            public Set<OWLOntology> getImports(boolean direct) {
                // TODO Auto-generated method stub
                return null;
            }

        };

        return ois;
    }

    /**
     * Activating the component
     * 
     * @param context
     */
    protected void activate(ComponentContext context) {

        /*
         * Read property to indicate if the the new eanchment metada must be append to the existing mGraph
         */
        graph_append = ((Boolean) context.getProperties().get(APPEND_OTHER_ENHANCEMENT_GRAPHS))
                .booleanValue();

        useEntityHub = ((Boolean) context.getProperties().get(USAGE_OF_ENTITY_HUB)).booleanValue();

        /*
         * Get the Scope Factory from the ONM of KReS that allows to create new scopes
         */
        OntologyScopeFactory scopeFactory = onManager.getOntologyScopeFactory();

        /*
         * Adding ontologies to the scope core ontology. 1) Get all the ontologies from the property. 2)
         * Create a base scope with an empity ontology. 3) Retrieve the ontology space from the scope. 4) Add
         * the ontologies to the scope via ontology space.
         */
        // Step 1
        Object obj = context.getProperties().get(SCOPE_CORE_ONTOLOGY);
        String[] coreScopeOntologySet;
        if (obj instanceof String[]) {
            coreScopeOntologySet = (String[]) obj;
        } else {
            String[] aux = new String[1];
            aux[0] = (String) obj;
            coreScopeOntologySet = aux;
        }

        // Step 2
        OntologyInputSource oisbase = new OntologyInputSource() {

            @Override
            public boolean hasRootOntology() {
                return true;
            }

            @Override
            public boolean hasPhysicalIRI() {
                return false;
            }

            @Override
            public OWLOntology getRootOntology() {

                try {
                    /*
                     * The input stream for the dbpedia ontology is obtained through the dereferencer
                     * component.
                     */
                    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
                    return manager.createOntology();
                } catch (OWLOntologyCreationException e) {
                    log.error("Cannot create the scope with empity ontology.", e);
                } catch (Exception e) {
                    log.error("Cannot create the scope with empity ontology.", e);
                }
                /** If some errors occur **/
                return null;
            }

            @Override
            public IRI getPhysicalIRI() {
                return null;
            }

            @Override
            public Set<OWLOntology> getImports(boolean direct) {
                // TODO Auto-generated method stub
                return null;
            }

        };

        // IRI dulcifierScopeIRI = IRI.create((String) context.getProperties().get(SCOPE));
        String scopeId = (String) context.getProperties().get(SCOPE);

        /*
         * The scope is created by the ScopeFactory or loaded from the scope registry of KReS
         */
        try {
            scope = scopeFactory.createOntologyScope(scopeId/* dulcifierScopeIRI */, oisbase);
        } catch (DuplicateIDException e) {
            ScopeRegistry scopeRegistry = onManager.getScopeRegistry();
            scope = scopeRegistry.getScope(scopeId/* dulcifierScopeIRI */);
        }

        /*
         * Step 3
         */
        OntologySpace ontologySpace = scope.getCoreSpace();

        /*
         * Step 4
         */
        ontologySpace.tearDown();
        for (int o = 0; o < coreScopeOntologySet.length; o++) {
            OntologyInputSource ois = oisForScope(coreScopeOntologySet[o]);
            try {
                ontologySpace.addOntology(ois);
            } catch (UnmodifiableOntologyCollectorException ex) {
                log.error("Unmodifiable Ontology SpaceException.", ex);
            }
        }
        ontologySpace.setUp();

        log.debug("The set of ontologies loaded in the core scope space is: "
                  + ontologySpace.getOntologies(true)
                  + "\nN.B. The root.owl ontology is the first (on the list) ontology added when the scope is created.");

        /*
         * The first thing to do is to create a recipe in the rule store that can be used by the engine to
         * refactor the enhancement graphs.
         */
        recipeIRI = IRI.create((String) context.getProperties().get(RECIPE_URI));

        log.debug("Start create the Recipe", this);

        ruleStore.addRecipe(recipeIRI, null);

        log.debug("The recipe has been created", this);

        /*
         * The set of rule to put in the recipe can be provided by the user. A default set of rules is
         * provided in /META-INF/default/seo_rules.sem. Use the property engine.refactor in the felix console
         * to pass to the engine your set of rules.
         */

        String recipeURI = (String) context.getProperties().get(RECIPE_URI);

        InputStream recipeStream = null;
        String recipeString = null;

        if (recipeURI != null && !recipeURI.isEmpty()) {
            IDereferencer dereferencer = new Dereferencer();
            try {
                recipeStream = dereferencer.resolve(recipeURI);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            recipeStream = RefactorEnhancementEngine.class
                    .getResourceAsStream("/META-INF/default/seo_rules.sem");
        }

        System.out.println("Refactorer engine recipe stream " + recipeStream);

        if (recipeStream != null) {

            recipeString = "";

            BufferedReader reader = new BufferedReader(new InputStreamReader(recipeStream));

            String line = null;
            try {
                while ((line = reader.readLine()) != null) {
                    recipeString += line;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /*
         * step 3
         */
        try {
            // ruleStore.addRuleToRecipe(recipeIRI.toString(), kReSRuleSyntax);
            ruleStore.addRuleToRecipe(recipeIRI.toString(), recipeString);
            log.debug("Added rules to recipe " + recipeIRI.toString());
        } catch (NoSuchRecipeException e) {
            log.error("The recipe does not exists: ", e);
        }
        log.info("Activated Dulcifier engine");

    }

    protected void deactivate(ComponentContext context) {

        /*
         * Deactivating the dulcifier. The procedure require: 1) get all the rules from the recipe 2) remove
         * the recipe. 3) remove the single rule. 4) tear down the scope ontologySpace and the scope itself.
         */

        try {
            /*
             * step 1: get all the rule
             */
            RuleList recipeRuleList = ruleStore.getRecipe(recipeIRI).getkReSRuleList();

            /*
             * step 2: remove the recipe
             */
            if (ruleStore.removeRecipe(recipeIRI)) {
                log.info("The recipe " + recipeIRI + " has been removed correctly");
            } else {
                log.error("The recipe " + recipeIRI + " can not be removed");
            }

            /*
             * step 3: remove the rules
             */
            for (Rule rule : recipeRuleList) {
                if (ruleStore.removeRule(rule)) {
                    log.info("The rule " + rule.getRuleName() + " has been removed correctly");
                } else {
                    log.error("The rule " + rule.getRuleName() + " can not be removed");
                }
            }

            /*
             * step 4:
             */
            scope.getCoreSpace().tearDown();
            scope.tearDown();

        } catch (NoSuchRecipeException ex) {
            log.error("The recipe " + recipeIRI + " doesn't exist", ex);
        }

        log.info("Deactivated Dulcifier engine");

    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(
            ServiceProperties.ENHANCEMENT_ENGINE_ORDERING,
            (Object) ServiceProperties.ORDERING_POST_PROCESSING));
    }

    /**
     * Fetch the OWLOntology containing the graph associated to an entity from Linked Data. It uses the Entity
     * Hub for accessing LOD and fetching entities.
     * 
     * @param entityURI
     *            {@link String}
     * @return the {@link OWLOntology} of the entity
     */
    private OWLOntology getEntityOntology(String entityURI) {

        OWLOntology fetchedOntology = null;

        log.debug("Asking entity: " + entityURI);
        /*
         * Ask to the entityhub the fetch the entity.
         */
        Entity entitySign = referencedSiteManager.getEntity(entityURI);

        /*
         * Wrap the entity graph into an owl ontology.
         */
        MGraph entityMGraph = null;

        if (entitySign != null) {
            Representation entityRepresentation = entitySign.getRepresentation();
            RdfRepresentation entityRdfRepresentation = RdfValueFactory.getInstance().toRdfRepresentation(
                entityRepresentation);
            TripleCollection tripleCollection = entityRdfRepresentation.getRdfGraph();
            entityMGraph = new SimpleMGraph();
            entityMGraph.addAll(tripleCollection);
        }

        if (entityMGraph != null) {
            /*
             * OWLOntologyManager manager = OWLManager.createOWLOntologyManager(); final OWLOntology fetched =
             * manager.loadOntologyFromOntologyDocument(dereferencer.resolve(entityReferenceString));
             */

            fetchedOntology = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(entityMGraph);
        }

        return fetchedOntology;

    }
}
