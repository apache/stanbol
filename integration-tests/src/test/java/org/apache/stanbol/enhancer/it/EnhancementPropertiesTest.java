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

import java.nio.charset.Charset;

import org.junit.Test;

/** Test that the default chain is called by requesting the "/enhancer" endpoint. */
public class EnhancementPropertiesTest extends EnhancerTestBase {

    private static final Charset UTF8 = Charset.forName("UTF8");

    public EnhancementPropertiesTest(){
        super();
    }
    protected EnhancementPropertiesTest(String endpoint){
        super(endpoint);
    }
    protected EnhancementPropertiesTest(String endpoint,String...assertEngines){
        super(endpoint,assertEngines);
    }
    /**
     * This tests if request scoped enhancement properties are correctly processed
     * by using the <code>enhancer.engines.dereference.languages</code> amd 
     * <code>enhancer.engines.dereference.fields</code> supported by the 
     * Dereference engine (<a href="https://issues.apache.org/jira/browse/STANBOL-1287">
     * STANBOL-1287</a>)
     * @throws Exception
     */
    @Test
    public void testDereferenceEngineProperties() throws Exception {
    	StringBuilder ehProps = new StringBuilder("?");
    	//arabic and russian labels
    	ehProps.append("enhancer.engines.dereference.languages").append('=').append("ar").append('&');
    	ehProps.append("enhancer.engines.dereference.languages").append('=').append("ru").append('&');
    	//only rdfs:label and foaf:depiction (also validates ns prefix support)
    	ehProps.append("enhancer.engines.dereference.fields").append('=').append("rdfs:label").append('&');
    	ehProps.append("enhancer.engines.dereference.fields").append('=').append("foaf:depiction");
        executor.execute(
            builder.buildPostRequest(getEndpoint()+ehProps.toString())
            .withHeader("Accept","text/rdf+nt")
            .withContent("The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.")
        )
        .assertStatus(200)
        .assertCharset(UTF8.name())
        .assertContentRegexp(
                //This expects Paris and Bob marley to be found
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Paris",
                "http://fise.iks-project.eu/ontology/entity-reference.*http://dbpedia.org/resource/Bob_Marley",
                //the Arabic and Russian label of paris
                "http://www.w3.org/2000/01/rdf-schema#label.*\"باريس\"@ar",
                "http://www.w3.org/2000/01/rdf-schema#label.*\"Париж\"@ru",
                //the Arabic and Russian label of Bob Marley
                "http://www.w3.org/2000/01/rdf-schema#label.*\"بوب مارلي\"@ar",
                "http://www.w3.org/2000/01/rdf-schema#label.*\"Марли, Боб\"@ru",
                //foaf:depiction triples for Paris and Bob_Marley
                "http://dbpedia.org/resource/Paris.*http://xmlns.com/foaf/0.1/depiction.*http://upload.wikimedia.org/wikipedia/.*",
                "http://dbpedia.org/resource/Bob_Marley.*http://xmlns.com/foaf/0.1/depiction.*http://upload.wikimedia.org/wikipedia/.*"
                )
        .assertContentRegexp(false,
        		//we do not expect German nor Italian labels
                "http://www.w3.org/2000/01/rdf-schema#label.*\"Paris\"@de",
                "http://www.w3.org/2000/01/rdf-schema#label.*\"Bob Marley\"@de",
                "http://www.w3.org/2000/01/rdf-schema#label.*\"Parigi\"@it",
                "http://www.w3.org/2000/01/rdf-schema#label.*\"Bob Marley\"@it",
                //no rdf:type triples for Paris and Bob_Marley
                "http://dbpedia.org/resource/Paris.*http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "http://dbpedia.org/resource/Bob_Marley.*http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
        		);
    }

}
