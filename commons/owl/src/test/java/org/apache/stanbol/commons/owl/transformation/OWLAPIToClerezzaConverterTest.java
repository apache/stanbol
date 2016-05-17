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
package org.apache.stanbol.commons.owl.transformation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.commons.owl.transformation.OWLAPIToClerezzaConverter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.vocabulary.RDF;

/**
 * It is a JUnit test class.<br>
 * It tests the methods of the class {@link OWLAPIToClerezzaConverter}.
 * 
 * @author andrea.nuzzolese
 * 
 */

public class OWLAPIToClerezzaConverterTest {

    private static OWLOntology ontology;
    private static Graph mGraph;
    private static String ns = "http://incubator.apache.org/stanbol/owl#";
    private static String foaf = "http://xmlns.com/foaf/0.1/";

    private static Logger log = LoggerFactory.getLogger(OWLAPIToClerezzaConverterTest.class);

    @BeforeClass
    public static void setupClass() {

        /*
         * Set-up the OWL ontology for the test. Simply add the axioms: AndreaNuzzolese isA Person -> class
         * assertion axiom EnricoDaga isA Person -> class assertion axiom AndreaNuzzolese knows EnricoDaga ->
         * object property assertion axiom
         */
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        try {
            ontology = manager.createOntology(org.semanticweb.owlapi.model.IRI.create(ns + "testOntology"));
        } catch (OWLOntologyCreationException e) {
            log.error(e.getMessage());
        }

        if (ontology != null) {
            OWLClass personClass = factory.getOWLClass(org.semanticweb.owlapi.model.IRI.create(foaf + "Person"));
            OWLNamedIndividual andreaNuzzoleseOWL = factory.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI
                    .create(ns + "AndreaNuzzolese"));
            OWLNamedIndividual enricoDagaOWL = factory.getOWLNamedIndividual(org.semanticweb.owlapi.model.IRI.create(ns + "EnricoDaga"));
            OWLObjectProperty knowsOWL = factory.getOWLObjectProperty(org.semanticweb.owlapi.model.IRI.create(foaf + "knows"));

            OWLAxiom axiom = factory.getOWLClassAssertionAxiom(personClass, andreaNuzzoleseOWL);
            manager.addAxiom(ontology, axiom);

            axiom = factory.getOWLClassAssertionAxiom(personClass, enricoDagaOWL);
            manager.addAxiom(ontology, axiom);

            axiom = factory.getOWLObjectPropertyAssertionAxiom(knowsOWL, andreaNuzzoleseOWL, enricoDagaOWL);
            manager.addAxiom(ontology, axiom);
        }

        /*
         * Set-up the Clerezza model for the test. As before simply add the triples: AndreaNuzzolese isA
         * Person EnricoDaga isA Person AndreaNuzzolese knows EnricoDaga
         */
        mGraph = new SimpleGraph();

        IRI knowsInClerezza = new IRI(ns + "knows");
        IRI rdfType = new IRI(RDF.getURI() + "type");
        IRI foafPersonInClerezza = new IRI(foaf + "Person");

        BlankNodeOrIRI andreaNuzzoleseInClerezza = new IRI(ns + "AndreaNuzzolese");
        BlankNodeOrIRI enricoDagaInClerezza = new IRI(ns + "EnricoDaga");

        Triple triple = new TripleImpl(andreaNuzzoleseInClerezza, rdfType, foafPersonInClerezza);
        mGraph.add(triple);
        triple = new TripleImpl(enricoDagaInClerezza, rdfType, foafPersonInClerezza);
        mGraph.add(triple);
        triple = new TripleImpl(andreaNuzzoleseInClerezza, knowsInClerezza, enricoDagaInClerezza);
        mGraph.add(triple);
    }

    @Test
    public void testGraphToOWLOntology() {
        /*
         * Transform the Clerezza Graph to an OWLOntology.
         */
        OWLOntology ontology = OWLAPIToClerezzaConverter.clerezzaGraphToOWLOntology(mGraph);

        /*
         * Print the number of axioms contained in the OWLOntology.
         */
        int axiomCount = ontology.getAxiomCount();
        log.info("The ontology contatins " + axiomCount + " axioms: ");

        /*
         * Print the axioms contained in the OWLOntology.
         */
        Set<OWLAxiom> axioms = ontology.getAxioms();
        for (OWLAxiom axiom : axioms) {
            log.info("    " + axiom.toString());
        }
    }

    @Test
    public void testOWLOntologyToGraph() {

        /*
         * Transform the OWLOntology into a Clerezza Graph.
         */
        Graph mGraph = OWLAPIToClerezzaConverter.owlOntologyToClerezzaGraph(ontology);

        /*
         * Print all the triples contained in the Clerezza Graph.
         */
        Iterator<Triple> tripleIt = mGraph.iterator();
        while (tripleIt.hasNext()) {
            Triple triple = tripleIt.next();
            log.info(triple.toString());
        }
    }

    @Test
    public void testOWLOntologyToTriples() {

        /*
         * Transform the OWLOntology into a collection of Clerezza triples.
         */
        Collection<Triple> triples = OWLAPIToClerezzaConverter.owlOntologyToClerezzaTriples(ontology);

        /*
         * Print the collection of Clerezza triples.
         */
        for (Triple triple : triples) {
            log.info(triple.toString());
        }
    }
}
