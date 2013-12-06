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
package org.apache.stanbol.commons.namespaceprefix.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.namespaceprefix.service.StanbolNamespacePrefixService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class StanbolNamespacePrefixServiceTest {

    private static NamespacePrefixService service;

    @BeforeClass
    public static void init() throws IOException {
        URL mappingURL = StanbolNamespacePrefixServiceTest.class.getClassLoader()
                .getResource("testnamespaceprefix.mappings");
        Assert.assertNotNull(mappingURL);
        File mappingFile;
        try {
          mappingFile = new File(mappingURL.toURI());
        } catch(URISyntaxException e) {
          mappingFile = new File(mappingURL.getPath());
        }
        Assert.assertTrue(mappingFile.isFile());
        service = new StanbolNamespacePrefixService(mappingFile);
    }
    @Test
    public void testInitialisationWithoutLocalMappingFIle() throws IOException {
        StanbolNamespacePrefixService service = new StanbolNamespacePrefixService(null);
        service.setPrefix("dummy", "http://www.dummy.org/dummy#");
        Assert.assertEquals("http://www.dummy.org/dummy#", service.getNamespace("dummy"));
        service.importPrefixMappings(new ByteArrayInputStream(
            "dummy2\thttp://www.dummy.org/dummy2#".getBytes(Charset.forName("UTF-8"))));
        Assert.assertEquals("http://www.dummy.org/dummy2#", service.getNamespace("dummy2"));
        
    }
    
    @Test
    public void testInitialisation(){
        //this tests the namespaces defined in namespaceprefix.mappings file
        Assert.assertEquals("http://www.example.org/test#", service.getNamespace("test")); 
        Assert.assertEquals("http://www.example.org/test-1/",service.getNamespace("test-1"));
        Assert.assertEquals("urn:example.text:",service.getNamespace("urn_test"));
    }
    
    @Test 
    public void testMultiplePrefixesForNamespace(){
        List<String> prefixes = service.getPrefixes("http://www.example.org/test#");
        Assert.assertEquals(2, prefixes.size());
        Assert.assertEquals("test",prefixes.get(0));
        Assert.assertEquals("test2",prefixes.get(1));
        Assert.assertEquals("http://www.example.org/test#", service.getNamespace("test"));
        Assert.assertEquals("http://www.example.org/test#", service.getNamespace("test2"));
    }
    @Test
    public void testConversionMethods(){
        Assert.assertEquals("http://www.example.org/test#localname", 
            service.getFullName("test:localname"));
        Assert.assertEquals("http://www.example.org/test#localname:test", 
            service.getFullName("test:localname:test"));
        Assert.assertEquals("http://www.example.org/test#localname", 
            service.getFullName("http://www.example.org/test#localname"));

        Assert.assertEquals("urn:example.text:localname", 
            service.getFullName("urn_test:localname"));
        Assert.assertEquals("urn:example.text:localname/test", 
            service.getFullName("urn_test:localname/test"));
        Assert.assertEquals("urn:example.text:localname", 
            service.getFullName("urn:example.text:localname"));

        Assert.assertNull(service.getFullName("nonExistentPrefix:localname"));
    }
}
