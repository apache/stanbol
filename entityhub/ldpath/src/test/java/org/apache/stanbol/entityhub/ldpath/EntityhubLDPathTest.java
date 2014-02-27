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
package org.apache.stanbol.entityhub.ldpath;

import static org.apache.stanbol.entityhub.ldpath.LDPathUtils.getReader;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.ldpath.impl.LDPathTestBase;
import org.apache.stanbol.entityhub.ldpath.transformer.ValueConverterTransformerAdapter;
import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityhubLDPathTest extends LDPathTestBase {

    private static final Logger log = LoggerFactory.getLogger(EntityhubLDPathTest.class);
    
    private static final ValueFactory vf = InMemoryValueFactory.getInstance();
    
    private static final String CONTEXT_LONDON = DBPEDIA+"London";
    
    private static final String DATA_TYPE_TEST_PROGRAM;
    static {
        StringBuilder builder = new StringBuilder();
        //NOTE: prefixes removed to test registration of the default namespaces
        //      as registered in the NamespaceEnum
        //builder.append("@prefix eh : <http://www.iks-project.eu/ontology/rick/model/>;");
        //builder.append("@prefix dct : <http://purl.org/dc/terms/>;");
        //builder.append("@prefix geo : <http://www.w3.org/2003/01/geo/wgs84_pos#> ;");
        //this test that even when selecting strings the language is preserved
        builder.append("name = rdfs:label :: xsd:string;");
        //this tests support for natural language texts as used by the entityhub
        builder.append("comment = rdfs:comment :: entityhub:text;");
        //this tests that Reference is used for URIs
        builder.append("categories = dc:subject :: xsd:anyURI;");
        //this tests support for Reference as used by the entityhub
        builder.append("type = rdf:type :: entityhub:ref;");
        builder.append("lat = geo:lat :: xsd:double;");
        DATA_TYPE_TEST_PROGRAM = builder.toString();
        
    }
    /**
     * This expects {@link Text} and {@link Reference} instances as results
     * of the execution.
     */
    private static final Map<String,Collection<?>> EXPECTED_RESULTS_LONDON;
    static {
        Map<String,Collection<?>> expected = new HashMap<String,Collection<?>>();
        expected.put("name", new HashSet<Text>(Arrays.asList(
            vf.createText("London", "en"),
            vf.createText("London","de"),
            vf.createText("Londres","pt"),
            vf.createText("Londra","tr"),
            vf.createText("Лондон","ru"),
            vf.createText("伦敦","zh")
            )));
        expected.put("comment", new HashSet<Text>(Arrays.asList(
            vf.createText("London Listen/ˈlʌndən/ is the capital city of England" +
            		" and the United Kingdom, the largest metropolitan area in the" +
            		" United Kingdom, and the largest urban zone in the European " +
            		"Union by most measures. Located on the River Thames, London " +
            		"has been a major settlement for two millennia, its history " +
            		"going back to its founding by the Romans, who named it " +
            		"Londinium. London's ancient core, the City of London, largely " +
            		"retains its square-mile mediaeval boundaries.","en"))));
        // NOTE: LDPath uses String to represent anyUri
        expected.put("categories", new HashSet<Reference>(Arrays.asList(
            vf.createReference("http://dbpedia.org/resource/Category:London"),
            vf.createReference("http://dbpedia.org/resource/Category:British_capitals"),
            vf.createReference("http://dbpedia.org/resource/Category:Populated_places_established_in_the_1st_century"),
            vf.createReference("http://dbpedia.org/resource/Category:Staple_ports"),
            vf.createReference("http://dbpedia.org/resource/Category:Articles_including_recorded_pronunciations_(UK_English)"),
            vf.createReference("http://dbpedia.org/resource/Category:Capitals_in_Europe"),
            vf.createReference("http://dbpedia.org/resource/Category:Host_cities_of_the_Commonwealth_Games"),
            vf.createReference("http://dbpedia.org/resource/Category:Host_cities_of_the_Summer_Olympic_Games"),
            vf.createReference("http://dbpedia.org/resource/Category:Port_cities_and_towns_in_the_United_Kingdom"),
            vf.createReference("http://dbpedia.org/resource/Category:Arthurian_locations"),
            vf.createReference("http://dbpedia.org/resource/Category:Robin_Hood_locations")
            )));
        expected.put("type", new HashSet<Reference>(Arrays.asList(
            vf.createReference("http://www.w3.org/2002/07/owl#Thing"),
            vf.createReference("http://www.opengis.net/gml/_Feature"),
            vf.createReference("http://dbpedia.org/ontology/Settlement"),
            vf.createReference("http://dbpedia.org/ontology/PopulatedPlace"),
            vf.createReference("http://dbpedia.org/ontology/Place")
            )));
        expected.put("lat", Collections.emptySet());
        EXPECTED_RESULTS_LONDON = Collections.unmodifiableMap(expected);
    }
    
    @Override
    protected Collection<String> checkContexts() {
        return Arrays.asList(CONTEXT_LONDON);
    }
    /**
     * Tests that the {@link LDPathUtils#createAndInitLDPath(RDFBackend, ValueFactory)}
     * correctly registers the {@link ValueConverterTransformerAdapter} for
     * {@link DataTypeEnum#Reference}, {@link DataTypeEnum#Text},
     * {@link DataTypeEnum#AnyUri} and {@link DataTypeEnum#String}.
     */
    @Test
    public void testTransformers() throws Exception {
        EntityhubLDPath ldPath = new EntityhubLDPath(backend);
        Program<Object> program = ldPath.parseProgram(
            getReader(DATA_TYPE_TEST_PROGRAM));
        assertNotNull("The Program MUST NOT be NULL", program);
        Map<String,Collection<?>> result = program.execute(backend, 
            vf.createReference(CONTEXT_LONDON));
        log.info("Results for {}:\n{}",CONTEXT_LONDON,result);
        log.info("Assert LDPath Result for {}:", CONTEXT_LONDON);
        assertLDPathResult(result, EXPECTED_RESULTS_LONDON);
    }
    @Test
    public void testReprentationMappings() throws Exception {
        EntityhubLDPath ldPath = new EntityhubLDPath(backend);
        Program<Object> program = ldPath.parseProgram(
            getReader(DATA_TYPE_TEST_PROGRAM));
        assertNotNull("The Program MUST NOT be NULL", program);
        Representation result = ldPath.execute(
            vf.createReference(CONTEXT_LONDON), program);
        assertEquals("The id of the Representation '"+
            result.getId()+"' is not the same as the parsed Context '"+
            CONTEXT_LONDON+"'!", CONTEXT_LONDON, result.getId());
        Iterator<Entry<String,Collection<?>>> entryIt = cloneExpected(EXPECTED_RESULTS_LONDON).entrySet().iterator();
        while(entryIt.hasNext()){
            Entry<String,Collection<?>> entry = entryIt.next();
            Iterator<Object> valueIt = result.get(entry.getKey());
            assertNotNull("The result is missing the expected field '"+
                entry.getKey()+"'!",valueIt);
            Collection<Object> values = ModelUtils.asCollection(valueIt);
            entry.getValue().removeAll(values);
            assertTrue("The following expected values "+
                entry.getValue()+"' are missing (present: "+
                values+")!",entry.getValue().isEmpty());
        }
    }
}
