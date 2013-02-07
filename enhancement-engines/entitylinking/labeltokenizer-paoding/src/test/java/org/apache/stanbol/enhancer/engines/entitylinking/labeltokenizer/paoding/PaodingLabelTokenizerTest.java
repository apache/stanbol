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
package org.apache.stanbol.enhancer.engines.entitylinking.labeltokenizer.paoding;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.stanbol.commons.solr.extras.paoding.Activator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

public class PaodingLabelTokenizerTest {

    
    protected static final String TEST_PAODING_DIC_PATH = File.separatorChar + "target" 
            + File.separatorChar + "paoding-dict";
    private static PaodingLabelTokenizer labelTokenizer;
    private static File paodingDict;

    @BeforeClass
    public static void init() throws ConfigurationException, IOException {
        String baseDir = System.getProperty("basedir") == null ? "." : System.getProperty("basedir");
        paodingDict = new File(baseDir,TEST_PAODING_DIC_PATH);
        if(!paodingDict.isDirectory()){
            Activator.initPaodingDictionary(paodingDict, PaodingLabelTokenizerTest.class.
                getClassLoader().getResourceAsStream(Activator.DICT_ARCHIVE));
        }
        Activator.initPaodingDictHomeProperty(paodingDict);
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        ComponentContext cc = new MockComponentContext(config);
        labelTokenizer = new PaodingLabelTokenizer();
        labelTokenizer.activate(cc);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullLabel(){
        labelTokenizer.tokenize(null, "en");
    }
    @Test
    public void testNullLanguate(){
        Assert.assertNull(labelTokenizer.tokenize("test", null));
    }
    @Test
    public void testUnsupportedLanguage(){
        Assert.assertNull(labelTokenizer.tokenize("test", "de"));
    }
    @Test
    public void testLuceneLabelTokenizer(){
        //BBC
        String label = "英国广播公司";
        String[] expected = new String[]{"英国","广播","公司"};
        String[] tokens = labelTokenizer.tokenize(label, "zh");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
        //Yellow Sea (one word??)
        label = "黄海";
        expected = new String[]{"黄海"};
        tokens = labelTokenizer.tokenize(label, "zh");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
        //Barack Obama
        label = "贝拉克·奥巴马";
        expected = new String[]{"贝拉","克","·","奥","巴马"};
        tokens = labelTokenizer.tokenize(label, "zh");
        Assert.assertNotNull(tokens);
        Assert.assertArrayEquals(expected, tokens);
    }
    @Test
    public void testEmptyLabel(){
        String[] tokens = labelTokenizer.tokenize("", "zh");
        Assert.assertNotNull(tokens);
        Assert.assertTrue(tokens.length == 0);
    }
    
    @AfterClass
    public static void close(){
        if(labelTokenizer != null){
            labelTokenizer.deactivate(null);
        }
    }
}
