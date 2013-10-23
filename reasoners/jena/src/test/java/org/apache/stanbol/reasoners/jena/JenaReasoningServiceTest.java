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
package org.apache.stanbol.reasoners.jena;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.reasoners.servicesapi.InconsistentInputException;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException;
import org.apache.stanbol.reasoners.servicesapi.UnsupportedTaskException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasonerFactory;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.ReasonerVocabulary;

/**
 * Here we develop tests related to the methods of the shared interface. All the tests here should work at the
 * simplest possible (RDFS) level, because inferences must be the same for all the standard Jena reasoners.
 * 
 */
public class JenaReasoningServiceTest {

    private static final Logger log = LoggerFactory.getLogger(JenaReasoningServiceTest.class);

    private List<JenaReasoningService> reasoners;

    /**
     * This method instantiate the reasoning services to work with in this tests
     */
    @Before
    public void setUp() {
        reasoners = new ArrayList<JenaReasoningService>();
        reasoners.add(new JenaOWLReasoningService());
        reasoners.add(new JenaRDFSReasoningService());
        reasoners.add(new JenaOWLMiniReasoningService());
    }

    @Test
    public void testClassify() {
        log.info("Testing the classify() method with all available reasoners");

        for (JenaReasoningService service : reasoners) {
            log.info("Testing : {}", service.getClass());
            testClassify(service);
        }
    }

    @Test
    public void testEnrich() {
        log.info("Testing the enrich() method with all available reasoners");

        for (JenaReasoningService service : reasoners) {
            log.info("Testing : {}", service.getClass());
            testEnrich(service);
        }
    }

    @Test
    public void testEnrich2() {
        log.info("Testing the enrich() method (filtered = false) with all available reasoners");

        for (JenaReasoningService service : reasoners) {
            log.info("Testing : {}", service.getClass());
            testEnrich2(service);
        }
    }

    @Test
    public void testIsConsistent() {
        log.info("Testing the isConsistent() method with all available reasoners with consistent data");

        /**
         * Note: since the inconsistency depends on the reasoner type, we move that check on the respective
         * test classes
         */
        for (JenaReasoningService service : reasoners) {
            log.info("Testing : {}", service.getClass());
            testIsConsistent(service);
        }
    }

    /**
     * Tests the classify() method
     */
    private void testClassify(JenaReasoningService reasoningService) {
        // Clean data
        TestData.alexdma.removeProperties();

        // Prepare data
        TestData.alexdma.addProperty(RDF.type, TestData.foaf_Person);

        // Setup input for the reasoner
        Model input = ModelFactory.createUnion(TestData.foaf, TestData.alexdma.getModel());

        // Run the method
        Set<Statement> inferred;
        try {
            inferred = reasoningService.runTask(ReasoningService.Tasks.CLASSIFY, input);

            boolean foafAgentExists = false;
            log.info("Check for statements to be rdf:type only");
            for (Statement stat : inferred) {
                // Here we want only rdf:type statements
                if (!stat.getPredicate().equals(RDF.type)) {
                    log.error("This statement is not rdf:type: {}", stat);
                }
                assertTrue(stat.getPredicate().equals(RDF.type));
                if (stat.getObject().isResource()) {
                    if (stat.getObject().asResource().equals(TestData.foaf_Agent)) {
                        foafAgentExists = true;
                    }
                }
            }

            log.info("Does the statement: example:me rdf:type foaf:Agent exists (true)? {}", foafAgentExists);
            assertTrue(foafAgentExists);
        } catch (ReasoningServiceException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        }
        // Clean data
        TestData.alexdma.removeProperties();
    }

    /**
     * Tests the enrich(Model data) method
     * 
     * @param service
     */
    private void testEnrich(JenaReasoningService service) {
        // Clean data
        TestData.alexdma.removeProperties();

        // Prepare data
        TestData.alexdma.addProperty(RDF.type, TestData.foaf_Person);

        // Setup input for the reasoner
        Model input = ModelFactory.createUnion(TestData.foaf, TestData.alexdma.getModel());
        try {
            // Run the method
            Set<Statement> inferred = service.runTask(ReasoningService.Tasks.ENRICH, input);

            // Prepare the input statements to check the output with
            Set<Statement> inputStatements = input.listStatements().toSet();

            boolean onlyInferred = true;
            log.info("Check for statements to be only the inferred ones");
            Set<Statement> badOnes = new HashSet<Statement>();
            for (Statement stat : inferred) {
                // Must not be a statement in input
                if (inputStatements.contains(stat)) {
                    onlyInferred = false;
                    badOnes.add(stat);
                }
            }

            log.info("Are there only inferred statements (true)? {}", onlyInferred);
            if (!onlyInferred) {
                for (Statement bad : badOnes) {
                    log.error("Found a bad statement in output: {}", bad);
                }
            }
            assertTrue(onlyInferred);

        } catch (ReasoningServiceException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        }
        // Clean data
        TestData.alexdma.removeProperties();
    }

