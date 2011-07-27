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
package org.apache.stanbol.ontologymanager.registry;

import static org.junit.Assert.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.CachingPolicy;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Test the correct creation and setup of {@link RegistryManager} implementations.
 */
public class TestRegistryManager {

    /*
     * This is the registry manager configuration (which varies across tests).
     */
    private static Dictionary<String,Object> configuration;

    /*
     * This can very well stay the same across
     */
    private static OfflineConfiguration offline;

    @BeforeClass
    public static void setup() throws Exception {
        configuration = new Hashtable<String,Object>();
        // We need this to make sure the local meta.owl (which does not import codolight) is loaded.
        configuration.put(OfflineConfiguration.ONTOLOGY_PATHS, new String[] {"/ontologies",
                                                                             "/ontologies/registry"});
        configuration.put(
            RegistryManager.REGISTRY_LOCATIONS,
            new String[] {
                          TestRegistryManager.class.getResource("/ontologies/registry/onmtest.owl")
                                  .toString(),
                          TestRegistryManager.class.getResource("/ontologies/registry/onmtest_additions.owl")
                                  .toString()});
        offline = new OfflineConfigurationImpl(configuration);
    }

    private RegistryManager regman;

    @Before
    public void setupTests() throws Exception {}

    /**
     * Verifies that by instantiating a new {@link RegistryManager} with a centralised caching policy and
     * loading two registries, they share the same cache ontology manager.
     * 
     * @throws Exception
     */
    @Test
    public void testCachingCentralised() throws Exception {
        // Change the caching policy and setup a new registry manager.
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.CENTRALISED);
        regman = new RegistryManagerImpl(offline, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);
        assertSame(CachingPolicy.CENTRALISED, regman.getCachingPolicy());
        // All registries must have the same cache.
        Iterator<Library> it = regman.getLibraries().iterator();
        OWLOntologyManager cache = it.next().getCache();
        while (it.hasNext())
            assertSame(cache, it.next().getCache());

        // Now "touch" a library.
        Registry reg;
        do
            reg = regman.getRegistries().iterator().next();
        while (!reg.hasChildren());
        assertNotNull(reg);

        // There has to be at least one non-empty lib from the test ontologies.
        Library lib = null;
        RegistryItem[] children = reg.getChildren();
        for (int i = 0; i < children.length && lib == null; i++)
            if (children[i] instanceof Library) lib = (Library) (children[i]);
        assertNotNull(lib);

        // Touch the library. Also test that the listener system works.
        assertFalse(lib.getOntologies().isEmpty());
    }

    /**
     * Verifies that by instantiating a new {@link RegistryManager} with a distributed caching policy and
     * loading two registries, they have different ontology managers.
     * 
     * @throws Exception
     */
    @Test
    public void testCachingDistributed() throws Exception {
        // Change the caching policy and setup a new registry manager.
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.DISTRIBUTED);
        regman = new RegistryManagerImpl(offline, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);
        assertSame(CachingPolicy.DISTRIBUTED, regman.getCachingPolicy());
        // Each registry must have its own distinct cache.
        Iterator<Library> it = regman.getLibraries().iterator();
        OWLOntologyManager cache = it.next().getCache();
        // Just checking against the first in the list.
        while (it.hasNext()) {
            assertNotSame(cache, it.next().getCache());
        }
    }

    @Test
    public void testLoadingEager() throws Exception {
        // Change the caching policy and setup a new registry manager.
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.DISTRIBUTED);
        configuration.put(RegistryManager.LAZY_LOADING, false);
        regman = new RegistryManagerImpl(offline, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);

        // Now pick a library.
        Registry reg;
        do
            reg = regman.getRegistries().iterator().next();
        while (!reg.hasChildren());
        assertNotNull(reg);

        // There has to be at least one non-empty library from the test registries...
        Library lib = null;
        RegistryItem[] children = reg.getChildren();
        for (int i = 0; i < children.length && lib == null; i++)
            if (children[i] instanceof Library) lib = (Library) (children[i]);
        assertNotNull(lib);
        // ...and its ontologies must already be loaded without having to request them.
        assertTrue(lib.isLoaded());
    }

    @Test
    public void testLoadingLazy() throws Exception {
        // Change the caching policy and setup a new registry manager.
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.CENTRALISED);
        configuration.put(RegistryManager.LAZY_LOADING, true);
        regman = new RegistryManagerImpl(offline, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);

        // Now pick a library.
        Registry reg;
        do
            reg = regman.getRegistries().iterator().next();
        while (!reg.hasChildren());
        assertNotNull(reg);

        // There has to be at least one non-empty library from the test registries...
        Library lib = null;
        RegistryItem[] children = reg.getChildren();
        for (int i = 0; i < children.length && lib == null; i++)
            if (children[i] instanceof Library) lib = (Library) (children[i]);
        assertNotNull(lib);
        // ...but its ontologies must not be loaded yet.
        assertFalse(lib.isLoaded());

        // Touch the library. Also test that the listener system works.
        assertFalse(lib.getOntologies().isEmpty());
        assertTrue(lib.isLoaded());
    }

}
