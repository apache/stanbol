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
package org.apache.stanbol.reasoners.hermit;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.reasoners.servicesapi.InconsistentInputException;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException;
import org.apache.stanbol.reasoners.servicesapi.UnsupportedTaskException;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests for the HermitReasoningService These tests should be significant for any OWLApiReasoningService.
 * 
 * TODO: we may want to isolate this is a separate test mini-framework.
 * 
 */
public class HermitReasoningServiceTest {
    private static final Logger log = LoggerFactory.getLogger(HermitReasoningServiceTest.class);

    private HermitReasoningService theinstance;

    /**
     * This method instantiate the reasoning services to work with in this tests
     */
    @Before
    public void setUp() {
        this.theinstance = new HermitReasoningService();
    }

    @Test
    public void testInstantiation() {
        log.info("Testing the innstantiation of an HermitReasoningService");
        OWLOntology foaf = TestData.manager.getOntology(IRI.create(TestData.FOAF_NS));
        // Here we want to be sure that the getReasoner() method creates an
        // instance correctly
        HermitReasoningService akindofhermit = new HermitReasoningService() {
            protected OWLReasoner getReasoner(OWLOntology ontology) {
                try {
                    OWLReasoner reasoner = super.getReasoner(ontology);
                    assertTrue(reasoner != null);
                    return reasoner;
                } catch (Throwable t) {
                    log.error(
                        "Some prolem occurred while instantiating the HermitReasoningService. Message was: {}",
                        t.getLocalizedMessage());
                    // We force tests to stop here
                    assertTrue(false);
                    return null;
                }
            }
        };
        long startHere = System.currentTimeMillis();
        // This is possible only because the subclass is in this scope...
        akindofhermit.getReasoner(foaf);
        long endHere = System.currentTimeMillis();
        log.info("Instantiating an Hermit reasoner lasts {} milliseconds", (endHere - startHere));
    }

