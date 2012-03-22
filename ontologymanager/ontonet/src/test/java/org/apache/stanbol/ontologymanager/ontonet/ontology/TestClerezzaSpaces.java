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

import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.parser;
import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.reset;
import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.tcManager;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.commons.owl.util.URIUtils;
import org.apache.stanbol.ontologymanager.ontonet.Constants;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.BlankOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.ParentPathInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.ClerezzaOntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.ClerezzaOWLUtils;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.OntologySpaceFactoryImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.ScopeRegistryImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class TestClerezzaSpaces {

    public static IRI baseIri = IRI.create(Constants.PEANUTS_MAIN_BASE), baseIri2 = IRI
            .create(Constants.PEANUTS_MINOR_BASE);

    private static OWLAxiom linusIsHuman = null;

    private static OntologySpaceFactory factory;

    private static OntologyInputSource<?,?> inMemorySrc, minorSrc, dropSrc, nonexSrc;

    private static OfflineConfiguration offline;

    private static OntologyInputSource<TripleCollection,?> getLocalSource(String resourcePath) {
        InputStream is = TestOntologySpaces.class.getResourceAsStream(resourcePath);
        return new GraphSource(parser.parse(is, SupportedFormat.RDF_XML));
    }

    private static OntologyInputSource<?,?> getLocalSource(String resourcePath, OWLOntologyManager mgr) throws OWLOntologyCreationException,
                                                                                                       URISyntaxException {
        URL url = TestOntologySpaces.class.getResource(resourcePath);
        File f = new File(url.toURI());
        return new ParentPathInputSource(f, mgr != null ? mgr
                : OWLOntologyManagerFactory.createOWLOntologyManager(offline.getOntologySourceLocations()
                        .toArray(new IRI[0])));
    }

    @BeforeClass
    public static void setup() throws Exception {
        offline = new OfflineConfigurationImpl(new Hashtable<String,Object>());
        ScopeRegistry reg = new ScopeRegistryImpl();

        // This one is created from scratch
        MGraph ont2 = ClerezzaOWLUtils.createOntology(baseIri2.toString());
        minorSrc = new GraphSource(ont2.getGraph());
        dropSrc = getLocalSource("/ontologies/droppedcharacters.owl");
        nonexSrc = getLocalSource("/ontologies/nonexistentcharacters.owl");
        inMemorySrc = new RootOntologyIRISource(IRI.create(TestClerezzaSpaces.class
                .getResource("/ontologies/maincharacters.owl")));

        OWLDataFactory df = OWLManager.getOWLDataFactory();
        OWLClass cHuman = df.getOWLClass(IRI.create(baseIri + "/" + Constants.humanBeing));
        OWLIndividual iLinus = df.getOWLNamedIndividual(IRI.create(baseIri + "/" + Constants.linus));
        linusIsHuman = df.getOWLClassAssertionAxiom(cHuman, iLinus);

        factory = new OntologySpaceFactoryImpl(reg, new ClerezzaOntologyProvider(tcManager, offline, parser),
                offline, IRI.create("http://stanbol.apache.org/ontology/"));
    }

    String scopeId = "Comics";

    @AfterClass
    public static void cleanup() {
        reset();
    }

    @Test
    public void testAddOntology() throws Exception {
        CustomOntologySpace space = null;
        IRI logicalId = IRI.create(OWLUtils.guessOntologyIdentifier(nonexSrc.getRootOntology())
                .getUnicodeString());

        space = factory.createCustomOntologySpace(scopeId, dropSrc, minorSrc);
        space.addOntology(minorSrc);
        space.addOntology(nonexSrc);

        assertTrue(space.hasOntology(logicalId));
        logicalId = IRI
                .create(OWLUtils.guessOntologyIdentifier(dropSrc.getRootOntology()).getUnicodeString());
        assertTrue(space.hasOntology(logicalId));
    }

    @Test
    public void testCoreLock() throws Exception {
        CoreOntologySpace space = factory.createCoreOntologySpace(scopeId, inMemorySrc);
        space.setUp();
        try {
            space.addOntology(minorSrc);
            fail("Modification was permitted on locked ontology space.");
        } catch (UnmodifiableOntologyCollectorException e) {
            assertSame(space, e.getOntologyCollector());
        }
    }

    @Test
    public void testCreateSpace() throws Exception {
        CustomOntologySpace space = factory.createCustomOntologySpace(scopeId, dropSrc);
        IRI logicalId = IRI.create(OWLUtils.guessOntologyIdentifier(dropSrc.getRootOntology())
                .getUnicodeString());
        assertTrue(space.hasOntology(logicalId));
    }

    @Test
    public void testCustomLock() throws Exception {
        CustomOntologySpace space = factory.createCustomOntologySpace(scopeId, inMemorySrc);
        space.setUp();
        try {
            space.addOntology(minorSrc);
            fail("Modification was permitted on locked ontology space.");
        } catch (UnmodifiableOntologyCollectorException e) {
            assertSame(space, e.getOntologyCollector());
        }
    }

    /**
     * Checks whether attempting to create ontology spaces with invalid identifiers or namespaces results in
     * the appropriate exceptions being thrown.
     * 
     * @throws Exception
     *             if an unexpected error occurs.
     */
    @Test
    public void testIdentifiers() throws Exception {
        OntologySpace shouldBeNull = null, shouldBeNotNull = null;

        /* First test space identifiers. */

        // Null identifier (invalid).
        try {
            shouldBeNull = factory.createOntologySpace(null, SpaceType.CORE, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite null scope identifier.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // More than one slash in identifier (invalid).
        try {
            shouldBeNull = factory.createOntologySpace("Sc0/p3", SpaceType.CORE, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite null scope identifier.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        /* Now test namespaces. */

        // Null namespace (invalid).
        factory.setNamespace(null);
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite null OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace with query (invalid).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology/?query=true"));
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite query in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace with fragment (invalid).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology#fragment"));
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite fragment in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace ending with hash (invalid).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology#"));
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite fragment in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace ending with neither (valid, should automatically add slash).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology"));
        shouldBeNotNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
        assertNotNull(shouldBeNotNull);
        assertTrue(shouldBeNotNull.getNamespace().toString().endsWith("/"));

        shouldBeNotNull = null;

        // Namespace ending with slash (valid).
        factory.setNamespace(IRI.create("http://stanbol.apache.org/ontology/"));
        shouldBeNotNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
        assertNotNull(shouldBeNotNull);
    }

    @Test
    public void testRemoveCustomOntology() throws Exception {
        CustomOntologySpace space = null;
        space = factory.createCustomOntologySpace(scopeId, dropSrc);
        IRI dropId = URIUtils.createIRI(OWLUtils.guessOntologyIdentifier(dropSrc.getRootOntology()));
        IRI nonexId = URIUtils.createIRI(OWLUtils.guessOntologyIdentifier(nonexSrc.getRootOntology()));

        space.addOntology(inMemorySrc);
        space.addOntology(nonexSrc);
        // The other remote ontologies may change base IRI...
        // baseIri is maincharacters
        assertTrue(space.hasOntology(baseIri));
        assertTrue(space.hasOntology(dropId));
        assertTrue(space.hasOntology(nonexId));
        space.removeOntology(dropId);
        assertFalse(space.hasOntology(dropId));
        space.removeOntology(nonexId);
        assertFalse(space.hasOntology(nonexId));
        // OntologyUtils.printOntology(space.getTopOntology(), System.err);

    }

    // @Test
    public void testSessionModification() throws Exception {
        SessionOntologySpace space = factory.createSessionOntologySpace(scopeId);
        space.setUp();
        try {
            // First add an in-memory ontology with a few axioms.
            space.addOntology(inMemorySrc);
            // Now add a real online ontology
            space.addOntology(dropSrc);
            // The in-memory ontology must be in the space.
            assertTrue(space.hasOntology(baseIri));
            // The in-memory ontology must still have its axioms.
            assertTrue(space.getOntology(baseIri).containsAxiom(linusIsHuman));

            // // The top ontology must still have axioms from in-memory
            // // ontologies. NO LONGER
            // assertTrue(space.getTopOntology().containsAxiom(linusIsHuman));
        } catch (UnmodifiableOntologyCollectorException e) {
            fail("Modification was denied on unlocked ontology space.");
        }
    }

}
