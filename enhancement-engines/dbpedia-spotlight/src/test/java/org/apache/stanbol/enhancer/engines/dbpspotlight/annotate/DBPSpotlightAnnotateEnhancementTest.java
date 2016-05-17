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
package org.apache.stanbol.enhancer.engines.dbpspotlight.annotate;

import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.ENHANCE_ASYNC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.dbpspotlight.Constants;
import org.apache.stanbol.enhancer.engines.dbpspotlight.TestDefaults;
import org.apache.stanbol.enhancer.engines.dbpspotlight.model.Annotation;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a JUnit test for DBpedia Spotlight Annotate
 * EnhancementEngine.
 * 
 * @author Iavor Jelev, babelmonkeys / GzEvD
 */
public class DBPSpotlightAnnotateEnhancementTest implements TestDefaults{

    
	/**
	 * This contains the logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(DBPSpotlightAnnotateEnhancementTest.class);
	private static String SPL_URL = System
			.getProperty(Constants.PARAM_URL_KEY) == null ? DEFAULT_SPL_URL
			: System.getProperty(Constants.PARAM_URL_KEY);
	private static String TEST_TEXT = "President Obama is meeting Angela Merkel in Berlin on Monday";
	private static DBPSpotlightAnnotateEnhancementEngine dbpslight;

	private static ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
	
	private ContentItem ci;
	private static Entry<IRI, Blob> textContentPart;

	@BeforeClass
	public static void oneTimeSetup() throws Exception {
	    Assume.assumeNotNull(SPL_URL);
		dbpslight = new DBPSpotlightAnnotateEnhancementEngine(new URL(SPL_URL + "/annotate"),10);
	}
	@Before
	public void initTest() throws IOException {
		//create the contentItem for testing
		ci = ciFactory.createContentItem(new StringSource(TEST_TEXT));
		assertNotNull(ci);
		textContentPart = ContentItemHelper.getBlob(ci, Collections.singleton("text/plain"));
		assertNotNull(textContentPart);
		//add the language of the text
		ci.getMetadata().add(new TripleImpl(ci.getUri(), Properties.DC_LANGUAGE, 
				new PlainLiteralImpl("en")));
		assertEquals("en", EnhancementEngineHelper.getLanguage(ci));
	}

	@Test
	public void testEntityExtraction() throws EngineException{
		Collection<Annotation> entities;
		try {
			entities = dbpslight.doPostRequest(TEST_TEXT,ci.getUri());
		} catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return;
		}
        LOG.info("Found entities: {}", entities.size());
        LOG.debug("Entities:\n{}", entities);
        Assert.assertFalse("No entities were found!", entities.isEmpty());
	}
	@Test
	public void testCanEnhance() throws EngineException {
		assertEquals(ENHANCE_ASYNC, dbpslight.canEnhance(ci));
	}
	
	/**
	 * Validates the Enhancements created by this engine
	 * @throws EngineException
	 */
	@Test
	public void testEnhancement() throws EngineException {
	    try {
	        dbpslight.computeEnhancements(ci);
	    } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
        }
        HashMap<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
        		dbpslight.getClass().getName()));
        //validate fise:TextAnnotations
		EnhancementStructureHelper.validateAllTextAnnotations(
				ci.getMetadata(), TEST_TEXT, expectedValues);
		//validate fise:EntityAnnotations
		EnhancementStructureHelper.validateAllEntityAnnotations(
				ci.getMetadata(), expectedValues);
	}
}
