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
package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.lucene;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.lucene.LuceneLabelTokenizer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

public class LuceneLabelTokenizerTest {

    
    private static final Object TOKENIZER_FACTORY_CLASS = "org.apache.lucene.analysis.core.WhitespaceTokenizerFactory";
    private static LuceneLabelTokenizer luceneLabelTokenizer;

    @BeforeClass
    public static void init() throws ConfigurationException {
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(LuceneLabelTokenizer.PROPERTY_TOKENIZER_FACTORY, TOKENIZER_FACTORY_CLASS);
        config.put(LabelTokenizer.SUPPORTED_LANUAGES, "en");
        ComponentContext cc = new MockComponentContext(config);
        luceneLabelTokenizer = new LuceneLabelTokenizer();
        luceneLabelTokenizer.activate(cc);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullLabel(){
        luceneLabelTokenizer.tokenize(null, "en");
    }
    @Test
    public void testNullLanguate(){
        Assert.assertNull(luceneLabelTokenizer.tokenize("test", null));
    }
    @Test
    public void testUnsupportedLanguage(){
        Assert.assertNull(luceneLabelTokenizer.tokenize("test", "de"));
    }
    @Test
    public void testLuceneLabelTokenizer(){
        String label = "This is only a Test";
        String[] expected = label.split(" ");
        String[] tokens = luceneLabelTokenizer.tokenize(label, "en");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
    }
    @Test
    public void testEmptyLabel(){
        String[] tokens = luceneLabelTokenizer.tokenize("", "en");
        Assert.assertNotNull(tokens);
        Assert.assertTrue(tokens.length == 0);
    }
    
    @AfterClass
    public static void close(){
        luceneLabelTokenizer.deactivate(null);
    }
}
