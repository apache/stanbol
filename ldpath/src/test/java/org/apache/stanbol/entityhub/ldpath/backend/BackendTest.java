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
package org.apache.stanbol.entityhub.ldpath.backend;

import static org.apache.stanbol.entityhub.ldpath.LDPathUtils.getReader;
import static org.junit.Assert.assertNotNull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.stanbol.entityhub.ldpath.impl.LDPathTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.LDPath;
import at.newmedialab.ldpath.api.backend.RDFBackend;
import at.newmedialab.ldpath.model.programs.Program;

public class BackendTest extends LDPathTestBase {

    private static final Logger log = LoggerFactory.getLogger(BackendTest.class);
    
    private static final String CONTEXT_PARIS = DBPEDIA+"Paris";
    private static final String DBPEDIA_TEST_PROGRAM;
    static {
        StringBuilder builder = new StringBuilder();
        //TODO:write LDPath test statement (or load it from test resources
        builder.append("@prefix geo : <http://www.w3.org/2003/01/geo/wgs84_pos#> ;");
        builder.append("title = rdfs:label :: xsd:string;");
        builder.append("title_en = rdfs:label[@en] :: xsd:string;");
        builder.append("type = rdf:type :: xsd:anyURI;");
        builder.append("all = * :: xsd:string;");
        builder.append("lat = geo:lat :: xsd:double;");
        DBPEDIA_TEST_PROGRAM = builder.toString();
    }

    private static final Map<String,Collection<?>> EXPECTED_RESULTS_PARIS;
    static {
        Map<String,Collection<?>> expected = new HashMap<String,Collection<?>>();
        expected.put("title_en", new HashSet<String>(Arrays.asList("Paris")));
        expected.put("title", new HashSet<String>(
                Arrays.asList("Paris","Parijs","Parigi","Pariisi","巴黎","Париж")));
        // NOTE: LDPath uses String to represent anyUri
        expected.put("type", new HashSet<String>(Arrays.asList(
            "http://www.w3.org/2002/07/owl#Thing",
            "http://dbpedia.org/ontology/Place",
            "http://dbpedia.org/ontology/PopulatedPlace",
            "http://dbpedia.org/ontology/Settlement",
            "http://www.opengis.net/gml/_Feature",
            "http://dbpedia.org/ontology/Settlement",
            "http://www.opengis.net/gml/_Feature")));
        //Add all previous and some additional to test the WIldcard implementation
        Collection<Object> allValues = new HashSet<Object>();
        for(Collection<?> values : expected.values()){
            allValues.addAll(values);
        }
        allValues.addAll(Arrays.asList(
            "http://dbpedia.org/resource/Category:Capitals_in_Europe",
            "http://dbpedia.org/resource/Category:Host_cities_of_the_Summer_Olympic_Games",
            "2.350833","0.81884754","2193031"));
        expected.put("all", allValues);
        expected.put("lat", Collections.emptySet());
        EXPECTED_RESULTS_PARIS = Collections.unmodifiableMap(expected);
    }
    
    private static final String CONTEXT_HARVARD_ALUMNI = DBPEDIA+"Category:Harvard_University_alumni";
    private static final String CATEGORIES_TEST_PROGRAM;
    static {
        StringBuilder builder = new StringBuilder();
        //TODO:write LDPath test statement (or load it from test resources
        builder.append("name = rdfs:label :: xsd:string;");
        builder.append("parent = skos:broader :: xsd:anyURI;");
        builder.append("childs = ^skos:broader :: xsd:anyURI;");
        builder.append("members = ^<http://purl.org/dc/terms/subject> :: xsd:anyURI;");
        CATEGORIES_TEST_PROGRAM = builder.toString();
    }

