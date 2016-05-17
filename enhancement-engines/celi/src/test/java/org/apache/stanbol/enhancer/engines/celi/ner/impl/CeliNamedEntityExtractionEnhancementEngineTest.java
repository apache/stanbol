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
package org.apache.stanbol.enhancer.engines.celi.ner.impl;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllTextAnnotations;

import java.io.IOException;
import java.net.UnknownHostException;
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
import org.apache.stanbol.enhancer.engines.celi.classification.impl.CeliClassificationEnhancementEngine;
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

public class CeliNamedEntityExtractionEnhancementEngineTest {

	static CeliNamedEntityExtractionEnhancementEngine nerEngine = new CeliNamedEntityExtractionEnhancementEngine();

	private static final Logger log = LoggerFactory.getLogger(CeliClassificationEnhancementEngine.class);
	
    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();

    private static final String TEXT_it = "Wolfgang Amadeus Mozart, nome di " +
    		"battesimo Joannes Chrysostomus Wolfgangus Theophilus Mozart " +
    		"(Salisburgo, 27 gennaio 1756 – Vienna, 5 dicembre 1791), è stato " +
    		"un compositore, pianista, organista e violinista.";
    private static final String TEXT_fr = "Brigitte Bardot, née  le 28 septembre " +
    		"1934 à Paris, est une actrice de cinéma et chanteuse française.";
    
    private static final String TEXT_fr2 = "Le premier ministre français a annoncé " +
    		"mardi 29 mai, dans un entretien à L’Express, que la baisse des rémunérations " +
    		"des grands patrons des entreprises publiques s’appliquerait aussi aux « " +
    		"contrats en cours ». « Je crois au patriotisme des dirigeants, qui peuvent " +
    		"comprendre que la crise suppose l’exemplarité des élites politiques et " +
    		"économiques », a-t-il argumenté. La rémunération du président de la " +
    		"République, du premier ministre et des ministres a du reste déjà été abaissée.";
    
	@BeforeClass
	public static void setUpServices() throws IOException, ConfigurationException {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EnhancementEngine.PROPERTY_NAME, "celiNer");
        properties.put(CeliConstants.CELI_TEST_ACCOUNT, "true");
	    properties.put(CeliNamedEntityExtractionEnhancementEngine.SERVICE_URL, "http://linguagrid.org/LSGrid/ws/com.celi-france.linguagrid.namedentityrecognition.v0u0.demo");
	    properties.put(CeliNamedEntityExtractionEnhancementEngine.SUPPORTED_LANGUAGES, "fr;it");
        properties.put(CeliConstants.CELI_CONNECTION_TIMEOUT, "5");
	    MockComponentContext context = new MockComponentContext(properties);
		nerEngine.activate(context);
	}

	@AfterClass
	public static void shutdownServices() {
		nerEngine.deactivate(null);
	}

    public static ContentItem wrapAsContentItem(final String text) throws IOException {
        return ciFactory.createContentItem(new StringSource(text));
    }
    
    private void testInput(String txt,String lang) throws EngineException, IOException{
    	ContentItem ci = wrapAsContentItem(txt);
		try {
		    //add a simple triple to statically define the language of the test content
		    ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl(lang)));
			nerEngine.computeEnhancements(ci);

			TestUtils.logEnhancements(ci);
			
			HashMap<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
			expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
			expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
			    nerEngine.getClass().getName()));
			int textAnnoNum = validateAllTextAnnotations(ci.getMetadata(), txt, expectedValues);
	        log.info(textAnnoNum + " TextAnnotations found ...");
	        int entityAnnoNum = EnhancementStructureHelper.validateAllEntityAnnotations(ci.getMetadata(),expectedValues);
	        log.info(entityAnnoNum + " EntityAnnotations found ...");
		} catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
		}
    }
    
	@Test
	public void tesetEngine() throws Exception {
		this.testInput(CeliNamedEntityExtractionEnhancementEngineTest.TEXT_it, "it");
		this.testInput(CeliNamedEntityExtractionEnhancementEngineTest.TEXT_fr, "fr");
		//fails again - deactivated as it only tests a server side bug and does
		//not directly test any thing related to this engine implementation
		//this.testInput(CeliNamedEntityExtractionEnhancementEngineTest.TEXT_fr2, "fr");
	}

//	private int checkAllEntityAnnotations(Graph g) {
//		Iterator<Triple> entityAnnotationIterator = g.filter(null, RDF_TYPE, ENHANCER_ENTITYANNOTATION);
//		int entityAnnotationCount = 0;
//		while (entityAnnotationIterator.hasNext()) {
//			IRI entityAnnotation = (IRI) entityAnnotationIterator.next().getSubject();
//			entityAnnotationCount++;
//		}
//		return entityAnnotationCount;
//	}
//
//	private int checkAllTextAnnotations(Graph g, String content) {
//		Iterator<Triple> textAnnotationIterator = g.filter(null, RDF_TYPE, ENHANCER_TEXTANNOTATION);
//		// test if a textAnnotation is present
//		assertTrue(textAnnotationIterator.hasNext());
//		int textAnnotationCount = 0;
//		while (textAnnotationIterator.hasNext()) {
//			IRI textAnnotation = (IRI) textAnnotationIterator.next().getSubject();
//			textAnnotationCount++;
//		}
//		return textAnnotationCount;
//	}

}
