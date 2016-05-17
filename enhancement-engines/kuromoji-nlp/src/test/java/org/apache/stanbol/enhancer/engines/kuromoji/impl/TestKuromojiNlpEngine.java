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
package org.apache.stanbol.enhancer.engines.kuromoji.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.commons.solr.utils.DataFileResourceLoader;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextUtils;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.apache.stanbol.enhancer.test.helper.EnhancementStructureHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

public class TestKuromojiNlpEngine {
    public static final String FAKE_BUNDLE_SYMBOLIC_NAME = "FAKE_BUNDLE_SYMBOLIC_NAME";
   
    private static DataFileProvider dataFileProvider;

    private static ContentItemFactory contentItemFactory;
    
    private static IRI id = new IRI("http://www.example.org/contentItem1");
    /**
     * Test text taken from the <a href ="http://ja.wikipedia.org/wiki/%E3%83%AD%E3%83%B3%E3%83%89%E3%83%B3">
     * Japanese wikipedia side for London</a>.
     */
    private static String text = "ロンドンはイングランドおよびイギリスの首都であり、イギリスや欧州"+
            "連合域内で最大の都市圏を形成している。ロンドンはテムズ川河畔に位置し、2,000年前のローマ帝国"+
            "によるロンディニウム創建が都市の起源である。ロンディニウム当時の街の中心部は、現在のシティ・"+
            "オブ・ロンドン（シティ）に当たる地域にあった。シティの市街壁内の面積は約1平方マイルあり、"+
            "中世以来その範囲はほぼ変わっていない。少なくとも19世紀以降、「ロンドン」の名称はシティの市"+
            "街壁を越えて開発が進んだシティ周辺地域をも含めて用いられている。 ロンドンは市街地の大部分は"+
            "コナベーションにより形成されている。ロンドンを管轄するリージョンであるグレーター・ロンドンでは"+
            "、選挙で選出された大ロンドン市長とロンドン議会により統治が行われている。";

    private KuromojiNlpEngine engine;
    
    private ContentItem contentItem;
    
    @BeforeClass
    public static void initDataFileProvicer(){
        dataFileProvider = new ClasspathDataFileProvider(FAKE_BUNDLE_SYMBOLIC_NAME);
        contentItemFactory = InMemoryContentItemFactory.getInstance();
    }
    
    @Before
    public void setUpServices() throws IOException , ConfigurationException {
        engine = new KuromojiNlpEngine();
        //we need to set some fields that would otherwise be injected by the container
        engine.parentResourceLoader = new DataFileResourceLoader(dataFileProvider);
        engine.analysedTextFactory = AnalysedTextFactory.getDefaultInstance();
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(EnhancementEngine.PROPERTY_NAME, "gosen-nlp");
        engine.activate(new MockComponentContext(config));
        contentItem = contentItemFactory.createContentItem(id, new StringSource(text));
        //add an annotation that this is Japanese
        contentItem.getMetadata().add(new TripleImpl(id, Properties.DC_LANGUAGE, 
            new PlainLiteralImpl("ja")));
    }
    
    @Test
    public void testEngine() throws EngineException {
        LiteralFactory lf = LiteralFactory.getInstance();
        Assert.assertEquals(EnhancementEngine.ENHANCE_ASYNC, engine.canEnhance(contentItem));
        engine.computeEnhancements(contentItem);
        //assert the results
        Map<IRI,RDFTerm> expected = new HashMap<IRI,RDFTerm>();
        expected.put(Properties.DC_CREATOR, lf.createTypedLiteral(engine.getClass().getName()));
        expected.put(Properties.ENHANCER_EXTRACTED_FROM,contentItem.getUri());
        Assert.assertEquals(16, EnhancementStructureHelper.validateAllTextAnnotations(
            contentItem.getMetadata(), text, expected));
        AnalysedText at = AnalysedTextUtils.getAnalysedText(contentItem);
        Assert.assertNotNull(at);
        List<Sentence> sentences = AnalysedTextUtils.asList(at.getSentences());
        Assert.assertNotNull(sentences);
        Assert.assertEquals(7, sentences.size());
        //TODO: values in the following arrays are based on the first run of the
        // engine. So this is only to detect changes in results. It can not validate
        // that the tokenization and NER detections are correct - sorry I do not
        // speak Japanese ...
        int[] expectedChunks = new int[]{ 5, 3, 1, 0, 1, 2, 4};
        int[]  expectedTokens = new int[]{ 25, 25, 25, 24, 33, 17, 32};
        int sentIndex = 0;
        for(Sentence sent : sentences){
            List<Chunk> sentenceNer = AnalysedTextUtils.asList(sent.getChunks());
            Assert.assertEquals(expectedChunks[sentIndex], sentenceNer.size());
            for(Chunk chunk : sentenceNer){
                Value<NerTag> nerValue = chunk.getAnnotation(NlpAnnotations.NER_ANNOTATION);
                Assert.assertNotNull(nerValue);
                Assert.assertNotNull(nerValue.value().getType());
            }
            List<Token> tokens = AnalysedTextUtils.asList(sent.getTokens());
           Assert.assertEquals(expectedTokens[sentIndex], tokens.size());
            for(Token token : tokens){
                Value<PosTag> posValue = token.getAnnotation(NlpAnnotations.POS_ANNOTATION);
                Assert.assertNotNull(posValue);
            }
            sentIndex++;
        }
    }
    

    @After
    public void cleanUpServices(){
        if(engine != null){
            engine.deactivate(null);
        }
        engine = null;
    }
    
}
