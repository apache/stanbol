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
package org.apache.stanbol.ontologymanager.ontonet.impl.registry;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryManager;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Library;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, metatype = true)
@Service(RegistryManager.class)
public class RegistryManagerImpl implements RegistryManager {

    private static final boolean _LAZY_LOADING_DEFAULT = false;

    @Property(name = RegistryManager.LAZY_LOADING, boolValue = _LAZY_LOADING_DEFAULT)
    private boolean lazyLoading = _LAZY_LOADING_DEFAULT;

    private Map<IRI,Set<IRI>> libraryIndex = new HashMap<IRI,Set<IRI>>();

    @Property(name = RegistryManager.REGISTRY_LOCATIONS, cardinality = 1000)
    private String[] locations;

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<IRI,Set<IRI>> ontologyIndex = new HashMap<IRI,Set<IRI>>();

    private Map<IRI,Registry> registries = new HashMap<IRI,Registry>();

    @Property(name = RegistryManager.CACHING_POLICY, options = {
                                                                @PropertyOption(value = '%'
                                                                                        + RegistryManager.CACHING_POLICY
                                                                                        + ".option.registry", name = "registry"),
                                                                @PropertyOption(value = '%'
                                                                                        + RegistryManager.CACHING_POLICY
                                                                                        + ".option.all", name = "all")}, value = "all")
    private String cachingPolicyString;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the RegistryManagerImpl instances do need to be configured!
     * YOU NEED TO USE {@link #RegistryManagerImpl(Dictionary)} or its overloads, to parse the configuration
     * and then initialise the rule store if running outside an OSGI environment.
     */
    public RegistryManagerImpl() {}

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param configuration
     */
    public RegistryManagerImpl(Dictionary<String,Object> configuration) {
        this();
        activate(configuration);
    }

    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) {
        log.info("in {} activate with context {}", getClass(), context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    protected void activate(Dictionary<String,Object> configuration) {
        // Parse configuration.
        try {
            lazyLoading = (Boolean) (configuration.get(RegistryManager.LAZY_LOADING));
        } catch (Exception ex) {
            lazyLoading = _LAZY_LOADING_DEFAULT;
        }
        locations = (String[]) configuration.get(RegistryManager.REGISTRY_LOCATIONS);
        if (locations == null) locations = new String[] {};
        // TODO manage enum constants for caching policy.
        
        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        // Load registries
        for (String loc : locations) {
            try {
                OWLOntology o = mgr.loadOntology(IRI.create(loc));
            } catch (OWLOntologyAlreadyExistsException e) {
                log.info("Skipping cached ontology {}.", e.getOntologyID());
                continue;
            } catch (OWLOntologyCreationException e) {
                log.warn("Failed to load ontology " + loc + " - Skipping...", e);
                continue;
            }
        }

    }

    @Override
    public void addRegistry(Registry registry) {
        try {
            registries.put(IRI.create(registry.getURL()), registry);
            updateLocations();
        } catch (URISyntaxException e) {
            log.error("Failed to add ontology registry.", e);
        }
    }

    @Override
    public void clearRegistries() {
        registries.clear();
        updateLocations();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        lazyLoading = _LAZY_LOADING_DEFAULT;
        locations = null;
        log.info("in {} deactivate with context {}", getClass(), context);
    }

    @Override
    public Set<Library> getLibraries(IRI ontologyID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Registry> getRegistries() {
        return new HashSet<Registry>(registries.values());
    }

    @Override
    public Set<Registry> getRegistries(IRI libraryID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Registry getRegistry(IRI id) {
        return registries.get(id);
    }

    @Override
    public boolean isLazyLoading() {
        return lazyLoading;
    }

    @Override
    public void removeRegistry(IRI registryId) {
        registries.remove(registryId);
        updateLocations();
    }

    @Override
    public void setLazyLoading(boolean lazy) {
        this.lazyLoading = lazy;
    }

    protected synchronized void updateLocations() {
        Set<IRI> locations = Collections.unmodifiableSet(registries.keySet());
        this.locations = locations.toArray(new String[0]);
    }

}
