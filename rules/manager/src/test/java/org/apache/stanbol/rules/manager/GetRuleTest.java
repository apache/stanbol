/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.util.Iterator;
import java.util.Vector;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.GetRule;
import org.apache.stanbol.rules.manager.changes.LoadRuleFile;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;
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
public class GetRuleTest {

    public GetRuleTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {
        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        store = new RuleStoreImpl(configuration, "./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
    }

    @After
    public void tearDown() {
        store = null;
    }

    public RuleStore store = null;

    /**
     * Test of getRule method, of class GetRule.
     */
    @Test
    public void testGetRule() {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        owl = load.getStore().getOntology();

        GetRule rule = new GetRule(store);
        HashMap<IRI,String> map = rule.getRule("MyRuleC");
        HashMap<IRI,String> expmap = new HashMap();
        expmap.put(IRI.create(ID + "MyRuleC"), "MyRuleCBody -> MyRuleCHead");
        if (map != null) {
            assertEquals(expmap, map);
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some error occurs for method GetRule of KReSGetRule");
        }
    }

    /**
     * Test of getAllRule method, of class GetRule.
     */
    @Test
    public void testGetAllRule() {
        // RuleStoreImpl store = new
        // RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        owl = load.getStore().getOntology();

        GetRule rule = new GetRule(store);

        HashMap<IRI,String> map = rule.getAllRules();
        HashMap<IRI,String> expmap = new HashMap();
        // MyRuleX
        String rulex = "PREFIX var http://kres.iksproject.eu/rules# ."
                       + "PREFIX dbs http://andriry.altervista.org/tesiSpecialistica/dbs_l1.owl# ."
                       + "PREFIX lmm http://www.ontologydesignpatterns.org/ont/lmm/LMM_L1.owl# ."
                       + "rule1[dbs:Table(?x) -> lmm:Meaning(?x)]";

        expmap.put(IRI.create(ID + "MyRuleA"), "MyRuleABody -> MyRuleAHead");
        expmap.put(IRI.create(ID + "MyRuleB"), "MyRuleBBody -> MyRuleBHead");
        expmap.put(IRI.create(ID + "MyRuleC"), "MyRuleCBody -> MyRuleCHead");
        expmap.put(IRI.create(ID + "MyRuleD"), "MyRuleDBody -> MyRuleDHead");
        expmap.put(IRI.create(ID + "MyRuleE"), "MyRuleEBody -> MyRuleEHead");
        expmap.put(IRI.create(ID + "MyRuleF"), "MyRuleFBody -> MyRuleFHead");
        expmap.put(IRI.create(ID + "MyRuleX"), rulex);

        if (map != null) {
            Iterator<IRI> key = map.keySet().iterator();
            int m = 0;
            while (key.hasNext()) {
                IRI k = key.next();
                if (expmap.keySet().contains(k)) if (expmap.get(k).equals(map.get(k))) m++;
            }

            assertEquals(expmap.size(), m);
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some error occurs for method GetAllRule of KReSGetRule");
        }
    }

    /**
     * Test of getRule method, of class GetRule.
     */
    @Test
    public void testGetRuleUsage() {
        // RuleStoreImpl store = new
        // RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        owl = load.getStore().getOntology();

        GetRule rule = new GetRule(store);
        Vector<IRI> vector = rule.getRuleUsage(IRI.create(ID + "MyRuleC"));

        if (vector != null) {
            assertEquals(2, vector.size());
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some error occurs for method getRuleUsage of KReSGetRule");
        }
    }

    @Test
    public void testGetRulesOfRecipe() {
        // RuleStoreImpl store = new
        // RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        owl = load.getStore().getOntology();

        GetRule rule = new GetRule(store);
        Vector<IRI> vector = rule.getRuleUsage(IRI.create(ID + "MyRuleC"));

        if (vector != null) {
            assertEquals(2, vector.size());
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some error occurs for method getRuleUsage of KReSGetRule");
        }
    }

}