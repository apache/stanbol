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

import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.onManager;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.reset;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.Constants;
import org.apache.stanbol.ontologymanager.servicesapi.collector.MissingOntologyException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpace.SpaceType;
import org.apache.stanbol.ontologymanager.servicesapi.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.sources.owlapi.BlankOntologySource;
import org.apache.stanbol.ontologymanager.sources.owlapi.ParentPathInputSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestOntologySpaces {

    public static IRI baseIri = IRI.create(Constants.PEANUTS_MAIN_BASE), baseIri2 = IRI
            .create(Constants.PEANUTS_MINOR_BASE);

    private static OntologySpaceFactory factory;
    private static OntologyInputSource<OWLOntology> inMemorySrc, minorSrc, dropSrc, nonexSrc;
    private static OWLAxiom linusIsHuman = null;

    private Logger log = LoggerFactory.getLogger(getClass());

    private static OWLOntology ont = null, ont2 = null;

    private static OntologyInputSource<OWLOntology> getLocalSource(String resourcePath, OWLOntologyManager mgr) throws OWLOntologyCreationException,
                                                                                                               URISyntaxException {
        URL url = TestOntologySpaces.class.getResource(resourcePath);
        File f = new File(url.toURI());
        return new ParentPathInputSource(f, mgr != null ? mgr
                : OWLOntologyManagerFactory.createOWLOntologyManager(onManager.getOfflineConfiguration()
                        .getOntologySourceLocations().toArray(new IRI[0])));
    }

    @BeforeClass
    public static void setup() throws Exception {
        factory = onManager.getOntologySpaceFactory();
        if (factory == null) fail("Could not instantiate ontology space factory");

        OWLOntologyManager mgr = OWLOntologyManagerFactory.createOWLOntologyManager(onManager
                .getOfflineConfiguration().getOntologySourceLocations().toArray(new IRI[0]));
        OWLDataFactory df = mgr.getOWLDataFactory();

        ont = mgr.createOntology(baseIri);
        inMemorySrc = new RootOntologySource(ont);
        // Let's state that Linus is a human being
        OWLClass cHuman = df.getOWLClass(IRI.create(baseIri + "/" + Constants.humanBeing));
        OWLIndividual iLinus = df.getOWLNamedIndividual(IRI.create(baseIri + "/" + Constants.linus));
        linusIsHuman = df.getOWLClassAssertionAxiom(cHuman, iLinus);
        mgr.applyChange(new AddAxiom(ont, linusIsHuman));

        ont2 = mgr.createOntology(baseIri2);
        minorSrc = new RootOntologySource(ont2);

        dropSrc = getLocalSource("/ontologies/droppedcharacters.owl", mgr);
        nonexSrc = getLocalSource("/ontologies/nonexistentcharacters.owl", mgr);
        minorSrc = new RootOntologySource(ont2);

    }

    @After
    public void cleanup() {
        reset();
    }

    @Test
    public void testAddOntology() throws Exception {
        OntologySpace space = null;
        IRI logicalId = nonexSrc.getRootOntology().getOntologyID().getOntologyIRI();

        space = factory.createCustomOntologySpace("testAddOntology", dropSrc);
        space.addOntology(minorSrc);
        space.addOntology(nonexSrc);

        assertTrue(space.hasOntology(logicalId));
        logicalId = dropSrc.getRootOntology().getOntologyID().getOntologyIRI();
        assertTrue(space.hasOntology(logicalId));
    }

    @Test
    public void testCoreLock() throws Exception {
        OntologySpace space = factory.createCoreOntologySpace("testCoreLock", inMemorySrc);
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
        OntologySpace space = factory.createCustomOntologySpace("testCreateSpace", dropSrc);
        IRI logicalId = dropSrc.getRootOntology().getOntologyID().getOntologyIRI();
        assertTrue(space.hasOntology(logicalId));
    }

    @Test
    public void testCustomLock() throws Exception {
        OntologySpace space = factory.createCustomOntologySpace("testCustomLock", inMemorySrc);
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
        factory.setDefaultNamespace(null);
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite null OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace with query (invalid).
        factory.setDefaultNamespace(IRI.create("http://stanbol.apache.org/ontology/?query=true"));
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
            fail("Expected IllegalArgumentException not thrown despite query in OntoNet namespace.");
        } catch (IllegalArgumentException ex) {}
        assertNull(shouldBeNull);

        // Namespace with fragment (invalid).
        factory.setDefaultNamespace(IRI.create("http://stanbol.apache.org/ontology#fragment"));
        try {
            shouldBeNull = factory.createOntologySpace("Sc0p3", SpaceType.CORE, new BlankOntologySource());
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
        space = factory.createCustomOntologySpace("testRemoveCustomOntology", dropSrc);

        IRI dropId = dropSrc.getRootOntology().getOntologyID().getOntologyIRI();
        IRI nonexId = nonexSrc.getRootOntology().getOntologyID().getOntologyIRI();

        space.addOntology(inMemorySrc);
        space.addOntology(nonexSrc);
        // The other remote ontologies may change base IRI...
        assertTrue(space.hasOntology(ont.getOntologyID().getOntologyIRI()));
        assertTrue(space.hasOntology(dropId));
        assertTrue(space.hasOntology(nonexId));

        IRI bogus = IRI.create("http://www.example.org/ontology/bogus");
        try {
            space.removeOntology(bogus);
            fail("Removing nonexisting ontology succeeded without an exception. This should not happen.");
        } catch (MissingOntologyException mex) {
            log.info("Expected exception caught when removing missing ontology {}", bogus);
        }

        space.removeOntology(dropId);
        assertFalse(space.hasOntology(dropId));
        space.removeOntology(nonexId);
        assertFalse(space.hasOntology(nonexId));
        // OntologyUtils.printOntology(space.getTopOntology(), System.err);

    }

}