    private static final Map<String,Collection<?>> EXPECTED_HARVARD_ALUMNI;
    static {
        Map<String,Collection<?>> expected = new HashMap<String,Collection<?>>();
        expected.put("name", new HashSet<String>(
                Arrays.asList("Harvard University alumni")
                ));
        expected.put("parent", new HashSet<String>(Arrays.asList(
            "http://dbpedia.org/resource/Category:Harvard_University_people",
            "http://dbpedia.org/resource/Category:Alumni_by_university_or_college_in_Massachusetts",
            "http://dbpedia.org/resource/Category:Ivy_League_alumni")
            ));
        expected.put("childs", new HashSet<String>(Arrays.asList(
            "http://dbpedia.org/resource/Category:John_F._Kennedy_School_of_Government_alumni",
            "http://dbpedia.org/resource/Category:Harvard_Law_School_alumni",
            "http://dbpedia.org/resource/Category:Harvard_Medical_School_alumni",
            "http://dbpedia.org/resource/Category:Harvard_Business_School_alumni")
            ));
        expected.put("members", new HashSet<String>(Arrays.asList(
            "http://dbpedia.org/resource/Edward_Said",
            "http://dbpedia.org/resource/Cole_Porter", 
            "http://dbpedia.org/resource/Theodore_Roosevelt",
            "http://dbpedia.org/resource/Al_Gore",
            "http://dbpedia.org/resource/T._S._Eliot",
            "http://dbpedia.org/resource/Henry_Kissinger",
            "http://dbpedia.org/resource/Robert_F._Kennedy",
            "http://dbpedia.org/resource/Benjamin_Netanyahu",
            "http://dbpedia.org/resource/Natalie_Portman",
            "http://dbpedia.org/resource/John_F._Kennedy",
            "http://dbpedia.org/resource/Michelle_Obama",
            "http://dbpedia.org/resource/Jacques_Chirac",
            "http://dbpedia.org/resource/Pierre_Trudeau",
            "http://dbpedia.org/resource/Jack_Lemmon",
            "http://dbpedia.org/resource/Franklin_D._Roosevelt",
            "http://dbpedia.org/resource/John_Adams") // and manny more
            ));
        EXPECTED_HARVARD_ALUMNI = Collections.unmodifiableMap(expected);
    }    

    @Override
    protected Collection<String> checkContexts() {
        return Arrays.asList(CONTEXT_PARIS,CONTEXT_HARVARD_ALUMNI);
    }
    /**
     * Test {@link RDFBackend} implementation including WildCard
     * @throws Exception
     */
    @Test
    public void testLDPath() throws Exception {
        LDPath<Object> ldPath = new LDPath<Object>(backend);
        Program<Object> program = ldPath.parseProgram(
            getReader(DBPEDIA_TEST_PROGRAM));
        assertNotNull("parsed Programm is null (Input: "+
            DBPEDIA_TEST_PROGRAM+")", program);
        log.info("LDPath Programm:\n{}",program.getPathExpression(backend));
        Object context = backend.createURI(CONTEXT_PARIS);
        Map<String,Collection<?>> result = program.execute(backend, context);
        log.info("Results for {}:\n{}",CONTEXT_PARIS,result);
        log.info("Assert LDPath Result for {}:", CONTEXT_PARIS);
        assertLDPathResult(result,EXPECTED_RESULTS_PARIS);
    }
    @Test
    public void testInversePath() throws Exception {
        LDPath<Object> ldPath = new LDPath<Object>(backend);
        Program<Object> program = ldPath.parseProgram(
            getReader(CATEGORIES_TEST_PROGRAM));
        assertNotNull("parsed Programm is null (Input: "+
            CATEGORIES_TEST_PROGRAM+")", program);
        log.info("LDPath Programm:\n{}",program.getPathExpression(backend));
        Object context = backend.createURI(CONTEXT_HARVARD_ALUMNI);
        Map<String,Collection<?>> result = program.execute(backend, context);
        log.info("Results for {}:\n{}",CONTEXT_HARVARD_ALUMNI,result);
        assertNotNull("The result of the LDPath execution MUST NOT be NULL " +
                "(entity: %s)",result);
        log.info("Assert LDPath Result for {}:", EXPECTED_HARVARD_ALUMNI);
        assertLDPathResult(result,EXPECTED_HARVARD_ALUMNI);
    }
}
