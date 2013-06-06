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
package org.apache.stanbol.commons.namespaceprefix;

import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.junit.Assert;
import org.junit.Test;

public class NamespaceMappingsUtilsTest {

    
    @Test
    public void testPrefixCheck(){
        Assert.assertTrue(NamespaceMappingUtils.checkPrefix(""));
        Assert.assertTrue(NamespaceMappingUtils.checkPrefix("12"));
        Assert.assertTrue(NamespaceMappingUtils.checkPrefix("ab"));
        Assert.assertTrue(NamespaceMappingUtils.checkPrefix("AB"));
        Assert.assertTrue(NamespaceMappingUtils.checkPrefix("a1B"));
        Assert.assertTrue(NamespaceMappingUtils.checkPrefix("a-b"));
        Assert.assertTrue(NamespaceMappingUtils.checkPrefix("a_b"));
        Assert.assertFalse(NamespaceMappingUtils.checkPrefix("a#"));
        Assert.assertFalse(NamespaceMappingUtils.checkPrefix("a/"));
        Assert.assertFalse(NamespaceMappingUtils.checkPrefix("a:"));
        Assert.assertFalse(NamespaceMappingUtils.checkPrefix(" a"));
        Assert.assertFalse(NamespaceMappingUtils.checkPrefix("a "));
        Assert.assertFalse(NamespaceMappingUtils.checkPrefix("å"));
        Assert.assertFalse(NamespaceMappingUtils.checkPrefix("?"));
    }
    @Test
    public void testNamespaceCheck(){
        Assert.assertTrue(NamespaceMappingUtils.checkNamespace("http://www.example.com/"));
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("http://www.example.com"));
        Assert.assertTrue(NamespaceMappingUtils.checkNamespace("http://www.example.com/test/"));
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("http://www.example.com/test"));
        Assert.assertTrue(NamespaceMappingUtils.checkNamespace("http://www.example.com/test#"));
        //for uris no ':'
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("http://www.example.com/test:"));
        //for urn's only : is allowd
        Assert.assertTrue(NamespaceMappingUtils.checkNamespace("urn:example.text:"));
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("urn:example.text"));
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("urn:example.text/"));
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("urn:example.text#"));
        //no relative URI namespaces
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("test/example/"));
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("test/example#"));
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("test/example:"));
        Assert.assertFalse(NamespaceMappingUtils.checkNamespace("test/example:test/"));
    }
    @Test
    public void testGetNamespace(){
        Assert.assertEquals("http://www.example.com/test#",
            NamespaceMappingUtils.getNamespace("http://www.example.com/test#"));
        Assert.assertEquals("http://www.example.com/test#",
            NamespaceMappingUtils.getNamespace("http://www.example.com/test#localname"));
        Assert.assertEquals("http://www.example.com/test/",
            NamespaceMappingUtils.getNamespace("http://www.example.com/test/localname"));
        Assert.assertEquals("http://www.example.com/test/",
            NamespaceMappingUtils.getNamespace("http://www.example.com/test/"));
        Assert.assertEquals("http://www.example.com/",
            NamespaceMappingUtils.getNamespace("http://www.example.com/test:localname"));
        Assert.assertEquals("http://www.example.com/",
            NamespaceMappingUtils.getNamespace("http://www.example.com/test?param=test&param1=test"));
        Assert.assertNull(NamespaceMappingUtils.getNamespace("http://www.example.com"));
        Assert.assertEquals("urn:example:",
            NamespaceMappingUtils.getNamespace("urn:example:"));
        Assert.assertEquals("urn:example:",
            NamespaceMappingUtils.getNamespace("urn:example:localname"));
        Assert.assertEquals("urn:example:",
            NamespaceMappingUtils.getNamespace("urn:example:localname/other"));
        Assert.assertEquals("urn:example:",
            NamespaceMappingUtils.getNamespace("urn:example:localname#other"));
    }
    @Test
    public void testGetPrefix(){
        Assert.assertEquals("",
            NamespaceMappingUtils.getPrefix("localname"));
        Assert.assertEquals("test",
            NamespaceMappingUtils.getPrefix("test:localname"));
        Assert.assertEquals("test",
            NamespaceMappingUtils.getPrefix("test:localname:test"));
        Assert.assertEquals("test",
            NamespaceMappingUtils.getPrefix("test:localname/test"));
        Assert.assertEquals("test",
            NamespaceMappingUtils.getPrefix("test:localname#test"));
        Assert.assertNull(NamespaceMappingUtils.getPrefix("urn:test"));
        Assert.assertNull(NamespaceMappingUtils.getPrefix("urn:test:localname"));
        Assert.assertNull(NamespaceMappingUtils.getPrefix("urn:test:localname#test"));
        Assert.assertNull(NamespaceMappingUtils.getPrefix("http://www.example.com"));
        Assert.assertNull(NamespaceMappingUtils.getPrefix("http://www.example.com/"));
        Assert.assertNull(NamespaceMappingUtils.getPrefix("http://www.example.com/test"));
        Assert.assertNull(NamespaceMappingUtils.getPrefix("http://www.example.com/test#test"));
        
    }
}
