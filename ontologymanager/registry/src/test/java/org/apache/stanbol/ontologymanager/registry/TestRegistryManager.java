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
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class TestRegistryManager {

    private RegistryManager regman;

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

    @Before
    public void setupTests() throws Exception {}

    @Test
    public void testCentralisedCaching() throws Exception {
        // Change the caching policy and setup a new registry manager.
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.CENTRALISED);
        regman = new RegistryManagerImpl(offline, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);
        assertSame(CachingPolicy.CENTRALISED, regman.getCachingPolicy());
        // All registries must have the same cache.
        Iterator<Registry> it = regman.getRegistries().iterator();
        OWLOntologyManager cache = it.next().getCache();
        while (it.hasNext())
            assertSame(cache, it.next().getCache());
    }

    @Test
    public void testDistributedCaching() throws Exception {
        // Change the caching policy and setup a new registry manager.
        configuration.put(RegistryManager.CACHING_POLICY, CachingPolicy.DISTRIBUTED);
        regman = new RegistryManagerImpl(offline, configuration);
        // Check that the configuration was set.
        assertNotNull(regman);
        assertSame(CachingPolicy.DISTRIBUTED, regman.getCachingPolicy());
        // Each registry must have its own distinct cache.
        Iterator<Registry> it = regman.getRegistries().iterator();
        OWLOntologyManager cache = it.next().getCache();
        while (it.hasNext())
            assertNotSame(cache, it.next().getCache());
    }

}
