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
import org.apache.stanbol.enhancer.engines.celi.lemmatizer.impl.CeliAnalyzedTextLemmatizerEngineTest;
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

public class CeliAnalyzedTextSentimentAnalysisEngineTest {

	public static final String CELI_SENTIMENT_ANALYSIS_SERVICE_URL = "http://linguagrid.org/LSGrid/ws/sentiment-analysis";
	  
    private static final Logger log = LoggerFactory.getLogger(CeliAnalyzedTextSentimentAnalysisEngineTest.class);
  
    private static final ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    private static final AnalysedTextFactory atFactory = AnalysedTextFactory.getDefaultInstance();
    
    
    /*
     * Data for the ITALIAN test
     */ 
	static CeliAnalyzedTextSentimentAnalysisEngine engine ;
	static 	private String text ="io amo Torino e odio le zanzare";
	
    @BeforeClass
    public static void initEngine() throws IOException, ConfigurationException {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(EnhancementEngine.PROPERTY_NAME, "celiSentimentAnalysis");
		properties.put(CeliConstants.CELI_TEST_ACCOUNT, "true");
		properties.put(CeliAnalyzedTextSentimentAnalysisEngine.SERVICE_URL, CELI_SENTIMENT_ANALYSIS_SERVICE_URL);
        properties.put(CeliConstants.CELI_CONNECTION_TIMEOUT, "5");
		MockComponentContext context = new MockComponentContext(properties);
        engine =  new CeliAnalyzedTextSentimentAnalysisEngine();
        engine.activate(context);
    } 
    
    @AfterClass
    public static void deactivate(){
        engine.deactivate(null);
        engine = null;
    }
    
    @Test
    public void testEngine() throws IOException, EngineException {
        ContentItem ci = ciFactory.createContentItem(new StringSource(text));
        Assert.assertNotNull(ci);
        AnalysedText at = atFactory.createAnalysedText(ci, ci.getBlob());
        Assert.assertNotNull(at);
        ci.getMetadata().add(new TripleImpl(ci.getUri(), DC_LANGUAGE, new PlainLiteralImpl("it")));
        Assert.assertEquals("it", EnhancementEngineHelper.getLanguage(ci));
        
        Assert.assertEquals("Can not enhance Test ContentItem", EnhancementEngine.ENHANCE_ASYNC,engine.canEnhance(ci));
        //compute the enhancements
        try {
            engine.computeEnhancements(ci);
        } catch (EngineException e) {
            RemoteServiceHelper.checkServiceUnavailable(e);
            return; //deactivate test
        }
        //now validate the enhancements
        int sentimentExpressionCnt=0;
        for(Iterator<Token> tokens = at.getTokens(); tokens.hasNext();){
            Token token = tokens.next();
            log.info("Token: {}",token);
            List<Value<Double>> sentimentExpressionsList = token.getAnnotations(NlpAnnotations.SENTIMENT_ANNOTATION);
            if(sentimentExpressionsList!=null && sentimentExpressionsList.size()>0)
            	sentimentExpressionCnt++;
        }
       
        Assert.assertTrue("2 sentiment expressions should be recognized in: "+text,sentimentExpressionCnt==2);
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
