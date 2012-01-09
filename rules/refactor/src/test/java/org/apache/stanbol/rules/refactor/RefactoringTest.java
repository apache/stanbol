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

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.sparql.QueryEngine;
import org.apache.clerezza.rdf.jena.sparql.JenaSparqlEngine;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.rules.base.api.NoSuchRecipeException;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.Rule;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.util.RecipeList;
import org.apache.stanbol.rules.base.api.util.RuleList;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.changes.RecipeImpl;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.apache.stanbol.rules.refactor.api.Refactorer;
import org.apache.stanbol.rules.refactor.api.RefactoringException;
import org.apache.stanbol.rules.refactor.impl.RefactorerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.AutoIRIMapper;

public class RefactoringTest {

    static RuleStore ruleStore;
    static OWLOntology ontology;
    static IRI recipeIRI;

    @BeforeClass
    public static void setup() throws Exception {

        recipeIRI = IRI.create("http://kres.iks-project.eu/ontology/meta/rmi_config.owl#MyTestRecipe");

        InputStream ontologyStream = RefactoringTest.class
                .getResourceAsStream("/META-INF/test/testKReSOnt.owl");
        InputStream recipeStream = RefactoringTest.class.getResourceAsStream("/META-INF/test/rmi_config.owl");

        OWLOntologyIRIMapper map1 = new AutoIRIMapper(new File(RefactoringTest.class.getResource(
            "/META-INF/test/").toURI()), false);

        OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
        mgr.addIRIMapper(map1);

        final OWLOntology recipeModel = mgr.loadOntologyFromOntologyDocument(recipeStream);

        mgr = OWLManager.createOWLOntologyManager();
        mgr.addIRIMapper(map1);

        ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(ontologyStream);

        ruleStore = new RuleStore() {

            @Override
            public void setStore(OWLOntology owl) {
                // TODO Auto-generated method stub

            }

            @Override
            public void saveOntology() throws OWLOntologyStorageException {
                // TODO Auto-generated method stub

            }

            @Override
            public RecipeList listRecipes() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Set<IRI> listIRIRecipes() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getRuleStoreNamespace() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Recipe getRecipe(IRI recipeIRI) throws NoSuchRecipeException {
                Recipe recipe = null;

                if (recipeIRI != null) {
                    OWLDataFactory factory = OWLManager.getOWLDataFactory();
                    OWLIndividual recipeIndividual = factory.getOWLNamedIndividual(recipeIRI);
                    if (recipeIndividual != null) {
                        String ruleNS = "http://kres.iks-project.eu/ontology/meta/rmi.owl#";

                        /**
                         * First get the recipe description in the rule/recipe ontology.
                         */
                        OWLDataProperty hasDescription = factory.getOWLDataProperty(IRI
                                .create(ruleNS + "hasDescription"));

                        String recipeDescription = null;

                        Set<OWLLiteral> descriptions = recipeIndividual.getDataPropertyValues(hasDescription,
                            recipeModel);
                        for (OWLLiteral description : descriptions) {
                            recipeDescription = description.getLiteral();
                        }

                        /**
                         * Then retrieve the rules associated to the recipe in the rule store.
                         */
                        OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI
                                .create(ruleNS + "hasRule"));
                        Set<OWLIndividual> rules = recipeIndividual.getObjectPropertyValues(objectProperty,
                            recipeModel);

                        String kReSRulesInKReSSyntax = "";

                        /**
                         * Fetch the rule content expressed as a literal in Rule Syntax.
                         */
                        OWLDataProperty hasBodyAndHead = factory.getOWLDataProperty(IRI
                                .create(ruleNS + "hasBodyAndHead"));
                        for (OWLIndividual rule : rules) {

                            Set<OWLLiteral> kReSRuleLiterals = rule.getDataPropertyValues(hasBodyAndHead,
                                recipeModel);

                            for (OWLLiteral kReSRuleLiteral : kReSRuleLiterals) {
                                String ruleTmp = kReSRuleLiteral.getLiteral().replace("&lt;", "<");
                                ruleTmp = ruleTmp.replace("&gt;", ">");
                                kReSRulesInKReSSyntax += ruleTmp + System.getProperty("line.separator");
                            }
                        }

                        /**
                         * Create the Recipe object.
                         */

                        RuleList ruleList = RuleParserImpl.parse(kReSRulesInKReSSyntax).getkReSRuleList();
                        recipe = new RecipeImpl(recipeIRI, recipeDescription, ruleList);
                    } else {
                        throw new NoSuchRecipeException(recipeIRI);
                    }
                }

                return recipe;
            }

            @Override
            public OWLOntology getOntology() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getFilePath() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean addRecipe(IRI recipeIRI, String recipeDescription) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public Recipe addRuleToRecipe(String recipeID, String kReSRuleInKReSSyntax) throws NoSuchRecipeException {
                return null;

            }

            @Override
            public Recipe addRuleToRecipe(Recipe recipe, String kReSRuleInKReSSyntax) {
                return null;
                // TODO Auto-generated method stub

            }

            @Override
            public void createRecipe(String recipeID, String rulesInKReSSyntax) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean removeRecipe(Recipe recipe) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean removeRecipe(IRI recipeIRI) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public boolean removeRule(Rule rule) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Recipe addRuleToRecipe(Recipe recipe, InputStream ruleInKReSSyntax) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Recipe addRuleToRecipe(String recipeID, InputStream ruleInKReSSyntax) throws NoSuchRecipeException {
                // TODO Auto-generated method stub
                return null;
            }
        };

    }

    @Test
    public void refactoringTest() throws Exception {
        Dictionary<String,Object> emptyConfig = new Hashtable<String,Object>();

        class SpecialTcManager extends TcManager {
            public SpecialTcManager(QueryEngine qe, WeightedTcProvider wtcp) {
                super();
                bindQueryEngine(qe);
                bindWeightedTcProvider(wtcp);
            }
        }

        QueryEngine qe = new JenaSparqlEngine();
        WeightedTcProvider wtcp = new SimpleTcProvider();
        TcManager tcm = new SpecialTcManager(qe, wtcp);


        Refactorer refactorer = new RefactorerImpl(null, new Serializer(), tcm, ruleStore,
                emptyConfig);
        try {
            refactorer.ontologyRefactoring(ontology, recipeIRI);
        } catch (RefactoringException e) {
            fail("Error while refactoring.");
        } catch (NoSuchRecipeException e) {
            fail("Error while refactoring: no such recipe");
        }
    }

    @Test
    public void easyRefactoringTest() throws Exception {
        Dictionary<String,Object> emptyConfig = new Hashtable<String,Object>();

        class SpecialTcManager extends TcManager {
            public SpecialTcManager(QueryEngine qe, WeightedTcProvider wtcp) {
                super();
                bindQueryEngine(qe);
                bindWeightedTcProvider(wtcp);
            }
        }

        QueryEngine qe = new JenaSparqlEngine();
        WeightedTcProvider wtcp = new SimpleTcProvider();
        TcManager tcm = new SpecialTcManager(qe, wtcp);

        String recipe = "rule[is(<http://kres.iks-project.eu/ontology.owl#Person>, ?x) -> is(<http://xmlns.com/foaf/0.1/Person>, ?x)]";

        KB kb = RuleParserImpl.parse(recipe);
        RuleList ruleList = kb.getkReSRuleList();
        Recipe actualRecipe = new RecipeImpl(null, null, ruleList);

        Refactorer refactorer = new RefactorerImpl(null, new Serializer(), tcm, ruleStore,
                emptyConfig);
        try {
            refactorer.ontologyRefactoring(ontology, actualRecipe);
        } catch (RefactoringException e) {
            fail("Error while refactoring.");
        }
    }

    @Test
    public void brokenRecipeTest() throws Exception {
        Dictionary<String,Object> emptyConfig = new Hashtable<String,Object>();

        class SpecialTcManager extends TcManager {
            public SpecialTcManager(QueryEngine qe, WeightedTcProvider wtcp) {
                super();
                bindQueryEngine(qe);
                bindWeightedTcProvider(wtcp);
            }
        }

        QueryEngine qe = new JenaSparqlEngine();
        WeightedTcProvider wtcp = new SimpleTcProvider();
        TcManager tcm = new SpecialTcManager(qe, wtcp);

        // broken recipe
        String recipe = "rule[is(<http://kres.iks-project.eu/ontology.owl#Person>) -> is(<http://xmlns.com/foaf/0.1/Person>, ?x)]";

        KB kb = null;
        try {
            kb = RuleParserImpl.parse(recipe);
        } catch (IllegalStateException e) {
            assertTrue(true);
        }
    }

}
