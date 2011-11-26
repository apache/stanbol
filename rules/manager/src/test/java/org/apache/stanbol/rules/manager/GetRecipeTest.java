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

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.changes.GetRecipe;
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
public class GetRecipeTest {

    public GetRecipeTest() {}

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
     * Test of getRule method, of class GetRecipe.
     */
    @Test
    public void testGetRecipe() {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();

        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        owl = load.getStore().getOntology();
        GetRecipe rule = new GetRecipe(store);
        HashMap<IRI,String> map = rule.getRecipe(IRI
                .create("http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRecipe"));
        HashMap<IRI,String> expmap = new HashMap();
        expmap.put(
            IRI.create(ID + "MyRecipe"),
            "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleC, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleB, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleA");
        if (map != null) {
            assertEquals(expmap, map);
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some error occurs for method GetRecipe of KReSGetRecipe");
        }
    }

    /**
     * Test of getAllRecipes method, of class GetRecipe.
     */
    @Test
    public void testGetAllRecipes() {
        // RuleStore store = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        String ID = owl.getOntologyID().toString().replace("<", "").replace(">", "") + "#";
        // Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",
                store);
        owl = load.getStore().getOntology();

        GetRecipe rule = new GetRecipe(store);

        HashMap<IRI,String> map = rule.getAllRecipes();
        HashMap<IRI,String> expmap = new HashMap();
        expmap.put(
            IRI.create(ID + "MyRecipe"),
            "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleC, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleB, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleA");
        expmap.put(
            IRI.create(ID + "MyRecipe2"),
            "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleE, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleD");
        expmap.put(IRI.create(ID + "MyRecipe3"), "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleF");
        expmap.put(
            IRI.create(ID + "MyRecipe4"),
            "http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleC, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleF, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleB, http://kres.iks-project.eu/ontology/meta/rmi.owl#MyRuleD");

        if (map != null) {
            assertEquals(expmap, map);
            // TODO review the generated test code and remove the default call to fail.
        } else {
            fail("Some error occurs for method GetAllRecipe of KReSGetRecipe");
        }
    }

}