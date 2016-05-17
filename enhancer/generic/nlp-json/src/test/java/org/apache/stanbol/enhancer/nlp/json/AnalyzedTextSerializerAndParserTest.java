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
package org.apache.stanbol.enhancer.nlp.json;

import static org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses.DBPEDIA_ORGANISATION;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.SpanTypeEnum;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.morpho.Case;
import org.apache.stanbol.enhancer.nlp.morpho.CaseTag;
import org.apache.stanbol.enhancer.nlp.morpho.Definitness;
import org.apache.stanbol.enhancer.nlp.morpho.Gender;
import org.apache.stanbol.enhancer.nlp.morpho.GenderTag;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.morpho.NumberFeature;
import org.apache.stanbol.enhancer.nlp.morpho.NumberTag;
import org.apache.stanbol.enhancer.nlp.morpho.Person;
import org.apache.stanbol.enhancer.nlp.morpho.Tense;
import org.apache.stanbol.enhancer.nlp.morpho.TenseTag;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMood;
import org.apache.stanbol.enhancer.nlp.morpho.VerbMoodTag;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.phrase.PhraseTag;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.Pos;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzedTextSerializerAndParserTest {

    private static Logger log = LoggerFactory.getLogger(AnalyzedTextSerializerAndParserTest.class);

    public static final String text = "The Stanbol enhancer can detect famous " +
            "cities such as Paris and people such as Bob Marley.";
    
    public static final Annotation<Number> testAnnotation = 
            new Annotation<Number>("test", Number.class);
    
    /* -----
     * Test data creates within the BeforeClass
     * -----
     */
    /**
     * AnalysedText instance filled in {@link #setup()} with test dats
     */
    private static AnalysedText analysedTextWithData;
    private static LinkedHashMap<Sentence,String> expectedSentences = new LinkedHashMap<Sentence,String>();
    private static LinkedHashMap<Chunk,String> expectedChunks = new LinkedHashMap<Chunk,String>();
    private static LinkedHashMap<Token,String> expectedTokens = new LinkedHashMap<Token,String>();
    
    /* -----
     * Test data creates before every single test
     * -----
     */
    /**
     * Empty AnalysedText instance created before each test
     */
    private static AnalysedText at;

    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static final AnalysedTextFactory atFactory = AnalysedTextFactory.getDefaultInstance();
    
    private static ContentItem ci;

    private static Entry<IRI,Blob> textBlob;
    
    @BeforeClass
    public static final void setup() throws IOException {
        ci = ciFactory.createContentItem(new StringSource(text));
        textBlob = ContentItemHelper.getBlob(ci, Collections.singleton("text/plain"));
        analysedTextWithData = createAnalysedText();
        int sentence = text.indexOf('.')+1;
        Sentence sent1 = analysedTextWithData.addSentence(0, sentence);
        expectedSentences.put(sent1, "The Stanbol enhancer can detect famous " +
            "cities such as Paris and people such as Bob Marley.");
        
        Token the = sent1.addToken(0, 3);
        expectedTokens.put(the, "The");
        the.addAnnotation(NlpAnnotations.POS_ANNOTATION, Value.value(
            new PosTag("PREP",Pos.Preposition), 0.85));
        
        Token stanbol = sent1.addToken(4,11);
        expectedTokens.put(stanbol, "Stanbol");
        stanbol.addAnnotation(NlpAnnotations.POS_ANNOTATION, Value.value(
            new PosTag("PN", Pos.ProperNoun),0.95));
        stanbol.addAnnotation(NlpAnnotations.SENTIMENT_ANNOTATION, Value.value(
            0.5));
        
        //use index to create Tokens
        int enhancerStart = sent1.getSpan().indexOf("enhancer");
        Token enhancer = sent1.addToken(enhancerStart,enhancerStart+"enhancer".length());
        expectedTokens.put(enhancer, "enhancer");
        enhancer.addAnnotation(NlpAnnotations.POS_ANNOTATION, Value.value(
            new PosTag("PN", Pos.ProperNoun),0.95));
        enhancer.addAnnotation(NlpAnnotations.POS_ANNOTATION, Value.value(
            new PosTag("N", LexicalCategory.Noun),0.87));
        MorphoFeatures morpho = new MorphoFeatures("enhance");
        morpho.addCase(new CaseTag("test-case-1",Case.Comitative));
        morpho.addCase(new CaseTag("test-case-2",Case.Abessive));
        morpho.addDefinitness(Definitness.Definite);
        morpho.addPerson(Person.First);
        morpho.addPos(new PosTag("PN", Pos.ProperNoun));
        morpho.addGender(new GenderTag("test-gender", Gender.Masculine));
        morpho.addNumber(new NumberTag("test-number", NumberFeature.Plural));
        morpho.addTense(new TenseTag("test-tense", Tense.Present));
        morpho.addVerbForm(new VerbMoodTag("test-verb-mood", VerbMood.ConditionalVerb));
        enhancer.addAnnotation(NlpAnnotations.MORPHO_ANNOTATION, Value.value(morpho));

        //create a chunk
        Chunk stanbolEnhancer = analysedTextWithData.addChunk(stanbol.getStart(), enhancer.getEnd());
        expectedChunks.put(stanbolEnhancer, "Stanbol enhancer");
        stanbolEnhancer.addAnnotation(NlpAnnotations.NER_ANNOTATION, Value.value(
            new NerTag("organization", DBPEDIA_ORGANISATION)));
        stanbolEnhancer.addAnnotation(NlpAnnotations.PHRASE_ANNOTATION, Value.value(
            new PhraseTag("NP", LexicalCategory.Noun),0.98));

    }
    @Before
    public void initAnalysedText() throws Exception {
        at = createAnalysedText();
    }
    /**
     * @throws IOException
     */
    private static AnalysedText createAnalysedText() throws IOException {
        return  atFactory.createAnalysedText(textBlob.getValue());
    }
    
    @Test
    public void testSerialization() throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        AnalyzedTextSerializer serializer = AnalyzedTextSerializer.getDefaultInstance();
        serializer.serialize(analysedTextWithData, bout, null);
        //get the serialized String and check for some expected elements
        byte[] data = bout.toByteArray();
        String serialized = new String(data,Charset.forName("UTF-8"));
        log.info(serialized);
        Assert.assertTrue(serialized.contains("\"spans\" : [ {"));
        Assert.assertTrue(serialized.contains("\"type\" : \"Text\""));
        Assert.assertTrue(serialized.contains("\"type\" : \"Sentence\""));
        Assert.assertTrue(serialized.contains("\"type\" : \"Token\""));
        Assert.assertTrue(serialized.contains("\"stanbol.enhancer.nlp.pos\" : {"));
        Assert.assertTrue(serialized.contains("\"class\" : \"org.apache.stanbol.enhancer.nlp.pos.PosTag\""));
        Assert.assertTrue(serialized.contains("\"stanbol.enhancer.nlp.ner\" : {"));
        Assert.assertTrue(serialized.contains("\"class\" : \"org.apache.stanbol.enhancer.nlp.ner.NerTag\""));
        Assert.assertTrue(serialized.contains("\"stanbol.enhancer.nlp.morpho\" : {"));
        Assert.assertTrue(serialized.contains("\"class\" : \"org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures\""));
        //deserialize
        AnalyzedTextParser parser = AnalyzedTextParser.getDefaultInstance();
        AnalysedText parsedAt = parser.parse(new ByteArrayInputStream(data), null, 
            atFactory.createAnalysedText(textBlob.getValue()));
        Assert.assertEquals(analysedTextWithData, parsedAt);
        Iterator<Span> origSpanIt = analysedTextWithData.getEnclosed(EnumSet.allOf(SpanTypeEnum.class));
        Iterator<Span> parsedSpanIt = parsedAt.getEnclosed(EnumSet.allOf(SpanTypeEnum.class));
        while(origSpanIt.hasNext() && parsedSpanIt.hasNext()){
            Span orig = origSpanIt.next();
            Span parsed = parsedSpanIt.next();
            Assert.assertEquals(orig, parsed);
            Set<String> origKeys = orig.getKeys();
            Set<String> parsedKeys = parsed.getKeys();
            Assert.assertEquals(origKeys, parsedKeys);
            for(String key : origKeys){
                List<Value<?>> origValues = orig.getValues(key);
                List<Value<?>> parsedValues = parsed.getValues(key);
                Assert.assertEquals(origValues, parsedValues);
            }
        }
        Assert.assertFalse("Original AnalyzedText MUST NOT have additional Spans",origSpanIt.hasNext());
        Assert.assertFalse("Parsed AnalyzedText MUST NOT have additional Spans",parsedSpanIt.hasNext());
    }
    
}
