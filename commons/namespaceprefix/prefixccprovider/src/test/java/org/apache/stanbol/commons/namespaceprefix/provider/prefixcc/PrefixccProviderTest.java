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
package org.apache.stanbol.commons.namespaceprefix.provider.prefixcc;


import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.namespaceprefix.service.StanbolNamespacePrefixService;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixccProviderTest {
    
    private static final Logger log = LoggerFactory.getLogger(PrefixccProviderTest.class);

    private static String foaf_ns = "http://xmlns.com/foaf/0.1/";
    private static String foaf_pf = "foaf";
    
        
    @Test
    public void test(){
        Date date = new Date();
        PrefixccProvider pcp = new PrefixccProvider(10,TimeUnit.SECONDS);
        Assert.assertNull(pcp.getPrefix(foaf_ns)); //async loading
        for(int i =0; i<5 && !pcp.isAvailable();i++){
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
        if(!pcp.isAvailable()){
            log.warn("Unable to obtain prefix.cc data after 5sec .. skipping further tests");
            return;
        }
        //assertMappings
        Assert.assertEquals(foaf_pf, pcp.getPrefix(foaf_ns));
        Assert.assertEquals(foaf_ns, pcp.getNamespace(foaf_pf));
        //assert cache time stamp
        Date cacheDate = pcp.getCacheTimeStamp();
        Assert.assertTrue(date.compareTo(cacheDate) == -1);
        Assert.assertTrue(new Date().compareTo(cacheDate) >= 0);
        //assert close
        pcp.close();
        Assert.assertFalse(pcp.isAvailable());
        Assert.assertNull(pcp.getCacheTimeStamp());
    }
    
    @Test
    public void testServiceLoader() throws IOException{
        
    	// Check if the service is down
    	PrefixccProvider pcp = new PrefixccProvider(10,TimeUnit.SECONDS);
    	if(!pcp.isAvailable()){
    		log.info("Unable to retrieve prefixes from http://prefix.cc ... deactivating "
    	               + PrefixccProvider.class.getSimpleName()+ "ServiceLoader support test");
    	           return;
    	}
    	
    	//this test works only if online
        try {
            PrefixccProvider.GET_ALL.getInputStream();
        } catch (IOException e) {
           log.info("Unable to connect to http://prefix.cc ... deactivating "
               + PrefixccProvider.class.getSimpleName()+ "ServiceLoader support test");
           return;
        }
        
        //this test for now does not use predefined mappings
        
        URL mappingURL = PrefixccProviderTest.class.getClassLoader()
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
        NamespacePrefixService service = new StanbolNamespacePrefixService(mappingFile);
        //assertMappings
        Assert.assertEquals(foaf_pf, service.getPrefix(foaf_ns));
        Assert.assertEquals(foaf_ns, service.getNamespace(foaf_pf));
        
    }
    
}
