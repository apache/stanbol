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
package org.apache.stanbol.commons.httpqueryheaders.it;

import static org.apache.stanbol.enhancer.it.DefaultChainTest.ACCEPT_FORMAT_TEST_DATA;

import org.apache.http.client.methods.HttpPost;
import org.apache.stanbol.enhancer.it.EnhancerTestBase;
import org.junit.Test;

public class HttpQueryHeaderPostTest extends EnhancerTestBase {
    
    @Test
    public void testSetAccept() throws Exception {
        for (int i = 0; i < ACCEPT_FORMAT_TEST_DATA.length; i += 3) {
            executor.execute(
                    builder.buildOtherRequest(new HttpPost(
                        builder.buildUrl(getEndpoint(), 
                            "header_Accept",ACCEPT_FORMAT_TEST_DATA[i])))
                    .withContent("Nothing")
            )
            .assertStatus(200)
            .assertContentType(ACCEPT_FORMAT_TEST_DATA[i+1])
            .assertContentRegexp(ACCEPT_FORMAT_TEST_DATA[i+2]);
        }

    }
    @Test
    public void testOverrideAccept() throws Exception {
        for (int i = 0; i < ACCEPT_FORMAT_TEST_DATA.length; i += 3) {
            executor.execute(
                    builder.buildOtherRequest(new HttpPost(
                        builder.buildUrl(getEndpoint(), 
                            "header_Accept",ACCEPT_FORMAT_TEST_DATA[i])))
                    //use an other Accept header
                    .withHeader("Accept", ACCEPT_FORMAT_TEST_DATA[(i+3)%ACCEPT_FORMAT_TEST_DATA.length])
                    .withContent("Nothing")
            )
            .assertStatus(200)
            .assertContentType(ACCEPT_FORMAT_TEST_DATA[i+1])
            .assertContentRegexp(ACCEPT_FORMAT_TEST_DATA[i+2]);
        }

    }
    @Test
    public void testRemoveAccept() throws Exception {
        executor.execute(
            builder.buildOtherRequest(new HttpPost(
                builder.buildUrl(getEndpoint(), 
                "header_Accept",""))) //override the parse Accept Header
            .withHeader("Accept","text/turtle") //set Accept to turtle (overridden) 
            .withContent("John Smith was born in London. But since ten years he " +
            		"works for the Smith Coorperation and lives in Paris.")
        )
        .assertStatus(200)
        //check for JSON-LD (the default content type
        .assertContentType("application/ld+json")
        .assertContentContains(
            "\"http://fise.iks-project.eu/ontology/entity-reference\" : [ {",
            "\"@id\" : \"http://dbpedia.org/resource/London\"",
            "\"http://purl.org/dc/terms/creator\" : [ {",
            "\"@value\" : \"org.apache.stanbol.enhancer.engines.langdetect.LanguageDetectionEnhancementEngine\"",
            "\"@value\" : \"org.apache.stanbol.enhancer.engines.entitytagging.impl.NamedEntityTaggingEngine\"");
    }
}
