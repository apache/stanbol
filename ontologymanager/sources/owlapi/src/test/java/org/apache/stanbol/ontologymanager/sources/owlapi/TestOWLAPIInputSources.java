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
package org.apache.stanbol.ontologymanager.sources.owlapi;

import static org.apache.stanbol.ontologymanager.sources.owlapi.Entities.ALEX;
import static org.apache.stanbol.ontologymanager.sources.owlapi.Entities.BEGONA;
import static org.apache.stanbol.ontologymanager.sources.owlapi.Entities.FOAF_KNOWS;
import static org.apache.stanbol.ontologymanager.sources.owlapi.Entities.FOAF_KNOWS_AP;
import static org.apache.stanbol.ontologymanager.sources.owlapi.Entities.FOAF_PERSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Set;

import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.WriterDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.AutoIRIMapper;

/**
 * Test suite for the correctness of {@link OntologyInputSource} implementations based on the OWL API.
 * 
 * @author alexdma
 * 
 */
public class TestOWLAPIInputSources {

    private static OWLDataFactory df;

    @BeforeClass
    public static void setUp() {
        df = OWLManager.getOWLDataFactory();
    }

    @Test
    public void fromInputStream() throws Exception {
        InputStream in = getClass().getResourceAsStream("/ontologies/dummy-01.turtle.rdf");
        OntologyInputSource<OWLOntology> src = new OntologyContentInputSource(in);
        assertNotNull(src);
        assertNull(src.getOrigin());
        OWLOntology o = src.getRootOntology();
        assertNotNull(o);
        OWLAxiom ax = df.getOWLClassAssertionAxiom(FOAF_PERSON, ALEX);
        assertTrue(o.containsAxiom(ax));
        ax = df.getOWLClassAssertionAxiom(FOAF_PERSON, BEGONA);
        assertTrue(o.containsAxiom(ax));

        // Not knowing how the foaf:knows assertion is interpreted, we check for either (but not both!).
        ax = df.getOWLObjectPropertyAssertionAxiom(FOAF_KNOWS, ALEX, BEGONA);
        OWLAxiom ann = df.getOWLAnnotationAssertionAxiom(FOAF_KNOWS_AP, ALEX.getIRI(), BEGONA.getIRI());
        assertTrue(o.containsAxiom(ann) || o.containsAxiom(ax));
        assertFalse(o.containsAxiom(ann) && o.containsAxiom(ax));

        assertSame(3, o.getAxiomCount()); // No other axioms exist.
    }

    @Test
    public void testAutoIRIMapper() throws Exception {

        URL url = getClass().getResource("/ontologies");
        assertNotNull(url);
        File file = new File(url.toURI());
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        OWLOntologyIRIMapper mapper = new AutoIRIMapper(file, true);

        IRI dummyiri = IRI.create("http://stanbol.apache.org/ontologies/peanuts/dummycharacters.owl");

        // Cleanup may be required if previous tests have failed.
        if (mapper.getDocumentIRI(dummyiri) != null) {
            new File(mapper.getDocumentIRI(dummyiri).toURI()).delete();
            ((AutoIRIMapper) mapper).update();
        }
        assertFalse(dummyiri.equals(mapper.getDocumentIRI(dummyiri)));

        // Create a new ontology in the test resources.
        OWLOntologyManager mgr = OWLOntologyManagerFactory.createOWLOntologyManager(null);
        OWLOntology o = mgr.createOntology(dummyiri);
        File f = new File(URI.create(url.toString() + "/dummycharacters.owl"));
        mgr.saveOntology(o, new WriterDocumentTarget(new FileWriter(f)));
        assertTrue(f.exists());

        ((AutoIRIMapper) mapper).update();
        // The old mapper should be able to locate the new ontology.
        assertFalse(dummyiri.equals(mapper.getDocumentIRI(dummyiri)));

        // A new mapper too
        OWLOntologyIRIMapper mapper2 = new AutoIRIMapper(new File(url.toURI()), true);
        assertFalse(dummyiri.equals(mapper2.getDocumentIRI(dummyiri)));

        // cleanup
        f.delete();
    }

    /**
     * Uses a {@link ParentPathInputSource} to load an ontology importing a modified FOAF, both located in the
     * same resource directory.
     * 
     * @throws Exception
     */
    @Test
    public void testOfflineImport() throws Exception {
        URL url = getClass().getResource("/ontologies/maincharacters.owl");
        assertNotNull(url);
        File f = new File(url.toURI());
        assertNotNull(f);
        OntologyInputSource<OWLOntology> coreSource = new ParentPathInputSource(f);

        // // Check that all the imports closure is made of local files
        // Set<OWLOntology> closure = coreSource.getImports(true);
        // for (OWLOntology o : closure)
        // assertEquals("file", o.getOWLOntologyManager().getOntologyDocumentIRI(o).getScheme());

        assertEquals(coreSource.getRootOntology().getOntologyID().getOntologyIRI(),
            IRI.create(Constants.PEANUTS_MAIN_BASE));
        // Linus is stated to be a foaf:Person
        OWLNamedIndividual iLinus = df.getOWLNamedIndividual(IRI.create(Constants.PEANUTS_MAIN_BASE
                                                                        + "#Linus"));
        // Lucy is stated to be a foaf:Perzon
        OWLNamedIndividual iLucy = df
                .getOWLNamedIndividual(IRI.create(Constants.PEANUTS_MAIN_BASE + "#Lucy"));
        OWLClass cPerzon = df.getOWLClass(IRI.create("http://xmlns.com/foaf/0.1/Perzon"));

        Set<OWLIndividual> instances = cPerzon.getIndividuals(coreSource.getRootOntology());
        assertTrue(!instances.contains(iLinus) && instances.contains(iLucy));
    }

    /**
     * Loads a modified FOAF by resolving a URI from a resource directory.
     * 
     * @throws Exception
     */
    @Test
    public void testOfflineSingleton() throws Exception {
        URL url = getClass().getResource("/ontologies/mockfoaf.rdf");
        assertNotNull(url);
        OntologyInputSource<OWLOntology> coreSource = new RootOntologySource(IRI.create(url));
        assertNotNull(df);
        /*
         * To check it fetched the correct ontology, we look for a declaration of the bogus class foaf:Perzon
         * (added in the local FOAF)
         */
        OWLClass cPerzon = df.getOWLClass(IRI.create("http://xmlns.com/foaf/0.1/Perzon"));
        assertTrue(coreSource.getRootOntology().getClassesInSignature().contains(cPerzon));
    }

}
