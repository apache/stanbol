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

    
    
    @Test
    public void testEnhancerConfig() throws IOException{
        executor.execute(
            builder.buildGetRequest(getEndpoint())
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(
            "<rdf:Description rdf:about=\"http://localhost:.*/enhancer\">",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#Enhancer\"/>",
            "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/dbpediaLinking\"/>",
            "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/langid\"/>",
            "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/tika\"/>",
            "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/ner\"/>",
            "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/default\"/>",
            "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/language\"/>",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#EnhancementChain\"/>",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#EnhancementEngine\"/>",
            "<rdfs:label>ner</rdfs:label>",
            "<rdfs:label>language</rdfs:label>"
         );
    }
    @Test
    public void testEngineConfig() throws IOException{
        executor.execute(
            builder.buildGetRequest(getEndpoint()+"/engine")
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(
            "<rdf:Description rdf:about=\"http://localhost:.*/enhancer\">",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#Enhancer\"/>",
            "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/dbpediaLinking\"/>",
            "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/langid\"/>",
            "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/tika\"/>",
            "<j.0:hasEngine rdf:resource=\"http://localhost:.*/enhancer/engine/ner\"/>",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#EnhancementEngine\"/>",
            "<rdfs:label>ner</rdfs:label>"
         );
    }
    @Test
    public void testChainConfig() throws IOException{
        executor.execute(
            builder.buildGetRequest(getEndpoint()+"/chain")
            .withHeader("Accept","application/rdf+xml")
        )
        .assertStatus(200)
        .assertContentRegexp(
            "<rdf:Description rdf:about=\"http://localhost:.*/enhancer\">",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#Enhancer\"/>",
            "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/default\"/>",
            "<j.0:hasChain rdf:resource=\"http://localhost:.*/enhancer/chain/language\"/>",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/enhancer#EnhancementChain\"/>",
            "<rdfs:label>language</rdfs:label>"
        );
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
