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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.impl.NamespacePrefixProviderImpl;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;

public class PropertyPrefixFilterTest {
    
    private static final String FB = "http://rdf.freebase.com/ns/";

    private static final String TEST_CONFIG = "prefix.config";
    
    
    private static NamespacePrefixProvider nsPrefixProvider;

    private static final Map<String,String> nsMappings = new HashMap<String,String>();
    static {
        nsMappings.put("fb", FB);
        nsMappings.put("rdf", NamespaceEnum.rdf.getNamespace());
        nsMappings.put("rdfs", NamespaceEnum.rdfs.getNamespace());
        nsMappings.put("skos", NamespaceEnum.skos.getNamespace());
    }
    
    private static List<String> configLines;
    
    private RdfImportFilter importFilter;
    
    @BeforeClass
    public static void init() throws IOException{
        nsPrefixProvider = new NamespacePrefixProviderImpl(nsMappings);
        InputStream in = PropertyPrefixFilterTest.class.getClassLoader().getResourceAsStream(TEST_CONFIG);
        Assert.assertNotNull("Unable to read test config",in);
        configLines = IOUtils.readLines(in, "UTF-8");
    }
    
    @Before
    public void createImportFilter(){
        importFilter = new PropertyPrefixFilter(nsPrefixProvider, configLines);
    }
    
    @Test
    public void testMappings(){
        Node subject = Node.createURI("urn:subject");
        Node value = Node.createURI("urn:value");
        
        Node rdfType = Node.createURI(NamespaceEnum.rdf+"type");
        Assert.assertTrue(importFilter.accept(subject,rdfType,value));

        Node rdfsLabel = Node.createURI(NamespaceEnum.rdfs+"label");
        Assert.assertTrue(importFilter.accept(subject,rdfsLabel,value));

        Node guid = Node.createURI(FB+"type.object.guid");
        Assert.assertFalse(importFilter.accept(subject,guid,value));
        
        Node permission = Node.createURI(FB+"type.object.permission");
        Assert.assertFalse(importFilter.accept(subject,permission,value));
        
        Node name = Node.createURI(FB+"type.object.name");
        Assert.assertTrue(importFilter.accept(subject,name,value));
        
        Node description = Node.createURI(FB+"type.object.description");
        Assert.assertTrue(importFilter.accept(subject,description,value));

        Node dummy = Node.createURI(FB+"type.dummy");
        Assert.assertFalse(importFilter.accept(subject,dummy,value));
        
        Node typePlain = Node.createURI(FB+"type");
        Assert.assertFalse(importFilter.accept(subject,typePlain,value));
        
        Node other = Node.createURI(NamespaceEnum.cc+"license");
        Assert.assertFalse(importFilter.accept(subject,other,value));
    }

}
