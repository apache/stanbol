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
package org.apache.stanbol.ontologymanager.core.scope;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.commons.stanboltools.offline.OfflineMode;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OntologyNetworkConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.MissingOntologyException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.io.StoredOntologySource;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.Multiplexer;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.NoSuchScopeException;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeEventListener;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeFactory;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.servicesapi.util.OntologyNetworkConfigurationUtils;
import org.apache.stanbol.ontologymanager.sources.owlapi.BlankOntologySource;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The running context of a Stanbol Ontology Network Manager instance. From this object it is possible to
 * obtain factories, indices, registries and what have you.
 * 
 * @see ScopeManager
 * 
 */
@Component(immediate = true, metatype = true)
@Service({ScopeRegistry.class, ScopeManager.class, ONManager.class})
public class ScopeManagerImpl extends ScopeRegistryImpl implements ONManager {

    public static final String _CONFIG_ONTOLOGY_PATH_DEFAULT = "";
    public static final String _CONNECTIVITY_POLICY_DEFAULT = "TIGHT";
    public static final String _ID_SCOPE_REGISTRY_DEFAULT = "ontology";

    /**
     * The singleton instance of {@link ScopeManagerImpl}.
     */
    private static ScopeManagerImpl me = null;

    /**
     * Returns the instance of {@link ScopeManagerImpl} that was activated most recently. It can be used for
     * referencing this component from an object that is not an OSGi component (for instance, if making it a
     * component would introduce an activation cycle).
     * 
     * @return the singleton instance of {@link ScopeManagerImpl}.
     */
    public static ScopeManagerImpl get() {
        return me;
    }

    @Property(name = ScopeManager.CONFIG_ONTOLOGY_PATH, value = _CONFIG_ONTOLOGY_PATH_DEFAULT)
    private String configPath;

    @Property(name = ScopeManager.CONNECTIVITY_POLICY, options = {
                                                                  @PropertyOption(value = '%'
                                                                                          + ScopeManager.CONNECTIVITY_POLICY
                                                                                          + ".option.tight", name = "TIGHT"),
                                                                  @PropertyOption(value = '%'
                                                                                          + ScopeManager.CONNECTIVITY_POLICY
                                                                                          + ".option.loose", name = "LOOSE")}, value = _CONNECTIVITY_POLICY_DEFAULT)
    private String connectivityPolicyString;

    /*
     * TODO when adapters are implemented for exporting, factory implementations can be Clerezza-independent
     * and this object can become a scope factory itself.
     */
    @Reference
    private ScopeFactory scopeFactory;

    @Reference
    private OntologySpaceFactory spaceFactory;

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

    @Reference
    private OntologyProvider<?> ontologyProvider;

    private IRI ontonetNS = null;

    @Property(name = ScopeManager.ID_SCOPE_REGISTRY, value = _ID_SCOPE_REGISTRY_DEFAULT)
    private String scopeRegistryId;

