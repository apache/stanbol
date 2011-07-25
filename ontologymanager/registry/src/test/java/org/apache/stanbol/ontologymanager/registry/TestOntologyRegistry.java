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

import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryOntology;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.apache.stanbol.ontologymanager.registry.io.RegistryIRISource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

public class TestOntologyRegistry {

    private static RegistryIRISource ontologySource;
    private static ONManager onm;
    private static RegistryManager regman;

    @BeforeClass
    public static void setup() {
        final Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(OfflineConfiguration.ONTOLOGY_PATHS, new String[] {"/ontologies", "/ontologies/registry"});
        OfflineConfiguration offline = new OfflineConfigurationImpl(config);
        regman = new RegistryManagerImpl(offline, config);
        // An ONManagerImpl with no store and same offline settings as the registry manager.
        onm = new ONManagerImpl(null, null, offline, config);
    }

    // private static boolean mapperIsSet = false;
    //
    // public void setupOfflineMapper() {
    // if (mapperIsSet) {} else {
    // ontologySource = new OntologyRegistryIRISource(testRegistryIri, ontologyManager, loader);
    // mapperIsSet = true;
    // }
    // }

    @Test
    public void testPopulateRegistry() throws Exception {
        OWLOntologyManager virginOntologyManager = OWLManager.createOWLOntologyManager();
        URL url = getClass().getResource("/ontologies/registry");
        assertNotNull(url);
        virginOntologyManager.addIRIMapper(new AutoIRIMapper(new File(url.toURI()), true));
        // Population is lazy; no need to add other mappers.

        OWLOntology oReg = virginOntologyManager.loadOntology(Locations._REGISTRY_TEST);
        Set<Registry> rs = regman.createModel(Collections.singleton(oReg));

        assertEquals(1, rs.size());
        Registry r = rs.iterator().next();
        assertTrue(r.hasChildren());
        // The nonexistent library should also be included, if using the more powerful algorithm.
        int count = 3; // set to 2 if using the less powerful algorithm.
        assertEquals(count, r.getChildren().length);

        for (RegistryItem ri : r.getChildren())
            assertTrue(ri.hasChildren());
    }

    /**
     * Verify that, when loading multiple registries that add library information to each other, the overall
     * model reflects the union of these registries.
     * 
     * @throws Exception
     */
    @Test
    public void testRegistryUnion() throws Exception {
        OWLOntologyManager virginOntologyManager = OWLManager.createOWLOntologyManager();
        URL url = getClass().getResource("/ontologies/registry");
        assertNotNull(url);
        virginOntologyManager.addIRIMapper(new AutoIRIMapper(new File(url.toURI()), true));
        // Population is lazy; no need to add other mappers.

        // Create the model from two overlapping registries.
        Set<OWLOntology> regs = new HashSet<OWLOntology>();
        regs.add(virginOntologyManager.loadOntology(Locations._REGISTRY_TEST));
        regs.add(virginOntologyManager.loadOntology(Locations._REGISTRY_TEST_ADDITIONS));
        Set<Registry> rs = regman.createModel(regs);

        for (Registry r : rs) {
            // The nonexistent library should also be included, if using the more powerful algorithm.
            if (Locations._REGISTRY_TEST.equals(r.getIRI())) assertEquals(3, r.getChildren().length); // set
                                                                                                      // to 2
                                                                                                      // if
                                                                                                      // using
                                                                                                      // the
                                                                                                      // less
                                                                                                      // powerful
                                                                                                      // algorithm.
            else if (Locations._REGISTRY_TEST_ADDITIONS.equals(r.getIRI())) assertEquals(1,
                r.getChildren().length);
            // check
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

    @Test
    public void testAddRegistryToSessionSpace() throws Exception {
        // setupOfflineMapper();
        IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
        SessionOntologySpace space = null;
        space = onm.getOntologySpaceFactory().createSessionOntologySpace(scopeIri);
        space.setUp();
        try {
            // space.addOntology(new
            // OntologyRegistryIRISource(testRegistryIri,onm.getOwlCacheManager(),onm.getRegistryLoader()));
            space.addOntology(ontologySource);
        } catch (UnmodifiableOntologySpaceException e) {
            fail("Adding libraries to session space failed. "
                 + "This should not happen for active session spaces.");
        }

        assertTrue(space.getTopOntology() != null);
        assertTrue(space.getOntologies().contains(space.getTopOntology()));
    }

    @Test
    public void testScopeCreationWithRegistry() {
        // setupOfflineMapper();
        IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
        OntologyScope scope = null;
        // The factory call also invokes loadRegistriesEager() and
        // gatherOntologies() so no need to test them individually.
        try {
            scope = onm.getOntologyScopeFactory().createOntologyScope(scopeIri, ontologySource);
        } catch (DuplicateIDException e) {
            fail("DuplicateID exception caught when creating test scope.");
        }

        assertTrue(scope != null && scope.getCoreSpace().getTopOntology() != null);
    }

    @Test
    public void testSpaceCreationWithRegistry() {
        // setupOfflineMapper();
        IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
        CoreOntologySpace space = null;
        // The factory call also invokes loadRegistriesEager() and
        // gatherOntologies() so no need to test them individually.
        space = onm.getOntologySpaceFactory().createCoreOntologySpace(scopeIri, ontologySource);
        assertTrue(space != null && space.getTopOntology() != null);
    }

}
