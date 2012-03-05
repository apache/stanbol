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
package org.apache.stanbol.enhancer.it;

import org.apache.stanbol.commons.testing.http.RequestDocumentor;
import org.junit.Test;

/** Test that the default chain is called by requesting the "/enhancer" endpoint. */
public class DefaultChainTest extends EnhancerTestBase {
    
    
    private final RequestDocumentor documentor = new RequestDocumentor(getClass().getName());
    /**
     * Contains values grouped by three elements: Accept header, 
     * Expected content-type, Expected regexp
     */
    public final static String [] ACCEPT_FORMAT_TEST_DATA  = new String[] {
        "application/json",
        "application/json", //now JSON LD uses application/json
        "\"creator\": \"org.apache.stanbol.enhancer.engines.langid.LangIdEnhancementEngine\",",
        
        "application/rdf+xml",
        "application/rdf+xml",
        "xmlns:rdf=.http://www.w3.org/1999/02/22-rdf-syntax-ns",
    
        "application/rdf+json", 
        "application/rdf+json", 
        "\\{.*value.*ontology.*TextAnnotation.*type.*uri.*}",
    
        "text/turtle", 
        "text/turtle", 
        "a.*ontology/TextAnnotation.*ontology/Enhancement.*;",
    
        "text/rdf+nt", 
        "text/rdf+nt", 
        "<urn:enhancement.*www.w3.org/1999/02/22-rdf-syntax-ns#type.*ontology/TextAnnotation>",
    };
    
    public DefaultChainTest(){
        super();
    }
    protected DefaultChainTest(String endpoint){
        super(endpoint);
    }
    protected DefaultChainTest(String endpoint,String...assertEngines){
        super(endpoint,assertEngines);
    }
    
    @Test
    public void testSimpleEnhancement() throws Exception {
        executor.execute(
            builder.buildPostRequest(getEndpoint()+"?executionmetadata=true")
            .withHeader("Accept","text/rdf+nt")
            .withContent("The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.")
        )
        .assertStatus(200)
        .assertContentRegexp(
                //check execution metadata
                "http://stanbol.apache.org/ontology/enhancer/executionmetadata#executionPart",
                //check execution of tika & if executionPlan is included
                "http://stanbol.apache.org/ontology/enhancer/executionplan#engine.*tika", 
                "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
                "http://purl.org/dc/terms/language.*en",
                "http://fise.iks-project.eu/ontology/entity-label.*Paris",
                "http://purl.org/dc/terms/creator.*org.apache.stanbol.enhancer.engines.opennlp.*EngineCore",
                "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley",
                //the following two lines test the use of plain literals (see STANBOL-509)
                "http://fise.iks-project.eu/ontology/selected-text.*\"Bob Marley\"@en",
                "http://fise.iks-project.eu/ontology/selection-context>.*people such as Bob Marley.\"@en"
                )
        .generateDocumentation(
                documentor,
                "title", 
                "Stateless text analysis",
                "description", 
                "A POST request to ${request.path} (TODO should be replaced by actual path) returns triples representing enhancements "
                + " of the POSTed text. Output format is defined by the Accept header."
        );
    }
    
    @Test
    public void testOutputFormats() throws Exception {
        for (int i = 0; i < ACCEPT_FORMAT_TEST_DATA.length; i += 3) {
            executor.execute(
                    builder.buildPostRequest(getEndpoint())
                    .withHeader("Accept", ACCEPT_FORMAT_TEST_DATA[i])
                    .withContent("Nothing")
            )
            .assertStatus(200)
            .assertContentType(ACCEPT_FORMAT_TEST_DATA[i+1])
            .assertContentRegexp(ACCEPT_FORMAT_TEST_DATA[i+2])
            .generateDocumentation(documentor,
                    "title", "Output format: " + ACCEPT_FORMAT_TEST_DATA[i],
                    "description", "Demonstrate " + ACCEPT_FORMAT_TEST_DATA[i] + " output"
                    );
        }
    }
    
    @Test
    public void testInvalidFormat() throws Exception {
        executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept", "INVALID_FORMAT")
            .withContent("Nothing")
        )
        .assertStatus(500);
    }
}
