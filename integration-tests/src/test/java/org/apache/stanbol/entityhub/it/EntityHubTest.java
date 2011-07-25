/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.entityhub.it;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.commons.web.base.writers.JsonLdSerializerProvider;
import org.apache.stanbol.enhancer.it.EnhancerTestBase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

//inherit from EnhancerTestBase, but we more care about the entityhub readiness than engine's one.
public class EntityHubTest extends EnhancerTestBase {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    final String PARIS_VALUE =
        "{"+
        "'selected': [ " +
        	"'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label', " +
        	"'http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type' " +
        	"], " +
        "'offset': '0', " +
        "'limit': '3', " +
        "'constraints': [{ " +
        	"'type': 'value', " +
        	"'value': 'Paris', " +
        	"'field': 'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label', " +
        	"'dataTypes': ['http:\\/\\/www.iks-project.eu\\/ontology\\/rick\\/model\\/text'] " +
        	"}]" +
        "}";
    
    final String PARIS_TEXT =
        "{"+
        "'selected': [ " +
        	"'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label', " +
        	"'http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type' " +
        	"], " +
        "'offset': '0', " +
        "'limit': '3', " +
        "'constraints': [{ " +
        	"'type': 'text', " +
        	"'text': 'Paris', " +
        	"'patternType' : 'none', " +
        	"'field': 'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label', " +
        	"'dataTypes': ['http:\\/\\/www.iks-project.eu\\/ontology\\/rick\\/model\\/text'] " +
        	"}]" +
        "}";
    
    String[] queryRequests = {PARIS_VALUE,PARIS_TEXT};
    
    @Test
    public void testQueryEndpoint() throws Exception {
    	int i = 0;
    	for(String s:queryRequests){
    		i += 1;
    		RequestExecutor re = executor.execute(
	        		builder.buildPostRequest("/entityhub/sites/query")
	        		.withHeader("Content-Type", "application/json")
	        		.withHeader("Accept", "application/json")
	        		.withContent(s)
	        	) ;
    		
    		log.info("Test request number {}/{} : ",i,queryRequests.length);
    		log.info(re.getContent());
    	
    		re.assertStatus(200)
            .assertContentType("application/json");
    		
    		JSONObject jso = new JSONObject(re.getContent());
    		JSONArray result = jso.getJSONArray("results");
    		Assert.assertNotSame(0, result.length());
    	}
    }
    
    @Test
    public void testSymbolFindEndpoint() throws Exception{
    	RequestExecutor re = executor.execute(
        		builder.buildPostRequest("/entityhub/sites/find")
        		.withHeader("Accept", "application/json")
        		.withContent("name=Paris&lang=de")
        	) ;
		
		log.info("Test request : ");
		log.info(re.getContent());
	
		re.assertStatus(200)
        .assertContentType("application/json");
		
		JSONObject jso = new JSONObject(re.getContent());
		JSONArray result = jso.getJSONArray("results");
		Assert.assertNotSame(0, result.length());
    }
}