    private void testRun(String testID, String expectedID) {
        log.info("Testing the run() method");
        OWLOntologyManager manager = TestData.manager;

        // We prepare the input ontology
        try {
            OWLOntology testOntology = manager.createOntology();
            OWLOntologyID testOntologyID = testOntology.getOntologyID();
            log.debug("Created test ontology with ID: {}", testOntologyID);
            AddImport addImport = new AddImport(testOntology, TestData.factory.getOWLImportsDeclaration(IRI
                    .create(testID)));
            manager.applyChange(addImport);
            // We just test class assertions
            List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
            gens.add(new InferredClassAssertionAxiomGenerator());

            // Maybe we want to see what is in before
            if (log.isDebugEnabled()) TestUtils.debug(manager.getOntology(testOntologyID), log);

            // Now we test the method
            log.debug("Running HermiT");
            Set<OWLAxiom> inferred = this.theinstance.run(manager.getOntology(testOntologyID), gens);

            // Maybe we want to see the inferred axiom list
            if (log.isDebugEnabled()) {
                TestUtils.debug(inferred, log);
            }
            // These are the set of expected axioms
            Set<OWLLogicalAxiom> expectedAxioms = manager.getOntology(IRI.create(expectedID))
                    .getLogicalAxioms();

            Set<OWLAxiom> missing = new HashSet<OWLAxiom>();
            for (OWLAxiom expected : expectedAxioms) {
                if (!inferred.contains(expected)) {
                    log.error("missing expected axiom: {}", expected);
                    missing.add(expected);
                }
            }
            log.info("Are all expected axioms in the result (true)? {}", missing.isEmpty());
            assertTrue(missing.isEmpty());

            // We want to remove the ontology from the manager
            manager.removeOntology(testOntology);
        } catch (OWLOntologyCreationException e) {
            log.error("An {} have been thrown while creating the input ontology for test", e.getClass());
            assertTrue(false);
        } catch (org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (org.apache.stanbol.reasoners.servicesapi.InconsistentInputException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        }
    }

    @Test
    public void testRun() {
        testRun(TestData.TEST_1_NS, TestData.TEST_1_expected_NS);
    }

    /**
     * Tests the classify() method
     */
    @Test
    public void testClassify() {
        testClassify(TestData.TEST_1_NS, TestData.TEST_1_expected_NS);
    }

    /**
     * We may want to test this method with more then 1 ontology. This is why the implementation is in
     * aprivate method. This method tests if all the logical axioms in testExpectedID ontology are inferences
     * of the testID ontology.
     * 
     * @param testID
     *            // The ID of the ontology to be the input (loaded in the TestData.manager)
     * @param testExpectedID
     *            // The ID of the ontology which contains logical axioms expected in the result
     */
    private void testClassify(String testID, String testExpectedID) {
        log.info("Testing the CLASSIFY task");

        OWLOntologyManager manager = TestData.manager;

        // We prepare the input ontology
        try {
            OWLOntology testOntology = manager.createOntology();
            OWLOntologyID testOntologyID = testOntology.getOntologyID();
            log.debug("Created test ontology with ID: {}", testOntologyID);
            manager.applyChange(new AddImport(testOntology, TestData.factory.getOWLImportsDeclaration(IRI
                    .create(testID))));

            // Maybe we want to see what is in before
            if (log.isDebugEnabled()) TestUtils.debug(manager.getOntology(testOntologyID), log);

            // Now we test the method
            log.debug("Running HermiT");
            Set<OWLAxiom> inferred = this.theinstance.runTask(ReasoningService.Tasks.CLASSIFY,
                manager.getOntology(testOntologyID));

            // Maybe we want to see the inferred axiom list
            if (log.isDebugEnabled()) {
                TestUtils.debug(inferred, log);
            }

            Set<OWLLogicalAxiom> expectedAxioms = manager.getOntology(IRI.create(testExpectedID))
                    .getLogicalAxioms();
            Set<OWLAxiom> missing = new HashSet<OWLAxiom>();
            for (OWLAxiom expected : expectedAxioms) {
                if (!inferred.contains(expected)) {
                    log.error("missing expected axiom: {}", expected);
                    missing.add(expected);
                }
            }
            assertTrue(missing.isEmpty());

            // We want only Class related axioms in the result set
            for (OWLAxiom a : inferred) {
                assertTrue(a instanceof OWLClassAssertionAxiom || a instanceof OWLSubClassOfAxiom
                           || a instanceof OWLEquivalentClassesAxiom || a instanceof OWLDisjointClassesAxiom);
            }
            // We want to remove the ontology from the manager
            manager.removeOntology(testOntology);
        } catch (OWLOntologyCreationException e) {
            log.error("An {} have been thrown while creating the input ontology for test", e.getClass());
            assertTrue(false);
        } catch (ReasoningServiceException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        }
    }

    /**
     * We may want to test this method with more then 1 ontology. This is why the implementation is in
     * aprivate method. This method tests if all the logical axioms in testExpectedID ontology are inferences
     * of the testID ontology.
     * 
     * TODO: This method is the same as testClassify(String,String), with the only difference - the task
     * called. We may want to have this procedure isolated.
     * 
     * @param testID
     *            // The ID of the ontology to be the input (loaded in the TestData.manager)
     * @param testExpectedID
     *            // The ID of the ontology which contains logical axioms expected in the result
     */
    private void testEnrich(String testID, String testExpectedID) {
        log.info("Testing the ENRICH task");

        OWLOntologyManager manager = TestData.manager;

        // We prepare the input ontology
        try {
            OWLOntology testOntology = manager.createOntology();
            OWLOntologyID testOntologyID = testOntology.getOntologyID();
            log.debug("Created test ontology with ID: {}", testOntologyID);
            OWLImportsDeclaration importTest = TestData.factory.getOWLImportsDeclaration(IRI.create(testID));
            manager.applyChange(new AddImport(testOntology, importTest));

            // Maybe we want to see what is in before
            if (log.isDebugEnabled()) TestUtils.debug(manager.getOntology(testOntologyID), log);

            // Now we test the method
            log.debug("Running HermiT");
            Set<OWLAxiom> inferred = this.theinstance.runTask(ReasoningService.Tasks.ENRICH,
                manager.getOntology(testOntologyID));

            // Maybe we want to see the inferred axiom list
            if (log.isDebugEnabled()) {
                TestUtils.debug(inferred, log);
            }

            Set<OWLLogicalAxiom> expectedAxioms = manager.getOntology(IRI.create(testExpectedID))
                    .getLogicalAxioms();

            Set<OWLAxiom> missing = new HashSet<OWLAxiom>();
            for (OWLAxiom expected : expectedAxioms) {
                if (!inferred.contains(expected)) {
                    log.error("missing expected axiom: {}", expected);
                    missing.add(expected);
                }
            }
            assertTrue(missing.isEmpty());

            // We want to remove the ontology from the manager
            manager.removeOntology(testOntology);
        } catch (OWLOntologyCreationException e) {
            log.error("An {} have been thrown while creating the input ontology for test", e.getClass());
            assertTrue(false);
        } catch (ReasoningServiceException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        }
    }

    /**
     * This method check if the result types are the ones expected
     * 
     */
    @Test
    public void testEnrichResultTypes() {
        // We want all kind of Axioms in the result set
        // Well, not all types, but at least 1 for each
        // InferredAxiomGenerator<?>
        AxiomType<?>[] types = {AxiomType.CLASS_ASSERTION, AxiomType.SUBCLASS_OF,
                                AxiomType.SUB_DATA_PROPERTY, AxiomType.SUB_OBJECT_PROPERTY,
                                AxiomType.DISJOINT_CLASSES, AxiomType.EQUIVALENT_DATA_PROPERTIES,
                                AxiomType.EQUIVALENT_OBJECT_PROPERTIES, AxiomType.INVERSE_OBJECT_PROPERTIES,
                                AxiomType.EQUIVALENT_CLASSES, AxiomType.DISJOINT_CLASSES};
        // Call the test method
        testEnrichResultTypes(TestData.TEST_1_NS, types);
    }

    /**
     * 
     * @param testID
     *            // The ontology to run
     * @param types
     *            // The type of axioms we expect in the result
     */
    private void testEnrichResultTypes(String testID, AxiomType<?>[] types) {

        List<AxiomType<?>> typelist = new ArrayList<AxiomType<?>>();
        typelist.addAll(Arrays.asList(types));
        log.info("Testing the enrich() method (result axioms types)");

        OWLOntologyManager manager = TestData.manager;

        // We prepare the input ontology
        try {
            OWLOntology testOntology = manager.createOntology();
            OWLOntologyID testOntologyID = testOntology.getOntologyID();
            log.debug("Created test ontology with ID: {}", testOntologyID);
            OWLImportsDeclaration importTest = TestData.factory.getOWLImportsDeclaration(IRI.create(testID));
            manager.applyChange(new AddImport(testOntology, importTest));

            // Maybe we want to see what is in before
            if (log.isDebugEnabled()) TestUtils.debug(manager.getOntology(testOntologyID), log);

            // Now we test the method
            log.debug("Running HermiT");
            Set<OWLAxiom> inferred = this.theinstance.runTask(ReasoningService.Tasks.ENRICH,
                manager.getOntology(testOntologyID));

            // Maybe we want to see the inferred axiom list
            if (log.isDebugEnabled()) {
                TestUtils.debug(inferred, log);
            }
            for (OWLAxiom a : inferred) {
                typelist.remove(a.getAxiomType());
            }
            if (!typelist.isEmpty()) {
                for (AxiomType<?> t : typelist)
                    log.error("Missing axiom type: {}", t);
            }
            assertTrue(typelist.isEmpty());

            // We want to remove the ontology from the manager
            manager.removeOntology(testOntology);
        } catch (OWLOntologyCreationException e) {
            log.error("An {} have been thrown while creating the input ontology for test", e.getClass());
            assertTrue(false);
        } catch (ReasoningServiceException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        }

    }

    @Test
    public void testEnrich() {
        testEnrich(TestData.TEST_1_NS, TestData.TEST_1_expected_NS);
    }

    /**
     * Test if the ontology is consistent or not.
     */
    @Test
    public void testIsConsistent() {
        // This first ontology is consistent
        testIsConsistent(TestData.TEST_1_NS, true);
        // The second is not consistent
        testIsConsistent(TestData.TEST_2_NS, false);
    }

    /**
     * Check whether the ontology is consistent
     * 
     * @param testID
     *            // The ID of the ontology to test
     * @param expected
     *            // If it is expected to be consistent or not
     */
    private void testIsConsistent(String testID, boolean expected) {
        log.info("Testing the isConsistent method");
        OWLOntologyManager manager = TestData.manager;

        // We prepare the input ontology
        try {
            OWLOntology testOntology = manager.createOntology();
            OWLOntologyID testOntologyID = testOntology.getOntologyID();
            log.debug("Created test ontology with ID: {}", testOntologyID);
            OWLImportsDeclaration importTest = TestData.factory.getOWLImportsDeclaration(IRI.create(testID));
            manager.applyChange(new AddImport(testOntology, importTest));

            // Maybe we want to see what is in before
            if (log.isDebugEnabled()) TestUtils.debug(manager.getOntology(testOntologyID), log);

            // Now we test the method
            log.debug("Running HermiT");

            try {
                assertTrue(this.theinstance.isConsistent(testOntology) == expected);
            } catch (ReasoningServiceException e) {
                log.error("Error while testing the isConsistent method. Message was: {}",
                    e.getLocalizedMessage());
                assertTrue(false);
            }

            // We want to remove the ontology from the manager
            manager.removeOntology(testOntology);
        } catch (OWLOntologyCreationException e) {
            log.error("An {} have been thrown while creating the input ontology for test", e.getClass());
            assertTrue(false);
        }
    }

    @Test
    public void testClassifyWithRules() {
        testClassifyWithRules(TestData.TEST_3_NS, TestData.TEST_3_rules_NS, TestData.TEST_3_expected_NS);
    }

    @Test
    public void testEnrichWithRules() {
        testEnrichWithRules(TestData.TEST_3_NS, TestData.TEST_3_rules_NS, TestData.TEST_3_expected_NS);
    }

    private void testClassifyWithRules(String testID, String rulesID, String testExpectedID) {
        log.info("Testing the task CLASSIFY with rules");

        OWLOntologyManager manager = TestData.manager;

        // We prepare the input ontology
        try {
            OWLOntology testOntology = manager.createOntology();
            OWLOntologyID testOntologyID = testOntology.getOntologyID();
            log.debug("Created test ontology with ID: {}", testOntologyID);
            OWLImportsDeclaration importTest = TestData.factory.getOWLImportsDeclaration(IRI.create(testID));
            manager.applyChange(new AddImport(testOntology, importTest));

            Set<SWRLRule> rules = manager.getOntology(IRI.create(rulesID)).getAxioms(AxiomType.SWRL_RULE);

            // Maybe we want to see the list of rules
            if (log.isDebugEnabled()) {
                log.debug("List of {} rules: ", rules.size());
                TestUtils.debug(rules, log);
            }
            log.debug("We add the rules to the ontology");
            manager.addAxioms(manager.getOntology(testOntologyID), rules);

            // Maybe we want to see what is in before
            if (log.isDebugEnabled()) log.debug("Content of the input is:");
            TestUtils.debug(manager.getOntology(testOntologyID), log);

            // Now we test the method
            log.debug("Running HermiT");
            Set<OWLAxiom> inferred = this.theinstance.runTask(ReasoningService.Tasks.CLASSIFY,
                manager.getOntology(testOntologyID));

            // Maybe we want to see the inferred axiom list
            if (log.isDebugEnabled()) {
                log.debug("{} inferred axioms:", inferred.size());
                TestUtils.debug(inferred, log);
            }

            Set<OWLLogicalAxiom> expectedAxioms = manager.getOntology(IRI.create(testExpectedID))
                    .getLogicalAxioms();

            Set<OWLAxiom> missing = new HashSet<OWLAxiom>();
            for (OWLAxiom expected : expectedAxioms) {
                // We consider here only two kind of axioms
                if (expected instanceof OWLSubClassOfAxiom || expected instanceof OWLClassAssertionAxiom) {
                    if (!inferred.contains(expected)) {
                        log.error("missing expected axiom: {}", expected);
                        missing.add(expected);
                    }
                }
            }
            assertTrue(missing.isEmpty());

            // We want to remove the ontology from the manager
            manager.removeOntology(testOntology);
        } catch (OWLOntologyCreationException e) {
            log.error("An {} have been thrown while creating the input ontology for test", e.getClass());
            assertTrue(false);
        } catch (ReasoningServiceException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        }
    }

    private void testEnrichWithRules(String testID, String rulesID, String testExpectedID) {
        log.info("Testing the task ENRICH with rules");

        OWLOntologyManager manager = TestData.manager;

        // We prepare the input ontology
        try {
            OWLOntology testOntology = manager.createOntology();
            OWLOntologyID testOntologyID = testOntology.getOntologyID();
            log.debug("Created test ontology with ID: {}", testOntologyID);
            OWLImportsDeclaration importTest = TestData.factory.getOWLImportsDeclaration(IRI.create(testID));
            manager.applyChange(new AddImport(testOntology, importTest));

            Set<SWRLRule> rules = manager.getOntology(IRI.create(rulesID)).getAxioms(AxiomType.SWRL_RULE);

            // Maybe we want to see the list of rules
            if (log.isDebugEnabled()) {
                log.debug("List of {} rules: ", rules.size());
                TestUtils.debug(rules, log);
            }
            log.debug("We add the rules to the ontology");
            manager.addAxioms(manager.getOntology(testOntologyID), rules);

            // Maybe we want to see what is in before
            if (log.isDebugEnabled()) log.debug("Content of the input is:");
            TestUtils.debug(manager.getOntology(testOntologyID), log);

            // Now we test the method
            log.debug("Running HermiT");
            Set<OWLAxiom> inferred = this.theinstance.runTask(ReasoningService.Tasks.ENRICH,
                manager.getOntology(testOntologyID));

            // Maybe we want to see the inferred axiom list
            if (log.isDebugEnabled()) {
                log.debug("{} inferred axioms:", inferred.size());
                TestUtils.debug(inferred, log);
            }

            Set<OWLLogicalAxiom> expectedAxioms = manager.getOntology(IRI.create(testExpectedID))
                    .getLogicalAxioms();

            Set<OWLAxiom> missing = new HashSet<OWLAxiom>();
            for (OWLAxiom expected : expectedAxioms) {
                // We consider here all kind of axioms
                if (!inferred.contains(expected)) {
                    log.error("missing expected axiom: {}", expected);
                    missing.add(expected);
                }
            }
            assertTrue(missing.isEmpty());

            // We want to remove the ontology from the manager
            manager.removeOntology(testOntology);
        } catch (OWLOntologyCreationException e) {
            log.error("An {} have been thrown while creating the input ontology for test", e.getClass());
            assertTrue(false);
        } catch (ReasoningServiceException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (InconsistentInputException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        } catch (UnsupportedTaskException e) {
            log.error("An {} have been thrown while executing the reasoning", e.getClass());
            assertTrue(false);
        }
    }
}
