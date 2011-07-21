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
package org.apache.stanbol.ontologymanager.ontonet.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.Locations;
import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManagerConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryLoader;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryManager;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.io.OntologyRegistryIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.RegistryManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

public class TestOntologyRegistry {

    private static OWLOntologyManager ontologyManager;
    private static RegistryLoader loader;
    private static OntologyRegistryIRISource ontologySource;
    private static ONManagerConfiguration configuration;
    private static ONManager onm;

    @BeforeClass
    public static void setup() {
        final Dictionary<String,Object> emptyConfig = new Hashtable<String,Object>();
        configuration = new ONManagerConfigurationImpl(emptyConfig);
        RegistryManager regman = new RegistryManagerImpl(emptyConfig);
        // An ONManagerImpl with no store and default settings
        onm = new ONManagerImpl(null, null, configuration, regman, emptyConfig);
        ontologyManager = onm.getOwlCacheManager();
        loader = onm.getRegistryLoader();

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
        Registry r = onm.getRegistryManager().populateRegistry(oReg);
        assertNotNull(r);
        int count = 2;
        assertEquals(count, r.getChildren().length);
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
        OWLOntology oReg = virginOntologyManager.loadOntology(Locations._REGISTRY_TEST);
        Registry r1 = onm.getRegistryManager().populateRegistry(oReg);
        // Now the second registry.
        oReg = virginOntologyManager.loadOntology(Locations._REGISTRY_TEST_ADDITIONS);
        Registry r2 = onm.getRegistryManager().populateRegistry(oReg);
        assertNotNull(r2);
        int count = 2;
        assertEquals(count, r1.getChildren().length);
//        for (RegistryItem lib : r1.getChildren()) {
//            System.out.println("\t"+lib);
//            for (RegistryItem ont : lib.getChildren()) {
//                System.out.println("\t\t"+ont);      
//            }
//        }
//        for (RegistryItem lib : r2.getChildren()) {
//            System.out.println("\t"+lib);
//            for (RegistryItem ont : lib.getChildren()) {
//                System.out.println("\t\t"+ont);      
//            }
//        }
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
