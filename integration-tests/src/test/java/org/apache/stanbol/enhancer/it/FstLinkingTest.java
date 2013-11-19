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

public class FstLinkingTest extends EnhancerTestBase {

    //NOTE: adapted text as part of STANBOL-1211 to avoid a single noun phrase 
    //"SPD candidate Peer Steinbrueck" avoiding the linking of SPD in this
    //Text.
    public static final String TEST_TEXT = "There has been a worried response in "
            + "Greece to the Sunday's election in Germany. The win of Chancellor "
            + "Angela Merkel means that there will not be a radical change in "
            + "European policy. Greeks would have preferred Peer Steinbrueck the"
            + "candidate of the SPD, whose party lost Sunday.";
    
    /**
     * 
     */
    public FstLinkingTest() {
        super(getChainEndpoint("dbpedia-fst-linking"), 
            "langdetect"," LanguageDetectionEnhancementEngine",
            "opennlp-sentence"," OpenNlpSentenceDetectionEngine",
            "opennlp-token"," OpenNlpTokenizerEngine",
            "opennlp-pos","OpenNlpPosTaggingEngine",
            "dbpedia-fst-linking","FstLinkingEngine");
    }
    
    
    @Test
    public void testFstLinkingEnhancement() throws Exception {
        executor.execute(
            builder.buildPostRequest(getEndpoint())
            .withHeader("Accept","text/rdf+nt")
            .withContent(TEST_TEXT)
        )
        .assertStatus(200)
        .assertContentRegexp( // it MUST detect the language
                "http://purl.org/dc/terms/creator.*LanguageDetectionEnhancementEngine",
                "http://purl.org/dc/terms/language.*en",
                //and the entityLinkingEngine
                "http://purl.org/dc/terms/creator.*FstLinkingEngine",
                //needs to suggest the following Entities
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Angela_Merkel",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Greece",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Germany",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Social_Democratic_Party_of_Germany",
                //for the following sections within the text
                "http://fise.iks-project.eu/ontology/selected-text.*Angela Merkel",
                "http://fise.iks-project.eu/ontology/selected-text.*Greece",
                "http://fise.iks-project.eu/ontology/selected-text.*Germany",
                "http://fise.iks-project.eu/ontology/selected-text.*SPD")
         //with STANBOL-1211 Chancellor MUST NOT be found as "Chancellor" does not
         //select more as 50% of the tokens of the chunk "Chancellor Angela Merkel"
         .assertContentRegexp(false, 
                 "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Chancellor",
                 "http://fise.iks-project.eu/ontology/selected-text.*Chancellor");
    }

    
}
