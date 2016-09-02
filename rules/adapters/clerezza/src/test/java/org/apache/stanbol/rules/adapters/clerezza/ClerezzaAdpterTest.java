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

package org.apache.stanbol.rules.adapters.clerezza;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;


import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.sparql.ParseException;
import org.apache.clerezza.rdf.core.sparql.QueryParser;
import org.apache.clerezza.rdf.core.sparql.query.ConstructQuery;
import org.apache.stanbol.rules.adapters.clerezza.ClerezzaAdapter;
import org.apache.stanbol.rules.adapters.impl.RuleAdaptersFactoryImpl;
import org.apache.stanbol.rules.base.api.Recipe;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdaptersFactory;
import org.apache.stanbol.rules.base.api.RuleAtomCallExeption;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.apache.stanbol.rules.base.api.UnsupportedTypeForExportException;
import org.apache.stanbol.rules.manager.ClerezzaRuleStore;
import org.apache.stanbol.rules.manager.KB;
import org.apache.stanbol.rules.manager.RecipeImpl;
import org.apache.stanbol.rules.manager.parse.RuleParserImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * 
 * @author anuzzolese
 * 
 */
public class ClerezzaAdpterTest {

    private static Logger log = LoggerFactory.getLogger(ClerezzaAdpterTest.class);

    private Recipe recipeGood;
    private Recipe recipeWrong;
    private static RuleAdapter ruleAdapter;

    @BeforeClass
    public static void setUpClass() {
        
        TcManager tcm = TcManager.getInstance();//new SpecialTcManager(qe, wtcp);

        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        RuleAdaptersFactory ruleAdaptersFactory = new RuleAdaptersFactoryImpl();
        RuleStore ruleStore = new ClerezzaRuleStore(configuration, tcm);
        
        ruleAdapter = new ClerezzaAdapter(configuration, ruleStore, ruleAdaptersFactory);
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

            List<ConstructQuery> constructQueries = (List<ConstructQuery>) ruleAdapter.adaptTo(recipeGood,
                ConstructQuery.class);

            for (ConstructQuery constructQuery : constructQueries) {

                ConstructQuery cq = (ConstructQuery) QueryParser.getInstance().parse(
                    constructQuery.toString());
                System.out.println(cq.toString());
            }

            Assert.assertTrue(true);
        } catch (UnavailableRuleObjectException e) {
            Assert.fail(e.getMessage());
        } catch (UnsupportedTypeForExportException e) {
            Assert.fail(e.getMessage());
        } catch (RuleAtomCallExeption e) {
            Assert.fail(e.getMessage());
        } catch (ParseException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void test2() {
        try {

            String query = "CONSTRUCT { ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://kres.iks-project.eu/ontology.owl#OldPerson> . } WHERE { ?x <http://kres.iks-project.eu/ontology.owl#age> ?age . ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://kres.iks-project.eu/ontology.owl#Person> . FILTER <http://www.w3.org/2005/xpath-functions#ends-with>(?t,\"string\") . FILTER ((?age) > (((\"70\"^^<http://www.w3.org/2001/XMLSchema#string>) - (?k)) + (?z))) }";

            QueryParser.getInstance().parse(query);

            Assert.assertTrue(true);
        } catch (ParseException e) {
            Assert.fail(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void wrongAdaptabeClassTest() {
        try {

            List<Rule> rules = (List<Rule>) ruleAdapter.adaptTo(recipeGood, Rule.class);
            for (Rule rule : rules) {
                log.debug(rule.toString());
                Assert.fail("The adapter for Clerezza should not accept " + Rule.class.getCanonicalName()
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

            List<ConstructQuery> constructQueries = (List<ConstructQuery>) ruleAdapter.adaptTo(recipeWrong,
                ConstructQuery.class);
            for (ConstructQuery constructQuery : constructQueries) {
                log.debug(constructQuery.toString());
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
