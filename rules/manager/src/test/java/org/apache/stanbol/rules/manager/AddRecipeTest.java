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
import org.apache.stanbol.rules.manager.changes.AddRecipe;
import org.apache.stanbol.rules.manager.changes.LoadRuleFile;
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
public class AddRecipeTest {

    public AddRecipeTest() {
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
        store = new RuleStoreImpl(configuration,"./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
    }

    @After
    public void tearDown() {
    	store = null;
    }

    public RuleStore store = null;

    /**
     * Test of addRecipe method, of class AddRecipe.
     */
    @Test
    public void testAddRecipe_3args_1() {
//        RuleStore store  = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        //Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);

        String ID = owl.getOntologyID().toString().replace("<","").replace(">","")+"#";
        
        String recipeName = "MyRecipeNew";
        Vector<IRI> rules = new Vector();
        rules.add(IRI.create(ID+"MyRuleC"));
        rules.add(IRI.create(ID+"MyRuleB"));
        rules.add(IRI.create(ID+"MyRuleA"));
        String recipeDescription = "My comment to the recipe";
        AddRecipe instance = new AddRecipe(load.getStore());
        boolean result = instance.addRecipe(recipeName, rules, recipeDescription);
        OWLOntology newonto = instance.getStore().getOntology();
        if(result){
            OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(IRI.create(ID + "MyRecipeNew"));
            int axiom = newonto.getAxioms(ruleind).size();
            assertEquals(8,axiom);
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some problem occurs with addRecipe of KReSAddRule");
        }
    }

    /**
     * Test of addRecipe method, of class AddRecipe.
     */
    @Test
    public void testAddRecipe_4args_1() {
//        RuleStore store  = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();


        String ID = owl.getOntologyID().toString().replace("<","").replace(">","")+"#";

        String recipeName = "MyRecipeNew";
        String recipeDescription = "My comment to the recipe";
        AddRecipe instance = new AddRecipe(store);
        boolean result = instance.addSimpleRecipe(recipeName,recipeDescription);
        OWLOntology newonto = instance.getStore().getOntology();
        if(result){
            OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(IRI.create(ID + "MyRecipeNew"));
           
            int axiom = newonto.getAxioms(ruleind).size();
            assertEquals(2,axiom);
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some problem occurs with addRecipe of KReSAddRule");
        }
    }

    /**
     * Test of addRecipe method, of class AddRecipe.
     */
    @Test
    public void testAddRecipe_3args_2() {
//     RuleStore store  = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        //Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        String ID = owl.getOntologyID().toString().replace("<","").replace(">","")+"#";

        IRI recipeName = IRI.create(ID+"MyRecipeNew");
        Vector<IRI> rules = new Vector();
        rules.add(IRI.create(ID+"MyRuleC"));
        rules.add(IRI.create(ID+"MyRuleB"));
        rules.add(IRI.create(ID+"MyRuleA"));
        String recipeDescription = "My comment to the recipe";
        AddRecipe instance = new AddRecipe(load.getStore());
        boolean result = instance.addRecipe(recipeName, rules, recipeDescription);
        OWLOntology newonto = instance.getStore().getOntology();

        if(result){
            OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(IRI.create(ID + "MyRecipeNew"));
            int axiom = newonto.getAxioms(ruleind).size();
            assertEquals(8,axiom);
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some problem occurs with addRecipe of KReSAddRule");
        }
    }

    /**
     * Test of addRecipeMap method, of class AddRecipe.
     */
    @Test
    public void testAddRecipeMap() {
//        RuleStore store  = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        //Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);
        String ID = owl.getOntologyID().toString().replace("<","").replace(">","")+"#";
        HashMap<String, Vector<IRI>> recipeMap = new HashMap();
        HashMap<String, String> recipeDescriptionMap = new HashMap();
        AddRecipe instance = new AddRecipe(load.getStore());

        Vector<IRI> rules = new Vector();
        rules.add(IRI.create(ID+"MyRuleC"));
        rules.add(IRI.create(ID+"MyRuleB"));
        rules.add(IRI.create(ID+"MyRuleA"));

        Vector<IRI> rules2 = new Vector();
        rules2.add(IRI.create(ID+"MyRuleE"));
        rules2.add(IRI.create(ID+"MyRuleF"));
        rules2.add(IRI.create(ID+"MyRuleD"));

        recipeMap.put("MyRecipeNEW1",rules);
        recipeMap.put("MyRecipeNEW2",rules2);
        recipeDescriptionMap.put("MyRecipeNEW1","My comment to the recipe new 1");
        recipeDescriptionMap.put("MyRecipeNEW2","My comment to the recipe new 2");

        boolean result = instance.addRecipeMap(recipeMap, recipeDescriptionMap);
        OWLOntology newonto = instance.getStore().getOntology();

       if(result){
            Iterator<String> keys = recipeMap.keySet().iterator();
            int axiom = 0;
            while(keys.hasNext()){
                OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(IRI.create(ID+keys.next()));
                axiom = axiom + newonto.getAxioms(ruleind).size();
            }
            assertEquals(16,axiom);
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some problem occurs with addRecipeMap of KReSAddRule");
        }
    }

    /**
     * Test of addRecipeMapIRI method, of class AddRecipe.
     */
    @Test
    public void testAddRecipeMapIRI() {
//        RuleStore store  = new RuleStoreImpl("./src/main/resources/RuleOntology/TestKReSOntologyRules.owl");
        OWLOntology owl = store.getOntology();
        //Load the example file
        LoadRuleFile load = new LoadRuleFile("./src/main/resources/RuleOntology/TestRuleFileExample.txt",store);

        String ID = owl.getOntologyID().toString().replace("<","").replace(">","")+"#";
        HashMap<IRI, Vector<IRI>> recipeMap = new HashMap();
        HashMap<IRI, String> recipeDescriptionMap = new HashMap();
        AddRecipe instance = new AddRecipe(load.getStore());

        Vector<IRI> rules = new Vector();
        rules.add(IRI.create(ID+"MyRuleC"));
        rules.add(IRI.create(ID+"MyRuleB"));
        rules.add(IRI.create(ID+"MyRuleA"));

        Vector<IRI> rules2 = new Vector();
        rules2.add(IRI.create(ID+"MyRuleE"));
        rules2.add(IRI.create(ID+"MyRuleF"));
        rules2.add(IRI.create(ID+"MyRuleD"));

        recipeMap.put(IRI.create(ID+"MyRecipeNEW1"),rules);
        recipeMap.put(IRI.create(ID+"MyRecipeNEW2"),rules2);
        recipeDescriptionMap.put(IRI.create(ID+"MyRecipeNEW1"),"My comment to the recipe new 1");
        recipeDescriptionMap.put(IRI.create(ID+"MyRecipeNEW2"),"My comment to the recipe new 2");

        boolean result = instance.addRecipeMapIRI(recipeMap, recipeDescriptionMap);
        OWLOntology newonto = instance.getStore().getOntology();

       if(result){
            Iterator<IRI> keys = recipeMap.keySet().iterator();
            int axiom = 0;
            while(keys.hasNext()){
                OWLNamedIndividual ruleind = newonto.getOWLOntologyManager().getOWLDataFactory().getOWLNamedIndividual(keys.next());
                
                axiom = axiom + newonto.getAxioms(ruleind).size();
            }
            assertEquals(16,axiom);
            // TODO review the generated test code and remove the default call to fail.
        }else{
            fail("Some problem occurs with addRecipeMapIRI of KReSAddRule");
        }
    }


}