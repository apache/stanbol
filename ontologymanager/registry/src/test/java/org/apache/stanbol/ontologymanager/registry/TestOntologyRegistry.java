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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.ontologymanager.core.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology.ClerezzaOntologyProvider;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * Verifies the correct setup of ontology registries. As these objects are virtually unaware of the OSGi
 * environment, the context is constructed using plain Java objects.
 */
public class TestOntologyRegistry {

    private static RegistryManager regman;

    /*
     * This ontology manager will be empty on every test, except that it will have mappings to test resources.
     */
    private OWLOntologyManager virginOntologyManager;

    /**
     * Sets the registry and ontology network managers, which are immutable across tests.
     */
    @BeforeClass
    public static void setup() {
        // We use a single Dictionary for storing all configurations.
        final Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(OfflineConfiguration.ONTOLOGY_PATHS, new String[] {"/ontologies", "/ontologies/registry"});
        OfflineConfiguration offline = new OfflineConfigurationImpl(config);
        // The registry manager can be updated via calls to createModel()
        regman = new RegistryManagerImpl(offline, new ClerezzaOntologyProvider(new SimpleTcProvider(),
                offline, Parser.getInstance()), config);
    }

    /**
     * Resets the virgin ontology manager for each test.
     * 
     * @throws Exception
     */
    @Before
    public void setupSources() throws Exception {
        virginOntologyManager = OWLManager.createOWLOntologyManager();
        // Add mappings for any ontologies found in ontologies/registry
        URL url = getClass().getResource("/ontologies/registry");
        assertNotNull(url);
        virginOntologyManager.addIRIMapper(new AutoIRIMapper(new File(url.toURI()), true));
        // Population is lazy; no need to add other mappers.
    }

    /**
     * Verifies that a call to {@link RegistryManager#createModel(Set)} with a registry location creates the
     * object model accordingly.
     * 
     * @throws Exception
     */
    @Test
    public void testPopulateRegistry() throws Exception {

        // Create the model from a single registry.
        OWLOntology oReg = virginOntologyManager.loadOntology(Locations._REGISTRY_TEST);
        Set<Registry> rs = regman.createModel(Collections.singleton(oReg));

        // There has to be a single registry, with the expected number of children.
        assertEquals(1, rs.size());
        Registry r = rs.iterator().next();
        assertTrue(r.hasChildren());
        // The nonexistent library should also be included, if using the more powerful algorithm.
        int count = 3; // set to 2 if using the less powerful algorithm.
        assertEquals(count, r.getChildren().length);
        // There are no libraries without ontologies in the test registry.
        for (RegistryItem ri : r.getChildren())
            assertTrue(ri.hasChildren());
    }

    @Test
    public void testLoopInLibrary() throws Exception {
        // Create the model from the looping registry.
        OWLOntology oReg = virginOntologyManager.loadOntology(Locations._REGISTRY_TEST_LOOP);
        Set<Registry> rs = regman.createModel(Collections.singleton(oReg));

        // There has to be a single registry, with the expected number of children (one).
        assertEquals(1, rs.size());
        Registry r = rs.iterator().next();
        assertTrue(r.hasChildren());
        int count = 1;
        assertEquals(count, r.getChildren().length);
        // There are no libreries without ontologies in the test registry.
        for (RegistryItem child : r.getChildren()) {
            assertTrue(child instanceof Library);
            // Check both parent-child relations.
            assertTrue(child.hasChildren());
            for (RegistryItem grandchild : child.getChildren()) {
                assertTrue(grandchild instanceof RegistryOntology);
                assertTrue(grandchild.hasParents());
                assertTrue(Arrays.asList(grandchild.getParents()).contains(child));
            }
        }

    }

    /**
     * Verifies that, when loading multiple registries that add library information to each other, the overall
     * model reflects the union of these registries.
     * 
     * @throws Exception
     */
    @Test
    public void testRegistryUnion() throws Exception {

        // Create the model from two overlapping registries.
        Set<OWLOntology> regs = new HashSet<OWLOntology>();
        regs.add(virginOntologyManager.loadOntology(Locations._REGISTRY_TEST));
        regs.add(virginOntologyManager.loadOntology(Locations._REGISTRY_TEST_ADDITIONS));
        Set<Registry> rs = regman.createModel(regs);

        for (Registry r : rs) {
            // The nonexistent library should also be included, if using the more powerful algorithm.
            int count = 3; // set to 2 if using the less powerful algorithm.
            if (Locations._REGISTRY_TEST.equals(r.getIRI())) assertEquals(count, r.getChildren().length);
            else if (Locations._REGISTRY_TEST_ADDITIONS.equals(r.getIRI())) assertEquals(1,
                r.getChildren().length);
            // Check that we find the expected ontology in the expected library.
            for (RegistryItem lib : r.getChildren()) {
                if (Locations.LIBRARY_TEST1.equals(lib.getIRI())) {
                    boolean found = false;
                    for (RegistryItem child : lib.getChildren()) {
                        if (child instanceof RegistryOntology && Locations.ONT_TEST1.equals(child.getIRI())) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue(found);
                    break;
                }

            }
        }
    }

}
