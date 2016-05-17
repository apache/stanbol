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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.scope;

import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.parser;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.reset;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.tcManager;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.util.Hashtable;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.stanbol.commons.owl.util.OWLUtils;
import org.apache.stanbol.ontologymanager.core.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.core.scope.ScopeRegistryImpl;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.Constants;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.collector.ClerezzaCollectorFactory;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology.ClerezzaOWLUtils;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology.ClerezzaOntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace.SpaceType;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.sources.clerezza.GraphSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.ParentPathInputSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;

public class TestClerezzaSpaces {

    public static IRI baseIri = IRI.create(Constants.PEANUTS_MAIN_BASE), baseIri2 = IRI
            .create(Constants.PEANUTS_MINOR_BASE);

    private static OWLAxiom linusIsHuman = null;

    private static OntologySpaceFactory factory;

    private static OntologyInputSource<Graph> minorSrc, dropSrc, nonexSrc;

    private static OntologyInputSource<OWLOntology> inMemorySrc;

    private static OfflineConfiguration offline;

    private static OntologyInputSource<Graph> getLocalSource(String resourcePath) {
        InputStream is = TestOntologySpaces.class.getResourceAsStream(resourcePath);
        return new GraphSource(parser.parse(is, SupportedFormat.RDF_XML));
    }

    @BeforeClass
    public static void setup() throws Exception {
        offline = new OfflineConfigurationImpl(new Hashtable<String,Object>());
        ScopeRegistry reg = new ScopeRegistryImpl();

        // This one is created from scratch
        Graph ont2 = ClerezzaOWLUtils.createOntology(baseIri2.toString());
        minorSrc = new GraphSource(ont2.getImmutableGraph());
        dropSrc = getLocalSource("/ontologies/droppedcharacters.owl");
        nonexSrc = getLocalSource("/ontologies/nonexistentcharacters.owl");
        inMemorySrc = new ParentPathInputSource(new File(TestClerezzaSpaces.class.getResource(
            "/ontologies/maincharacters.owl").toURI()));

        OWLDataFactory df = OWLManager.getOWLDataFactory();
        OWLClass cHuman = df.getOWLClass(IRI.create(baseIri + "/" + Constants.humanBeing));
        OWLIndividual iLinus = df.getOWLNamedIndividual(IRI.create(baseIri + "/" + Constants.linus));
        linusIsHuman = df.getOWLClassAssertionAxiom(cHuman, iLinus);

        factory = new ClerezzaCollectorFactory(new ClerezzaOntologyProvider(tcManager, offline, parser),
                new Hashtable<String,Object>());
        factory.setDefaultNamespace(IRI.create("http://stanbol.apache.org/ontology/"));
    }

    String scopeId = "Comics";

    @AfterClass
    public static void cleanup() {
        reset();
    }

    @Test
    public void testAddOntology() throws Exception {
        OntologySpace space = null;

        OWLOntologyID logicalId = OWLUtils.extractOntologyID(nonexSrc.getRootOntology());
        assertNotNull(logicalId);

        space = factory.createCustomOntologySpace(scopeId, dropSrc, minorSrc);
        space.addOntology(minorSrc);
        space.addOntology(nonexSrc);
        assertTrue(space.hasOntology(logicalId));

        logicalId = OWLUtils.extractOntologyID(dropSrc.getRootOntology());
        assertNotNull(logicalId);
        assertTrue(space.hasOntology(logicalId));
    }

    @Test
    public void testCoreLock() throws Exception {
        OntologySpace space = factory.createCoreOntologySpace(scopeId, inMemorySrc);
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
        OntologySpace space = factory.createCustomOntologySpace(scopeId, dropSrc);
        OWLOntologyID logicalId = null;
        Object o = dropSrc.getRootOntology();
        if (o instanceof Graph) logicalId = OWLUtils.extractOntologyID((Graph) o);
        else if (o instanceof OWLOntology) logicalId = OWLUtils.extractOntologyID((OWLOntology) o);
        assertNotNull(logicalId);
        assertTrue(space.hasOntology(logicalId));
    }

    @Test
    public void testCustomLock() throws Exception {
        OntologySpace space = factory.createCustomOntologySpace(scopeId, inMemorySrc);
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
            shouldBeNull = factory.createOntologySpace(null, SpaceType.CORE);
            fail("Expected IllegalArgumentException not thrown despite null scope identifier.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // More than one slash in identifier (invalid).
        try {
            shouldBeNull = factory.createOntologySpace("Sc0/p3", SpaceType.CORE);
            fail("Expected IllegalArgumentException not thrown despite null scope identifier.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        /* Now test namespaces. */

        // Null namespace (invalid).
        factory.setDefaultNamespace(null);
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE);
            fail("Expected IllegalArgumentException not thrown despite null OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace with query (invalid).
        factory.setDefaultNamespace(IRI.create("http://stanbol.apache.org/ontology/?query=true"));
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE);
            fail("Expected IllegalArgumentException not thrown despite query in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace with fragment (invalid).
        factory.setDefaultNamespace(IRI.create("http://stanbol.apache.org/ontology#fragment"));
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE);
            fail("Expected IllegalArgumentException not thrown despite fragment in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace ending with hash (invalid).
        factory.setDefaultNamespace(IRI.create("http://stanbol.apache.org/ontology#"));
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE);
            fail("Expected IllegalArgumentException not thrown despite fragment in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace ending with neither (valid, should automatically add slash).
        factory.setDefaultNamespace(IRI.create("http://stanbol.apache.org/ontology"));
        shouldBeNotNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE);
        assertNotNull(shouldBeNotNull);
        assertTrue(shouldBeNotNull.getDefaultNamespace().toString().endsWith("/"));

        shouldBeNotNull = null;

        // Namespace ending with slash (valid).
        factory.setDefaultNamespace(IRI.create("http://stanbol.apache.org/ontology/"));
        shouldBeNotNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE);
        assertNotNull(shouldBeNotNull);
    }

    @Test
    public void testRemoveCustomOntology() throws Exception {
        OntologySpace space = null;
        space = factory.createCustomOntologySpace(scopeId, dropSrc);
        OWLOntologyID dropId = OWLUtils.extractOntologyID(dropSrc.getRootOntology());
        OWLOntologyID nonexId = OWLUtils.extractOntologyID(nonexSrc.getRootOntology());

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

}
