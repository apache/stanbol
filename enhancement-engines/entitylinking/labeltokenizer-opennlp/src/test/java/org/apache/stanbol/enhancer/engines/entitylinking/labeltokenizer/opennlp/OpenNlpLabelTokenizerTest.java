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
package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.opennlp;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.commons.opennlp.OpenNLP;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

public class OpenNlpLabelTokenizerTest {
    
    private static OpenNlpLabelTokenizer tokenizer;
    private String label = "This is only a Test";
    private String[] expected = label.split(" ");
    
    @BeforeClass
    public static final void init() throws ConfigurationException {
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(LabelTokenizer.SUPPORTED_LANUAGES, "*");
        ComponentContext cc = new MockComponentContext(config);
        tokenizer = new OpenNlpLabelTokenizer();
        tokenizer.openNlp = new OpenNLP(new ClasspathDataFileProvider(null));
        tokenizer.activate(cc);
    }
    @Test(expected=IllegalArgumentException.class)
    public void testNullLabel(){
        tokenizer.tokenize(null, "en");
    }
    @Test
    public void testNullLanguage(){
        String[] tokens = tokenizer.tokenize(label, null);
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
    }
    @Test
    public void testTokenizer(){
        String[] tokens = tokenizer.tokenize(label, "en");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
    }
    @Test
    public void testEmptyLabel(){
        String[] tokens = tokenizer.tokenize("", "en");
        Assert.assertNotNull(tokens);
        Assert.assertTrue(tokens.length == 0);
    }
    
    @AfterClass
    public static void close(){
        tokenizer.deactivate(null);
    }
}
