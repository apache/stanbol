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
package org.apache.stanbol.enhancer.engines.paoding.token;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.commons.solr.extras.paoding.Activator;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

public class TestPaodingTokenizerEngine {
    public static final String FAKE_BUNDLE_SYMBOLIC_NAME = "FAKE_BUNDLE_SYMBOLIC_NAME";
   

    private static ContentItemFactory contentItemFactory;
    
    private static UriRef id = new UriRef("http://www.example.org/contentItem1");
    /**
     * Test text taken from the <a href ="http://zh.wikipedia.org/wiki/Barack_Obama">
     * Chinese wikipedia side for Barack Obama</a>.
     */
    private static String text = "巴拉克·侯赛因·奥巴马二世，美国民主黨籍政治家，第44任美国总统，"
            + "為第一位非裔美国总统，同時擁有黑（盧歐族）白（英德爱混血）血统，於2008年初次當選美國總統，"
            + "並於2012年成功連任，。\n 奥巴马1961年出生於美国夏威夷州檀香山，童年和青少年時期分别在印尼和"
            + "夏威夷度过。1991年，奥巴马以优等生荣誉从哈佛法学院毕业。1996年，当选伊利诺州参议员。2000年，"
            + "競選美国众议院席位失败，后一直从事州参议员工作，且於2002年获得连任。2004年，"
            + "在美国民主党全国代表大会上发表主题演讲，因此成为全美知名的政界人物。同年11月，"
            + "以70%的选票当选代表伊利诺州的美国联邦参议员，是美國歷史上第五位有非裔血统的联邦参议员。";

    private PaodingTokenizerEngine engine;
    
    private ContentItem contentItem;

    protected static final String TEST_PAODING_DIC_PATH = File.separatorChar + "target" 
            + File.separatorChar + "paoding-dict";
    private static File paodingDict;
    
    @BeforeClass
    public static void initDataFileProvicer() throws IOException{
        String baseDir = System.getProperty("basedir") == null ? "." : System.getProperty("basedir");
        paodingDict = new File(baseDir,TEST_PAODING_DIC_PATH);
        if(!paodingDict.isDirectory()){
            Activator.initPaodingDictionary(paodingDict, TestPaodingTokenizerEngine.class.
                getClassLoader().getResourceAsStream(Activator.DICT_ARCHIVE));
        }
        Activator.initPaodingDictHomeProperty(paodingDict);
        contentItemFactory = InMemoryContentItemFactory.getInstance();
    }
    
    @Before
    public void setUpServices() throws IOException , ConfigurationException {
        engine = new PaodingTokenizerEngine();
        engine.analysedTextFactory = AnalysedTextFactory.getDefaultInstance();
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(EnhancementEngine.PROPERTY_NAME, "paoding-token");
        engine.activate(new MockComponentContext(config));
        contentItem = contentItemFactory.createContentItem(id, new StringSource(text));
        //add an annotation that this is Japanese
        contentItem.getMetadata().add(new TripleImpl(id, Properties.DC_LANGUAGE, 
            new PlainLiteralImpl("zh")));
    }
    
    @Test
    public void testEngine() throws EngineException {
        Assert.assertEquals(EnhancementEngine.ENHANCE_ASYNC, engine.canEnhance(contentItem));
        engine.computeEnhancements(contentItem);
        //assert the results
        AnalysedText at = AnalysedTextUtils.getAnalysedText(contentItem);
        Assert.assertNotNull(at);
        Assert.assertTrue(at.getTokens().hasNext()); //assert that tokens are present
    }
    

    @After
    public void cleanUpServices(){
        if(engine != null){
            engine.deactivate(null);
        }
        engine = null;
    }
    
}
