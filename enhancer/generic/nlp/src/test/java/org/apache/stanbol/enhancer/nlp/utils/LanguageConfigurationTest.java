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
package org.apache.stanbol.enhancer.nlp.utils;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;


import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

public class LanguageConfigurationTest {

    
    @Test
    public void testExplicitLanguages() throws ConfigurationException{
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put("test", "de,en");
        LanguageConfiguration lc = new LanguageConfiguration("test", null);
        lc.setConfiguration(config);
        Assert.assertTrue(lc.isLanguage("de"));
        Assert.assertTrue(lc.isLanguage("en"));
        Assert.assertFalse(lc.isLanguage("jp"));
        config.put("test2", new String[]{"ru","fi"});
        lc = new LanguageConfiguration("test2", null);
        lc.setConfiguration(config);
        Assert.assertTrue(lc.isLanguage("ru"));
        Assert.assertTrue(lc.isLanguage("fi"));
        Assert.assertFalse(lc.isLanguage("jp"));
        config.put("test3", Arrays.asList("zh","jp"));
        lc = new LanguageConfiguration("test3", null);
        lc.setConfiguration(config);
        Assert.assertTrue(lc.isLanguage("zh"));
        Assert.assertTrue(lc.isLanguage("jp"));
        Assert.assertFalse(lc.isLanguage("de"));
    }
    @Test
    public void testExclusions() throws ConfigurationException{
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put("test", "*,!de");
        LanguageConfiguration lc = new LanguageConfiguration("test", null);
        lc.setConfiguration(config);
        Assert.assertTrue(lc.isLanguage(null));
        Assert.assertFalse(lc.isLanguage("de"));
        Assert.assertTrue(lc.isLanguage("en"));
        Assert.assertTrue(lc.isLanguage("jp"));
        config.put("test2", new String[]{"!ru","!fi","*"});
        lc = new LanguageConfiguration("test2", null);
        lc.setConfiguration(config);
        Assert.assertFalse(lc.isLanguage("ru"));
        Assert.assertFalse(lc.isLanguage("fi"));
        Assert.assertTrue(lc.isLanguage("jp"));
        config.put("test3", Arrays.asList("!zh","*","!jp"));
        lc = new LanguageConfiguration("test3", null);
        lc.setConfiguration(config);
        Assert.assertFalse(lc.isLanguage("zh"));
        Assert.assertFalse(lc.isLanguage("jp"));
        Assert.assertTrue(lc.isLanguage("de"));
    }
    
