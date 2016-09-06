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

package org.apache.stanbol.enhancer.engines.entitycoreference;

import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_CONFIDENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.DCTERMS_LINGUISTIC_SYSTEM;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.coref.CorefFeature;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

/**
 * Main test class
 * 
 * @author Cristian Petroaca
 *
 */
public class EntityCoReferenceEngineTest {
	private static final String SPATIAL_SENTENCE_1 = "Angela Merkel visited China.";
	private static final String SPATIAL_SENTENCE_2 = "The German politician met the Chinese prime minister.";
	private static final String SPATIAL_TEXT = SPATIAL_SENTENCE_1 + SPATIAL_SENTENCE_2;

	private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
	private static final AnalysedTextFactory atFactory = AnalysedTextFactory.getDefaultInstance();

	private EntityCoReferenceEngine engine;

	@Before
	public void setUpServices() throws IOException, ConfigurationException {
		engine = new EntityCoReferenceEngine();
		// we need to set some fields that would otherwise be injected by the
		// container
		engine.siteManager = new MockSiteManager();

		Dictionary<String, Object> config = new Hashtable<String, Object>();
		config.put(EnhancementEngine.PROPERTY_NAME, "entity-coreference");
		config.put(EntityCoReferenceEngine.CONFIG_LANGUAGES, "en");
		config.put(EntityCoReferenceEngine.REFERENCED_SITE_ID, MockEntityCorefDbpediaSite.SITE_ID);
		config.put(EntityCoReferenceEngine.MAX_DISTANCE, 1);
		config.put(EntityCoReferenceEngine.ENTITY_URI_BASE, "http://dbpedia.org/resource/");
		config.put(EntityCoReferenceEngine.SPATIAL_ATTR_FOR_PERSON, Constants.DEFAULT_SPATIAL_ATTR_FOR_PERSON);
		config.put(EntityCoReferenceEngine.SPATIAL_ATTR_FOR_ORGANIZATION,
				Constants.DEFAULT_SPATIAL_ATTR_FOR_ORGANIZATION);
		config.put(EntityCoReferenceEngine.SPATIAL_ATTR_FOR_PLACE, Constants.DEFAULT_SPATIAL_ATTR_FOR_PLACE);
		config.put(EntityCoReferenceEngine.ORG_ATTR_FOR_PERSON, Constants.DEFAULT_ORG_ATTR_FOR_PERSON);
		config.put(EntityCoReferenceEngine.ENTITY_CLASSES_TO_EXCLUDE, Constants.DEFAULT_ENTITY_CLASSES_TO_EXCLUDE);

		engine.activate(new MockComponentContext(config));
	}

	@Test
	public void testSpatialCoref() throws EngineException, IOException {
		ContentItem ci = ciFactory.createContentItem(new StringSource(SPATIAL_TEXT));
		Graph graph = ci.getMetadata();
		IRI textEnhancement = EnhancementEngineHelper.createTextEnhancement(ci, engine);
		graph.add(new TripleImpl(textEnhancement, DC_LANGUAGE, new PlainLiteralImpl("en")));
		graph.add(new TripleImpl(textEnhancement, ENHANCER_CONFIDENCE, new PlainLiteralImpl("100.0")));
		graph.add(new TripleImpl(textEnhancement, DC_TYPE, DCTERMS_LINGUISTIC_SYSTEM));

		Entry<IRI, Blob> textBlob = ContentItemHelper.getBlob(ci, Collections.singleton("text/plain"));
		AnalysedText at = atFactory.createAnalysedText(ci, textBlob.getValue());

		Sentence sentence1 = at.addSentence(0, SPATIAL_SENTENCE_1.indexOf(".") + 1);
		Chunk angelaMerkel = sentence1.addChunk(0, "Angela Merkel".length());
		angelaMerkel.addAnnotation(NlpAnnotations.NER_ANNOTATION,
				Value.value(new NerTag("Angela Merkel", OntologicalClasses.DBPEDIA_PERSON)));

		Sentence sentence2 = at.addSentence(SPATIAL_SENTENCE_1.indexOf(".") + 1,
				SPATIAL_SENTENCE_1.length() + SPATIAL_SENTENCE_2.indexOf(".") + 1);
		int theStartIdx = sentence2.getSpan().indexOf("The");
		int germanStartIdx = sentence2.getSpan().indexOf("German");
		int chancellorStartIdx = sentence2.getSpan().indexOf("politician");
		Token the = sentence2.addToken(theStartIdx, theStartIdx + "The".length());
		the.addAnnotation(NlpAnnotations.POS_ANNOTATION,
				Value.value(new PosTag("The", LexicalCategory.PronounOrDeterminer, Pos.Determiner)));

		Token german = sentence2.addToken(germanStartIdx, germanStartIdx + "German".length());
		german.addAnnotation(NlpAnnotations.POS_ANNOTATION,
				Value.value(new PosTag("German", LexicalCategory.Adjective)));

		Token politician = sentence2.addToken(chancellorStartIdx, chancellorStartIdx + "politician".length());
		politician.addAnnotation(NlpAnnotations.POS_ANNOTATION,
				Value.value(new PosTag("politician", LexicalCategory.Noun)));

		Chunk theGermanChancellor = sentence2.addChunk(theStartIdx, chancellorStartIdx + "politician".length());
		theGermanChancellor.addAnnotation(NlpAnnotations.PHRASE_ANNOTATION,
				Value.value(new PhraseTag("The German politician", LexicalCategory.Noun)));

		engine.computeEnhancements(ci);

		Value<CorefFeature> representativeCorefValue = angelaMerkel.getAnnotation(NlpAnnotations.COREF_ANNOTATION);
		Assert.assertNotNull(representativeCorefValue);
		CorefFeature representativeCoref = representativeCorefValue.value();
		Assert.assertTrue(representativeCoref.isRepresentative());
		Assert.assertTrue(representativeCoref.getMentions().contains(theGermanChancellor));

		Value<CorefFeature> subordinateCorefValue = theGermanChancellor.getAnnotation(NlpAnnotations.COREF_ANNOTATION);
		Assert.assertNotNull(subordinateCorefValue);
		CorefFeature subordinateCoref = subordinateCorefValue.value();
		Assert.assertTrue(!subordinateCoref.isRepresentative());
		Assert.assertTrue(subordinateCoref.getMentions().contains(angelaMerkel));
	}
}
