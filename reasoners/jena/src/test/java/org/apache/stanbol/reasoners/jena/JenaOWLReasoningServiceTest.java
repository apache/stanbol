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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.ValidityReport;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Tests specific for the JenaOWLReasoningService implementation
 */
public class JenaOWLReasoningServiceTest  {

	private JenaReasoningService reasoningService;
	
	private static final Logger log = LoggerFactory
			.getLogger(JenaOWLReasoningServiceTest.class);

	/**
	 * We prepare some static variables to be used in tests
	 */
	@Before
	public void setUp() {
		
		// The single instance
		reasoningService = new JenaOWLReasoningService();

	}

	/**
	 * Tests rdfs:subClassOf inference with OWLReasoner
	 */
	@Test
	public void testSubclassOf() {
		log.info("Testing rdfs:subClassOf inference with OWL reasoner");

		// Prepare data
		TestData.alexdma.addProperty(RDF.type, TestData.foaf_Person);

		// Setup input for the reasoner
		Model input = ModelFactory.createUnion(TestData.foaf, TestData.alexdma.getModel());
		
		// Am I a foaf:Agent?
		log.info("Instantiating the OWL reasoner");

		InfModel inferred = reasoningService.run(input);

		Statement isAgent = TestData.model
				.createStatement(TestData.alexdma, RDF.type, TestData.foaf_Agent);

//		log.info("Statements: {}",
//				TestUtils.printStatements(inferred, TestData.alexdma, RDF.type));
		log.info("Is any foaf:Person a foaf:Agent...? {}",
				inferred.contains(isAgent));
		assertTrue(inferred.contains(isAgent));

		// Reset resource to be clean for other tests
		TestData.alexdma.removeProperties();
	}

	/**
	 * Tests the owl:disjointWith inferences
	 */
	@Test
	public void testDisjointness() {
		log.info("Testing owl:disjointWith inferences with OWL reasoner");

		// We assure everything is clean
		TestData.alexdma.removeProperties();
		TestData.alexdma.addProperty(RDF.type, TestData.foaf_Organization);
		TestData.alexdma.addProperty(RDF.type, TestData.foaf_Person);

		// Setup input for the reasoner
		Model input = ModelFactory.createUnion(TestData.foaf, TestData.alexdma.getModel());

		// Can I be a foaf:Organization and a foaf:Person at the same time?
		log.info("Instantiating the OWL reasoner");

		// Run the reasoner
		InfModel inferred = reasoningService.run(input);
		
//		log.info("Statements: {}",
//				TestUtils.printStatements(inferred, TestData.alexdma, RDF.type));
		ValidityReport validity = inferred.validate();

		log.info(
				"Can I be a foaf:Organization and a foaf:Person at the same time...? {}",
				validity.isValid());
		assertTrue(!validity.isValid());
		// Clean shared resource
		TestData.alexdma.removeProperties();
	}

	public void isNotConsistent(){
		// Clean data
		TestData.alexdma.removeProperties();

		// Prepare data
		TestData.alexdma.addProperty(RDF.type, TestData.foaf_Person);
		TestData.alexdma.addProperty(RDF.type, TestData.foaf_Organization);
		
		// Setup input for the reasoner
		Model input = ModelFactory.createUnion(TestData.foaf, TestData.alexdma.getModel());

		try {
            assertFalse(reasoningService.isConsistent(input));
        } catch (ReasoningServiceException e) {
            log.error("Error thrown: {}",e);
        }

		// Clean data
		TestData.alexdma.removeProperties();
	}
}
