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

import static org.apache.stanbol.ontologymanager.registry.MockOsgiContext.parser;
import static org.apache.stanbol.ontologymanager.registry.MockOsgiContext.tcManager;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.stanbol.ontologymanager.core.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology.ClerezzaOntologyProvider;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.CachingPolicy;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Test the correct creation and setup of {@link RegistryManager} implementations.
 */
public class TestRegistryManager {

    /*
     * This is the registry manager configuration (which varies across tests).
     */
    private static Dictionary<String,Object> configuration;

    /*
     * This can very well stay the same across tests.
     */
    private static OfflineConfiguration offline;

    /**
     * Resets all configurations (the offline and registry manager ones).
     * 
     * @throws Exception
     */
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

        MockOsgiContext.reset();

    }

    private RegistryManager regman;

    private OntologyProvider<?> provider;

    @Before
    public void setupTests() throws Exception {}

    @After
    public void cleanup() throws Exception {
        MockOsgiContext.reset();
        provider = new ClerezzaOntologyProvider(tcManager, offline, parser);
    }

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
        regman = new RegistryManagerImpl(offline, provider, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);
        assertSame(CachingPolicy.CENTRALISED, regman.getCachingPolicy());
        // All registries must have the same cache.
        Iterator<Library> it = regman.getLibraries().iterator();
        OntologyProvider<?> cache = it.next().getCache();
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
        assertFalse(lib.getOntologies(OWLOntology.class).isEmpty());
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
        regman = new RegistryManagerImpl(offline, provider, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);
        assertSame(CachingPolicy.DISTRIBUTED, regman.getCachingPolicy());
        // Each registry must have its own distinct cache.
        Iterator<Library> it = regman.getLibraries().iterator();
        OntologyProvider<?> cache = it.next().getCache();
        // Just checking against the first in the list.
        while (it.hasNext()) {
            assertNotSame(cache, it.next().getCache());
        }
    }

    /**
     * Verifies that by setting the loading policy to eager (LAZY_LOADING = false), any random library will
     * respond true to a call to {@link Library#isLoaded()} without ever "touching" its content.
     * 
     * @throws Exception
     */
    @Test
    public void testLoadingEager() throws Exception {
        // Change the caching policy and setup a new registry manager.
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.DISTRIBUTED);
        configuration.put(RegistryManager.LAZY_LOADING, false);
        regman = new RegistryManagerImpl(offline, provider, configuration);
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

    /**
     * Verifies that by setting the loading policy to lazy (LAZY_LOADING = true), any random library will
     * respond false to a call to {@link Library#isLoaded()}, until its content is "touched" via a call to
     * {@link Library#getOntologies()}, only after which will it return true.
     * 
     * @throws Exception
     */
    @Test
    public void testLoadingLazy() throws Exception {
        // Change the caching policy and setup a new registry manager.
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.CENTRALISED);
        configuration.put(RegistryManager.LAZY_LOADING, true);
        regman = new RegistryManagerImpl(offline, provider, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);

        // Now pick a library.
        Registry reg;
        Iterator<Registry> it = regman.getRegistries().iterator();
        do
            reg = it.next();
        // We need a registry with at least 2 libraries to check that only one will be loaded.
        while (it.hasNext() && !reg.hasChildren() || reg.getChildren().length < 2);
        assertNotNull(reg);

        // There has to be at least one library with 2 children or more from the test registries...
        Library lib1 = null, lib2 = null;
        RegistryItem[] children = reg.getChildren();
        assertTrue(children.length >= 2);
        for (int i = 0; i < children.length - 1 && lib1 == null && lib2 == null; i++) {
            if (children[i] instanceof Library) lib1 = (Library) (children[i]);
            if (children[i + 1] instanceof Library) lib2 = (Library) (children[i + 1]);
        }
        assertFalse(lib1 == lib2);
        assertNotNull(lib1);
        // ...but its ontologies must not be loaded yet.
        assertFalse(lib1.isLoaded());
        assertFalse(lib2.isLoaded());

        // Touch the library. Also test that the listener system works.
        assertFalse(lib1.getOntologies(OWLOntology.class).isEmpty());
        assertTrue(lib1.isLoaded());
        assertFalse(lib2.isLoaded());
    }

}
