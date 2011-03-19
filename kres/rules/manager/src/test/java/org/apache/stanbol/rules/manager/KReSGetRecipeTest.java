/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManager;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.KReSGetRecipe;
import org.apache.stanbol.rules.manager.changes.KReSLoadRuleFile;
import org.apache.stanbol.rules.manager.changes.KReSRuleStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 *
 * @author elvio
 */
public class KReSGetRecipeTest {

    public KReSGetRecipeTest() {
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
    	onm = new ONManager(null,null, new Hashtable<String, Object>());
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
     * Test of getRule method, of class KReSGetRecipe.
     */
    @Test
    public void testGetRecipe() {
//        RuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "")+"#";
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        owl = load.getStore().getOntology();
        KReSGetRecipe rule = new KReSGetRecipe(store);
        HashMap<IRI, String> map = rule.getRecipe(IRI.create("http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRecipe"));
        HashMap<IRI, String> expmap = new HashMap();
        expmap.put(IRI.create(ID+"MyRecipe"), "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleC, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleB, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleA");
        if(map!=null){
         assertEquals(expmap, map);
        // TODO review the generated test code and remove the default call to fail.
        }else{
          fail("Some error occurs for method GetRecipe of KReSGetRecipe");
        }
    }

    /**
     * Test of getAllRecipes method, of class KReSGetRecipe.
     */
    @Test
    public void testGetAllRecipes() {
//        RuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "")+"#";
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        owl = load.getStore().getOntology();

        KReSGetRecipe rule = new KReSGetRecipe(store);

        HashMap<IRI, String> map = rule.getAllRecipes();
        HashMap<IRI, String> expmap = new HashMap();
        expmap.put(IRI.create(ID+"MyRecipe"), "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleC, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleB, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleA");
        expmap.put(IRI.create(ID+"MyRecipe2"), "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleE, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleD");
        expmap.put(IRI.create(ID+"MyRecipe3"), "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleF");
        expmap.put(IRI.create(ID+"MyRecipe4"), "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleC, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleF, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleB, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleD");

        if(map!=null){
         assertEquals(expmap, map);
        // TODO review the generated test code and remove the default call to fail.
        }else{
          fail("Some error occurs for method GetAllRecipe of KReSGetRecipe");
        }
    }

}