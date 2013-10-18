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
package org.apache.stanbol.entityhub.indexing.source.jenatdb;

import org.junit.Assert;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;

public class LanguageLiteralFilterTest {

    @Test(expected=IllegalArgumentException.class)
    public void testIncludeExcludeConfig1(){
        new LiteralLanguageFilter("en,de,!de");
    }
    @Test(expected=IllegalArgumentException.class)
    public void testIncludeExcludeConfig2(){
        new LiteralLanguageFilter("en,!de,de");
    }
    @Test
    public void testDataTypes(){
        RdfImportFilter filter = new LiteralLanguageFilter("en,de");
        
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "en", false)));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test")));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("10",XSDDatatype.XSDint)));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createAnon()));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createURI("urn:test")));
    }
    @Test
    public void testIncludeTest(){
        RdfImportFilter filter = new LiteralLanguageFilter("en,de");
        
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "en", false)));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "de", false)));
        Assert.assertFalse(filter.accept(null, null, 
            Node.createLiteral("test", "fr", false)));
    }
    @Test
    public void testExcludeTest(){
        RdfImportFilter filter = new LiteralLanguageFilter("*,en,!de");
        
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "en", false)));
        Assert.assertFalse(filter.accept(null, null, 
            Node.createLiteral("test", "de", false)));
        Assert.assertTrue(filter.accept(null, null, 
            Node.createLiteral("test", "fr", false)));
    }
    
}
