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

import java.util.Set;

import org.apache.stanbol.reasoners.jena.filters.PropertyFilter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Statement;
/**
 * This class tests the PropertyFilter
 */
public class PropertyFilterTest {

	private static final Logger log = LoggerFactory
			.getLogger(PropertyFilterTest.class);

	@Test
	public void test() {
		PropertyFilter filter = new PropertyFilter(
				TestData.foaf_firstname);
		log.info("Testing the {} class", filter.getClass());
		Set<Statement> output = TestData.alexdma.getModel().listStatements()
				.filterKeep(filter).toSet();
		for(Statement statement : output){
			assertTrue(statement.getPredicate().equals(TestData.foaf_firstname));
		}
	}
}
