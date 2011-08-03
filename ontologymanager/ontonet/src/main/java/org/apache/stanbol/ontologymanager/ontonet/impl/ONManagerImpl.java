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
package org.apache.stanbol.ontologymanager.ontonet.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.io.BlankOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.NoSuchScopeException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyIndex;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.ontonet.conf.OntologyNetworkConfigurationUtils;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.InMemoryOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.OWLOntologyManagerFactoryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.OntologyIndexImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.OntologyScopeFactoryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.OntologySpaceFactoryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.ScopeRegistryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.session.ScopeSessionSynchronizer;
import org.apache.stanbol.ontologymanager.ontonet.impl.session.SessionManagerImpl;
import org.apache.stanbol.owl.OWLOntologyManagerFactory;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The running context of a Stanbol Ontology Network Manager instance. From this object it is possible to
 * obtain factories, indices, registries and what have you.
 * 
 * @see ONManager
 * 
 */
@Component(immediate = true, metatype = true)
@Service(ONManager.class)
public class ONManagerImpl implements ONManager {

    /**
     * Utility class to speed up ontology network startup. <br>
     * TODO: it's most likely useless, remove it.
     * 
     * @author enrico
     * 
     */
    private class Helper {

        /**
         * Adds the ontology fromt he given iri to the custom space of the given scope
         * 
         * @param scopeID
         * @param locationIri
         */
        public synchronized void addToCustomSpace(String scopeID, String[] locationIris) {
            OntologyScope scope = getScopeRegistry().getScope(IRI.create(scopeID));

            scope.getCustomSpace().tearDown();
            for (String locationIri : locationIris) {
                try {
                    scope.getCustomSpace().addOntology(createOntologyInputSource(locationIri));
                    log.debug("Added " + locationIri + " to scope " + scopeID + " in the custom space.", this);
                } catch (UnmodifiableOntologySpaceException e) {
                    log.error("An error occurred while trying to add the ontology from location: "
                              + locationIri, e);
                }
            }
            scope.getCustomSpace().setUp();
        }

        private OntologyInputSource createOntologyInputSource(final String uri) {
            try {
                return new RootOntologyIRISource(IRI.create(uri));
            } catch (OWLOntologyCreationException e) {
                log.error("Cannot load the ontology {}", uri, e);
                return null;
            } catch (Exception e) {
                log.error("Cannot load the ontology {}", uri, e);
                return null;
            }
        }

    }

    public static final String _CONFIG_ONTOLOGY_PATH_DEFAULT = "";

    public static final String _ID_DEFAULT = "ontonet";

    public static final String _ONTOLOGY_NETWORK_NS_DEFAULT = "http://stanbol.apache.org/";

    @Property(name = ONManager.CONFIG_ONTOLOGY_PATH, value = _CONFIG_ONTOLOGY_PATH_DEFAULT)
    private String configPath;

    private Helper helper = null;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private OfflineConfiguration offline;

    /**
     * The {@link OfflineMode} is used by Stanbol to indicate that no external service should be referenced.
     * For this engine that means it is necessary to check if the used {@link ReferencedSite} can operate
     * offline or not.
     * 
     * @see #enableOfflineMode(OfflineMode)
     * @see #disableOfflineMode(OfflineMode)
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC, bind = "enableOfflineMode", unbind = "disableOfflineMode", strategy = ReferenceStrategy.EVENT)
    private OfflineMode offlineMode;

    private OntologyIndex oIndex;

    private OWLOntologyManagerFactoryImpl omgrFactory;

    private OntologyScopeFactory ontologyScopeFactory;

    private OntologySpaceFactory ontologySpaceFactory;

    @Property(name = ONManager.ID, value = _ID_DEFAULT)
    private String ontonetID;

    @Property(name = ONManager.ONTOLOGY_NETWORK_NS, value = _ONTOLOGY_NETWORK_NS_DEFAULT)
    private String ontonetNS;

    private OWLOntologyManager owlCacheManager;

    private OWLDataFactory owlFactory;

    private ScopeRegistry scopeRegistry;

    private SessionManager sessionManager;

    private ClerezzaOntologyStorage storage;

    @Reference
    private TcManager tcm;

    /*
     * The identifiers (not yet parsed as IRIs) of the ontology scopes that should be activated.
     */
    private String[] toActivate = new String[] {};

