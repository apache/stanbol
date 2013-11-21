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

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** https://issues.apache.org/jira/browse/STANBOL-943: Add integration-test for the disambiguation-mlt engine */
public class DbpediaDisambiguationChainTest extends EnhancerTestBase {
	
	private final Logger log = LoggerFactory.getLogger(DbpediaDisambiguationChainTest.class);
    
    /* This chains included for testing
         - NLP processing 
         - EntityLinking configured for disambiguation 
         - Disambiguation MLT 
   */
    public DbpediaDisambiguationChainTest() {
        super(getChainEndpoint("dbpedia-disambiguation"),
            "langdetect"," LanguageDetectionEnhancementEngine",
            "opennlp-sentence"," OpenNlpSentenceDetectionEngine",
            "opennlp-token"," OpenNlpTokenizerEngine",
            "opennlp-pos","OpenNlpPosTaggingEngine",
            "opennlp-chunker","OpenNlpChunkingEngine",
            "dbpedia-disamb-linking","EntityLinkingEngine",
            "disambiguation-mlt","DisambiguatorEngine");
    }
    
    @Test
    public void testSimpleEnhancement() throws Exception {
    	String content = "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.";
    	log.info("Testing Disambiguation-MLT with sentence: " + content);
        executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withContent(content)
        ).assertStatus(200)
        .assertContentType("text/rdf+nt")
        .assertContentRegexp(                 
                // Detect the language
                "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
                "http://purl.org/dc/terms/language.*en",
                
                // EntityLinkingEngine
                "http://purl.org/dc/terms/creator.*EntityLinkingEngine",
                
                // Suggestions for possible ambiguations  
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Paris_M%C3%A9tro",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Bob_Marley_&_The_Wailers",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Salon_\\(Paris\\)",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Paris",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Bob_Marley",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Paris_Opera",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Paris_Commune",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Paris_Masters",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Paris_Hilton",

                // Selected text by disambiguation-mlt engine
                "http://fise.iks-project.eu/ontology/selected-text.*Bob Marley",
                "http://fise.iks-project.eu/ontology/selected-text.*Paris"
                );        
    }
}

