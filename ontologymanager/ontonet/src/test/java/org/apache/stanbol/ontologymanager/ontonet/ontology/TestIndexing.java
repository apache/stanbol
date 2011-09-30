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
package org.apache.stanbol.ontologymanager.ontonet.ontology;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.ParentPathInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyIndex;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.owl.OWLOntologyManagerFactory;
import org.junit.BeforeClass;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class TestIndexing {

    private static ONManager onm;

    private static OWLOntologyManager mgr;

    private static IRI iri_minor = IRI
            .create("http://stanbol.apache.org/ontologies/pcomics/minorcharacters.owl"), iri_main = IRI
            .create("http://stanbol.apache.org/ontologies/pcomics/maincharacters.owl"), scopeIri = IRI
            .create("http://stanbol.apache.org/scope/IndexingTest"), testRegistryIri = IRI
            .create("http://stanbol.apache.org/ontologies/registries/onmtest.owl");

    private static OntologyScope scope = null;

    @BeforeClass
    public static void setup() {
        final Dictionary<String,Object> emptyConfig = new Hashtable<String,Object>();
        final OfflineConfiguration offline = new OfflineConfigurationImpl(emptyConfig);
        // RegistryManager regman = new RegistryManagerImpl(emptyConfig);
        // An ONManagerImpl with no store and default settings
        onm = new ONManagerImpl(null, null, offline, emptyConfig);
        mgr = OWLOntologyManagerFactory.createOWLOntologyManager(offline.getOntologySourceLocations()
                .toArray(new IRI[0]));

        // Since it is registered, this scope must be unique, or subsequent
        // tests will fail on duplicate ID exceptions!
        scopeIri = IRI.create("http://stanbol.apache.org/scope/IndexingTest");
        IRI coreroot = IRI.create(scopeIri + "/core/root.owl");

        @SuppressWarnings("unused")
        OWLOntology oParent = null;
        try {
            oParent = mgr.createOntology(coreroot);
        } catch (OWLOntologyCreationException e1) {
            // Uncomment if annotated with @BeforeClass instead of @Before
            fail("Could not create core root ontology.");
        }
        // The factory call also invokes loadRegistriesEager() and
        // gatherOntologies() so no need to test them individually.
        // try {
        // scope = onm.getOntologyScopeFactory().createOntologyScope(
        // scopeIri,
        // new RegistryIRISource(testRegistryIri, onm.getOwlCacheManager(), onm
        // .getRegistryLoader(), null
        // // new RootOntologySource(oParent
        // ));
        //
        // onm.getScopeRegistry().registerScope(scope);
        // } catch (DuplicateIDException e) {
        // // Uncomment if annotated with @BeforeClass instead of @Before ,
        // // comment otherwise.
        // fail("DuplicateID exception caught when creating test scope.");
        // }
    }

    // @Test
    public void testAddOntology() throws Exception {
        OntologyIndex index = onm.getOntologyIndex();

        // Load communities ODP (and its import closure) from local resource.
        URL url = getClass().getResource("/ontologies/characters_all.owl");
        assertNotNull(url);
        File f = new File(url.toURI());
        assertNotNull(f);
        OntologyInputSource commSrc = new ParentPathInputSource(f);

        OntologySpace cust = scope.getCustomSpace();
        cust.addOntology(commSrc);

        assertTrue(index.isOntologyLoaded(iri_minor));

        url = getClass().getResource("/ontologies/minorcharacters.owl");
        assertNotNull(url);
        f = new File(url.toURI());
        assertNotNull(f);

        cust.removeOntology(new ParentPathInputSource(f));
        // cust.removeOntology(commSrc);

        assertFalse(index.isOntologyLoaded(iri_minor));
    }

    // @Test
    public void testGetOntology() throws Exception {
        // Load the original objectRole ODP
        OWLOntology oObjRole = mgr.loadOntology(iri_main);
        assertNotNull(oObjRole);
        // Compare it against the one indexed.
        // FIXME reinstate these checks
        // OntologyIndex index = onm.getOntologyIndex();
        // assertNotNull(index.getOntology(objrole));
        // // assertSame() would fail.
        // assertEquals(index.getOntology(objrole), oObjRole);
    }

    // @Test
    public void testIsOntologyLoaded() {
        OntologyIndex index = onm.getOntologyIndex();
        IRI coreroot = IRI.create(scopeIri + "/core/root.owl");
        IRI dne = IRI.create("http://www.ontologydesignpatterns.org/cp/owl/doesnotexist.owl");
        IRI objrole = IRI.create("http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl");

        // FIXME reinstate these checks
        // assertTrue(index.isOntologyLoaded(coreroot));
        // assertTrue(index.isOntologyLoaded(objrole));
        // TODO : find a way to index anonymous ontologies
        assertTrue(!index.isOntologyLoaded(dne));
    }

}
