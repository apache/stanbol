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

/**
 *
 * @author elvio
 */
public class RunReasonerTest {

    public OWLOntologyManager owlmanager;
    public OWLOntology owl;

    public RunReasonerTest() throws OWLOntologyCreationException {
        this.owlmanager = OWLManager.createOWLOntologyManager();
        this.owl = owlmanager.loadOntologyFromOntologyDocument(new File("./src/main/resources/TestFile/ProvaParent.owl"));
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

   

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
        System.out.println(":::::::::::::::: consistency check "+run.isConsistent());

        int ax = inf.getAxiomCount();
        
        if(inf!=null){
            assertEquals(10, ax);
        // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur with runClassifyInference of KReSCreateReasoner.");
        }

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
        System.out.println(":::::::::::::::: consistency check "+run.isConsistent());

        int ax = owl.getAxiomCount();

        if(owl!=null){
            assertEquals(true,(ax>contin));
        // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur with runClassifyInference of KReSCreateReasoner.");
        }

    }

    /**
     * Test of isConsistence method, of class RunReasoner.
     */
    @Test
    public void testIsConsistence() {
        OWLReasoner expris = (new ReasonerFactory()).createReasoner(owl);
        expris.prepareReasoner();
        CreateReasoner reasoner = new CreateReasoner(owl);
        RunReasoner instance = new RunReasoner(reasoner.getReasoner());
        boolean expResult = expris.isConsistent();
        boolean result = instance.isConsistent();
        if(result){
            assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur with isConsistence of KReSCreateReasoner.");
        }
    }

    /**
     * Test of runGeneralInference method, of class RunReasoner.
     */
    @Test
    public void testRunGeneralInference_0args(){
        CreateReasoner reasoner = new CreateReasoner(owl);
        RunReasoner instance = new RunReasoner(reasoner.getReasoner());
        OWLOntology result = instance.runGeneralInference();

        CreateReasoner reasonerforcheck = new CreateReasoner(result);
        RunReasoner run = new RunReasoner(reasonerforcheck.getReasoner());
        System.out.println(":::::::::::::::: consistency check "+run.isConsistent());

        OWLReasoner expris = (new ReasonerFactory()).createReasoner(owl);
        expris.prepareReasoner();
        InferredOntologyGenerator iogpellet  = new InferredOntologyGenerator(expris);

        iogpellet.fillOntology(owlmanager, owl);

        Set<OWLAxiom> setx = owl.getAxioms();
        Iterator<OWLAxiom> iter = setx.iterator();

            while(iter.hasNext()){
                OWLAxiom axiom = iter.next();
                if(axiom.toString().contains("Equivalent")){
                 owl.getOWLOntologyManager().removeAxiom(owl,axiom);
                }
            }

        if(result!=null){
            assertEquals(owl, result);
        // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur with RunGeneralInference of KReSCreateReasoner.");
        }
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
        System.out.println(":::::::::::::::: consistency check "+run.isConsistent());
        
        OWLReasoner expris = (new ReasonerFactory()).createReasoner(owl);
        expris.prepareReasoner();
        InferredOntologyGenerator iogpellet  =new InferredOntologyGenerator(expris);
        iogpellet.fillOntology(owlmanager, owl);
        OWLOntology expResult = owl;

        if(result!=null){
         assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some errors occur with RunGeneralInference with new ontology of KReSCreateReasoner.");
        }
    }

}