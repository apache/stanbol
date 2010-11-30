/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.reasoners;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredOntologyGenerator;

/**
 *
 * @author elvio
 */
public class KReSRunReasonerTest {

    public OWLOntologyManager owlmanager;
    public OWLOntology owl;

    public KReSRunReasonerTest() throws OWLOntologyCreationException {
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
     * Test of runClassifyInference method, of class KReSRunReasoner.
     */
    @Test
    public void testRunClassifyInference_arg0() throws OWLOntologyCreationException {

        OWLOntology inf = OWLManager.createOWLOntologyManager().createOntology(owl.getOntologyID());
        KReSCreateReasoner reasoner = new KReSCreateReasoner(owl);
        KReSRunReasoner instance = new KReSRunReasoner(reasoner.getReasoner());

        inf = instance.runClassifyInference(inf);

        KReSCreateReasoner reasonerforcheck = new KReSCreateReasoner(inf);
        KReSRunReasoner run = new KReSRunReasoner(reasonerforcheck.getReasoner());
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
     * Test of runClassifyInference method, of class KReSRunReasoner.
     */
    @Test
    public void testRunClassifyInference() throws OWLOntologyCreationException {
        int contin = owl.getAxiomCount();

        KReSCreateReasoner reasoner = new KReSCreateReasoner(owl);
        KReSRunReasoner instance = new KReSRunReasoner(reasoner.getReasoner());    
        
        owl = instance.runClassifyInference(owl);

        KReSCreateReasoner reasonerforcheck = new KReSCreateReasoner(owl);
        KReSRunReasoner run = new KReSRunReasoner(reasonerforcheck.getReasoner());
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
     * Test of isConsistence method, of class KReSRunReasoner.
     */
    @Test
    public void testIsConsistence() {
        OWLReasoner expris = (new ReasonerFactory()).createReasoner(owl);
        expris.prepareReasoner();
        KReSCreateReasoner reasoner = new KReSCreateReasoner(owl);
        KReSRunReasoner instance = new KReSRunReasoner(reasoner.getReasoner());
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
     * Test of runGeneralInference method, of class KReSRunReasoner.
     */
    @Test
    public void testRunGeneralInference_0args(){
        KReSCreateReasoner reasoner = new KReSCreateReasoner(owl);
        KReSRunReasoner instance = new KReSRunReasoner(reasoner.getReasoner());
        OWLOntology result = instance.runGeneralInference();

        KReSCreateReasoner reasonerforcheck = new KReSCreateReasoner(result);
        KReSRunReasoner run = new KReSRunReasoner(reasonerforcheck.getReasoner());
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
     * Test of runGeneralInference method, of class KReSRunReasoner.
     */
    @Test
    public void testRunGeneralInference_OWLOntology() throws OWLOntologyCreationException {
        OWLOntology newmodel = OWLManager.createOWLOntologyManager().createOntology(owl.getOntologyID());
        KReSCreateReasoner reasoner = new KReSCreateReasoner(owl);
        KReSRunReasoner instance = new KReSRunReasoner(reasoner.getReasoner());
        OWLOntology result = instance.runGeneralInference(newmodel);

        KReSCreateReasoner reasonerforcheck = new KReSCreateReasoner(result);
        KReSRunReasoner run = new KReSRunReasoner(reasonerforcheck.getReasoner());
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