    @Reference
    private WeightedTcProvider wtcp;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ReengineerManagerImpl instances do need to be configured!
     * YOU NEED TO USE {@link #ONManagerImpl(TcManager, WeightedTcProvider, OfflineConfiguration, Dictionary)}
     * or its overloads, to parse the configuration and then initialise the rule store if running outside an
     * OSGI environment.
     */
    public ONManagerImpl() {
        super();
        // All bindings are deferred to the activator
    }

    /**
     * @deprecated use {@link #ONManagerImpl(TcManager, WeightedTcProvider, OfflineConfiguration, Dictionary)}
     *             instead. Note that if the deprecated method is used instead, its effect will be to copy the
     *             Dictionary context to a new {@link OfflineConfiguration} object.
     * @param tcm
     * @param wtcp
     * @param configuration
     */
    @Deprecated
    public ONManagerImpl(TcManager tcm, WeightedTcProvider wtcp, Dictionary<String,Object> configuration) {
        // Copy the same configuration to the ONManagerConfigurationImpl.
        this(tcm, wtcp, new OfflineConfigurationImpl(configuration), configuration);
    }

    /**
     * Constructor to be invoked by non-OSGi environments.
     * 
     * @param tcm
     *            the triple collection manager to be used for storing ontologies.
     * @param wtcp
     *            the triple collection provider to be used for storing ontologies.
     * @param onmconfig
     *            the configuration of this ontology network manager.
     * @param configuration
     *            additional parameters for the ONManager not included in {@link OfflineConfiguration}.
     */
    public ONManagerImpl(TcManager tcm,
                         WeightedTcProvider wtcp,
                         OfflineConfiguration onmconfig,
                         Dictionary<String,Object> configuration) {
        this();
        // Assume this.tcm this.wtcp and this.wtcp were not filled in by OSGi-DS.
        this.tcm = tcm;
        this.wtcp = wtcp;
        this.offline = onmconfig;
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
        log.info("in " + ONManagerImpl.class + " activate with context " + context);
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

        // Parse configuration
        ontonetID = (String) configuration.get(ONManager.ID);
        if (ontonetID == null) ontonetID = _ID_DEFAULT;
        ontonetNS = (String) configuration.get(ONManager.ONTOLOGY_NETWORK_NS);
        if (ontonetNS == null) ontonetNS = _ONTOLOGY_NETWORK_NS_DEFAULT;
        configPath = (String) configuration.get(ONManager.CONFIG_ONTOLOGY_PATH);
        if (configPath == null) configPath = _CONFIG_ONTOLOGY_PATH_DEFAULT;

        if (ontonetID == null || ontonetID.isEmpty()) {
            log.warn("The Ontology Network Manager configuration does not define a ID for the Ontology Network Manager");
        }

        // Bind components, starting with the local directories.
        List<String> dirs = new ArrayList<String>();
        try {
            for (IRI iri : offline.getOntologySourceLocations())
                dirs.add(iri.toString());
        } catch (NullPointerException ex) {
            // Ok, go empty
        }

        omgrFactory = new OWLOntologyManagerFactoryImpl(dirs);
        owlFactory = OWLManager.getOWLDataFactory();
        owlCacheManager = OWLOntologyManagerFactory.createOWLOntologyManager(offline
                .getOntologySourceLocations().toArray(new IRI[0]));

        // These depend on one another
        scopeRegistry = new ScopeRegistryImpl();
        oIndex = new OntologyIndexImpl(this);

        bindResources(this.tcm, this.wtcp);

        // String tfile = (String) configuration.get(CONFIG_FILE_PATH);
        // if (tfile != null) this.configPath = tfile;
        // String tns = (String) configuration.get(KRES_NAMESPACE);
        // if (tns != null) this.kresNs = tns;

        // configPath = (String) configuration.get(CONFIG_FILE_PATH);

        /*
         * If there is no configuration file, just start with an empty scope set
         */

        String configPath = getOntologyNetworkConfigurationPath();

        if (configPath != null && !configPath.trim().isEmpty()) {
            OWLOntology oConf = null;
            OWLOntologyManager tempMgr = omgrFactory.createOntologyManager(true);
            OWLOntologyDocumentSource oConfSrc = null;

            try {
                log.debug("Try to load the configuration ontology from a local bundle relative path");
                InputStream is = this.getClass().getResourceAsStream(configPath);
                oConfSrc = new StreamDocumentSource(is);
            } catch (Exception e1) {
                try {
                    log.debug("Cannot load from a local bundle relative path", e1);
                    log.debug("Try to load the configuration ontology resolving the given IRI");
                    IRI iri = IRI.create(configPath);
                    if (!iri.isAbsolute()) throw new Exception("IRI seems to be not absolute! value was: "
                                                               + iri.toQuotedString());
                    oConfSrc = new IRIDocumentSource(iri);
                } catch (Exception e) {
                    try {
                        log.debug("Cannot load from the web", e1);
                        log.debug("Try to load the configuration ontology as full local file path");
                        oConfSrc = new FileDocumentSource(new File(configPath));
                    } catch (Exception e2) {
                        log.error("Cannot load the configuration ontology from parameter value: "
                                  + configPath, e2);
                    }
                }
            }

            if (oConfSrc == null) {
                log.warn("No ONM configuration file found at path " + configPath
                         + ". Starting with blank scope set.");
            } else {
                try {
                    oConf = tempMgr.loadOntologyFromOntologyDocument(oConfSrc);
                } catch (OWLOntologyCreationException e) {
                    log.error("Cannot create the configuration ontology", e);
                }
            }

            // Create and populate the scopes from the config ontology.
            bootstrapOntologyNetwork(oConf);

        }
        log.debug("ONManager activated.");

    }

    protected void bindResources(TcManager tcm, WeightedTcProvider wtcp) {
        // At this stage we know if tcm and wtcp have been provided or not.

        /*
         * With the current implementation of OntologyStorage, we cannot live with either component being
         * null. So create the object only if both are not null.
         */

        if (tcm != null && wtcp != null) storage = new ClerezzaOntologyStorage(tcm, wtcp);
        // Manage this in-memory, so it won't have to be null.
        else {
            storage = new InMemoryOntologyStorage();
        }

        // Now create everything that depends on the Storage object.

        // These may require the OWL cache manager
        ontologySpaceFactory = new OntologySpaceFactoryImpl(scopeRegistry, storage, offline);
        ontologyScopeFactory = new OntologyScopeFactoryImpl(scopeRegistry, ontologySpaceFactory);
        ontologyScopeFactory.addScopeEventListener(oIndex);

        // // This requires the OWL cache manager
        // registryLoader = new RegistryLoaderImpl(this);

        // TODO : assign dynamically in case the FISE persistence store is not
        // available.
        // storage = new FISEPersistenceStorage();

        sessionManager = new SessionManagerImpl(IRI.create("http://kres.iks-project.eu/"),
                getScopeRegistry(), storage);
        sessionManager.addSessionListener(new ScopeSessionSynchronizer(this));
    }

    private void bootstrapOntologyNetwork(OWLOntology configOntology) {
        if (configOntology == null) {
            log.debug("Ontology Network Manager starting with empty scope set.");
            return;
        }
        try {

            /**
             * We create and register the scopes before activating
             */
            for (String scopeIRI : OntologyNetworkConfigurationUtils.getScopes(configOntology)) {

                String[] cores = OntologyNetworkConfigurationUtils
                        .getCoreOntologies(configOntology, scopeIRI);
                String[] customs = OntologyNetworkConfigurationUtils.getCustomOntologies(configOntology,
                    scopeIRI);

                // "Be a man. Use printf"
                log.debug("Scope " + scopeIRI);
                for (String s : cores) {
                    log.debug("\tCore ontology " + s);
                }
                for (String s : customs) {
                    log.debug("\tCustom ontology " + s);
                }

                // Create the scope
                IRI iri = IRI.create(scopeIRI);
                OntologyScope sc = null;
                sc = ontologyScopeFactory.createOntologyScope(iri, new BlankOntologySource());

                // Populate the core space
                if (cores.length > 0) {
                    OntologySpace corespc = sc.getCoreSpace();
                    corespc.tearDown();
                    for (int i = 0; i < cores.length; i++)
                        try {
                            corespc.addOntology(new RootOntologyIRISource(IRI.create(cores[i])));
                        } catch (Exception ex) {
                            log.warn("KReS :: failed to import ontology " + cores[i], ex);
                            continue;
                        }
                    // TODO: this call should be automatic
                    ((CustomOntologySpace) sc.getCustomSpace()).attachCoreSpace((CoreOntologySpace) corespc,
                        false);
                }

                sc.setUp();
                scopeRegistry.registerScope(sc);

                // getScopeHelper().createScope(scopeIRI);
                // getScopeHelper().addToCoreSpace(scopeIRI, cores);
                getScopeHelper().addToCustomSpace(scopeIRI, customs);
            }

            /**
             * Try to get activation policies
             */
            toActivate = OntologyNetworkConfigurationUtils.getScopesToActivate(configOntology);

            for (String scopeID : toActivate) {
                try {
                    IRI scopeId = IRI.create(scopeID.trim());
                    scopeRegistry.setScopeActive(scopeId, true);
                    log.info("Ontology scope " + scopeID + " activated.");
                } catch (NoSuchScopeException ex) {
                    log.warn("Tried to activate unavailable scope " + scopeID + ".");
                } catch (Exception ex) {
                    log.error("Exception caught while activating scope " + scopeID + " . Skipping.", ex);
                    continue;
                }
            }

        } catch (Throwable e) {
            log.warn("Invalid ONM configuration file found. " + "Starting with blank scope set.", e);
        }

    }

    /**
     * Deactivation of the ONManagerImpl resets all its resources.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        ontonetID = null;
        ontonetNS = null;
        configPath = null;
        log.info("in " + ONManagerImpl.class + " deactivate with context " + context);
    }

    /**
     * Called by the ConfigurationAdmin to unbind the {@link #offlineMode} if the service becomes unavailable
     * 
     * @param mode
     */
    protected final void disableOfflineMode(OfflineMode mode) {
        this.offlineMode = null;
    }

    /**
     * Called by the ConfigurationAdmin to bind the {@link #offlineMode} if the service becomes available
     * 
     * @param mode
     */
    protected final void enableOfflineMode(OfflineMode mode) {
        this.offlineMode = mode;
    }

    @Override
    public String getID() {
        // TODO Auto-generated method stub
        return null;
    }

    public OntologyIndex getOntologyIndex() {
        return oIndex;
    }

    public OWLOntologyManagerFactoryImpl getOntologyManagerFactory() {
        return omgrFactory;
    }

    @Override
    public String getOntologyNetworkConfigurationPath() {
        return configPath;
    }

    @Override
    public String getOntologyNetworkNamespace() {
        return ontonetNS;
    }

    /**
     * Returns the ontology scope factory that was created along with the manager context.
     * 
     * @return the ontology scope factory
     */
    public OntologyScopeFactory getOntologyScopeFactory() {
        return ontologyScopeFactory;
    }

    /**
     * Returns the ontology space factory that was created along with the manager context.
     * 
     * @return the ontology space factory
     */
    public OntologySpaceFactory getOntologySpaceFactory() {
        return ontologySpaceFactory;
    }

    public ClerezzaOntologyStorage getOntologyStore() {
        return storage;
    }

    public OWLOntologyManager getOwlCacheManager() {
        // return OWLManager.createOWLOntologyManager();
        return owlCacheManager;
    }

    /**
     * Returns a factory object that can be used for obtaining OWL API objects.
     * 
     * @return the default OWL data factory
     */
    public OWLDataFactory getOwlFactory() {
        return owlFactory;
    }

    public Helper getScopeHelper() {
        if (helper == null) {
            helper = new Helper();
        }
        return helper;
    }

    /**
     * Returns the unique ontology scope registry for this context.
     * 
     * @return the ontology scope registry
     */
    public ScopeRegistry getScopeRegistry() {
        return scopeRegistry;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public String[] getUrisToActivate() {
        return toActivate;
    }

    /**
     * Returns <code>true</code> only if Stanbol operates in {@link OfflineMode}.
     * 
     * @return the offline state
     */
    protected final boolean isOfflineMode() {
        return offlineMode != null;
    }

}
