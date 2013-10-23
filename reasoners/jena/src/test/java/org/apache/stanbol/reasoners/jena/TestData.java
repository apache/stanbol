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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * We share this data between all the tests. Note: take care, do not change the
 * content of the data, for example clean the 'resource' object after you change it:
 * resource.removeProperties();
 */
public class TestData {

	private static final Logger log = LoggerFactory.getLogger(TestData.class);

	/**
	 * Constants (local resources)
	 */
	static final String LOCAL_RESOURCE_FOAF = "foaf.rdf";
	static final String LOCAL_RESOURCE_GENERIC_RDFS_RULES = "generic.rdfs.rules";
	/**
	 * Constants
	 */
	static final String TEST_NS = "http://www.example.org/";
	static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
	static final String FOAF_firstName = FOAF_NS + "firstName";
	static final String FOAF_familyName = FOAF_NS + "familyName";
	static final String FOAF_name = FOAF_NS + "name";
	static final String FOAF_knows = FOAF_NS + "knows";
	static final String FOAF_Agent = FOAF_NS + "Agent";
	static final String FOAF_Person = FOAF_NS + "Person";
	static final String FOAF_Organization = FOAF_NS + "Organization";
	static final String FOAF_workplaceHomepage = FOAF_NS + "workplaceHomepage";

	/**
	 * Data
	 */
	static Model model; // Just a default model for utilities
	static Model foaf; // The foaf ontology
	static Resource alexdma; // A simple resource as subject for
	static Resource enridaga; // A simple resource as subject for
	// tests
	static Resource foaf_Person; // the foaf:Person class
	static Resource foaf_Agent; // the foaf:Agent class
	static Resource foaf_Organization; // the foaf:Organization class
	static Property foaf_firstname;
	static Property foaf_knows;
	static Property foaf_workplaceHomepage;

	static {
		TestData.model = ModelFactory.createDefaultModel();

		TestData.alexdma = TestData.model.createResource(TestData.TEST_NS
				+ "alexdma");
		TestData.enridaga = TestData.model.createResource(TestData.TEST_NS
				+ "enridaga");
		TestData.foaf_Agent = TestData.model
				.createResource(TestData.FOAF_Agent);
		TestData.foaf_Person = TestData.model
				.createResource(TestData.FOAF_Person);
		TestData.foaf_Organization = TestData.model
				.createResource(TestData.FOAF_Organization);
		TestData.foaf_firstname = TestData.model
				.createProperty(TestData.FOAF_firstName);
		TestData.foaf_knows = TestData.model
				.createProperty(TestData.FOAF_knows);
		TestData.foaf_workplaceHomepage = TestData.model
				.createProperty(TestData.FOAF_workplaceHomepage);

		TestData.foaf = ModelFactory.createDefaultModel();

		log.info(
				"Loading FOAF ontology from {}",
				TestData.class.getResource("/"+LOCAL_RESOURCE_FOAF));
		TestData.foaf.read(TestData.class.getResource("/"+LOCAL_RESOURCE_FOAF)
				.toString());
	}
}
