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

import static org.junit.Assert.*;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.Constants;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.ParentPathInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceModificationException;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
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

public class TestOntologySpaces {

    public static IRI baseIri = IRI.create(Constants.PEANUTS_MAIN_BASE), baseIri2 = IRI
            .create(Constants.PEANUTS_MINOR_BASE), scopeIri = IRI
            .create("http://stanbol.apache.org/scope/Comics");

    private static OWLAxiom linusIsHuman = null;

    private static ONManager onm;

    private static OWLOntology ont = null, ont2 = null;

    private static OntologyInputSource ontSrc, minorSrc, dropSrc, nonexSrc;

    private static OntologySpaceFactory spaceFactory;

    private static OntologyInputSource getLocalSource(String resourcePath, OWLOntologyManager mgr) throws OWLOntologyCreationException,
                                                                                                  URISyntaxException {
        URL url = TestOntologySpaces.class.getResource(resourcePath);
        File f = new File(url.toURI());
        return new ParentPathInputSource(f, mgr != null ? mgr : onm.getOntologyManagerFactory()
                .createOntologyManager(true));
    }

    @BeforeClass
    public static void setup() throws Exception {

        // An ONManagerImpl with no store and default settings
        onm = new ONManagerImpl(null, null, null, new Hashtable<String,Object>());
        spaceFactory = onm.getOntologySpaceFactory();
        if (spaceFactory == null) fail("Could not instantiate ontology space factory");

        OWLOntologyManager mgr = onm.getOntologyManagerFactory().createOntologyManager(true);
        OWLDataFactory df = mgr.getOWLDataFactory();

        ont = mgr.createOntology(baseIri);
        ontSrc = new RootOntologySource(ont, null);
        // Let's state that Linus is a human being
        OWLClass cHuman = df.getOWLClass(IRI.create(baseIri + "/" + Constants.humanBeing));
        OWLIndividual iLinus = df.getOWLNamedIndividual(IRI.create(baseIri + "/" + Constants.linus));
        linusIsHuman = df.getOWLClassAssertionAxiom(cHuman, iLinus);
        mgr.applyChange(new AddAxiom(ont, linusIsHuman));

        ont2 = mgr.createOntology(baseIri2);
        minorSrc = new RootOntologySource(ont2);

        dropSrc = getLocalSource("/ontologies/droppedcharacters.owl", mgr);
        nonexSrc = getLocalSource("/ontologies/nonexistentcharacters.owl", mgr);
        minorSrc = new RootOntologySource(ont2, null);

    }

    @Test
    public void testAddOntology() throws Exception {
        CustomOntologySpace space = null;
        IRI logicalId = nonexSrc.getRootOntology().getOntologyID().getOntologyIRI();

        space = spaceFactory.createCustomOntologySpace(scopeIri, dropSrc);
        space.addOntology(minorSrc);
        space.addOntology(nonexSrc);

        assertTrue(space.containsOntology(logicalId));
        logicalId = dropSrc.getRootOntology().getOntologyID().getOntologyIRI();
        assertTrue(space.containsOntology(logicalId));
    }

    @Test
    public void testCoreLock() throws Exception {
        CoreOntologySpace space = spaceFactory.createCoreOntologySpace(scopeIri, ontSrc);
        space.setUp();
        try {
            space.addOntology(minorSrc);
            fail("Modification was permitted on locked ontology space.");
        } catch (UnmodifiableOntologySpaceException e) {
            assertSame(space, e.getSpace());
        }
    }

    @Test
    public void testCreateSpace() throws Exception {
        CustomOntologySpace space = spaceFactory.createCustomOntologySpace(scopeIri, dropSrc);
        IRI logicalId = dropSrc.getRootOntology().getOntologyID().getOntologyIRI();
        assertTrue(space.containsOntology(logicalId));
    }

    @Test
    public void testCustomLock() throws Exception {
        CustomOntologySpace space = spaceFactory.createCustomOntologySpace(scopeIri, ontSrc);
        space.setUp();
        try {
            space.addOntology(minorSrc);
            fail("Modification was permitted on locked ontology space.");
        } catch (UnmodifiableOntologySpaceException e) {
            assertSame(space, e.getSpace());
        }
    }

    @Test
    public void testRemoveCustomOntology() throws Exception {
        CustomOntologySpace space = null;
        space = spaceFactory.createCustomOntologySpace(scopeIri, dropSrc);
        IRI pizzaId = dropSrc.getRootOntology().getOntologyID().getOntologyIRI();
        IRI wineId = nonexSrc.getRootOntology().getOntologyID().getOntologyIRI();
        try {
            space.addOntology(ontSrc);
            space.addOntology(nonexSrc);
            // The other remote ontologies may change base IRI...
            assertTrue(space.containsOntology(ont.getOntologyID().getOntologyIRI())
                       && space.containsOntology(pizzaId) && space.containsOntology(wineId));
            space.removeOntology(dropSrc);
            assertFalse(space.containsOntology(pizzaId));
            space.removeOntology(nonexSrc);
            assertFalse(space.containsOntology(wineId));
            // OntologyUtils.printOntology(space.getTopOntology(), System.err);
        } catch (UnmodifiableOntologySpaceException e) {
            fail("Modification was disallowed on non-locked ontology space.");
        } catch (OntologySpaceModificationException e) {
            fail("Modification failed on ontology space " + e.getSpace().getID());
        }
    }

    @Test
    public void testSessionModification() throws Exception {
        SessionOntologySpace space = spaceFactory.createSessionOntologySpace(scopeIri);
        space.setUp();
        try {
            // First add an in-memory ontology with a few axioms.
            space.addOntology(ontSrc);
            // Now add a real online ontology
            space.addOntology(dropSrc);
            // The in-memory ontology must be in the space.
            assertTrue(space.getOntologies().contains(ont));
            // The in-memory ontology must still have its axioms.
            assertTrue(space.getOntology(ont.getOntologyID().getOntologyIRI()).containsAxiom(linusIsHuman));
            // The top ontology must still have axioms from in-memory
            // ontologies.
            assertTrue(space.getTopOntology().containsAxiom(linusIsHuman));
        } catch (UnmodifiableOntologySpaceException e) {
            fail("Modification was denied on unlocked ontology space.");
        }
    }

}