    /**
     * Tests the enrich(Model data,boolean filtered) method
     * 
     * @param service
     */
    private void testEnrich2(JenaReasoningService service) {
        // Clean data
        TestData.alexdma.removeProperties();

        // Prepare data
        TestData.alexdma.addProperty(RDF.type, TestData.foaf_Person);

        // Setup input for the reasoner
        Model input = ModelFactory.createUnion(TestData.foaf, TestData.alexdma.getModel());

        try {
            // Run the method
            Set<Statement> inferred = service.runTask(ReasoningService.Tasks.ENRICH,input, null, false, null);

            // Prepare the input statements to check the output with
            Set<Statement> inputStatements = input.listStatements().toSet();

            log.info("All the input statements must be in the inferred output");
            Set<Statement> notInOutput = new HashSet<Statement>();
            for (Statement stat : inputStatements) {
                if (!inferred.contains(stat)) {
                    notInOutput.add(stat);
                }
            }

            log.info("Are all input statements in the inferred set (true)? {}", notInOutput.isEmpty());
            if (!notInOutput.isEmpty()) {
                for (Statement bad : notInOutput) {
                    log.error("Found a statement not included in output: {}", bad);
                }
            }
            assertTrue(notInOutput.isEmpty());
        } catch (ReasoningServiceException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        }
        // Clean data
        TestData.alexdma.removeProperties();
    }

    /**
     * 
     * @param service
     */
    private void testIsConsistent(JenaReasoningService service) {
        // Clean data
        TestData.alexdma.removeProperties();

        // Prepare data
        TestData.alexdma.addProperty(RDF.type, TestData.foaf_Person);

        // Setup input for the reasoner
        Model input = ModelFactory.createUnion(TestData.foaf, TestData.alexdma.getModel());

        try {
            // Run the method
            assertTrue(service.isConsistent(input));
        } catch (ReasoningServiceException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        }
        // Clean data
        TestData.alexdma.removeProperties();
    }

    /**
     * Test the GenericRuleReasoner
     * 
     */
    @Test
    public void testGenericRuleReasoner() {
        log.info("Test a Generic Rule Reasoner with RDFS rules");

        // Configuration
        Model m = ModelFactory.createDefaultModel();
        Resource configuration = m.createResource();
        configuration.addProperty(ReasonerVocabulary.PROPruleMode, "hybrid");
        configuration.addProperty(ReasonerVocabulary.PROPruleSet, "generic.rdfs.rules");

        // Create an instance of such a reasoner
        Reasoner reasoner = GenericRuleReasonerFactory.theInstance().create(configuration);

        // This should behave as the RDFSReasoner
        JenaReasoningService genericRdfs = new AbstractJenaReasoningService(reasoner) {

            @Override
            public String getPath() {
                // We don't need this now
                return null;
            }
        };

        // Then test the three methods
        testClassify(genericRdfs);
        testEnrich(genericRdfs);
        testIsConsistent(genericRdfs);
    }

    /**
     * Run reasoner with a simple rule set as additional input
     */
    @Test
    public void testRunWithRules() {
        log.info("Testing the run(Model model, List<Rule> rules) method with all available reasoners");

        for (JenaReasoningService service : reasoners) {
            log.info("Testing : {}", service.getClass());
            testRunWithRules(service);
        }
    }

    /**
     * Check consistency with a simple rule set as additional input
     */
    @Test
    public void testIsConsistentWithRules() {
        log.info("Testing the isConsistent(Model model, List<Rule> rules) method with all available reasoners");

        for (JenaReasoningService service : reasoners) {
            log.info("Testing : {}", service.getClass());
            testIsConsistentWithRules(service);
        }
    }

