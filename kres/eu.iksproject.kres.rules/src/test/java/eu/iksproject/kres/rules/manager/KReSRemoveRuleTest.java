/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.rules.manager;

import java.util.Dictionary;
import java.util.Hashtable;

import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.manager.ONManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 *
 * @author elvio
 */
public class KReSRemoveRuleTest {

    public KReSRemoveRuleTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    	Dictionary<String, Object> configuration = new Hashtable<String, Object>();
    	onm = new ONManager(null, new Hashtable<String, Object>());
    	store = new KReSRuleStore(onm, configuration,"./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
    }

    @After
    public void tearDown() {
    	store = null;
    	onm = null;
    }

    public RuleStore store = null;
    public KReSONManager onm = null;

    /**
     * Test of removeRule method, of class KReSRemoveRule.
     */
    @Test
    public void testRemoveRule() {
//        RuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        KReSAddRule rule = new KReSAddRule(load.getStore());
        rule.addRule("MyRuleProva","Body -> Head",null);
        String ruleName = "MyRuleProva";
        KReSRemoveRule instance = new KReSRemoveRule(rule.getStore());
        boolean expResult = true;
        boolean result = instance.removeRule(ruleName);
        if(result){
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        }else{fail("Some errors occur with removeRule of KReSRemoveRule.");}
    }

    /**
     * Test of removeRule method, of class KReSRemoveRule.
     */
    @Test
    public void testRemoveSingleRule() throws OWLOntologyStorageException {
//        RuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        String owlID = store.getOntology().getOntologyID().toString().replace("<", "").replace(">", "") + "#";

        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        IRI rule = IRI.create(owlID+"MyRuleB");
        IRI recipe = IRI.create(owlID+"MyRecipe");
        KReSRemoveRule instance = new KReSRemoveRule(load.getStore());
        boolean expResult = true;
        boolean result = instance.removeRuleFromRecipe(rule, recipe);
       
        if(result){
            assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        }else{fail("Some errors occur with removeRule of KReSRemoveRule.");}
    }

}