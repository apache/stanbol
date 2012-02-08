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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.engines.refactor.dereferencer.Dereferencer;
import org.apache.stanbol.enhancer.engines.refactor.dereferencer.IDereferencer;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.AbstractEnhancementEngine;
import org.apache.stanbol.entityhub.core.utils.OsgiUtils;
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
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.owl.transformation.OWLAPIToClerezzaConverter;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
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

@Component(
        configurationFactory=true,
        policy=ConfigurationPolicy.REQUIRE,
        specVersion="1.1",
        metatype = true,
        immediate = true,
        inherit = true
        )
@Service
@Properties(value={
		@Property(name=EnhancementEngine.PROPERTY_NAME, value="seo_refactoring")
        
})
public class RefactorEnhancementEngine extends AbstractEnhancementEngine<RuntimeException, RuntimeException> implements EnhancementEngine, ServiceProperties {

    /*
     * TODO This are the scope and recipe IDs to be used by this implementation In future implementation this
     * will be configurable
     */
	@Property(value="seo")
	public static final String SCOPE = "engine.refactor.scope";
 
	@Property(value="")
	public static final String RECIPE_LOCATION = "engine.refactor.recipe.location";
 
	@Property(value="google_rich_snippet_rules")
	public static final String RECIPE_ID = "engine.refactor.recipe.id";
	
	@Property(	cardinality = 1000, 
            	value={
                   "http://ontologydesignpatterns.org/ont/iks/kres/dbpedia_demo.owl", 
                   ""
            	}
			)
	public static final String SCOPE_CORE_ONTOLOGY = "engine.refactor.scope.core.ontology";
 
	@Property(boolValue=true)
	public static final String APPEND_OTHER_ENHANCEMENT_GRAPHS = "engine.refactor.append.graphs";

	@Property(boolValue=true)
	public static final String USE_ENTITY_HUB  = "engine.refactor.entityhub";
	
	

    @Reference
    ONManager onManager;

    @Reference
    SessionManager sessionManager;

    @Reference
    ReferencedSiteManager referencedSiteManager;

    @Reference
    RuleStore ruleStore;

    @Reference
    Refactorer refactorer;

    private RefactorEnhancementEngineConf engineConfiguration;
    
    private ComponentInstance refactorEngineComponentInstance;
    
    private OntologyScope scope;
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final Object lock = new Object();

    private ComponentContext context;
    
    
    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        /*
         * The Refactor can enhance only content items that are previously enhanced by other enhancement engines,
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
        //String sessionID = null;
        
        Session tmpSession = null;
        try {
            tmpSession = sessionManager.createSession();
        } catch (SessionLimitException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        

        if(tmpSession != null){
            
            final Session session = tmpSession;
        
            //final String sessionIdentifier = sessionID;
            
            /*
             * We retrieve the session space
             */
            //OntologySpace sessionSpace = scope.getSessionSpace(sessionIdentifier);
    
            
            log.debug("The session space is " + session);
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
    
