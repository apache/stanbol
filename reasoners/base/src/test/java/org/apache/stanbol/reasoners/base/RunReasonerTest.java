/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.reasoners.base;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.apache.stanbol.reasoners.base.commands.CreateReasoner;
import org.apache.stanbol.reasoners.base.commands.RunReasoner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author elvio
 */
public class RunReasonerTest {

    public OWLOntologyManager owlmanager;

    public OWLOntology owl;

    private Logger log = LoggerFactory.getLogger(getClass());

    public RunReasonerTest() throws OWLOntologyCreationException {
        this.owlmanager = OWLManager.createOWLOntologyManager();
        this.owl = owlmanager.loadOntologyFromOntologyDocument(new File(
                "./src/main/resources/TestFile/ProvaParent.owl"));
    }

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {}

    @After
    public void tearDown() {}

    /**
     * Test of runClassifyInference method, of class RunReasoner.
     */
    @Test
    public void testRunClassifyInference_arg0() throws OWLOntologyCreationException {

        OWLOntology inf = OWLManager.createOWLOntologyManager().createOntology(owl.getOntologyID());
        CreateReasoner reasoner = new CreateReasoner(owl);
        RunReasoner instance = new RunReasoner(reasoner.getReasoner());

        inf = instance.runClassifyInference(inf);

        CreateReasoner reasonerforcheck = new CreateReasoner(inf);
        RunReasoner run = new RunReasoner(reasonerforcheck.getReasoner());
        log.debug("Ontology {} is " + (run.isConsistent() ? "consistent" : "NOT consistent") + ".",
            inf.getOntologyID());
        assertNotNull(inf);
        int ax = inf.getAxiomCount();
        assertEquals(10, ax);

    }

    /**
     * Test of runClassifyInference method, of class RunReasoner.
     */
    @Test
    public void testRunClassifyInference() throws OWLOntologyCreationException {
        int contin = owl.getAxiomCount();

        CreateReasoner reasoner = new CreateReasoner(owl);
        RunReasoner instance = new RunReasoner(reasoner.getReasoner());

        owl = instance.runClassifyInference(owl);

        CreateReasoner reasonerforcheck = new CreateReasoner(owl);
        RunReasoner run = new RunReasoner(reasonerforcheck.getReasoner());

        log.debug("Ontology {} is " + (run.isConsistent() ? "consistent" : "NOT consistent") + ".",
            owl.getOntologyID());
        assertNotNull(owl);
        int ax = owl.getAxiomCount();
        assertTrue(ax > contin);

    }

    /**
     * Test of isConsistence method, of class RunReasoner.
     */
    @Test
    public void testIsConsistence() {
        OWLReasoner expris = (new ReasonerFactory()).createReasoner(owl);
        // expris.prepareReasoner();
        CreateReasoner reasoner = new CreateReasoner(owl);
        RunReasoner instance = new RunReasoner(reasoner.getReasoner());
        boolean expResult = expris.isConsistent();
        boolean result = instance.isConsistent();

        assertTrue(result);
        assertEquals(expResult, result);
    }

    /**
     * Test of runGeneralInference method, of class RunReasoner.
     */
    @Test
    public void testRunGeneralInference_0args() {
        CreateReasoner reasoner = new CreateReasoner(owl);
        RunReasoner instance = new RunReasoner(reasoner.getReasoner());
        OWLOntology result = instance.runGeneralInference();

        CreateReasoner reasonerforcheck = new CreateReasoner(result);
        RunReasoner run = new RunReasoner(reasonerforcheck.getReasoner());
        log.debug("Ontology {} is " + (run.isConsistent() ? "consistent" : "NOT consistent") + ".",
            owl.getOntologyID());

        OWLReasoner expris = (new ReasonerFactory()).createReasoner(owl);
        // expris.prepareReasoner();
        InferredOntologyGenerator iogpellet = new InferredOntologyGenerator(expris);

        iogpellet.fillOntology(owlmanager, owl);

        Set<OWLAxiom> setx = owl.getAxioms();
        Iterator<OWLAxiom> iter = setx.iterator();

        while (iter.hasNext()) {
            OWLAxiom axiom = iter.next();
            if (axiom.toString().contains("Equivalent")) {
                owl.getOWLOntologyManager().removeAxiom(owl, axiom);
            }
        }

        assertNotNull(result);
        assertEquals(owl, result);
    }

    /**
     * Test of runGeneralInference method, of class RunReasoner.
     */
    @Test
    public void testRunGeneralInference_OWLOntology() throws OWLOntologyCreationException {
        OWLOntology newmodel = OWLManager.createOWLOntologyManager().createOntology(owl.getOntologyID());
        CreateReasoner reasoner = new CreateReasoner(owl);
        RunReasoner instance = new RunReasoner(reasoner.getReasoner());
        OWLOntology result = instance.runGeneralInference(newmodel);

        CreateReasoner reasonerforcheck = new CreateReasoner(result);
        RunReasoner run = new RunReasoner(reasonerforcheck.getReasoner());
        log.debug("Ontology {} is " + (run.isConsistent() ? "consistent" : "NOT consistent") + ".",
            owl.getOntologyID());

        OWLReasoner expris = (new ReasonerFactory()).createReasoner(owl);
        // expris.prepareReasoner();
        InferredOntologyGenerator iogpellet = new InferredOntologyGenerator(expris);
        iogpellet.fillOntology(owlmanager, owl);
        OWLOntology expResult = owl;

        assertNotNull(result);
        assertEquals(expResult, result);
    }

}