    /*
     * The identifiers (not yet parsed as IRIs) of the ontology scopes that should be activated.
     */
    private String[] toActivate = new String[] {};

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ONManagerImpl instances do need to be configured! YOU
     * NEED TO USE
     * {@link #ONManagerImpl(OntologyProvider, OfflineConfiguration, OntologySpaceFactory, Dictionary)} or its
     * overloads, to parse the configuration and then initialise the rule store if running outside an OSGI
     * environment.
     */
    public ScopeManagerImpl() {
        super();
        // All bindings are deferred to the activator
    }

    /**
     * Used to instantiate an ONManagerImpl outside of an OSGi environment.
     * 
     * @param ontologyProvider
     *            the ontology provider that will be used for storing ontologies.
     * @param offline
     *            the offline configuration
     * @param spaceFactory
     *            the factory implementation to be used for creating ontology spaces
     * @param configuration
     *            the configuration of this ONManagerImpl
     */
    public ScopeManagerImpl(OntologyProvider<?> ontologyProvider,
                            OfflineConfiguration offline,
                            ScopeFactory scopeFactory,
                            OntologySpaceFactory spaceFactory,
                            Dictionary<String,Object> configuration) {
        this();
        this.ontologyProvider = ontologyProvider;
        this.scopeFactory = scopeFactory;
        this.spaceFactory = spaceFactory;
        this.offline = offline;
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
        log.info("in " + ScopeManagerImpl.class + " activate with context " + context);
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

        long before = System.currentTimeMillis();

        me = this; // Assign singleton instance

        // Parse configuration
        if (offline != null) ontonetNS = offline.getDefaultOntologyNetworkNamespace();

        scopeRegistryId = (String) configuration.get(ScopeManager.ID_SCOPE_REGISTRY);
        if (scopeRegistryId == null) scopeRegistryId = _ID_SCOPE_REGISTRY_DEFAULT;
        configPath = (String) configuration.get(ScopeManager.CONFIG_ONTOLOGY_PATH);
        if (configPath == null) configPath = _CONFIG_ONTOLOGY_PATH_DEFAULT;

        // Bind components, starting with the local directories.
        List<String> dirs = new ArrayList<String>();
        try {
            for (IRI iri : offline.getOntologySourceLocations())
                dirs.add(iri.toString());
        } catch (NullPointerException ex) {
            // Ok, go empty
        }

        bindResources();

        // String tfile = (String) configuration.get(CONFIG_FILE_PATH);
        // if (tfile != null) this.configPath = tfile;
        // String tns = (String) configuration.get(KRES_NAMESPACE);
        // if (tns != null) this.kresNs = tns;

        // configPath = (String) configuration.get(CONFIG_FILE_PATH);

        // If there is no configuration file, just start with an empty scope set

        Object connectivityPolicy = configuration.get(ScopeManager.CONNECTIVITY_POLICY);
        if (connectivityPolicy == null) {
            this.connectivityPolicyString = _CONNECTIVITY_POLICY_DEFAULT;
        } else {
            this.connectivityPolicyString = connectivityPolicy.toString();
        }

        String configPath = getOntologyNetworkConfigurationPath();

        if (configPath != null && !configPath.trim().isEmpty()) {
            OWLOntology oConf = null;
            OWLOntologyManager tempMgr = OWLOntologyManagerFactory.createOWLOntologyManager(offline
                    .getOntologySourceLocations().toArray(new IRI[0]));
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

        } else { // No ontology supplied. Access the local graph
            rebuildScopes();
        }

        log.debug(ScopeManager.class + " activated. Time : {} ms.", System.currentTimeMillis() - before);

    }

    protected void bindResources() {
        // if (ontologySpaceFactory == null) {
        // if (ontologyProvider.getStore() instanceof TcProvider) ontologySpaceFactory = new
        // ClerezzaOntologySpaceFactory(
        // (OntologyProvider<TcProvider>) ontologyProvider, new Hashtable<String,Object>());
        // }
        IRI iri = IRI.create(ontonetNS + scopeRegistryId + "/");
        spaceFactory.setDefaultNamespace(iri);

        // Add listeners
        // if (ontologyProvider instanceof ScopeEventListener) factory
        // .addScopeEventListener((ScopeEventListener) ontologyProvider);
        Multiplexer multiplexer = ontologyProvider.getOntologyNetworkDescriptor();
        this.addScopeRegistrationListener(multiplexer);
        this.addScopeEventListener(multiplexer);
    }

    private void bootstrapOntologyNetwork(OWLOntology configOntology) {
        if (configOntology == null) {
            log.info("Ontology Network Manager starting with empty scope set.");
            return;
        }
        try {

            /**
             * We create and register the scopes before activating
             */
            for (String scopeId : OntologyNetworkConfigurationUtils.getScopes(configOntology)) {

                String[] cores = OntologyNetworkConfigurationUtils.getCoreOntologies(configOntology, scopeId);
                String[] customs = OntologyNetworkConfigurationUtils.getCustomOntologies(configOntology,
                    scopeId);

                // "Be a man. Use printf"
                log.debug("Detected scope \"{}\"", scopeId);
                for (String s : cores)
                    log.debug("\tDetected core ontology {}", s);
                for (String s : customs)
                    log.debug("\tDetected custom ontology {}", s);

                // Create the scope
                log.debug("Rebuilding scope \"{}\"", scopeId);
                Scope sc = null;
                sc = /* factory. */createOntologyScope(scopeId, new BlankOntologySource());

                // Populate the core space
                if (cores.length > 0) {
                    OntologySpace corespc = sc.getCoreSpace();
                    corespc.tearDown();
                    for (int i = 0; i < cores.length; i++)
                        try {
                            corespc.addOntology(new RootOntologySource(IRI.create(cores[i])));
                        } catch (Exception ex) {
                            log.warn("Failed to import ontology " + cores[i], ex);
                            continue;
                        }
                }

                sc.setUp();
                registerScope(sc);
                sc.getCustomSpace().tearDown();
                for (String locationIri : customs) {
                    try {
                        OntologyInputSource<?> src = new RootOntologySource(IRI.create(locationIri));
                        sc.getCustomSpace().addOntology(src);
                        log.debug("Added ontology from location {}", locationIri);
                    } catch (UnmodifiableOntologyCollectorException e) {
                        log.error("An error occurred while trying to add the ontology from location: "
                                  + locationIri, e);
                        continue;
                    }
                }
                sc.getCustomSpace().setUp();
            }

            /**
             * Try to get activation policies
             */
            toActivate = OntologyNetworkConfigurationUtils.getScopesToActivate(configOntology);

            for (String scopeID : toActivate) {
                try {
                    scopeID = scopeID.trim();
                    setScopeActive(scopeID, true);
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

    private void configureScope(Scope scope) throws DuplicateIDException {
        if (scope.getCustomSpace() != null) {
            OntologySpace.ConnectivityPolicy policy;
            try {
                policy = OntologySpace.ConnectivityPolicy.valueOf(connectivityPolicyString);
            } catch (IllegalArgumentException e) {
                log.warn("The value {}", connectivityPolicyString);
                log.warn(" -- configured as default ConnectivityPolicy does not match any value of the Enumeration!");
                log.warn(" -- Setting the default policy as defined by the {}.",
                    OntologySpace.ConnectivityPolicy.class);
                policy = OntologySpace.ConnectivityPolicy.valueOf(_CONNECTIVITY_POLICY_DEFAULT);
            }
            scope.getCustomSpace().setConnectivityPolicy(policy);
        }
        // Commented out: for the time being we try not to propagate additions to scopes.
        // if (ontologyProvider instanceof OntologyCollectorListener) scope
        // .addOntologyCollectorListener((OntologyCollectorListener) ontologyProvider);
        fireScopeCreated(scope);
        this.registerScope(scope);
    }

    @Override
    public Scope createOntologyScope(String scopeID, OntologyInputSource<?>... coreOntologies) throws DuplicateIDException {
        Scope sc = scopeFactory.createOntologyScope(scopeID, coreOntologies);
        configureScope(sc);
        return sc;
    }

    /**
     * Deactivation of the ONManagerImpl resets all its resources.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        ontonetNS = null;
        configPath = null;
        log.info("in " + ScopeManagerImpl.class + " deactivate with context " + context);
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
    public OfflineConfiguration getOfflineConfiguration() {
        return offline;
    }

    @Override
    public String getOntologyNetworkConfigurationPath() {
        return configPath;
    }

    @Override
    public String getOntologyNetworkNamespace() {
        return ontonetNS.toString();
    }

    @Override
    public ScopeRegistry getScopeRegistry() {
        return this;
    }

    /**
     * Returns <code>true</code> only if Stanbol operates in {@link OfflineMode}.
     * 
     * @return the offline state
     */
    protected final boolean isOfflineMode() {
        return offlineMode != null;
    }

    private void rebuildScopes() {
        OntologyNetworkConfiguration struct = ontologyProvider.getOntologyNetworkConfiguration();
        for (String scopeId : struct.getScopeIDs()) {
            long before = System.currentTimeMillis();
            log.debug("Rebuilding scope with ID \"{}\".", scopeId);
            Collection<OWLOntologyID> coreOnts = struct.getCoreOntologyKeysForScope(scopeId);
            OntologyInputSource<?>[] srcs = new OntologyInputSource<?>[coreOnts.size()];
            int i = 0;
            for (OWLOntologyID coreOnt : coreOnts) {
                log.debug("Core ontology key : {}", coreOnts);
                srcs[i++] = new StoredOntologySource(coreOnt);
            }
            Scope scope;
            try {
                scope = createOntologyScope(scopeId, srcs);
            } catch (DuplicateIDException e) {
                String dupe = e.getDuplicateID();
                log.warn("Scope \"{}\" already exists and will be reused.", dupe);
                scope = getScope(dupe);
            }
            OntologySpace custom = scope.getCustomSpace();
            // Register even if some ontologies were to fail to be restored afterwards.
            scopeMap.put(scopeId, scope);
            for (OWLOntologyID key : struct.getCustomOntologyKeysForScope(scopeId))
                try {
                    log.debug("Custom ontology key : {}", key);
                    custom.addOntology(new StoredOntologySource(key));
                } catch (MissingOntologyException ex) {
                    log.error(
                        "Could not find an ontology with public key {} to be managed by scope \"{}\". Proceeding to next ontology.",
                        key, scopeId);
                    continue;
                } catch (Exception ex) {
                    log.error("Exception caught while trying to add ontology with public key " + key
                              + " to rebuilt scope \"" + scopeId + "\". proceeding to next ontology", ex);
                    continue;
                }
            log.info("Scope \"{}\" rebuilt in {} ms.", scopeId, System.currentTimeMillis() - before);
        }
    }

    @Override
    public synchronized void registerScope(Scope scope) throws DuplicateIDException {
        if (scope == null) throw new IllegalArgumentException("scope cannot be null.");
        String id = scope.getID();
        if (this.containsScope(id)) throw new DuplicateIDException(id,
                "Scope registry already contains ontology scope with ID " + id);
        // if (this.containsScope(id)) {
        // if (scope != getScope(id)) {
        // log.warn("Overriding different scope with same ID {}", id);
        // super.registerScope(scope);
        // } else log.warn("Ignoring unnecessary call to already registered scope {}", id);
        // } else
        super.registerScope(scope);
    }

    @Override
    public void setOntologyNetworkNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) throw new IllegalArgumentException(
                "namespace must be a non-null and non-empty string.");
        if (!namespace.endsWith("/")) {
            log.warn("OntoNet namespaces must be slash URIs, adding '/'.");
            namespace += "/";
        }
        this.ontonetNS = IRI.create(namespace);
    }

    protected void fireScopeCreated(Scope scope) {
        for (ScopeEventListener l : listeners)
            l.scopeCreated(scope);
    }

    @Override
    public OntologySpaceFactory getOntologySpaceFactory() {
        return spaceFactory;
    }

}
