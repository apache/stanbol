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
package org.apache.stanbol.rules.refactor;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;


import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.commons.owl.transformation.JenaToClerezzaConverter;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaAdapter;
import org.apache.stanbol.rules.adapters.impl.RuleAdaptersFactoryImpl;
import org.apache.stanbol.rules.adapters.impl.RuleAdaptersManagerImpl;
import org.apache.stanbol.rules.base.api.AlreadyExistingRecipeException;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RecipeConstructionException;
import org.apache.stanbol.rules.base.api.RecipeEliminationException;
import org.apache.stanbol.rules.base.api.RuleAdapterManager;
import org.apache.stanbol.rules.base.api.RuleAdaptersFactory;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.manager.ClerezzaRuleStore;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.apache.stanbol.rules.refactor.impl.RefactorerImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * 
 * @author anuzzolese
 * 
 */
public class RefactoringTest {

    private static Refactorer refactorer;
    private static TcManager tcm;
    private static RuleStore store;
    private Graph tripleCollection;
    private String rule;

    @BeforeClass
    public static void setUpClass() throws Exception {

        // recipeIRI = IRI.create("http://kres.iks-project.eu/ontology/meta/rmi_config.owl#MyTestRecipe");

        tcm = TcManager.getInstance();

        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        store = new ClerezzaRuleStore(configuration, tcm);

        Dictionary<String,Object> configuration2 = new Hashtable<String,Object>();

        RuleAdaptersFactory ruleAdaptersFactory = new RuleAdaptersFactoryImpl();

        Dictionary<String,Object> configuration3 = new Hashtable<String,Object>();
        new ClerezzaAdapter(configuration3, store, ruleAdaptersFactory);

        RuleAdapterManager ruleAdapterManager = new RuleAdaptersManagerImpl(configuration2,
                ruleAdaptersFactory);

        Dictionary<String,Object> configuration4 = new Hashtable<String,Object>();

        refactorer = new RefactorerImpl(tcm, store, ruleAdapterManager, configuration4);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        store = null;
        tcm = null;
        refactorer = null;
    }

    @Before
    public void setUp() {
        String separator = System.getProperty("line.separator");
        rule = "kres = <http://kres.iks-project.eu/ontology.owl#> . " + separator
               + "foaf = <http://xmlns.com/foaf/0.1/> . " + separator
               + "rule1[ is(kres:Person, ?x) . endsWith(str(?x), \"Person\") -> is(foaf:Person, ?x) ]";

        InputStream inputStream = RefactoringTest.class.getResourceAsStream("/META-INF/test/testKReSOnt.owl");
        Model jenaModel = ModelFactory.createDefaultModel();
        jenaModel = jenaModel.read(inputStream, null);

        tripleCollection = JenaToClerezzaConverter.jenaModelToClerezzaGraph(jenaModel);

        Graph mGraph = tcm.createGraph(new IRI(
                "http://incubator.apache.com/stanbol/rules/refactor/test/graph"));
        mGraph.addAll(tripleCollection);

        Recipe recipe;
        try {
            recipe = store.createRecipe(new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/recipeA"),
                "Recipe for testing the Refactor.");
            recipe = store.addRulesToRecipe(recipe, rule, "Test");
        } catch (AlreadyExistingRecipeException e) {
            Assert.fail(e.getMessage());
        }

    }

    @After
    public void tearDown() {
        tcm.deleteGraph(new IRI("http://incubator.apache.com/stanbol/rules/refactor/test/graph"));

        try {
            store.removeRecipe(new IRI("http://incubator.apache.com/stanbol/rules/refactor/test/recipeA"));
        } catch (RecipeEliminationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void refactoringTest() throws Exception {

        Recipe recipe = store.getRecipe(new IRI(
                "http://incubator.apache.com/stanbol/rules/refactor/test/recipeA"));

        Graph tc = refactorer.graphRefactoring(new IRI(
                "http://incubator.apache.com/stanbol/rules/refactor/test/graph"), recipe.getRecipeID());

        Assert.assertNotNull(tc);

    }

    @Test
    public void easyRefactoringTest() throws Exception {

        Recipe recipe = store.getRecipe(new IRI(
                "http://incubator.apache.com/stanbol/rules/refactor/test/recipeA"));
        try {

            Graph tc = refactorer.graphRefactoring(tripleCollection, recipe);

            Assert.assertNotNull(tc);

        } catch (RefactoringException e) {
            fail("Error while refactoring.");
        }
    }

    @Test
    public void refactoringWithNonExistentRecipeTest() throws Exception {

        try {

            refactorer.graphRefactoring(new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/refactoredGraph"), new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/graph"), new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/recipeB"));
            Assert.fail();

        } catch (NoSuchRecipeException e) {
            Assert.assertTrue(e.getMessage(), true);
        }

    }

    @Test
    public void refactoringWithARecipeWithNotSupportedAtoms() {
        String separator = System.getProperty("line.separator");

        // the localname atom is not supported by the clerezza adapter and should throw an exception.
        String rule = "kres = <http://kres.iks-project.eu/ontology.owl#> . " + separator
                      + "foaf = <http://xmlns.com/foaf/0.1/> . " + separator
                      + "rule2[ is(kres:Person, ?x) . same(localname(?y), \"text\") -> is(foaf:Person, ?x) ]";

        try {
            Recipe recipe = store.getRecipe(new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/recipeA"));

            recipe = store.addRulesToRecipe(recipe, rule, "Test");

            refactorer.graphRefactoring(new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/refactoredGraph"), new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/graph"), new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/recipeA"));

        } catch (NoSuchRecipeException e) {
            Assert.fail();
        } catch (RecipeConstructionException e) {
            Assert.fail();
        } catch (RefactoringException e) {
            Assert.assertTrue(e.getMessage(), true);
        }

    }

    @Test
    public void persistentRefactoringTest() throws Exception {

        try {

            refactorer.graphRefactoring(new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/refactoredGraph"), new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/graph"), new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/recipeA"));

            Graph tc = tcm.getGraph(new IRI(
                    "http://incubator.apache.com/stanbol/rules/refactor/test/refactoredGraph"));

            Assert.assertNotNull(tc);

        } catch (RefactoringException e) {
            fail("Error while refactoring.");
        }
    }

    @Test
    public void brokenRecipeTest() throws Exception {
        // broken recipe
        String recipe = "rule[is(<http://kres.iks-project.eu/ontology.owl#Person>) -> is(<http://xmlns.com/foaf/0.1/Person>, ?x)]";

        try {
            RuleParserImpl.parse("http://kres.iks-project.eu/ontology.owl#", recipe);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

}