                    if (engineConfiguration.isEntityHubUsed()) {
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

                            @Override
                            public String getStorageKey() {
                                // TODO Auto-generated method stub
                                return null;
                            }

                            @Override
                            public Object getTriplesProvider() {
                                // TODO Auto-generated method stub
                                return null;
                            }
    
                        };
                        session.addOntology(ontologySource);
                    }
    
                    log.debug("Added " + entityReferenceString + " to the session space of scope "
                              + engineConfiguration.getScope(), this);
    
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
                    ontologies.addAll(session.getOntologies(true));
    
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
    
                log.debug("Refactoring recipe IRI is : " + engineConfiguration.getRecipeId());
    
                /*
                 * We pass the ontology and the recipe IRI to the Refactor that returns the refactored graph
                 * expressed by using the given vocabulary.
                 */
                try {
    
                    Recipe recipe = ruleStore.getRecipe(IRI.create(engineConfiguration.getRecipeId()));
    
                    log.debug("Rules in the recipe are : " + recipe.getkReSRuleList().size(), this);
    
                    log.debug("The ontology to be refactor is : " + ontology, this);
    
                    ontology = refactorer.ontologyRefactoring(ontology, IRI.create(engineConfiguration.getRecipeId()));
    
                } catch (RefactoringException e) {
                    log.error("The refactoring engine failed the execution.", e);
                } catch (NoSuchRecipeException e) {
                    log.error("The recipe with ID " + engineConfiguration.getRecipeId() + " does not exists", e);
                }
    
                log.debug("Merged ontologies in " + ontology);
    
                /*
                 * The new generated ontology is converted to Clarezza format and than added os substitued to the
                 * old mGraph.
                 */
                if (engineConfiguration.isInGraphAppendMode()) {
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
                sessionManager.destroySession(session.getID());
                
                
            } catch (OWLOntologyCreationException e) {
                log.error("Cannot create the ontology for the refactoring", e);
            }
        }
    }

    // /**
    // * Setup the KReS session
    // *
    // * @return
    // */
    // private String createAndAddSessionSpaceToScope() {
    // /*
    // * Retrieve the session manager
    // */
    // // SessionManager sessionManager = onManager.getSessionManager();
    // log.debug("Starting create session for the dulcifier");
    //
    // /*
    // * Create and setup the session. TODO FIXME This is an operation that should be made easier for
    // * developers to do through the API
    // */
    // Session session = sessionManager.createSession();
    // // OntologySpaceFactory ontologySpaceFactory = onManager.getOntologySpaceFactory();
    // // OntologySpace sessionSpace = ontologySpaceFactory.createSessionOntologySpace(scope.getID());
    // // try {
    // // scope.addSessionSpace(sessionSpace, session.getID());
    // // } catch (UnmodifiableOntologySpaceException e) {
    // // log.error("Failed to add session space to unmodifiable scope " + scope, e);
    // // }
    //
    // /*
    // * Finally, we return the session ID to be used by the caller
    // */
    // log.debug("Session " + session.getID() + " created", this);
    // return session.getID();
    // }

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

            @Override
            public String getStorageKey() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Object getTriplesProvider() {
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
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(final ComponentContext context) throws ConfigurationException {
    	super.activate(context);
		
        this.context = context;
        
        Map<String,Object> config = new HashMap<String,Object>();
        Dictionary<String,Object> properties = (Dictionary<String,Object>)context.getProperties();
        //copy the properties to a map
        
        for(Enumeration<String> e = properties.keys();e.hasMoreElements();){
            String key = e.nextElement();
            config.put(key, properties.get(key));
            log.info("Configuration property: " + key + " :- " + properties.get(key));
        }
        
        engineConfiguration = new DefaultRefactorEnhancementEngineConf(properties);
        
        initEngine(engineConfiguration);
        
        log.info("Activated Refactor Enhancement Engine");

    }
    
    private void initEngine(RefactorEnhancementEngineConf engineConfiguration){
        
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
        String[] coreScopeOntologySet = engineConfiguration.getScopeCoreOntology();
        /*
        String[] coreScopeOntologySet;
        if (obj instanceof String[]) {
            coreScopeOntologySet = (String[]) obj;
        } else {
            String[] aux = new String[1];
            aux[0] = (String) obj;
            coreScopeOntologySet = aux;
        }
        */
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

            @Override
            public String getStorageKey() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Object getTriplesProvider() {
                // TODO Auto-generated method stub
                return null;
            }

        };

        // IRI dulcifierScopeIRI = IRI.create((String) context.getProperties().get(SCOPE));
        String scopeId = engineConfiguration.getScope();

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
        log.debug("Start create the Recipe", this);

        ruleStore.addRecipe(IRI.create(engineConfiguration.getRecipeId()), null);

        log.debug("The recipe has been created", this);

        /*
         * The set of rule to put in the recipe can be provided by the user. A default set of rules is
         * provided in /META-INF/default/seo_rules.sem. Use the property engine.refactor in the felix console
         * to pass to the engine your set of rules.
         */

        String recipeLocation = engineConfiguration.getRecipeLocation();

        InputStream recipeStream = null;
        String recipeString = null;

        if (recipeLocation != null && !recipeLocation.isEmpty()) {
            IDereferencer dereferencer = new Dereferencer();
            try {
                recipeStream = dereferencer.resolve(recipeLocation);
            } catch (FileNotFoundException e) {
                log.error("Recipe Stream is null.", this);
            }
            log.debug("Loaded recipe from an external source.", this);
        } else {
            recipeStream = RefactorEnhancementEngine.class
                    .getResourceAsStream("/META-INF/default/seo_rules.sem");
            log.debug("Loaded default recipe.", this);
        }
        
        
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
        
        log.debug("Recipe: "+recipeString, this);

        /*
         * step 3
         */
        try {
            // ruleStore.addRuleToRecipe(recipeIRI.toString(), kReSRuleSyntax);
            ruleStore.addRuleToRecipe(engineConfiguration.getRecipeId(), recipeString);
            log.debug("Added rules to recipe " + engineConfiguration.getRecipeId());
        } catch (NoSuchRecipeException e) {
            log.error("The recipe does not exists: ", e);
        }
    }
    
    
    @SuppressWarnings("unchecked")
    protected void createRefactorEngineComponent(ComponentFactory factory){
        //both create*** methods sync on the searcherAndDereferencerLock to avoid
        //multiple component instances because of concurrent calls
        synchronized (this.lock ) {
            if(refactorEngineComponentInstance == null){
                this.refactorEngineComponentInstance = factory.newInstance(OsgiUtils.copyConfig(context.getProperties()));
            }
        }
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
            log.debug("Removing recipe " + engineConfiguration.getRecipeId() + " from RuleStore.", this);
            RuleList recipeRuleList = ruleStore.getRecipe(IRI.create(engineConfiguration.getRecipeId())).getkReSRuleList();

            /*
             * step 2: remove the recipe
             */
            if (ruleStore.removeRecipe(IRI.create(engineConfiguration.getRecipeId()))) {
                log.debug("The recipe " + engineConfiguration.getRecipeId() + " has been removed correctly");
            } else {
                log.error("The recipe " + engineConfiguration.getRecipeId() + " can not be removed");
            }

            /*
             * step 3: remove the rules
             */
            for (Rule rule : recipeRuleList) {
                if (ruleStore.removeRule(rule)) {
                    log.debug("The rule " + rule.getRuleName() + " has been removed correctly");
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
            log.error("The recipe " + engineConfiguration.getRecipeId() + " doesn't exist", ex);
        }

        log.info("Deactivated Refactor Enhancement Engine");

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
