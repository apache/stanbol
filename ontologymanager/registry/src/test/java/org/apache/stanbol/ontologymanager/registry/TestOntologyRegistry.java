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

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.apache.stanbol.ontologymanager.registry.io.RegistryIRISource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * Verifies the correct setup of ontology registries.
 */
public class TestOntologyRegistry {

    private IRI scopeIri = IRI.create(Locations.__STANBOL_ONT_NAMESPACE + "Scope");
    private static RegistryIRISource ontologySource; // Null until the RegistryIRISource stays in place.
    private static ONManager onm;
    private static RegistryManager regman;
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
        regman = new RegistryManagerImpl(offline, config);
        // An ONManager with no storage support and same offline settings as the registry manager.
        onm = new ONManagerImpl(null, null, offline, config);
    }

    /**
     * Resets the virgin ontology manager for each test.
     * 
     * @throws Exception
     */
    @Before
    public void setupSources() throws Exception {
        virginOntologyManager = OWLManager.createOWLOntologyManager();
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
        // There are no libreries without ontologies in the test registry.
        for (RegistryItem ri : r.getChildren())
            assertTrue(ri.hasChildren());
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

    /**
     * Verifies that the addition of a null or valid registry source to a session space works.
     */
    @Test
    public void testAddRegistryToSessionSpace() throws Exception {
        SessionOntologySpace space = null;
        space = onm.getOntologySpaceFactory().createSessionOntologySpace(scopeIri);
        space.setUp();
        // space.addOntology(new
        // OntologyRegistryIRISource(testRegistryIri,onm.getOwlCacheManager(),onm.getRegistryLoader()));
        space.addOntology(ontologySource);
        assertTrue(space.getTopOntology() != null);
        assertTrue(space.getOntologies().contains(space.getTopOntology()));
    }

    /**
     * Verifies that an ontology scope with a null or valid registry source is created correctly.
     */
    @Test
    public void testScopeCreationWithRegistry() throws Exception {
        OntologyScope scope = null;
        // The input source instantiation automatically loads the entire content of a registry, no need to
        // test loading methods individually.
        scope = onm.getOntologyScopeFactory().createOntologyScope(scopeIri, ontologySource);
        assertTrue(scope != null && scope.getCoreSpace().getTopOntology() != null);
    }

    /**
     * Verifies that an ontology space with a null or valid registry source is created correctly.
     */
    @Test
    public void testSpaceCreationWithRegistry() throws Exception {
        // setupOfflineMapper();
        CoreOntologySpace space = null;
        // The input source instantiation automatically loads the entire content of a registry, no need to
        // test loading methods individually.
        space = onm.getOntologySpaceFactory().createCoreOntologySpace(scopeIri, ontologySource);
        assertTrue(space != null && space.getTopOntology() != null);
    }

}
