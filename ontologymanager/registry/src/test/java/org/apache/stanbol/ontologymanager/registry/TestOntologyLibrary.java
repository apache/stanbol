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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.registry.api.RegistryLoader;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.api.model.Registry;
import org.apache.stanbol.ontologymanager.registry.api.model.RegistryItem;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryLoaderImpl;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.apache.stanbol.ontologymanager.registry.impl.util.RegistryUtils;
import org.apache.stanbol.ontologymanager.registry.io.LibrarySource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * This class tests the correct loading of ontology libraries from an OWL repository. It also checks that
 * ontologies referred to only by other libraries in the same repository are not loaded.
 */
public class TestOntologyLibrary {

    private String registryResource = "/ontologies/registry/onmtest.owl";

    private static ONManager onm;

    private static RegistryLoader loader;

    private OWLOntologyManager virginOntologyManager;

    /**
     * Sets the ontology network manager and registry loader before running the tests.
     */
    @BeforeClass
    public static void setupTest() throws Exception {
        final Dictionary<String,Object> emptyConfig = new Hashtable<String,Object>();
        RegistryManager regman = new RegistryManagerImpl(emptyConfig);
        // An ONManagerImpl with no store and default settings
        onm = new ONManagerImpl(null, null, new ONManagerConfigurationImpl(emptyConfig), emptyConfig);
        loader = new RegistryLoaderImpl(regman, onm);
    }

    /**
     * Resets the {@link OWLOntologyManager} used for tests, since caching phenomena across tests could bias
     * the results.
     * 
     * @throws Exception
     *             if any error occurs;
     */
    @Before
    public void resetOntologyManager() throws Exception {
        virginOntologyManager = OWLManager.createOWLOntologyManager();
        URL url = getClass().getResource("/ontologies");
        virginOntologyManager.addIRIMapper(new AutoIRIMapper(new File(url.toURI()), true));
        url = getClass().getResource("/ontologies/registry");
        virginOntologyManager.addIRIMapper(new AutoIRIMapper(new File(url.toURI()), true));

        // *Not* adding mappers to empty resource directories.
        // It seems the Maven surefire plugin won't copy them.
        // url = getClass().getResource("/ontologies/odp");
        // virginOntologyManager.addIRIMapper(new AutoIRIMapper(new File(url.toURI()), true));
    }

    /**
     * Uses a plain {@link RegistryLoader} to load a single ontology library and checks for its expected hits
     * and misses.
     * 
     * @throws Exception
     */
    @Test
    public void testLibraryLoad() throws Exception {
        IRI localTestRegistry = IRI.create(getClass().getResource(registryResource));
        Registry reg = loader.loadRegistry(localTestRegistry, virginOntologyManager);
        assertTrue(reg.hasChildren());
        Library lib = null;
        // Look for test #Library2
        for (RegistryItem item : reg.getChildren()) {
            if (Locations.LIBRARY_TEST2.equals(item.getIRI())) {
                lib = (Library) item;
                break;
            }
        }
        assertNotNull(lib);
        // Should be in the library.
        boolean hasShould = RegistryUtils.containsOntologyRecursive(lib, Locations.CHAR_DROPPED);
        // Should NOT be in the library (belongs to another library in the same registry).
        boolean hasShouldNot = RegistryUtils.containsOntologyRecursive(lib, Locations.CHAR_ACTIVE);
        assertTrue(hasShould);
        assertFalse(hasShouldNot);
    }

    /**
     * Tests the creation of an ontology input source from a single library. Because the test is run offline,
     * import statements might be file URIs, so tests should not fail on this.
     * 
     * @throws Exception
     */
    @Test
    public void testLibrarySourceCreation() throws Exception {
        IRI localTestRegistry = IRI.create(getClass().getResource(registryResource));
        OntologyInputSource src = new LibrarySource(Locations.LIBRARY_TEST1, localTestRegistry,
                virginOntologyManager, loader);
        OWLOntology o = src.getRootOntology();
        boolean hasImporting = false, hasImported = false;
        for (OWLImportsDeclaration ax : o.getImportsDeclarations()) {
            // Since we added a local IRI mapping, import statements might be using file: IRIs instead of
            // HTTP, in which case IRI equality would fail. So it is enough here to just check the filename.
            String tmpstr = ax.getIRI().toString();
            if (!hasImporting && tmpstr.endsWith("characters_all.owl")) hasImporting = true;
            else if (!hasImported && tmpstr.endsWith("maincharacters.owl")) hasImported = true;
            if (hasImporting && hasImported) break;
        }
        assertTrue(hasImporting);
        assertTrue(hasImported);
    }

}
