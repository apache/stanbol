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
package org.apache.stanbol.enhancer.it;

import org.junit.Test;

public class LanguageChainTest extends EnhancerTestBase {

    public LanguageChainTest() {
        super(getChainEndpoint("language"),
            "langdetect","LanguageDetectionEnhancementEngine");
    }
    
    
    @Test
    public void testSimpleEnhancement() throws Exception {
        executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withContent("This Stanbol chain does not detect detect famous cities " +
            		"such as Paris and people such as Bob Marley because it only" +
            		"includes Engines that detect the langauge of the parsed text!")
        )
        .assertStatus(200)
        .assertContentRegexp( // it MUST detect the language
                "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
                "http://purl.org/dc/terms/language.*en")
        .assertContentRegexp(false, //MUST NOT contain because NER is not in this chain
                "http://fise.iks-project.eu/ontology/entity-label.*Paris", //No entitylinking
                "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
                "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley");
        
    }
}
