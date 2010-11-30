/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.rules.manager;

import eu.iksproject.kres.api.rules.RuleStore;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import java.io.File;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author elvio
 */
public class KReSRuleStoreTest {

    public KReSRuleStoreTest() {
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

    @Test
    public void testKReSRuleStore(){
        RuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owlmodel = store.getOntology();
        System.out.println(store.getFilePath());
        if(owlmodel!=null){
            OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
            try{
                try{
                    assertEquals(owlmodel, owlmanager.loadOntologyFromOntologyDocument(new File("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl")));
                }catch (Exception e){
                    assertEquals(owlmodel, owlmanager.loadOntologyFromOntologyDocument(new File("./src/main/resources/RuleOntology/OffLineKReSOntologyRules.owl")));
                }
            }catch(OWLOntologyCreationException e){
                e.printStackTrace();
                fail("OWLOntologyCreationException catched");
            }
        }else{
        // TODO review the generated test code and remove the default call to fail.
        fail("The test fail for KReSRuleStore");
        }
    }

    @Test
    public void testKReSRuleStore_2(){
        RuleStore store  = new KReSRuleStore("");
        OWLOntology owlmodel = store.getOntology();
        System.out.println("ECCOMI "+store.getFilePath());
        if(!owlmodel.isEmpty()){
            assertTrue(!owlmodel.isEmpty());
        }else{
        // TODO review the generated test code and remove the default call to fail.
        fail("The test fail for KReSRuleStore");
        }
    }
    

}