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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.sparql.QueryEngine;
import org.apache.clerezza.rdf.jena.sparql.JenaSparqlEngine;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.rules.base.api.NoSuchRuleInRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set of tests for the validation of the features provided by the RuleStore.
 * 
 * @author anuzzolese
 */
public class RuleStoreTest {

    private Logger log = LoggerFactory.getLogger(getClass());

    public RuleStoreTest() {}

    @BeforeClass
    public static void setUpClass() throws Exception {
        /*class SpecialTcManager extends TcManager {
            public SpecialTcManager(QueryEngine qe, WeightedTcProvider wtcp) {
                super();
                bindQueryEngine(qe);
                bindWeightedTcProvider(wtcp);
            }
        }

        QueryEngine qe = new JenaSparqlEngine();
        WeightedTcProvider wtcp = new SimpleTcProvider();*/
        TcManager tcm = TcManager.getInstance();//new SpecialTcManager(qe, wtcp);

        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        store = new ClerezzaRuleStore(configuration, tcm);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        store = null;
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    public static RuleStore store = null;
    
    /**
     * Calls all the other (now private) test methods to ensure the correct
     * order of execution
     * @throws Exception
     */
    @Test
    public void testRuleStore() throws Exception {
        createRecipeTest();
        addRuleToRecipeTest();
        getRecipeTest();
        getNotExistingRuleByNameInRecipeTest();
        getNotExistingRuleByIdInRecipeTest();
        getExistingRuleByIdInRecipeTest();
        getExistingRuleByNameInRecipeTest();
        findRecipesByDescriptionTest();
        findRulesByDescriptionTest();
        findRulesByNameTest();
        removeRuleInRecipeTest();
        removeRecipeTest();
    }
    
    private void createRecipeTest() throws Exception {
        Recipe recipe = store.createRecipe(new IRI(
                "http://incubator.apache.com/stanbol/rules/test/recipeA"), "The text recipe named A.");

        if (recipe == null) {
            Assert.fail();
        }

        log.debug("Created recipe with ID " + recipe.getRecipeID().toString());

    }

    private void addRuleToRecipeTest() throws Exception {
        Recipe recipe = store.getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));

        String separator = System.getProperty("line.separator");
        String rule = "rule1[" + separator + "	is(<http://dbpedia.org/ontology/Person>, ?x) . " + separator
                      + "	has(<http://dbpedia.org/ontology/playsInTeam>, ?x, ?y) . " + separator
                      + "	is (<http://dbpedia.org/ontology/FootballTeam>, ?y) " + separator + "		-> "
                      + separator + "	is(<http://dbpedia.org/ontology/FootballPlayer>, ?x)" + separator
                      + "] . " + "rule2[" + separator
                      + "	is(<http://dbpedia.org/ontology/Organisation>, ?x) . " + separator
                      + "	has(<http://dbpedia.org/ontology/hasProduct>, ?x, ?y)" + separator + "		-> "
                      + separator + "	is(<http://dbpedia.org/ontology/Company>, ?x)" + separator + "]";

        store.addRulesToRecipe(recipe, rule, "This is a test rule.");

        if (recipe == null) {
            Assert.fail();
        }

        log.debug("Got recipe with ID " + recipe.getRecipeID().toString());

    }

    private void getRecipeTest() throws Exception {
        Recipe recipe = store.getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));

        if (recipe == null) {
            Assert.fail();
        } else {
            System.out.println("Recipe: " + recipe.toString());
        }

        log.debug("Got recipe with ID " + recipe.getRecipeID().toString());

    }

    private void getNotExistingRuleByNameInRecipeTest() throws Exception {
        Recipe recipe = store.getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));

        try {
            recipe.getRule("ruleX");
            Assert.fail();
        } catch (NoSuchRuleInRecipeException e) {
            Assert.assertTrue(true);
        }

    }

    private void getNotExistingRuleByIdInRecipeTest() throws Exception {
        Recipe recipe = store.getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));

        try {
            recipe.getRule(new IRI("http://foo.org/ruleX"));
            Assert.fail();
        } catch (NoSuchRuleInRecipeException e) {
            Assert.assertTrue(true);
        }

    }

    private void getExistingRuleByIdInRecipeTest() throws Exception {
        Recipe recipe = store.getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));

        try {
            Rule rule = recipe.getRule(recipe.listRuleIDs().get(0));
            Assert.assertNotNull(rule);
        } catch (NoSuchRuleInRecipeException e) {
            Assert.fail();
        }

    }

    private void getExistingRuleByNameInRecipeTest() throws Exception {
        Recipe recipe = store.getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));

        try {
            Rule rule = recipe.getRule(recipe.listRuleNames().get(0));
            Assert.assertNotNull(rule);
        } catch (NoSuchRuleInRecipeException e) {
            Assert.fail();
        }

    }

    private void findRecipesByDescriptionTest() throws Exception {
        RecipeList recipes = store.findRecipesByDescription("recipe named A");
        if (recipes.isEmpty()) {
            Assert.fail();
        } else {
            Assert.assertTrue(true);
        }

    }

    private void findRulesByDescriptionTest() throws Exception {
        RuleList rules = store.findRulesByDescription("a test rule.");
        if (rules.isEmpty()) {
            Assert.fail();
        } else {
            Assert.assertTrue(true);
        }
    }

    private void findRulesByNameTest() throws Exception {
        RuleList rules = store.findRulesByName("1");

        if (rules.isEmpty()) {
            Assert.fail();
        } else {
            Assert.assertTrue(true);
        }
    }

    private void removeRuleInRecipeTest() throws Exception {
        Recipe recipe = store.getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));

        String tmp = recipe.toString();
        Rule rule = recipe.getRule(recipe.listRuleNames().get(0));

        store.removeRule(recipe, rule);

        Recipe recipe2 = store
                .getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));

        String tmp2 = recipe2.toString();

        Assert.assertNotSame(tmp, tmp2);

    }

    private void removeRecipeTest() throws Exception {

        RecipeList recipeListInitial = store.listRecipes();
        Recipe[] initialRecipes = new Recipe[recipeListInitial.size()];
        initialRecipes = recipeListInitial.toArray(initialRecipes);

        Recipe recipe = store.getRecipe(new IRI("http://incubator.apache.com/stanbol/rules/test/recipeA"));
        store.removeRecipe(recipe);

        RecipeList recipeListFinal = store.listRecipes();
        Recipe[] finalRecipes = new Recipe[recipeListInitial.size()];
        finalRecipes = recipeListFinal.toArray(finalRecipes);

        Assert.assertNotSame(initialRecipes, finalRecipes);

    }

}