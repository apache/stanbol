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
package org.apache.stanbol.enhancer.serviceapi.helper;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.Assert;
import org.junit.Test;


public class EnhancementEngineHelperTest {

    private static final LiteralFactory lf = LiteralFactory.getInstance();
    
    /**
     * internally used as argument for {@link EnhancementEngineHelper} methods
     */
    private static final EnhancementEngine dummyEngine = new EnhancementEngine(){

        @Override
        public int canEnhance(ContentItem ci) throws EngineException {
            return 0;
        }
    
        @Override
        public void computeEnhancements(ContentItem ci) throws EngineException {
        }
    
        @Override
        public String getName() {
            return "DummyEngine";
        }
        
    };
    
    
    @Test
    public void testTextAnnotationNewModel(){
        String content = "The Stanbol Enhancer can extract Entities form parsed Text.";
        Language lang = new Language("en");
        int start = content.indexOf("Stanbol");
        int end = start+"Stanbol Enhancer".length();
        IRI ciUri = new IRI("http://www.example.org/contentItem#1");
        Graph metadata = new IndexedGraph();
        IRI ta = EnhancementEngineHelper.createTextEnhancement(metadata, dummyEngine, ciUri);
        EnhancementEngineHelper.setOccurrence(metadata, ta, content, start, end, lang, -1, true);
        Assert.assertEquals("The ", EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_PREFIX));
        Assert.assertEquals("Stanbol Enhancer", EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTED_TEXT));
        Assert.assertEquals(" can extra", EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_SUFFIX));
        Assert.assertEquals(Integer.valueOf(start), EnhancementEngineHelper.get(
            metadata, ta, Properties.ENHANCER_START, Integer.class, lf));
        Assert.assertEquals(Integer.valueOf(end), EnhancementEngineHelper.get(
            metadata, ta, Properties.ENHANCER_END, Integer.class, lf));
        //head and tail should be null
        Assert.assertNull(EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_HEAD));
        Assert.assertNull(EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_TAIL));
        
        content = "Ich habe den Schlüssel fürs Donaudampfschiffahrtsgesellschaftskapitänskajütenschloss verlohren.";
        start = content.indexOf("Donaudampfschi");
        end = content.indexOf(" verlohren");
        ta = EnhancementEngineHelper.createTextEnhancement(metadata, dummyEngine, ciUri);
        EnhancementEngineHelper.setOccurrence(metadata, ta, content, start, end, lang, -1, true);
        Assert.assertEquals("ssel fürs ", EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_PREFIX));
        Assert.assertEquals(" verlohren", EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_SUFFIX));
        Assert.assertEquals(Integer.valueOf(start), EnhancementEngineHelper.get(
            metadata, ta, Properties.ENHANCER_START, Integer.class, lf));
        Assert.assertEquals(Integer.valueOf(end), EnhancementEngineHelper.get(
            metadata, ta, Properties.ENHANCER_END, Integer.class, lf));
        //selected text is expected to be null
        Assert.assertNull(EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTED_TEXT));
        //tail and head should be present
        Assert.assertEquals("Donaudampf", EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_HEAD));
        Assert.assertEquals("tenschloss", EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_TAIL));
        
        //finally the same but deactivating head/tail
        ta = EnhancementEngineHelper.createTextEnhancement(metadata, dummyEngine, ciUri);
        EnhancementEngineHelper.setOccurrence(metadata, ta, content, start, end, lang, -1, false);
        Assert.assertEquals("Donaudampfschiffahrtsgesellschaftskapitänskajütenschloss", 
            EnhancementEngineHelper.getString(metadata, ta,Properties.ENHANCER_SELECTED_TEXT));
        Assert.assertNull(EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_HEAD));
        Assert.assertNull(EnhancementEngineHelper.getString(
            metadata, ta,Properties.ENHANCER_SELECTION_TAIL));
    }
    /**
     * Tests positive cases for {@link EnhancementEngineHelper#parseConfigValues(Object, Class, boolean)}
     */
    @Test
    public void testConfigValues(){
        Object value = new String[]{"23","27.25","-23"};
        Assert.assertEquals(
            Arrays.asList(Float.valueOf(23),Float.valueOf(27.25f),Float.valueOf(-23)), 
            EnhancementEngineHelper.parseConfigValues(value, Float.class));
        Assert.assertEquals(
            Arrays.asList(Double.valueOf(23),Double.valueOf(27.25f),Double.valueOf(-23)), 
            EnhancementEngineHelper.parseConfigValues(value, Double.class));
        Assert.assertEquals(
            Arrays.asList("23","27.25","-23"), 
            EnhancementEngineHelper.parseConfigValues(value, String.class));
        
        
        value = new float[]{23f,27.25f,-23f};
        Assert.assertEquals(
            Arrays.asList(Float.valueOf(23),Float.valueOf(27.25f),Float.valueOf(-23)), 
            EnhancementEngineHelper.parseConfigValues(value, Float.class));
        Assert.assertEquals(
            Arrays.asList(Double.valueOf(23),Double.valueOf(27.25f),Double.valueOf(-23)), 
            EnhancementEngineHelper.parseConfigValues(value, Double.class));
        Assert.assertEquals(
            Arrays.asList("23.0","27.25","-23.0"), 
            EnhancementEngineHelper.parseConfigValues(value, String.class));

    
        value = new String[]{"23","27.25",null,"-23"};
        Assert.assertEquals(
            Arrays.asList(Float.valueOf(23),Float.valueOf(27.25f),Float.valueOf(-23)), 
            EnhancementEngineHelper.parseConfigValues(value, Float.class));
        Assert.assertEquals(
            Arrays.asList(Float.valueOf(23),Float.valueOf(27.25f),null,Float.valueOf(-23)), 
            EnhancementEngineHelper.parseConfigValues(value, Float.class,true));

        value = "23";
        Assert.assertEquals(Arrays.asList(Integer.valueOf(23)), 
            EnhancementEngineHelper.parseConfigValues(value, Integer.class));
        Assert.assertEquals(Arrays.asList(Float.valueOf(23)), 
            EnhancementEngineHelper.parseConfigValues(value, Float.class));
        Assert.assertEquals(Arrays.asList("23"), 
            EnhancementEngineHelper.parseConfigValues(value, String.class));
        
        value = null;
        Assert.assertNull(EnhancementEngineHelper.parseConfigValues(value, String.class));
        Assert.assertNull(EnhancementEngineHelper.parseConfigValues(value, Double.class));

        //This tests parsing an Set
        value = new String[]{"23","27","-23","27"};
        Assert.assertEquals(
            new HashSet<Integer>(Arrays.asList(Integer.valueOf(23),Integer.valueOf(27),Integer.valueOf(-23))), 
            EnhancementEngineHelper.parseConfigValues(value, Integer.class, new HashSet<Integer>(),false));
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList("23","27","-23")), 
            EnhancementEngineHelper.parseConfigValues(value, String.class, new HashSet<String>(),false));

        //This tests parsing a Set the the config values method that allows to parse
        //a custom collection
        value = new String[]{"23","27",null,"-23","27", null};
        Assert.assertEquals(
            new HashSet<Integer>(Arrays.asList(Integer.valueOf(23),Integer.valueOf(27),Integer.valueOf(-23))), 
            EnhancementEngineHelper.parseConfigValues(value, Integer.class, new HashSet<Integer>()));
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList("23","27","-23")), 
            EnhancementEngineHelper.parseConfigValues(value, String.class, new HashSet<String>()));

        Assert.assertEquals(
            new HashSet<Integer>(Arrays.asList(null, Integer.valueOf(23),Integer.valueOf(27),Integer.valueOf(-23))), 
            EnhancementEngineHelper.parseConfigValues(value, Integer.class, new HashSet<Integer>(),true));
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList(null,"23","27","-23")), 
            EnhancementEngineHelper.parseConfigValues(value, String.class, new HashSet<String>(),true));

    }
    
    /**
     * Tests positive cases for {@link EnhancementEngineHelper#parseFirstConfigValue(Object, Class)}
     */
    @Test
    public void testFirstConfigValue(){
        Object value = null;
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        
        value = new String[]{null};
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));

        value = new String[]{null,null,null};
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));

        value = Arrays.asList((String)null);
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));

        value = Arrays.asList(null,null,null);
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertNull(EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));

        value = "23";
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));
        
        value = new String[]{"23"};
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));

        value = new int[]{23};
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));

        value = new String[]{"23","24"};
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));

        value = new int[]{23, 24};
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));

        value = new String[]{null,"23"};
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));

        value = Arrays.asList("23");
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));

        value = Arrays.asList("23","24");
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));

        value = Arrays.asList(null,"23");
        Assert.assertEquals("23", EnhancementEngineHelper.parseFirstConfigValue(value, String.class));
        Assert.assertEquals(Integer.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Integer.class));
        Assert.assertEquals(Double.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, Double.class));
        Assert.assertEquals(BigInteger.valueOf(23), EnhancementEngineHelper.parseFirstConfigValue(value, BigInteger.class));
    }
    
}
