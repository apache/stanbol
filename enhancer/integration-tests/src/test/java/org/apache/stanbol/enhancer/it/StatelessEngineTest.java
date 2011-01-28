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

import org.junit.Test;

/** Test the stateless text enhancement engines */
public class StatelessEngineTest extends StanbolTestBase {
    
    @Test
    public void testSimpleEnhancement() throws Exception {
        executor.execute(
            builder.buildPostRequest("/engines")
            .withHeader("Accept","text/rdf+nt")
            .withContent("The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.")
        )
        .assertStatus(200)
        .assertContentRegexp(
                "http://purl.org/dc/terms/creator.*MetaxaEngine",
                "http://purl.org/dc/terms/creator.*LangIdEnhancementEngine",
                "http://purl.org/dc/terms/language.*en",
                "http://purl.org/dc/terms/creator.*LocationEnhancementEngine",
                "http://fise.iks-project.eu/ontology/entity-label.*Paris",
                "http://purl.org/dc/terms/creator.*NamedEntityExtractionEnhancementEngine",
                "http://fise.iks-project.eu/ontology/entity-label.*Bob Marley"
                );
    }
    
    @Test
    public void testOutputFormats() throws Exception {
        final String [] formats = {
            // Each group of 3 elements is: Accept header, Expected content-type, Expected regexp     
            "application/json",
            "application/rdf+json", 
            "\\{.*http.*ontology.*confidence.*:",
            
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
        
        for(int i=0 ; i < formats.length; i+=3) {
            executor.execute(
                    builder.buildPostRequest("/engines")
                    .withHeader("Accept", formats[i])
                    .withContent("Nothing")
            )
            .assertStatus(200)
            .assertContentType(formats[i+1])
            .assertContentRegexp(formats[i+2]);
        }
    }
    
    @Test
    public void testInvalidFormat() throws Exception {
        executor.execute(
            builder.buildPostRequest("/engines")
            .withHeader("Accept", "INVALID_FORMAT")
            .withContent("Nothing")
        )
        .assertStatus(500);
    }
}
