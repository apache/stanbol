/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.stanbol.rules.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.impl.ONManager;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.KReSRuleStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author elvio
 */
public class KReSRuleStoreTest {

	private Logger log = LoggerFactory.getLogger(getClass());

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
		Dictionary<String, Object> configuration = new Hashtable<String, Object>();
		store = new KReSRuleStore(new ONManager(null,null, configuration),
				configuration,
				"./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
		blankStore = new KReSRuleStore(new ONManager(null,null, configuration),
				configuration, "");
    }

    @After
    public void tearDown() {
		store = null;
		blankStore = null;
    }

	public RuleStore store = null, blankStore = null;

    @Test
    public void testKReSRuleStore(){
        OWLOntology owlmodel = store.getOntology();
		log.debug("Path for default store config is "
				+ blankStore.getFilePath());
		assertNotNull(owlmodel);
            OWLOntologyManager owlmanager = OWLManager.createOWLOntologyManager();
		String src = "";
            try{
			src = "./src/main/resources/RuleOntology/TestKReSOntologyRules.owl";
			assertEquals(owlmodel, owlmanager
					.loadOntologyFromOntologyDocument(new File(src)));
                }catch (Exception e){
			try {
				src = "./src/main/resources/RuleOntology/OffLineKReSOntologyRules.owl";
				assertEquals(owlmodel, owlmanager
						.loadOntologyFromOntologyDocument(new File(src)));
			} catch (OWLOntologyCreationException ex) {
				fail("OWLOntologyCreationException caught when loading from "
						+ src);
                }
        }
    }

    @Test
    public void testKReSRuleStore_2(){
		OWLOntology owlmodel = blankStore.getOntology();
		System.out.println("Path for default store config is "
				+ blankStore.getFilePath());
		assertNotNull(owlmodel);
            assertTrue(!owlmodel.isEmpty());
        }

}