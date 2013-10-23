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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * Tests specific for the JenaRDFSReasoningService implementation
 * 
 */
public class JenaRDFSReasoningServiceTest {

	private JenaReasoningService reasoningService;

	private static final Logger log = LoggerFactory
			.getLogger(JenaRDFSReasoningServiceTest.class);

	/**
	 * We prepare some static variables to be used in tests
	 */
	@Before
	public void setUp() {

		// The single instance
		reasoningService = new JenaRDFSReasoningService();

	}

	/**
	 * Tests rdfs:subClassOf inference with RDFSReasoner
	 */
	@Test
	public void testRDFSSubclassOf() {
		log.info("Testing rdfs:subClassOf inference with RDFS reasoner");

		// Prepare data
		TestData.alexdma.addProperty(RDF.type, TestData.foaf_Person);

		// Setup input for the reasoner
		Model input = ModelFactory.createUnion(TestData.foaf,
				TestData.alexdma.getModel());

		// Is alexdma foaf:Agent?
		InfModel inferred = reasoningService.run(input);

		Statement isAgent = TestData.model.createStatement(TestData.alexdma,
				RDF.type, TestData.foaf_Agent);

//		log.info("Statements: {}",
//				TestUtils.printStatements(inferred, TestData.alexdma, RDF.type));
		log.info("Is any foaf:Person is also a foaf:Agent...(true)? {}",
				inferred.contains(isAgent));
		assertTrue(inferred.contains(isAgent));

		// Reset resource to be clean for other tests
		TestData.alexdma.removeProperties();
	}

	@Test
	public void testRDFSRange() {
		log.info("Testing rdfs:range inference with RDFS reasoner");

		// Prepare data
		TestData.alexdma.addProperty(TestData.foaf_knows, TestData.enridaga);

		// Setup input for the reasoner
		Model input = ModelFactory.createUnion(TestData.foaf,
				TestData.alexdma.getModel());

		// Is enridaga a foaf:Person?
		InfModel inferred = reasoningService.run(input);

		Statement isPerson = TestData.model.createStatement(TestData.enridaga,
				RDF.type, TestData.foaf_Person);

//		log.info("Statements: {}", TestUtils.printStatements(inferred,
//				TestData.enridaga, RDF.type));
		log.info("Is any rdfs:range of foaf:knows a foaf:Person...(true)? {}",
				inferred.contains(isPerson));

		assertTrue(inferred.contains(isPerson));

		// Reset resource to be clean for other tests
		TestData.alexdma.removeProperties();
	}

	@Test
	public void testRDFSDomain() {
		log.info("Testing rdfs:domain inference with RDFS reasoner");

		// Prepare data
		TestData.alexdma.addProperty(TestData.foaf_knows, TestData.enridaga);

		// Setup input for the reasoner
		Model input = ModelFactory.createUnion(TestData.foaf,
				TestData.alexdma.getModel());

		// Is alexdma a foaf:Person?
		InfModel inferred = reasoningService.run(input);

		Statement isPerson = TestData.model.createStatement(TestData.alexdma,
				RDF.type, TestData.foaf_Person);

//		log.info("Statements: {}", TestUtils.printStatements(inferred,
//				TestData.enridaga, RDF.type));
		log.info("Is any rdfs:domain of foaf:knows a foaf:Person...(true)? {}",
				inferred.contains(isPerson));

		assertTrue(inferred.contains(isPerson));

		// Reset resource to be clean for other tests
		TestData.alexdma.removeProperties();
	}

	@Test
	public void testRDFSSubPropertyOf() {
		log.info("Testing rdfs:subPropertyOf inference with RDFS reasoner");

		// We invent a property to extend foaf:knows
		Property collegueOf = TestData.model.createProperty(TestData.TEST_NS
				+ "collegueOf");
		collegueOf.addProperty(RDFS.subPropertyOf, TestData.foaf_knows);
		
		// Prepare data
		TestData.alexdma.addProperty(collegueOf, TestData.enridaga);

		// Setup input for the reasoner
		Model input = ModelFactory.createUnion(TestData.foaf,
				TestData.alexdma.getModel());

		// Does alexdma know enridaga?
		InfModel inferred = reasoningService.run(input);

		Statement knowsHim = TestData.model.createStatement(TestData.alexdma,
				TestData.foaf_knows, TestData.enridaga);

		// log.info("Statements: {}", TestUtils.printStatements(inferred,
		//		TestData.enridaga, RDF.type));
		log.info("Does alexdma foaf:knows enridaga?...(true)? {}",
				inferred.contains(knowsHim));

		assertTrue(inferred.contains(knowsHim));

		// Reset resource to be clean for other tests
		TestData.alexdma.removeProperties();
	}
}
