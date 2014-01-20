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

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.MissingImportEvent;
import org.semanticweb.owlapi.model.MissingImportListener;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We share this data between all the tests. Note: take care, do not leave the
 * content of these object changed, for example remove your example ontology
 * from the testDataManager
 * 
 */
public class TestData {

	private static final Logger log = LoggerFactory.getLogger(TestData.class);
	/**
	 * Constants (local resources)
	 */
	static final String LOCAL_RESOURCE_FOAF = "foaf.rdf";
	static URL FOAF_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_FOAF);
	static final String LOCAL_RESOURCE_DBPEDIA = "dbpedia_3.6.owl";
	static URL DBPEDIA_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_DBPEDIA);

	static final String LOCAL_RESOURCE_TEST_1 = "test_1.owl";
	static URL TEST_1_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_TEST_1);
	static final String LOCAL_RESOURCE_TEST_1_EXPECTED = "test_1_expected.owl";
	static URL TEST_1_expected_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_TEST_1_EXPECTED);
	
	static final String LOCAL_RESOURCE_TEST_2 = "test_2.owl";
	static URL TEST_2_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_TEST_2);
	static final String LOCAL_RESOURCE_TEST_2_EXPECTED = "test_2_expected.owl";
	static URL TEST_2_expected_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_TEST_2_EXPECTED);

	static final String LOCAL_RESOURCE_TEST_3 = "test_3.owl";
	static URL TEST_3_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_TEST_3);
	static final String LOCAL_RESOURCE_TEST_3_RULES = "test_3_rules.owl";
	static URL TEST_3_rules_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_TEST_3_RULES);
	static final String LOCAL_RESOURCE_TEST_3_EXPECTED = "test_3_expected.owl";
	static URL TEST_3_expected_LOCATION = TestData.class.getResource(System
			.getProperty("file.separator") + LOCAL_RESOURCE_TEST_3_EXPECTED);

	/**
	 * Constants
	 */
	// EXAMPLE
	static final String TEST_NS = "http://www.example.org/test/";
	static final String TEST_1_NS = TEST_NS + "1/";
	static final String TEST_1_expected_NS = TEST_1_NS + "expected/";
	static final String TEST_2_NS = TEST_NS +"2/";
	static final String TEST_2_expected_NS = TEST_2_NS + "expected/";
	static final String TEST_3_NS = TEST_NS +"3/";
	static final String TEST_3_rules_NS = TEST_3_NS + "rules/";
	static final String TEST_3_expected_NS = TEST_3_NS + "expected/";
	
	// FOAF
	static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";

	/**
	 * Manager, factory
	 */
	static final OWLOntologyManager manager = OWLManager
			.createOWLOntologyManager();
	static final OWLDataFactory factory = OWLManager
			.getOWLDataFactory();

	static {
		try {
			/**
			 * Configuration of the manager
			 */
			manager.setSilentMissingImportsHandling(true);
			manager
					.addMissingImportListener(new MissingImportListener() {
						@Override
						public void importMissing(MissingImportEvent event) {
							log.warn("Missing import! URI was: {}",
									event.getImportedOntologyURI());
						}
					});
			manager
					.addOntologyChangeListener(new OWLOntologyChangeListener() {
						@Override
						public void ontologiesChanged(
								List<? extends OWLOntologyChange> arg0)
								throws OWLException {
							for (OWLOntologyChange change : arg0) {
								log.debug("{} TO {}", change,
										change.getOntology());
							}
						}
					});
			/**
			 * Loading test ontologies once for all
			 */
			manager.loadOntologyFromOntologyDocument(FOAF_LOCATION
					.openStream());
			manager.loadOntologyFromOntologyDocument(DBPEDIA_LOCATION
					.openStream());
			// Test 1
			manager.loadOntologyFromOntologyDocument(TEST_1_LOCATION
					.openStream());
			manager.loadOntologyFromOntologyDocument(TEST_1_expected_LOCATION
					.openStream());
			// Test 2
			manager.loadOntologyFromOntologyDocument(TEST_2_LOCATION
					.openStream());
			manager.loadOntologyFromOntologyDocument(TEST_2_expected_LOCATION
					.openStream());
			// Test 3
			manager.loadOntologyFromOntologyDocument(TEST_3_LOCATION
					.openStream());
			manager.loadOntologyFromOntologyDocument(TEST_3_rules_LOCATION
					.openStream());
			manager.loadOntologyFromOntologyDocument(TEST_3_expected_LOCATION
					.openStream());
			
		} catch (OWLOntologyCreationException e) {
			log.error("A {} have been thrown while creating the ontology"
					+ ". Message was: {}", e.getClass(),
					e.getLocalizedMessage());
		} catch (IOException e) {
			log.error(
					"A {} have been thrown while loading the ontology from the location {}",
					e.getClass(), FOAF_LOCATION.toString());
			log.error("Message was: {}", e.getLocalizedMessage());
		}
	}
}