    /**
     * Tests the isConsistent(Model,List<Rule>) method of a service
     * 
     * @param service
     */
    private void testIsConsistentWithRules(JenaReasoningService service) {
        log.info("Testing reasoner of type {}", service.getClass());

        // Prepare the rule set
        String source = ""
                        + "\n@prefix foaf: <"
                        + TestData.FOAF_NS
                        + ">."
                        + "\n@prefix ex: <"
                        + TestData.TEST_NS
                        + ">."
                        + "\n[rule1: (?a foaf:knows ?b) (?a foaf:workplaceHomepage ?w) (?b foaf:workplaceHomepage ?w) -> (?a ex:collegueOf ?b)] "
                        + "\n[rule2: (?b foaf:knows ?a) -> (?a foaf:knows ?b)] "
                        + "\n[rule3: (?a ex:collegueOf ?b) -> (?b ex:collegueOf ?a)] ";

        // log.info("This is the ruleset: \n {}", source);
        List<Rule> rules = TestUtils.parseRuleStringAsFile(source);
        log.info("Loaded {} rules", rules.size());

        // Clean data
        TestData.alexdma.removeProperties();
        TestData.enridaga.removeProperties();

        Resource wphomepage = TestData.model.createResource("http://stlab.istc.cnr.it");

        // Prepare data
        TestData.alexdma.addProperty(TestData.foaf_knows, TestData.enridaga);
        TestData.alexdma.addProperty(TestData.foaf_workplaceHomepage, wphomepage);
        TestData.enridaga.addProperty(TestData.foaf_workplaceHomepage, wphomepage);

        // Setup input for the reasoner
        Model input = ModelFactory.createUnion(TestData.enridaga.getModel(), TestData.alexdma.getModel());
        input = ModelFactory.createUnion(input, TestData.foaf);
        try {
            // Run the method
            boolean isConsistent = service.isConsistent(input, rules);

            // Assert true
            log.info("Is consistent (true)? {}", isConsistent);
            assertTrue(isConsistent);
        } catch (ReasoningServiceException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        }
        // Clean data
        TestData.alexdma.removeProperties();
        TestData.enridaga.removeProperties();
    }

    /**
     * Tests the run(Model,List<Rule>) method of a service
     * 
     * @param service
     */
    private void testRunWithRules(JenaReasoningService service) {

        // Prepare the rule set
        String source = ""
                        + "\n@prefix foaf: <"
                        + TestData.FOAF_NS
                        + ">."
                        + "\n@prefix ex: <"
                        + TestData.TEST_NS
                        + ">."
                        + "\n[rule1: (?a foaf:knows ?b) (?a foaf:workplaceHomepage ?w) (?b foaf:workplaceHomepage ?w) -> (?a ex:collegueOf ?b)] "
                        + "\n[rule2: (?b foaf:knows ?a) -> (?a foaf:knows ?b)] "
                        + "\n[rule3: (?a ex:collegueOf ?b) -> (?b ex:collegueOf ?a)] ";

        // log.info("This is the ruleset: \n {}", source);
        List<Rule> rules = TestUtils.parseRuleStringAsFile(source);
        log.info("Loaded {} rules", rules.size());

        // Clean data
        TestData.alexdma.removeProperties();
        TestData.enridaga.removeProperties();

        Resource wphomepage = TestData.model.createResource("http://stlab.istc.cnr.it");

        // Prepare data
        TestData.alexdma.addProperty(TestData.foaf_knows, TestData.enridaga);
        TestData.alexdma.addProperty(TestData.foaf_workplaceHomepage, wphomepage);
        TestData.enridaga.addProperty(TestData.foaf_workplaceHomepage, wphomepage);

        // Setup input for the reasoner
        Model input = ModelFactory.createUnion(TestData.enridaga.getModel(), TestData.alexdma.getModel());
        input = ModelFactory.createUnion(input, TestData.foaf);

        // Run the method
        Set<Statement> inferred = service.run(input, rules).listStatements().toSet();

        // Expected statements
        Property collegueOf = TestData.model.createProperty(TestData.TEST_NS + "collegueOf");
        Set<Statement> expected = new HashSet<Statement>();
        expected.add(TestData.model.createStatement(TestData.alexdma, collegueOf, TestData.enridaga));
        expected.add(TestData.model.createStatement(TestData.enridaga, collegueOf, TestData.alexdma));

        log.info("All the expected statements must be in the inferred output");
        Set<Statement> notInOutput = TestUtils.expectedStatementsCheck(inferred, expected);
        log.info("Are all expected statements in the inferred set (true)? {}", notInOutput.isEmpty());
        if (!notInOutput.isEmpty()) {
            for (Statement bad : notInOutput) {
                log.error("The following statement is not included in the reasoner output: {}", bad);
            }
        }
        assertTrue(notInOutput.isEmpty());

        // Clean data
        TestData.alexdma.removeProperties();
        TestData.enridaga.removeProperties();
    }

