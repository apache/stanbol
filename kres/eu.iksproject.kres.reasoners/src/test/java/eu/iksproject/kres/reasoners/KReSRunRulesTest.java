/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.reasoners;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 *
 * @author elvio
 */
public class KReSRunRulesTest {

    public OWLOntologyManager owlmanagertarget;
    public OWLOntologyManager owlnamagerswrlt;
    public OWLOntology owltarget;
    public OWLOntology owlswrl;
    public OntModel jenaswrl;

    public KReSRunRulesTest() throws OWLOntologyCreationException, IOException {
        this.owltarget = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File("./src/main/resources/TestFile/ProvaParent.owl"));
        this.owlswrl = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(new File("./src/main/resources/TestFile/OnlyRuledProvaParent.owl"));
        this.owlmanagertarget = owltarget.getOWLOntologyManager();
        this.owlnamagerswrlt = owlswrl.getOWLOntologyManager();
        this.jenaswrl = ModelFactory.createOntologyModel();
        this.jenaswrl.read("file:./src/main/resources/TestFile/OnlyRuledProvaParentRDFXML.owl", "RDF/XML");
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
     * Test of runRulesReasoner method, of class KReSRunRules.
     */
    @Test
    public void testRunRulesReasoner_OWLOntology_1() throws OWLOntologyCreationException{

        OWLOntology newmodel = OWLManager.createOWLOntologyManager().createOntology(owltarget.getOntologyID());
        KReSRunRules instance = new KReSRunRules(owlswrl,owltarget);
       
        newmodel = instance.runRulesReasoner(newmodel);
        
        KReSCreateReasoner reasonerforcheck = new KReSCreateReasoner(newmodel);
        KReSRunReasoner run = new KReSRunReasoner(reasonerforcheck.getReasoner());
        System.out.println(":::::::::::::::: consistency check "+run.isConsistent());
        
        Iterator<OWLAxiom> axiom = newmodel.getAxioms().iterator();
        Iterator<OWLAxiom> axt = owltarget.getAxioms().iterator();

        String inferedaxiom ="ObjectPropertyAssertion(<http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#hasUncle> <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#Tom> <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#John>)";
        String ax;
        
        if(newmodel!=null){
        while(axt.hasNext()){
            ax = axt.next().toString();
            if(ax.equals(inferedaxiom))
                fail("Some errors occur with runRulesReasoner with new ontology in KReSRunRules.");
        // TODO review the generated test code and remove the default call to fail.
        }}else{
            fail("Some errors occur with runRulesReasoner with new ontology in KReSRunRules.");
        }

        if(newmodel!=null){
        while(axiom.hasNext()){
            ax = axiom.next().toString();
            if(ax.equals(inferedaxiom))
                assertEquals(inferedaxiom, ax.toString());
        // TODO review the generated test code and remove the default call to fail.
        }}else{
            fail("Some errors occur with runRulesReasoner with new ontology in KReSRunRules.");
        }
    }

    /**
     * Test of runRulesReasoner method, of class KReSRunRules.
     */
    @Test
    public void testRunRulesReasoner_0args_1(){

        KReSRunRules instance = new KReSRunRules(owlswrl,owltarget);
        OWLOntology newmodel = instance.runRulesReasoner();

        KReSCreateReasoner reasonerforcheck = new KReSCreateReasoner(newmodel);
        KReSRunReasoner run = new KReSRunReasoner(reasonerforcheck.getReasoner());
        System.out.println(":::::::::::::::: consistency check "+run.isConsistent());

        Iterator<OWLAxiom> axiom = newmodel.getAxioms().iterator();

        String inferedaxiom ="ObjectPropertyAssertion(<http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#hasUncle> <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#Tom> <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#John>)";
        String ax;

        if(newmodel!=null){
        while(axiom.hasNext()){
            ax = axiom.next().toString();
            if(ax.equals(inferedaxiom))
                assertEquals(inferedaxiom, ax.toString());
        // TODO review the generated test code and remove the default call to fail.
        }}else{
            fail("Some errors occur with runRulesReasoner in KReSRunRules.");
        }
    }

    /**
     * Test of runRulesReasoner method, of class KReSRunRules.
     */
    @Test
    public void testRunRulesReasoner_OWLOntology_2() throws OWLOntologyCreationException {

        OWLOntology newmodel = OWLManager.createOWLOntologyManager().createOntology(owltarget.getOntologyID());
        KReSRunRules instance = new KReSRunRules(jenaswrl.getBaseModel(),owltarget);

        newmodel = instance.runRulesReasoner(newmodel);

        KReSCreateReasoner reasonerforcheck = new KReSCreateReasoner(newmodel);
        KReSRunReasoner run = new KReSRunReasoner(reasonerforcheck.getReasoner());
        System.out.println(":::::::::::::::: consistency check "+run.isConsistent());

        Iterator<OWLAxiom> axiom = newmodel.getAxioms().iterator();
        Iterator<OWLAxiom> axt = owltarget.getAxioms().iterator();

        String inferedaxiom ="ObjectPropertyAssertion(<http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#hasUncle> <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#Tom> <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#John>)";
        String ax;

        if(newmodel!=null){
        while(axt.hasNext()){
            ax = axt.next().toString();
            if(ax.equals(inferedaxiom))
                fail("Some errors occur with runRulesReasoner with new ontology in KReSRunRules.");
        // TODO review the generated test code and remove the default call to fail.
        }}else{
            fail("Some errors occur with runRulesReasoner with new ontology in KReSRunRules.");
        }
        
        if(newmodel!=null){
        while(axiom.hasNext()){
            ax = axiom.next().toString();
            if(ax.equals(inferedaxiom))
                assertEquals(inferedaxiom, ax.toString());
        // TODO review the generated test code and remove the default call to fail.
        }}else{
            fail("Some errors occur with runRulesReasoner with new ontology in KReSRunRules.");
        }
    }

    /**
     * Test of runRulesReasoner method, of class KReSRunRules.
     */
    @Test
    public void testRunRulesReasoner_0args_2(){

        KReSRunRules instance = new KReSRunRules(jenaswrl,owltarget);
        OWLOntology newmodel = instance.runRulesReasoner();

        KReSCreateReasoner reasonerforcheck = new KReSCreateReasoner(newmodel);
        KReSRunReasoner run = new KReSRunReasoner(reasonerforcheck.getReasoner());
        System.out.println(":::::::::::::::: consistency check "+run.isConsistent());

        Iterator<OWLAxiom> axiom = newmodel.getAxioms().iterator();

        String inferedaxiom ="ObjectPropertyAssertion(<http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#hasUncle> <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#Tom> <http://www.semanticweb.org/ontologies/2010/6/ProvaParent.owl#John>)";
        String ax;

        if(newmodel!=null){
        while(axiom.hasNext()){
            ax = axiom.next().toString();
            if(ax.equals(inferedaxiom))
                assertEquals(inferedaxiom, ax.toString());
        // TODO review the generated test code and remove the default call to fail.
        }}else{
            fail("Some errors occur with runRulesReasoner in KReSRunRules.");
        }
    }

}