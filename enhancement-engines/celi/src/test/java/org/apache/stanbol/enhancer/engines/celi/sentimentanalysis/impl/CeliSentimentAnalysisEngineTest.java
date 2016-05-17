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
package org.apache.stanbol.enhancer.engines.celi.sentimentanalysis.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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

public class CeliSentimentAnalysisEngineTest {

	public static final String CELI_SENTIMENT_ANALYSIS_SERVICE_URL = "http://linguagrid.org/LSGrid/ws/sentiment-analysis";
	private static final Logger log = LoggerFactory.getLogger(CeliSentimentAnalysisEngine.class);

	static CeliSentimentAnalysisEngine sentimentAnalysisEngine = new CeliSentimentAnalysisEngine();

	private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();

	private static final String TEXT_it = "Io amo Torino ma odio le zanzare";
	private static final String TEXT_fr = "J'aime Turin mais je déteste les moustiques";

	@BeforeClass
	public static void setUpServices() throws IOException, ConfigurationException {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EnhancementEngine.PROPERTY_NAME, "celiSentimentAnalysis");
		properties.put(CeliConstants.CELI_TEST_ACCOUNT, "true");
		properties.put(CeliSentimentAnalysisEngine.SERVICE_URL, CELI_SENTIMENT_ANALYSIS_SERVICE_URL);
		properties.put(CeliSentimentAnalysisEngine.SUPPORTED_LANGUAGES, "fr;it");
        properties.put(CeliConstants.CELI_CONNECTION_TIMEOUT, "5");
		MockComponentContext context = new MockComponentContext(properties);
		sentimentAnalysisEngine.activate(context);
	}

	@AfterClass
	public static void shutdownServices() {
		sentimentAnalysisEngine.deactivate(null);
	}

	public static ContentItem wrapAsContentItem(final String text) throws IOException {
		return ciFactory.createContentItem(new StringSource(text));
	}

	private void testInput(String txt, String lang) throws EngineException, IOException {
		ContentItem ci = wrapAsContentItem(txt);
		try {
			// add a simple triple to statically define the language of the test content
			ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl(lang)));
			sentimentAnalysisEngine.computeEnhancements(ci);

			TestUtils.logEnhancements(ci);

			HashMap<IRI, RDFTerm> expectedValues = new HashMap<IRI, RDFTerm>();
			expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
			expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(sentimentAnalysisEngine.getClass().getName()));
			expectedValues.put(DC_TYPE, CeliConstants.SENTIMENT_EXPRESSION);
			int textAnnoNum = validateAllTextAnnotations(ci.getMetadata(), txt, expectedValues);
			log.info(textAnnoNum + " TextAnnotations found ...");
			assertTrue("2 sentiment expressions should be recognized in: "+txt,textAnnoNum==2);
			int entityAnnoNum = EnhancementStructureHelper.validateAllEntityAnnotations(ci.getMetadata(), expectedValues);
			assertTrue("0 entity annotations should be recognized in: "+txt,entityAnnoNum==0);
		} catch (EngineException e) {
			RemoteServiceHelper.checkServiceUnavailable(e);
		}
	}

	@Test
	public void tesetEngine() throws Exception {
		this.testInput(CeliSentimentAnalysisEngineTest.TEXT_it, "it");
		this.testInput(CeliSentimentAnalysisEngineTest.TEXT_fr, "fr");
	}
}
