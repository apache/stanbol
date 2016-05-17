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
package org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl;

import static org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.CeliLemmatizerEnhancementEngine.MORPHOLOGICAL_ANALYSIS;
import static org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.CeliLemmatizerEnhancementEngine.SERVICE_URL;
import static org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.CeliLemmatizerEnhancementEngine.hasLemmaForm;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateAllEntityAnnotations;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateEnhancement;
import static org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper.validateTextAnnotation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.CeliMorphoFeatures;
import org.apache.stanbol.enhancer.engines.celi.testutils.MockComponentContext;
import org.apache.stanbol.enhancer.engines.celi.testutils.TestUtils;
import org.apache.stanbol.enhancer.nlp.morpho.Gender;
import org.apache.stanbol.enhancer.nlp.morpho.NumberFeature;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CeliLemmatizerEnhancementEngineTest {
	
	static final String OLIA_NAMESPACE = "http://purl.org/olia/olia.owl#";

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();

    private static final Logger log = LoggerFactory.getLogger(CeliLemmatizerEnhancementEngine.class);
	private static final String TEXT = "Torino è la principale città del Piemonte.";
	private static final String TERM = "casa";

	private CeliLemmatizerEnhancementEngine initEngine(boolean completeMorphoAnalysis) throws IOException, ConfigurationException {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EnhancementEngine.PROPERTY_NAME, "celiLemmatizer");
        properties.put(CeliConstants.CELI_TEST_ACCOUNT, "true");
	    properties.put(SERVICE_URL, "http://linguagrid.org/LSGrid/ws/morpho-analyser");
        properties.put(CeliConstants.CELI_CONNECTION_TIMEOUT, "5");
	    properties.put(MORPHOLOGICAL_ANALYSIS, completeMorphoAnalysis);
		MockComponentContext context = new MockComponentContext(properties);
		CeliLemmatizerEnhancementEngine morphoAnalysisEngine = new CeliLemmatizerEnhancementEngine();
		morphoAnalysisEngine.activate(context);
		return morphoAnalysisEngine;
	}

	private static void shutdownEngine(CeliLemmatizerEnhancementEngine morphoAnalysisEngine) {
		morphoAnalysisEngine.deactivate(null);
	}

    private static ContentItem wrapAsContentItem(final String text) throws IOException {
        return ciFactory.createContentItem(new StringSource(text));
    }

    
	@Test
	public void testEngine() throws Exception {
		ContentItem ci = wrapAsContentItem(TEXT);
		
        //add a simple triple to statically define the language of the test
        //content
        ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl("it")));
        //unit test should not depend on each other (if possible)
        //CeliLanguageIdentifierEnhancementEngineTest.addEnanchements(ci);
        CeliLemmatizerEnhancementEngine morphoAnalysisEngine = initEngine(false);
        try {
			morphoAnalysisEngine.computeEnhancements(ci);
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return;
        }

		TestUtils.logEnhancements(ci);
		//validate enhancement
        HashMap<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
            morphoAnalysisEngine.getClass().getName()));
        Iterator<Triple> lemmaTextAnnotationIterator = ci.getMetadata().filter(null, RDF_TYPE, ENHANCER_TEXTANNOTATION);
        assertTrue("A TextAnnotation is expected by this Test", lemmaTextAnnotationIterator.hasNext());
        BlankNodeOrIRI lemmaTextAnnotation = lemmaTextAnnotationIterator.next().getSubject();
        assertTrue("TextAnnoations MUST BE IRIs!",lemmaTextAnnotation instanceof IRI);
        assertFalse("Only a single TextAnnotation is expected by this Test", lemmaTextAnnotationIterator.hasNext());
        //validate the enhancement metadata
        validateEnhancement(ci.getMetadata(), (IRI)lemmaTextAnnotation, expectedValues);
        //validate the lemma form TextAnnotation
        int lemmaForms = validateLemmaFormProperty(ci.getMetadata(), lemmaTextAnnotation,"it");
        assertTrue("Only a single LemmaForm property is expected if '"+ MORPHOLOGICAL_ANALYSIS+"=false'",lemmaForms == 1);
        shutdownEngine(morphoAnalysisEngine);
	}


	
    @Test
    public void testCompleteMorphoAnalysis() throws Exception {
        ContentItem ci = wrapAsContentItem(TERM);
        //add a simple triple to statically define the language of the test
        //content
        ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl("it")));

        CeliLemmatizerEnhancementEngine morphoAnalysisEngine = initEngine(true);
        try {
            morphoAnalysisEngine.computeEnhancements(ci);
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return;
        }

        TestUtils.logEnhancements(ci);
        //validate enhancements
        HashMap<IRI,RDFTerm> expectedValues = new HashMap<IRI,RDFTerm>();
        expectedValues.put(Properties.ENHANCER_EXTRACTED_FROM, ci.getUri());
        expectedValues.put(Properties.DC_CREATOR, LiteralFactory.getInstance().createTypedLiteral(
            morphoAnalysisEngine.getClass().getName()));

        Iterator<Triple> textAnnotationIterator = ci.getMetadata().filter(null,
        RDF_TYPE, ENHANCER_TEXTANNOTATION);
        // test if a textAnnotation is present
        //assertTrue(textAnnotationIterator.hasNext()); 
        //  -> this might be used to test that there are no TextAnnotations
        int textAnnotationCount = 0;
        while (textAnnotationIterator.hasNext()) {
            IRI textAnnotation = (IRI) textAnnotationIterator.next().getSubject();
            // test if selected Text is added
            validateTextAnnotation(ci.getMetadata(), textAnnotation,TERM,expectedValues);
            textAnnotationCount++;
            //perform additional tests for "hasMorphologicalFeature" and "hasLemmaForm"
            validateMorphoFeatureProperty(ci.getMetadata(),textAnnotation);
        }
        log.info("{} TextAnnotations found and validated ...",textAnnotationCount);
        int entityAnnoNum = validateAllEntityAnnotations(ci.getMetadata(), expectedValues);
        //no EntityAnnotations expected
        Assert.assertEquals("No EntityAnnotations expected by this test", 0, entityAnnoNum);        shutdownEngine(morphoAnalysisEngine);
    }

    /**
     * [1..*] values of an {@link PlainLiteral} in the same language as the
     * analyzed text
     * @param enhancements The graph with the enhancements
     * @param textAnnotation the TextAnnotation to check
     * @param lang the language of the analyzed text
     * @return The number of lemma forms found
     */
    private int validateLemmaFormProperty(Graph enhancements, BlankNodeOrIRI textAnnotation, String lang) {
        Iterator<Triple> lemmaFormsIterator = enhancements.filter(textAnnotation, hasLemmaForm, null);
        assertTrue("No lemma form value found for TextAnnotation "+textAnnotation+"!", lemmaFormsIterator.hasNext());
        int lemmaFormCount = 0;
        while(lemmaFormsIterator.hasNext()){
            lemmaFormCount++;
            RDFTerm lemmaForms = lemmaFormsIterator.next().getObject();
            assertTrue("Lemma Forms value are expected of type Literal", lemmaForms instanceof Literal);
            assertFalse("Lemma forms MUST NOT be empty",((Literal)lemmaForms).getLexicalForm().isEmpty());
            assertNotNull("Language of the Lemma Form literal MUST BE not null",((Literal)lemmaForms).getLanguage());
            assertEquals("Language of the Lemma Form literal MUST BE the same as for the parsed text",
                lang, ((Literal)lemmaForms).getLanguage().toString());
        }
        return lemmaFormCount;
    }
    /**
     * [1..*] values of an {@link TypedLiteral} in the form {key=value}
     * @param enhancements The graph with the enhancements
     * @param textAnnotation the TextAnnotation to check
     */
    private void validateMorphoFeatureProperty(Graph enhancements, BlankNodeOrIRI textAnnotation) {
    	//This taste checks for known morpho features of a given input (constant TERM)
        Iterator<Triple> morphoFeatureIterator = enhancements.filter(textAnnotation, RDF_TYPE, null);
        assertTrue("No POS Morpho Feature value found for TextAnnotation "+textAnnotation+"!", morphoFeatureIterator.hasNext());
        while(morphoFeatureIterator.hasNext()){
            RDFTerm morphoFeature = morphoFeatureIterator.next().getObject();
            assertTrue("Morpho Feature value are expected of typed literal", morphoFeature instanceof IRI);
            String feature=((IRI)morphoFeature).getUnicodeString();
            assertFalse("Morpho Feature MUST NOT be empty",feature.isEmpty());
            if(feature.startsWith(OLIA_NAMESPACE)){
            	String key=feature.substring(OLIA_NAMESPACE.length());
            	LexicalCategory cat=LexicalCategory.valueOf(key);
            	assertTrue("Part of Speech of "+TERM+" should be "+LexicalCategory.Noun , (cat==LexicalCategory.Noun));
            }
        }
        morphoFeatureIterator = enhancements.filter(textAnnotation, CeliMorphoFeatures.HAS_GENDER, null);
        assertTrue("No Gender Morpho Feature value found for TextAnnotation "+textAnnotation+"!", morphoFeatureIterator.hasNext());
        if(morphoFeatureIterator.hasNext()){
            RDFTerm morphoFeature = morphoFeatureIterator.next().getObject();
            assertTrue("Morpho Feature value are expected of typed literal", morphoFeature instanceof IRI);
            String feature=((IRI)morphoFeature).getUnicodeString();
            assertFalse("Morpho Feature MUST NOT be empty",feature.isEmpty());
            if(feature.startsWith(OLIA_NAMESPACE)){
            	String key=feature.substring(OLIA_NAMESPACE.length());
            	Gender cat=Gender.valueOf(key);
            	assertTrue("Gender of "+TERM+" should be "+Gender.Feminine , (cat==Gender.Feminine));
            }
        }
        morphoFeatureIterator = enhancements.filter(textAnnotation, CeliMorphoFeatures.HAS_NUMBER, null);
        assertTrue("No Number Morpho Feature value found for TextAnnotation "+textAnnotation+"!", morphoFeatureIterator.hasNext());
        if(morphoFeatureIterator.hasNext()){
            RDFTerm morphoFeature = morphoFeatureIterator.next().getObject();
            assertTrue("Morpho Feature value are expected of typed literal", morphoFeature instanceof IRI);
            String feature=((IRI)morphoFeature).getUnicodeString();
            assertFalse("Morpho Feature MUST NOT be empty",feature.isEmpty());
            if(feature.startsWith(OLIA_NAMESPACE)){
            	String key=feature.substring(OLIA_NAMESPACE.length());
            	NumberFeature cat=NumberFeature.valueOf(key);
            	assertTrue("Number of "+TERM+" should be "+Gender.Feminine , (cat==NumberFeature.Singular));
            }
        }
        morphoFeatureIterator = enhancements.filter(textAnnotation, CeliLemmatizerEnhancementEngine.hasLemmaForm, null);
        assertTrue("No Number Morpho Feature value found for TextAnnotation "+textAnnotation+"!", morphoFeatureIterator.hasNext());
        if(morphoFeatureIterator.hasNext()){
            RDFTerm morphoFeature = morphoFeatureIterator.next().getObject();
            assertTrue("Lemma Forms value are expected of type Literal", morphoFeature instanceof Literal);
            assertFalse("Lemma forms MUST NOT be empty",((Literal)morphoFeature).getLexicalForm().isEmpty());
            String feature=((Literal)morphoFeature).getLexicalForm();
            assertTrue("Lemma of "+TERM+" should be "+TERM , (feature.equals(TERM)));
        }
        
    }
}
