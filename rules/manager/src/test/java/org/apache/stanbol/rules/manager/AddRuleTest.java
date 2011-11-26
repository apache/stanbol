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

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.AddRule;
import org.apache.stanbol.rules.manager.changes.RuleStoreImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * 
 * @author elvio
 */
public class AddRuleTest {

    public AddRuleTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {}

    @AfterClass
    public static void tearDownClass() throws Exception {}

    @Before
    public void setUp() {
        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        store = new RuleStoreImpl(configuration,
                "./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
    }

    @After
    public void tearDown() {
        store = null;
    }

    public RuleStore store = null;

    /**
     * Test of addRule method, of class AddRule.
     */
    @Test
    public void testAddRule_3args_1() {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");

        String ruleName = "MyRuleA";
        String ruleBodyHead = "MyRuleABody -> MyRuleAHead";
        String ruleDescription = "My comment to the rule A";
        AddRule instance = new AddRule(store);

        boolean result = instance.addRule(ruleName, ruleBodyHead, ruleDescription);
        OWLOntology newonto = instance.getStore().getOntology();
        String ID = newonto.getOntologyID().toString().replace("<", "").replace(">", "") + "#";

        if (result) {
            OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory()
                    .getOWLNamedIndividual(IRI.create(ID + "MyRuleA"));
            int axiom = newonto.getAxioms(ruleind).size();
            assertEquals(3, axiom);
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some problem occurs with addRule of KReSAddRule");
        }
    }

    /**
     * Test of addRule method, of class AddRule.
     */
    @Test
    public void testAddRule_3args_2() {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        IRI ruleName = IRI.create(ID + "MyRuleA");
        String ruleBodyHead = "MyRuleABody -> MyRuleAHead";
        String ruleDescription = "My comment to the rule A";
        AddRule instance = new AddRule(store);

        boolean result = instance.addRule(ruleName, ruleBodyHead, ruleDescription);
        OWLOntology newonto = instance.getStore().getOntology();

        if (result) {
            OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory()
                    .getOWLNamedIndividual(ruleName);
            int axiom = newonto.getAxioms(ruleind).size();
            assertEquals(3, axiom);
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some problem occurs with addRule of KReSAddRule");
        }
    }

    /**
     * Test of addRuleMap method, of class AddRule.
     */
    @Test
    public void testAddRuleMap() {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        HashMap<String,String> ruleBodyHeadMap = new HashMap();
        HashMap<String,String> ruleDescriptionMap = new HashMap();

        ruleBodyHeadMap.put("MyRuleA", "MyRuleABody -> MyRuleAHead");
        ruleBodyHeadMap.put("MyRuleB", "MyRuleBBody -> MyRuleBHead");
        ruleDescriptionMap.put("MyRuleA", "My comment to the rule A");
        ruleDescriptionMap.put("MyRuleB", "My comment to the rule B");

        AddRule instance = new AddRule(store);

        boolean result = instance.addRuleMap(ruleBodyHeadMap, ruleDescriptionMap);
        OWLOntology newonto = instance.getStore().getOntology();

        if (result) {
            Iterator<String> keys = ruleBodyHeadMap.keySet().iterator();
            int axiom = 0;
            while (keys.hasNext()) {
                OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory()
                        .getOWLNamedIndividual(IRI.create(ID + keys.next()));
                axiom = axiom + newonto.getAxioms(ruleind).size();
            }
            assertEquals(6, axiom);
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some problem occurs with addRuleMap of KReSAddRule");
        }
    }

    /**
     * Test of addRuleMapIRI method, of class AddRule.
     */
    @Test
    public void testAddRuleMapIRI() {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        HashMap<IRI,String> ruleBodyHeadMap = new HashMap();
        HashMap<IRI,String> ruleDescriptionMap = new HashMap();

        ruleBodyHeadMap.put(IRI.create(ID + "MyRuleA"), "MyRuleABody -> MyRuleAHead");
        ruleBodyHeadMap.put(IRI.create(ID + "MyRuleB"), "MyRuleBBody -> MyRuleBHead");
        ruleDescriptionMap.put(IRI.create(ID + "MyRuleA"), "My comment to the rule A");
        ruleDescriptionMap.put(IRI.create(ID + "MyRuleB"), "My comment to the rule B");

        AddRule instance = new AddRule(store);

        boolean result = instance.addRuleMapIRI(ruleBodyHeadMap, ruleDescriptionMap);
        OWLOntology newonto = instance.getStore().getOntology();

        if (result) {
            Iterator<IRI> keys = ruleBodyHeadMap.keySet().iterator();
            int axiom = 0;
            while (keys.hasNext()) {
                OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory()
                        .getOWLNamedIndividual(keys.next());
                axiom = axiom + newonto.getAxioms(ruleind).size();
            }
            assertEquals(6, axiom);
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some problem occurs with addRuleMap of KReSAddRule");
        }
    }

}