    @Test
    public void testLanguageParameter() throws ConfigurationException{
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put("test", "*,de;param1=test");
        LanguageConfiguration lc = new LanguageConfiguration("test", null);
        lc.setConfiguration(config);
        Assert.assertTrue(lc.isLanguage("de"));
        Assert.assertTrue(lc.isLanguage("en"));
        Assert.assertEquals("test", lc.getParameter("de", "param1"));
        Assert.assertNull(lc.getParameter("en", "param1"));
        Assert.assertNull(lc.getParameter("de", "noParam"));
        Map<String,String> params = lc.getParameters("de");
        Assert.assertNotNull(params);
        Assert.assertEquals(1, params.size());
        Assert.assertEquals("test", params.get("param1"));

        lc = new LanguageConfiguration("test2", null);
        config.put("test2", new String[]{"*","!ru","fi;param1=test1;param2=test2;param3=test3"});
        lc.setConfiguration(config);
        Assert.assertTrue(lc.isLanguage("fi"));
        Assert.assertTrue(lc.isLanguage("en"));
        Assert.assertFalse(lc.isLanguage("ru"));
        Assert.assertEquals("test1", lc.getParameter("fi", "param1"));
        Assert.assertEquals("test2", lc.getParameter("fi", "param2"));
        Assert.assertEquals("test3", lc.getParameter("fi", "param3"));
        params = lc.getParameters("fi");
        Assert.assertNotNull(params);
        Assert.assertEquals(3, params.size());
        Assert.assertEquals("test1", params.get("param1"));
        Assert.assertEquals("test2", params.get("param2"));
        Assert.assertEquals("test3", params.get("param3"));
    }
    @Test(expected=ConfigurationException.class)
    public void testParamsOnExcludedLanguage() throws ConfigurationException {
        LanguageConfiguration lc = new LanguageConfiguration("test", null);
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put("test", "*,!de;param=notAllowed");
        lc.setConfiguration(config);
    }
    @Test(expected=ConfigurationException.class)
    public void testParamsIncludedAndExcludedLanguage() throws ConfigurationException {
        LanguageConfiguration lc = new LanguageConfiguration("test", null);
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put("test", "de,ru,!de");
        lc.setConfiguration(config);
    }
    @Test
    public void testDefaultParametersOnWildcard() throws ConfigurationException {
        LanguageConfiguration lc = new LanguageConfiguration("test", null);
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put("test", "*;param=default,de;param1=test1,!ru,es;param=overridden");
        lc.setConfiguration(config);
        //test defaults
        Assert.assertEquals("default", lc.getParameter("en", "param"));
        //test merging with specific
        Assert.assertEquals("default", lc.getParameter("de", "param"));
        Assert.assertEquals("test1", lc.getParameter("de", "param1"));
        Map<String,String> params = lc.getParameters("de");
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("default", params.get("param"));
        Assert.assertEquals("test1", params.get("param1"));
        //test overriding
        Assert.assertEquals("overridden", lc.getParameter("es", "param"));
        //test that ru is excluded
        Assert.assertFalse(lc.isLanguage("ru"));
        Assert.assertNull(lc.getParameters("ru"));
    }
    @Test
    public void testDefaultParametersWithoutWildcard() throws ConfigurationException {
        LanguageConfiguration lc = new LanguageConfiguration("test", null);
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put("test", ";param=default,de;param1=test1,ru,es;param=overridden");
        lc.setConfiguration(config);
        //test no wildcard
        Assert.assertFalse(lc.isLanguage("en"));
        //test defaults
        Assert.assertEquals("default", lc.getParameter("ru", "param"));
        //test merging with specific
        Assert.assertEquals("default", lc.getParameter("de", "param"));
        Assert.assertEquals("test1", lc.getParameter("de", "param1"));
        Map<String,String> params = lc.getParameters("de");
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("default", params.get("param"));
        Assert.assertEquals("test1", params.get("param1"));
        //test overriding
        Assert.assertEquals("overridden", lc.getParameter("es", "param"));
        
    }
    @Test
    public void testCountrySpecificConfigurations() throws ConfigurationException {
        LanguageConfiguration lc = new LanguageConfiguration("test", null);
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put("test", ";param=default,de-AT;param1=test1,de;param2=test2");
        lc.setConfiguration(config);
        //test no wildcard
        Assert.assertFalse(lc.isLanguage("en"));
        Assert.assertTrue(lc.isLanguage("de"));
        Assert.assertTrue(lc.isLanguage("de-AT"));
        Assert.assertTrue(lc.isLanguage("de-CH"));
        //test defaults
        Assert.assertEquals("default", lc.getParameter("de", "param"));
        Assert.assertEquals("default", lc.getParameter("de-AT", "param"));
        Assert.assertEquals("default", lc.getParameter("de-CH", "param"));
        //test specific
        Assert.assertEquals("test2", lc.getParameter("de", "param2"));
        Assert.assertEquals("test2", lc.getParameter("de-CH", "param2"));
        Assert.assertEquals("test2",lc.getParameter("de-AT", "param2")); //fallback from de-AT to de

        //test Country specificspecific
        Assert.assertEquals("test1", lc.getParameter("de-AT", "param1"));
        Assert.assertNull(lc.getParameter("de", "param1"));
        Assert.assertNull(lc.getParameter("de-CH", "param1"));
        
    }
    
}
