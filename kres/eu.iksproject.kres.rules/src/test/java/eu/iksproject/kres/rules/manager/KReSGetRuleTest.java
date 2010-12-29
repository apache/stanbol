/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.iksproject.kres.rules.manager;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.rules.RuleStore;
import eu.iksproject.kres.manager.ONManager;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import java.util.HashMap;
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
public class KReSGetRuleTest {

    public KReSGetRuleTest() {
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
     * Test of getRule method, of class KReSGetRule.
     */
    @Test
    public void testGetRule() {
//        RuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "")+"#";
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        owl = load.getStore().getOntology();
        
        KReSGetRule rule = new KReSGetRule(store);
        HashMap<IRI, String> map = rule.getRule("MyRuleC");
        HashMap<IRI, String> expmap = new HashMap();
        expmap.put(IRI.create(ID+"MyRuleC"), "MyRuleCBody -> MyRuleCHead");
        if(map!=null){
         assertEquals(expmap, map);
        // TODO review the generated test code and remove the default call to fail.
        }else{
          fail("Some error occurs for method GetRule of KReSGetRule");
        }
    }

    /**
     * Test of getAllRule method, of class KReSGetRule.
     */
    @Test
    public void testGetAllRule() {
//        KReSRuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "")+"#";
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        owl = load.getStore().getOntology();

        KReSGetRule rule = new KReSGetRule(store);
    
        HashMap<IRI, String> map = rule.getAllRules();
        HashMap<IRI, String> expmap = new HashMap();
        //MyRuleX
        String rulex = "PREFIX var http://kres.iksproject.eu/rules# ." +
		  "PREFIX dbs http://andriry.altervista.org/tesiSpecialistica/dbs_l1.owl# ." +
		  "PREFIX lmm http://www.ontologydesignpatterns.org/ont/lmm/LMM_L1.owl# ." +
		  "rule1[dbs:Table(?x) -> lmm:Meaning(?x)]";
        
        expmap.put(IRI.create(ID+"MyRuleA"), "MyRuleABody -> MyRuleAHead");
        expmap.put(IRI.create(ID+"MyRuleB"), "MyRuleBBody -> MyRuleBHead");
        expmap.put(IRI.create(ID+"MyRuleC"), "MyRuleCBody -> MyRuleCHead");
        expmap.put(IRI.create(ID+"MyRuleD"), "MyRuleDBody -> MyRuleDHead");
        expmap.put(IRI.create(ID+"MyRuleE"), "MyRuleEBody -> MyRuleEHead");
        expmap.put(IRI.create(ID+"MyRuleF"), "MyRuleFBody -> MyRuleFHead");
        expmap.put(IRI.create(ID+"MyRuleX"),rulex);

        if(map!=null){
            Iterator<IRI> key = map.keySet().iterator();
            int m = 0;
            while(key.hasNext()){
                IRI k = key.next();
                if(expmap.keySet().contains(k))
                    if(expmap.get(k).equals(map.get(k)))
                        m++;
            }

         assertEquals(expmap.size(),m);
        // TODO review the generated test code and remove the default call to fail.
        }else{
          fail("Some error occurs for method GetAllRule of KReSGetRule");
        }
    }

    /**
     * Test of getRule method, of class KReSGetRule.
     */
    @Test
    public void testGetRuleUsage() {
//        KReSRuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "")+"#";
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        owl = load.getStore().getOntology();

        KReSGetRule rule = new KReSGetRule(store);
        Vector<IRI> vector = rule.getRuleUsage(IRI.create(ID + "MyRuleC"));
 
        if(vector!=null){
         assertEquals(2, vector.size());
        // TODO review the generated test code and remove the default call to fail.
        }else{
          fail("Some error occurs for method getRuleUsage of KReSGetRule");
        }
    }
    
    @Test
    public void testGetRulesOfRecipe() {
//        KReSRuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "")+"#";
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        owl = load.getStore().getOntology();

        KReSGetRule rule = new KReSGetRule(store);
        Vector<IRI> vector = rule.getRuleUsage(IRI.create(ID + "MyRuleC"));
 
        if(vector!=null){
         assertEquals(2, vector.size());
        // TODO review the generated test code and remove the default call to fail.
        }else{
          fail("Some error occurs for method getRuleUsage of KReSGetRule");
        }
    }


}