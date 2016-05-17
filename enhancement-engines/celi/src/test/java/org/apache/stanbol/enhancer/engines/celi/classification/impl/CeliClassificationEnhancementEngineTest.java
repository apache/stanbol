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
package org.apache.stanbol.enhancer.engines.celi.classification.impl;

import static junit.framework.Assert.assertEquals;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTopicAnnotations;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.testutils.MockComponentContext;
import org.apache.stanbol.enhancer.engines.celi.testutils.TestUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CeliClassificationEnhancementEngineTest {
	
	static CeliClassificationEnhancementEngine classificationEngine = new CeliClassificationEnhancementEngine();

	private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
	
	private static final Logger log = LoggerFactory.getLogger(CeliClassificationEnhancementEngine.class);
	
	private static final String TEXT = "Brigitte Bardot, née  le 28 septembre " +
			"1934 à Paris, est une actrice de cinéma et chanteuse française.";

	@BeforeClass
	public static void setUpServices() throws IOException, ConfigurationException {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EnhancementEngine.PROPERTY_NAME, "celiClassification");
		properties.put(CeliConstants.CELI_TEST_ACCOUNT, "true");
		properties.put(CeliConstants.CELI_CONNECTION_TIMEOUT, "5");
	    properties.put(CeliClassificationEnhancementEngine.SERVICE_URL, "http://linguagrid.org/LSGrid/ws/dbpedia-classification");
	 	
		MockComponentContext context = new MockComponentContext(properties);
		classificationEngine.activate(context);
	}

	@AfterClass
	public static void shutdownServices() {
		classificationEngine.deactivate(null);
	}

	public static ContentItem wrapAsContentItem(final String text) throws IOException {
		return ciFactory.createContentItem(new StringSource(text));
	}

	@Test
	public void tesetEngine() throws Exception {
		ContentItem ci = wrapAsContentItem(TEXT);
		try {
	        //add a simple triple to statically define the language of the test
            //content
            ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl("fr")));
            //unit test should not depend on each other (if possible)
            //CeliLanguageIdentifierEnhancementEngineTest.addEnanchements(ci);
    			
			classificationEngine.computeEnhancements(ci);

	        TestUtils.logEnhancements(ci);
	         HashMap<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
	            expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
	            expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
	                classificationEngine.getClass().getName()));

			int textAnnoNum = EnhancementStructureHelper.validateAllTextAnnotations(ci.getMetadata(), TEXT,expectedValues);
			assertEquals("Only a single fise:TextAnnotation is expeted", 1, textAnnoNum);
			int numTopicAnnotations = validateAllTopicAnnotations(ci.getMetadata()  , expectedValues);
			assertTrue("No TpocisAnnotations found", numTopicAnnotations > 0);
		} catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return;
        }
	}

}
