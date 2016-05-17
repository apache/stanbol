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

package org.apache.stanbol.rules.adapters.sparql;

import java.util.List;

import junit.framework.Assert;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.SPARQLObject;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.RecipeImpl;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anuzzolese
 * 
 */
public class SPARQLAdpterTest {

    private static Logger log = LoggerFactory.getLogger(SPARQLAdpterTest.class);

    private Recipe recipeGood;
    private Recipe recipeWrong;
    private static RuleAdapter ruleAdapter;

    @BeforeClass
    public static void setUpClass() {
        ruleAdapter = new SPARQLAdapter();
    }

    @AfterClass
    public static void tearDownClass() {
        ruleAdapter = null;
    }

    @Before
    public void setUp() {
        String separator = System.getProperty("line.separator");
        String recipeString = "kres = <http://kres.iks-project.eu/ontology.owl#> . "
                              + separator
                              + "foaf = <http://xmlns.com/foaf/0.1/> . "
                              + separator
                              + "rule1[ is(kres:Person, ?x) . has(kres:friend, ?x, ?y) -> is(foaf:Person, ?x) . has(foaf:knows, ?x, ?y) . is(foaf:Person, ?y)] . "
                              + "rule2[ is(kres:Person, ?x) . values(kres:age, ?x, ?age) . endsWith(?t, \"string\") . gt(?age, sum(sub(70, ?k), ?z)) -> is(kres:OldPerson, ?x)]";

        KB kb = RuleParserImpl.parse("http://incubator.apache.com/stanbol/rules/adapters/jena/test/",
            recipeString);

        recipeGood = new RecipeImpl(
                new IRI("http://incubator.apache.com/stanbol/rules/adapters/jena/test"), "A recipe.",
                kb.getRuleList());

        recipeString = "kres = <http://kres.iks-project.eu/ontology.owl#> . "
                       + separator
                       + "foaf = <http://xmlns.com/foaf/0.1/> . "
                       + separator
                       + "rule1[ is(kres:Person, ?x) . has(kres:friend, ?x, ?y) -> is(foaf:Person, ?x) . has(foaf:knows, ?x, ?y) . is(foaf:Person, ?y)] . "
                       + "rule2[ is(kres:Person, ?x) . same(\"Andrea\", localname(?x)) -> is(kres:OldPerson, ?x)]";

        kb = RuleParserImpl.parse("http://incubator.apache.com/stanbol/rules/adapters/jena/test/",
            recipeString);

        recipeWrong = new RecipeImpl(new IRI(
                "http://incubator.apache.com/stanbol/rules/adapters/jena/test"), "A recipe.",
                kb.getRuleList());
    }

    @After
    public void tearDown() {
        recipeGood = null;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        try {

            List<SPARQLObject> sparqlObjects = (List<SPARQLObject>) ruleAdapter.adaptTo(recipeGood,
                SPARQLObject.class);

            for (SPARQLObject sparqlObject : sparqlObjects) {

                log.debug(sparqlObject.getObject());

            }

            Assert.assertTrue(true);
        } catch (UnavailableRuleObjectException e) {
            Assert.fail(e.getMessage());
        } catch (UnsupportedTypeForExportException e) {
            Assert.fail(e.getMessage());
        } catch (RuleAtomCallExeption e) {
            Assert.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void wrongAdaptabeClassTest() {
        try {

            List<String> rules = (List<String>) ruleAdapter.adaptTo(recipeGood, List.class);
            for (String rule : rules) {
                log.debug(rule);
                Assert.fail("The adapter for SPARQL should not accept " + List.class.getCanonicalName()
                            + " objects.");
            }
        } catch (UnavailableRuleObjectException e) {
            Assert.fail(e.getMessage());
        } catch (UnsupportedTypeForExportException e) {
            log.debug(e.getMessage());
        } catch (RuleAtomCallExeption e) {
            Assert.fail(e.getMessage());
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    public void unavailableRuleObjectTest() {
        try {

            List<SPARQLObject> sparqlObjects = (List<SPARQLObject>) ruleAdapter.adaptTo(recipeWrong,
                SPARQLObject.class);
            for (SPARQLObject sparqlObject : sparqlObjects) {
                log.debug(sparqlObject.toString());
            }
        } catch (UnavailableRuleObjectException e) {
            Assert.fail(e.getMessage());
        } catch (UnsupportedTypeForExportException e) {
            Assert.fail(e.getMessage());
        } catch (RuleAtomCallExeption e) {
            Assert.assertTrue(e.getMessage(), true);
        }

    }
}
