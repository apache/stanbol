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
package org.apache.stanbol.commons.namespaceprefix.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.service.StanbolNamespacePrefixServiceTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class NamespacePrefixProviderImplTest {
    
    private static File mappingFile;
    
    
    @BeforeClass
    public static void init() throws IOException {
        URL mappingURL = StanbolNamespacePrefixServiceTest.class.getClassLoader()
                .getResource("testnamespaceprefix.mappings");
        Assert.assertNotNull(mappingURL);
        try {
          mappingFile = new File(mappingURL.toURI());
        } catch(URISyntaxException e) {
          mappingFile = new File(mappingURL.getPath());
        }
        Assert.assertTrue(mappingFile.isFile());
    }

    @Test
    public void testReadingFromFile() throws IOException {
        NamespacePrefixProvider provider = new NamespacePrefixProviderImpl(
            new FileInputStream(mappingFile));
        //this tests the namespaces defined in namespaceprefix.mappings file
        Assert.assertEquals("http://www.example.org/test#", provider.getNamespace("test")); 
        Assert.assertEquals("http://www.example.org/test-1/",provider.getNamespace("test-1"));
        Assert.assertEquals("urn:example.text:",provider.getNamespace("urn_test"));
        List<String> prefixes = provider.getPrefixes("http://www.example.org/test#");
        Assert.assertEquals(2, prefixes.size());
        Assert.assertEquals("test",prefixes.get(0));
        Assert.assertEquals("test2",prefixes.get(1));
        Assert.assertEquals("http://www.example.org/test#", provider.getNamespace("test"));
        Assert.assertEquals("http://www.example.org/test#", provider.getNamespace("test2"));
    }
    
    @Test
    public void testReadingFromMap() {
        Map<String,String> mappings = new LinkedHashMap<String,String>();
        mappings.put("test","http://www.example.org/test#");
        mappings.put("test-1","http://www.example.org/test-1/");
        mappings.put("urn_test","urn:example.text:");
        mappings.put("test2","http://www.example.org/test#");
        //add some invalid mappings
        mappings.put("test:3","http://www.example.org/test#");
        mappings.put("test3","http://www.example.org/test");
        
        NamespacePrefixProvider provider = new NamespacePrefixProviderImpl(mappings);
        //this tests the namespaces defined in namespaceprefix.mappings file
        Assert.assertEquals("http://www.example.org/test#", provider.getNamespace("test")); 
        Assert.assertEquals("http://www.example.org/test-1/",provider.getNamespace("test-1"));
        Assert.assertEquals("urn:example.text:",provider.getNamespace("urn_test"));
        List<String> prefixes = provider.getPrefixes("http://www.example.org/test#");
        Assert.assertEquals(2, prefixes.size());
        Assert.assertEquals("test",prefixes.get(0));
        Assert.assertEquals("test2",prefixes.get(1));
        Assert.assertEquals("http://www.example.org/test#", provider.getNamespace("test"));
        Assert.assertEquals("http://www.example.org/test#", provider.getNamespace("test2"));
        
        //test that illegal mappings are not imported
        Assert.assertNull(provider.getNamespace("test:3"));
        Assert.assertNull(provider.getNamespace("test3"));
        
    }
    
}
