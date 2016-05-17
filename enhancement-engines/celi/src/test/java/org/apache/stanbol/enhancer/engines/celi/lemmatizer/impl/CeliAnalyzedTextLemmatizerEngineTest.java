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

import static org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.CeliLemmatizerEnhancementEngine.SERVICE_URL;
import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_LANGUAGE;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.engines.celi.CeliConstants;
import org.apache.stanbol.enhancer.engines.celi.testutils.MockComponentContext;
import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.morpho.MorphoFeatures;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.test.helper.RemoteServiceHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CeliAnalyzedTextLemmatizerEngineTest {
    
    private static final Logger log = LoggerFactory.getLogger(CeliAnalyzedTextLemmatizerEngineTest.class);
  
    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static final AnalysedTextFactory atFactory = AnalysedTextFactory.getDefaultInstance();
    
    
    /*
     * Data for the GERMAN test
     */
    public static final String de_verb = "verbrachten";
    public static final String de_adjective = "kaiserlichen";//"sensationellen"; //"schönen";
    public static final String de_noun = "Urlaub";
    
    public static final String de_text = String.format(
        "Wir %s einen %s %s in der Schweiz",
        de_verb,de_adjective,de_noun);
    
    public static final int de_verbStart = de_text.indexOf(de_verb);
    public static final double de_verbProb = 0.98765d;

    public static final int de_adjectiveStart = de_text.indexOf(de_adjective);
    public static final double de_adjectiveProb = 0.87654d;
    
    public static final int de_nounStart = de_text.indexOf(de_noun);
    public static final double de_nounProb = 0.998877d;
    
    public static CeliAnalyzedTextLemmatizerEngine engine;
    
    @BeforeClass
    public static void initEngine() throws IOException, ConfigurationException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EnhancementEngine.PROPERTY_NAME, "celiLemmatizer");
        properties.put(CeliConstants.CELI_TEST_ACCOUNT, "true");
        properties.put(CeliConstants.CELI_CONNECTION_TIMEOUT, "5");
        properties.put(SERVICE_URL, "http://linguagrid.org/LSGrid/ws/morpho-analyser");
        MockComponentContext context = new MockComponentContext(properties);
        engine = new CeliAnalyzedTextLemmatizerEngine();
        engine.activate(context);
    } 
    @AfterClass
    public static void deactivate(){
        engine.deactivate(null);
        engine = null;
    }
    
    @Test
    public void testEngineDe() throws IOException, EngineException {
        ContentItem ci = ciFactory.createContentItem(new StringSource(de_text));
        Assert.assertNotNull(ci);
        AnalysedText at = atFactory.createAnalysedText(ci, ci.getBlob());
        Assert.assertNotNull(at);
        ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl("de")));
        Assert.assertEquals("de", EnhancementEngineHelper.getLanguage(ci));
        
        //Add some Tokens with POS annotations to test the usage of
        //existing POS annotations by the lemmatizer
        Token verbrachten = at.addToken(de_verbStart,de_verbStart+de_verb.length());
        verbrachten.addAnnotation(POS_ANNOTATION, Value.value(
            new PosTag("V",LexicalCategory.Verb), de_verbProb));
        
        Token schonen = at.addToken(de_adjectiveStart,de_adjectiveStart+de_adjective.length()); 
        schonen.addAnnotation(POS_ANNOTATION, Value.value(
            new PosTag("ADJ",LexicalCategory.Adjective), de_adjectiveProb));
        
        Token urlaub = at.addToken(de_nounStart,de_nounStart+de_noun.length()); 
        urlaub.addAnnotation(POS_ANNOTATION, Value.value(
            new PosTag("NC",LexicalCategory.Noun), de_nounProb));
        
        Assert.assertEquals("Can not enhance Test ContentItem",
            EnhancementEngine.ENHANCE_ASYNC,engine.canEnhance(ci));
        //compute the enhancements
        try {
            engine.computeEnhancements(ci);
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return; //deactivate test
        }
        //now validate the enhancements
        boolean foundVerb = false;
        boolean foundAdjective = false;
        boolean foundNoun = false;
        for(Iterator<Token> tokens = at.getTokens(); tokens.hasNext();){
            Token token = tokens.next();
            log.info("Token: {}",token);
            List<Value<MorphoFeatures>> mfs = token.getAnnotations(NlpAnnotations.MORPHO_ANNOTATION);
            if(de_verb.equals(token.getSpan())){
                foundVerb = !mfs.isEmpty();
                validateMorphFeatureProbability(mfs,LexicalCategory.Verb,de_verbProb);
            } else if(de_adjective.equals(token.getSpan())){
                foundAdjective = !mfs.isEmpty();
                validateMorphFeatureProbability(mfs,LexicalCategory.Adjective,de_adjectiveProb);
            } else if(de_noun.equals(token.getSpan())){
                foundNoun = !mfs.isEmpty();
                validateMorphFeatureProbability(mfs,LexicalCategory.Noun,de_nounProb);
            }
            for(Value<MorphoFeatures> mf : mfs){
                log.info("  - {}",mf);
                Assert.assertNotNull(mf.value().getLemma());
            }
        }
        Assert.assertTrue("No MorphoFeatures found for '"+de_verb+"'!",foundVerb);
        Assert.assertTrue("No MorphoFeatures found for '"+de_adjective+"'!",foundAdjective);
        Assert.assertTrue("No MorphoFeatures found for '"+de_noun+"'!",foundNoun);
    }
    private void validateMorphFeatureProbability(List<Value<MorphoFeatures>> mfs, LexicalCategory lc, double prob) {
        for(Value<MorphoFeatures> mf : mfs){
            for(PosTag pos : mf.value().getPosList()){
                if(pos.hasCategory(lc)){
                    Assert.assertEquals(prob, mf.probability());
                }
            }
        }
    }
    
}
