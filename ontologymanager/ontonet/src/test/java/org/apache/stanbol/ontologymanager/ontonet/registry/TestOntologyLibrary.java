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

import static org.junit.Assert.*;

import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.Locations;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManagerConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.RegistryLoader;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.io.LibrarySource;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.Registry;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.models.RegistryItem;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.RegistryLoaderImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.registry.model.RegistryImpl;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * This class tests the correct loading of ontology libraries from an OWL repository. It also checks that
 * ontologies referred to only by other libraries in the same repository are not loaded.
 */
public class TestOntologyLibrary {

    private String registryResource = "/ontologies/registry/onmtest.owl";

    private ONManager onm;

    private RegistryLoader loader;

    /**
     * Reset the ontology network manager and registry loader before running each test.
     */
    @Before
    public void setupTest() throws Exception {

        // An ONManagerImpl with no store and default settings
        ONManagerConfiguration configuration = new ONManagerConfigurationImpl(new Hashtable<String,Object>());
        onm = new ONManagerImpl(null, null, configuration, new Hashtable<String,Object>());

        // *Not* adding mappers to empty resource directories.
        // It seems the Maven surefire plugin won't copy them.

        // onm.getOwlCacheManager().addIRIMapper(
        // new AutoIRIMapper(new File(getClass().getResource("/ontologies/odp").toURI()), true));

        loader = new RegistryLoaderImpl(onm);

    }

    /**
     * Utility method to recurse into registry items.
     * 
     * TODO: move this to main?
     * 
     * @param item
     * @param ontologyId
     * @return
     */
    private boolean containsOntologyRecursive(RegistryItem item, IRI ontologyId) {

        boolean result = false;
        if (item.isOntology()) {
            // An Ontology MUST have a non-null URI.
            try {
                IRI iri = IRI.create(item.getURL());
                result |= iri.equals(ontologyId);
            } catch (Exception e) {
                return false;
            }
        } else if (item.isLibrary() || item instanceof RegistryImpl)
        // Inspect children
        for (RegistryItem child : ((RegistryItem) item).getChildren()) {
            result |= containsOntologyRecursive(child, ontologyId);
            if (result) break;
        }
        return result;

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
        Registry lib = loader.loadLibraryEager(localTestRegistry, Locations.LIBRARY_TEST2);
        assertTrue(lib.hasChildren());
        // Should be in the library.
        boolean hasShould = containsOntologyRecursive(lib, Locations.CHAR_DROPPED);
        // Should NOT be in the library (belongs to another library in the same registry).
        boolean hasShouldNot = containsOntologyRecursive(lib, Locations.CHAR_ACTIVE);
        assertTrue(hasShould && !hasShouldNot);
    }

    /**
     * Tests the creation of an ontology input source from a single library. Because the test is run offline,
     * import statements might be file URIs, so tests will not fail on this.
     * 
     * @throws Exception
     */
    @Test
    public void testLibrarySourceCreation() throws Exception {
        IRI localTestRegistry = IRI.create(getClass().getResource(registryResource));
        OntologyInputSource src = new LibrarySource(Locations.LIBRARY_TEST1, localTestRegistry,
                onm.getOwlCacheManager(), loader);
        OWLOntology o = src.getRootOntology();
        boolean hasImporting = false, hasImported = false;
        for (OWLImportsDeclaration ax : o.getImportsDeclarations()) {
            // Since we added a local IRI mapping, import statements might be using file: IRIs instead of
            // HTTP, in which case IRI equality would fail.
            String tmpstr = ax.getIRI().toString();
            if (!hasImporting && tmpstr.endsWith("characters_all.owl")) hasImporting = true;
            else if (!hasImported && tmpstr.endsWith("maincharacters.owl")) hasImported = true;
            if (hasImporting && hasImported) break;
        }
        assertTrue(hasImporting && hasImported);
    }

}
