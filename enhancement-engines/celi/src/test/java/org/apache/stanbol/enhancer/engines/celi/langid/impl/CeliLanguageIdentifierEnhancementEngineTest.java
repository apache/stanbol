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
package org.apache.stanbol.enhancer.engines.celi.langid.impl;

import static org.junit.Assert.assertEquals;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllEntityAnnotations;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.testutils.MockComponentContext;
import org.apache.stanbol.enhancer.engines.celi.testutils.TestUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

public class CeliLanguageIdentifierEnhancementEngineTest {
	
    public static final String CELI_LANGID_SERVICE_URL = "http://linguagrid.org/LSGrid/ws/language-identifier";
    
    static CeliLanguageIdentifierEnhancementEngine langIdentifier = new CeliLanguageIdentifierEnhancementEngine();

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
	
	private static final String TEXT = "Brigitte Bardot, née  le 28 septembre 1934 à Paris, est une actrice de cinéma et chanteuse française.";

	@BeforeClass
	public static void setUpServices() throws IOException, ConfigurationException {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EnhancementEngine.PROPERTY_NAME, "celiLangIdentifier");
        properties.put(CeliConstants.CELI_TEST_ACCOUNT, "true");
        properties.put(CeliConstants.CELI_CONNECTION_TIMEOUT, "5");
	    properties.put(CeliLanguageIdentifierEnhancementEngine.SERVICE_URL, CELI_LANGID_SERVICE_URL);
	    
		MockComponentContext context = new MockComponentContext(properties);
		langIdentifier.activate(context);
	}

	@AfterClass
	public static void shutdownServices() {
		langIdentifier.deactivate(null);
	}

	public static ContentItem wrapAsContentItem(final String text) throws IOException {
		return ciFactory.createContentItem(new StringSource(text));
	}

	@Test
	public void tesetEngine() throws Exception {
		ContentItem ci = wrapAsContentItem(TEXT);
		try {
			langIdentifier.computeEnhancements(ci);

	        TestUtils.logEnhancements(ci);
			
			HashMap<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
	        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
	        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
	            langIdentifier.getClass().getName()));
	        int numTextAnnotations = validateAllTextAnnotations(ci.getMetadata(), TEXT, expectedValues);
	        assertEquals("A single TextAnnotation is expected by this Test", 1,numTextAnnotations);
	        //even through this tests do not validate service quality but rather
	        //the correct integration of the CELI service as EnhancementEngine
	        //we expect the "fr" is detected for the parsed text
	        assertEquals("The detected language for text '"+TEXT+"' MUST BE 'fr'","fr",EnhancementEngineHelper.getLanguage(ci));

	        int entityAnnoNum = validateAllEntityAnnotations(ci.getMetadata(), expectedValues);
	        assertEquals("No EntityAnnotations are expected",0, entityAnnoNum);
		} catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
		}
	}

}
