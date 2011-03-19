/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.KReSONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManager;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.KReSLoadRuleFile;
import org.apache.stanbol.rules.manager.changes.KReSRemoveRecipe;
import org.apache.stanbol.rules.manager.changes.KReSRuleStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

/**
 *
 * @author elvio
 */
public class KReSRemoveRecipeTest {

    public KReSRemoveRecipeTest() {
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
     * Test of removeRule method, of class KReSRemoveRecipe.
     */
    @Test
    public void testRemoveRule_String() throws OWLOntologyCreationException {
//        RuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        String owlID = store.getOntology().getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        OWLOntology owlstart = load.getStore().getOntology();
        String recipeName = owlID+"MyRecipe";
        KReSRemoveRecipe instance = new KReSRemoveRecipe(load.getStore());

        boolean expResult = true;
        boolean result = instance.removeRecipe(IRI.create(recipeName));
        OWLOntology owlend = instance.getStore().getOntology();

        if(result){
        assertEquals(expResult,(owlstart.getAxiomCount()>owlend.getAxiomCount()));
        // TODO review the generated test code and remove the default call to fail.
        }else{fail("Some errors occur with removeRule of KReSRemoveRule.");}
    }

    /**
     * Test of removeRule method, of class KReSRemoveRecipe.
     */
    @Test
    public void testRemoveRuleName_String() throws OWLOntologyCreationException {
//        RuleStore store  = new KReSRuleStore("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        //Load the example file
        KReSLoadRuleFile load = new KReSLoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        OWLOntology owlstart = load.getStore().getOntology();
        String recipeName ="MyRecipe";
        KReSRemoveRecipe instance = new KReSRemoveRecipe(load.getStore());

        boolean expResult = true;
        boolean result = instance.removeRecipe(recipeName);
        OWLOntology owlend = instance.getStore().getOntology();

        if(result){
        assertEquals(expResult,(owlstart.getAxiomCount()>owlend.getAxiomCount()));
        // TODO review the generated test code and remove the default call to fail.
        }else{fail("Some errors occur with removeRule of KReSRemoveRule.");}
    }

   

}