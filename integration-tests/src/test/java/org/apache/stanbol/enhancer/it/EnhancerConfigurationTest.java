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

import java.io.IOException;

import org.junit.Test;

public class EnhancerConfigurationTest extends EnhancerTestBase {

    public static final String[] EXPECTED_ENGINES = new String[]{
        "<rdf:Description rdf:about=\"http://localhost:.*/enhancer\">",
        "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#Enhancer\"/>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/dbpediaLinking\"/>",
        "<rdfs:label>dbpediaLinking</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/langid\"/>",
        "<rdfs:label>langid</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/langdetect\"/>",
        "<rdfs:label>langdetect</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/tika\"/>",
        "<rdfs:label>tika</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/opennlp-sentence\"/>",
        "<rdfs:label>opennlp-sentence</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/opennlp-token\"/>",
        "<rdfs:label>opennlp-token</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/opennlp-pos\"/>",
        "<rdfs:label>opennlp-pos</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/opennlp-ner\"/>",
        "<rdfs:label>opennlp-ner</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/opennlp-chunker\"/>",
        "<rdfs:label>opennlp-chunker</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/sentiment-wordclassifier\"/>",
        "<rdfs:label>sentiment-wordclassifier</rdfs:label>",
        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/xmpextractor\"/>",
        "<rdfs:label>xmpextractor</rdfs:label>",
//NOT AVAILABLE DURING TESTS, BECAUSE OF OFFLINE MODE!
//        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/dbpspotlightdisambiguate\"/>",
//        "<rdfs:label>dbpspotlightdisambiguate</rdfs:label>",
//        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/dbpspotlightannotate\"/>",
//        "<rdfs:label>dbpspotlightannotate</rdfs:label>",
//        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/dbpspotlightcandidates\"/>",
//        "<rdfs:label>dbpspotlightcandidates</rdfs:label>",
//        "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/dbpspotlightspot\"/>",
//        "<rdfs:label>dbpspotlightspot</rdfs:label>",
        "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#EnhancementEngine\"/>"
    };
    public static final String[] EXPECTED_CHAINS = new String[]{
        "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/default\"/>",
        "<rdfs:label>default</rdfs:label>",
        "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/language\"/>",
        "<rdfs:label>language</rdfs:label>",
        "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/dbpedia-proper-noun\"/>",
        "<rdfs:label>dbpedia-proper-noun</rdfs:label>",
        "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/dbpedia-spotlight\"/>",
        "<rdfs:label>dbpedia-spotlight</rdfs:label>",
        "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/all-active\"/>",
        "<rdfs:label>all-active</rdfs:label>",
        "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#EnhancementChain\"/>",    
    };
    
    public static final String[] EXPECTED_CONFIG = new String[EXPECTED_CHAINS.length+EXPECTED_ENGINES.length];
    static {
        System.arraycopy(EXPECTED_CHAINS, 0, EXPECTED_CONFIG, 0, EXPECTED_CHAINS.length);
        System.arraycopy(EXPECTED_ENGINES, 0, EXPECTED_CONFIG, EXPECTED_CHAINS.length, EXPECTED_ENGINES.length);
    }
    
    @Test
    public void testEnhancerConfig() throws IOException{
        executor.execute(
            builder.buildGetRequest(getEndpoint())
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(EXPECTED_CONFIG);
    }
    @Test
    public void testEngineConfig() throws IOException{
        executor.execute(
            builder.buildGetRequest(getEndpoint()+"/engine")
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(EXPECTED_ENGINES);
    }
    @Test
    public void testChainConfig() throws IOException{
        executor.execute(
            builder.buildGetRequest(getEndpoint()+"/chain")
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(EXPECTED_CHAINS);
    }
    @Test
    public void testSparqlConfig() throws IOException{
        StringBuilder query = new StringBuilder();
        query.append("PREFIX enhancer: <http://stanbol.apache.org/ontology/enhancer/enhancer#>");
        query.append("PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#>");
        query.append("SELECT distinct ?name ?chain ");
        query.append("WHERE {");
        query.append("?chain a enhancer:EnhancementChain .");
        query.append("?chain rdfs:label ?name .");
        query.append("}");
        query.append("ORDER BY ASC(?name)");
        executor.execute(
            builder.buildGetRequest(getEndpoint()+"/sparql","query",query.toString())
            .withHeader("Accept","application/sparql-results+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(
            "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\">",
            "<head>",
            "<variable name=\"chain\"/>",
            "<variable name=\"name\"/>",
            "</head>",
            "<results>",
            "<result>",
            "<binding name=\"chain\">",
            "<uri>http://localhost:.*/enhancer/chain/default</uri>",
            "<uri>http://localhost:.*/enhancer/chain/language</uri>",
            "<uri>http://localhost:.*/enhancer/chain/dbpedia-proper-noun</uri>",
            "<binding name=\"name\">",
            "<literal>default</literal>",
            "<literal>language</literal>"
        );
    }
    
    public void testExecutionPlan() throws IOException{
        //We need not to validate the executionplan data.
        //This is already done by other tests. 
        //only check for the rdf:types to check if the correct RDF data are returned
        String [] validate = new String[]{
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionplan#ExecutionNode\"/>",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionplan#ExecutionPlan\"/>"
        };
        executor.execute(
            builder.buildGetRequest(getEndpoint()+"/ep")
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(validate);
        executor.execute(
            builder.buildGetRequest(getEndpoint()+"/chain/language/ep")
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(validate);
        executor.execute(
            builder.buildGetRequest(getEndpoint()+"/chain/engine/tika")
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(validate);
        
    }
    
}
