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
package org.apache.stanbol.commons.namespaceprefix.provider.stanbol;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.namespaceprefix.mappings.DefaultNamespaceMappingsEnum;
import org.apache.stanbol.commons.namespaceprefix.service.StanbolNamespacePrefixService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultProviderTest {

    private static NamespacePrefixService service;

    @BeforeClass
    public static void init() throws IOException {
        //this test for now does not use predefined mappings
        URL mappingURL = DefaultProviderTest.class.getClassLoader()
                .getResource("testnamespaceprefix.mappings");
        //Assert.assertNotNull(mappingURL);
        File mappingFile;
        if(mappingURL != null){
            try {
              mappingFile = new File(mappingURL.toURI());
            } catch(URISyntaxException e) {
              mappingFile = new File(mappingURL.getPath());
            }
            //Assert.assertTrue(mappingFile.isFile());
        } else {
            mappingFile = new File("testnamespaceprefix.mappings");
        }
        service = new StanbolNamespacePrefixService(mappingFile);
    }

    @Test
    public void testDefaultMappings(){
        //this tests both the implementation of the getNamespace and getPrefix mappings
        //and that the defaultNamespaceMappingProvider is correctly loaded by the ServiceLoader 
        for(DefaultNamespaceMappingsEnum defaultMapping : DefaultNamespaceMappingsEnum.values()){
            Assert.assertEquals(defaultMapping.getNamespace(), 
                service.getNamespace(defaultMapping.getPrefix()));
            Assert.assertTrue(service.getPrefixes(defaultMapping.getNamespace()).contains(defaultMapping.getPrefix()));
        }
    }
}