    /**
     * Check consistency with a simple rule set as additional input
     */
    @Test
    public void testClassifyWithRules() {
        log.info("Testing the classify(Model model, List<Rule> rules) method with all available reasoners");

        for (JenaReasoningService service : reasoners) {
            log.info("Testing : {}", service.getClass());
            testClassifyWithRule(service);
        }
    }

    /**
     * Tests the classify(Model data, List<Rule> rules) method
     */
    private void testClassifyWithRule(JenaReasoningService service) {
        log.info("Testing {}", service.getClass());

        // Prepare the rule set
        String source = ""
                        + "\n@prefix rdf: <"
                        + RDF.getURI()
                        + ">."
                        + "\n@prefix foaf: <"
                        + TestData.FOAF_NS
                        + ">."
                        + "\n@prefix ex: <"
                        + TestData.TEST_NS
                        + ">."
                        + "\n[rule: (?a foaf:workplaceHomepage ?w) (?w rdf:type ex:SWResearchLab) -> (?a rdf:type ex:SWResearcher)] ";

        // log.info("This is the ruleset: \n {}", source);
        List<Rule> rules = TestUtils.parseRuleStringAsFile(source);
        log.info("Loaded {} rules", rules.size());

        // Clean data
        TestData.alexdma.removeProperties();
        TestData.enridaga.removeProperties();

        Resource wphomepage = TestData.model.createResource("http://stlab.istc.cnr.it");
        Resource swResearchLab = TestData.model.createResource(TestData.TEST_NS + "SWResearchLab");

        // Prepare data
        TestData.alexdma.addProperty(TestData.foaf_workplaceHomepage, wphomepage);
        TestData.enridaga.addProperty(TestData.foaf_workplaceHomepage, wphomepage);
        wphomepage.addProperty(RDF.type, swResearchLab);

        // Setup input for the reasoner
        Model input = ModelFactory.createUnion(TestData.enridaga.getModel(), TestData.alexdma.getModel());
        input = ModelFactory.createUnion(input, wphomepage.getModel());
        input = ModelFactory.createUnion(input, TestData.foaf);

        try {
            // Run the method
            Set<Statement> inferred = service.runTask(ReasoningService.Tasks.CLASSIFY,input, rules, false, null);

            // Expected statements
            Resource swResearcher = TestData.model.createResource(TestData.TEST_NS + "SWResearcher");
            Set<Statement> expected = new HashSet<Statement>();
            expected.add(TestData.model.createStatement(TestData.alexdma, RDF.type, swResearcher));
            expected.add(TestData.model.createStatement(TestData.enridaga, RDF.type, swResearcher));

            log.info("All the expected statements must be in the inferred output");
            Set<Statement> notInOutput = TestUtils.expectedStatementsCheck(inferred, expected);
            log.info("Are all expected statements in the inferred set (true)? {}", notInOutput.isEmpty());
            if (!notInOutput.isEmpty()) {
                for (Statement bad : notInOutput) {
                    log.error("The following statement is not included in the reasoner output: {}", bad);
                }
            }
            assertTrue(notInOutput.isEmpty());

            // There must be only rdf:type output
            boolean onlyRdf = true;
            for (Statement stat : inferred) {
                // Here we want only rdf:type statements
                if (!stat.getPredicate().equals(RDF.type)) {
                    log.error("This statement is not rdf:type: {}", stat);
                }
                if (!stat.getPredicate().equals(RDF.type)) {
                    onlyRdf = false;
                }
            }
            log.info("Check for statements to be rdf:type only (true): {}", onlyRdf);
            assertTrue(onlyRdf);
        } catch (ReasoningServiceException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("Error thrown: {}", e);
            assertTrue(false);
        }
        // Clean data
        TestData.alexdma.removeProperties();
        TestData.enridaga.removeProperties();
    }
}
