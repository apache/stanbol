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
package org.apache.stanbol.entityhub.it.query;

import java.io.IOException;
import java.util.Arrays;

import org.apache.stanbol.entityhub.it.ReferencedSiteTest;
import org.apache.stanbol.entityhub.it.SitesManagerTest;
import org.apache.stanbol.entityhub.test.query.FieldQueryTestCase;
import org.apache.stanbol.entityhub.test.query.FindQueryTestCase;
import org.apache.stanbol.entityhub.test.query.QueryTestBase;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.slf4j.LoggerFactory;
/**
 * Tests for the "/query" (Field Query) and "/find" (label search) 
 * implementation of the Entityhub.<p>
 * All the tests defined by this class assume the default data set for 
 * dbpedia as provided by the 
 * <code>org.apache.stanbol.data.sites.dbpedia.default</code> bundle. <p>
 * This test cases are used to test both the ReferencedSiteManager and 
 * the ReferencedSite. This is also the reason why having this abstract super
 * class defining the tests.
 * @see ReferencedSiteTest
 * @see SitesManagerTest
 * @author Rupert Westenthaler
 *
 */
public abstract class DbpediaQueryTest extends QueryTestBase {

    
    public DbpediaQueryTest(String path,String referencedSite) {
        super(path,referencedSite);
    }
    @Test
    public void testFindNameQuery() throws IOException, JSONException {
        FindQueryTestCase test = new FindQueryTestCase("Paris",
            Arrays.asList(
                "http://dbpedia.org/resource/Paris",
                "http://dbpedia.org/resource/Paris_Saint-Germain_F.C.",
                "http://dbpedia.org/resource/University_of_Paris"));//,
                //"http://dbpedia.org/resource/Paris_Hilton"));
        executeQuery(test);
    }
    @Test
    public void testFindLimitAndOffsetQuery() throws IOException, JSONException {
        //only the first result
        FindQueryTestCase test = new FindQueryTestCase("Paris",
            Arrays.asList(
                "http://dbpedia.org/resource/Paris"),
            Arrays.asList(
                "http://dbpedia.org/resource/Paris_Saint-Germain_F.C.",
                "http://dbpedia.org/resource/University_of_Paris",
                "http://dbpedia.org/resource/Paris_Hilton"));
        test.setLimit(1);
        executeQuery(test);
        //the second result
        test = new FindQueryTestCase("Paris",
            Arrays.asList(
                "http://dbpedia.org/resource/Paris_Saint-Germain_F.C.",
                "http://dbpedia.org/resource/University_of_Paris"),
            Arrays.asList(
                "http://dbpedia.org/resource/Paris",
                "http://dbpedia.org/resource/Paris_Hilton"));
        test.setLimit(2);
        test.setOffset(1);
        executeQuery(test);
        //the second and third
        test = new FindQueryTestCase("Paris",
            Arrays.asList(
                "http://dbpedia.org/resource/University_of_Paris",
                "http://dbpedia.org/resource/Paris_Hilton"),
            Arrays.asList(
                "http://dbpedia.org/resource/Paris_Saint-Germain_F.C.",
                "http://dbpedia.org/resource/Paris"));
        test.setLimit(2);
        test.setOffset(2);
        executeQuery(test);
    }
    @Test
    public void testFindLanguageQuery() throws IOException, JSONException {
        FindQueryTestCase test = new FindQueryTestCase("Parigi",
            Arrays.asList(
                "http://dbpedia.org/resource/Paris",
                "http://dbpedia.org/resource/University_of_Paris",
                "http://dbpedia.org/resource/Paris_M%C3%A9tro"));
        executeQuery(test);

        //now the same test but only in English labels
        test = new FindQueryTestCase("Parigi",false); //no results
        test.setLanguage("en");
        executeQuery(test);
        
        //now in Italian
        test = new FindQueryTestCase("Parigi",
            Arrays.asList(
                "http://dbpedia.org/resource/Paris",
                "http://dbpedia.org/resource/University_of_Paris",
                "http://dbpedia.org/resource/Paris%E2%80%93Roubaix",
                "http://dbpedia.org/resource/Dakar_Rally"));
        test.setLanguage("it");
        executeQuery(test);

        //now search for Paris in Italian labels
        test = new FindQueryTestCase("Paris",
            Arrays.asList(
                "http://dbpedia.org/resource/Paris_Hilton",
                "http://dbpedia.org/resource/Paris%E2%80%93Nice",
                "http://dbpedia.org/resource/Paris,_Texas"));
        test.setLanguage("it");
        executeQuery(test);
    }
    @Test
    public void testFindWildcards() throws IOException, JSONException {
        //first a search without wildcards
        FindQueryTestCase test = new FindQueryTestCase("cia",
            Arrays.asList(
                "http://dbpedia.org/resource/CIA",
                "http://dbpedia.org/resource/CIA_World_Factbook"),
            Arrays.asList(
                "http://dbpedia.org/resource/Ciara"));
        test.setLanguage("en");
        executeQuery(test);
        //now the same search with wildcards
        test = new FindQueryTestCase("cia*",
            Arrays.asList(
                "http://dbpedia.org/resource/CIA",
                "http://dbpedia.org/resource/Ciara",
                "http://dbpedia.org/resource/CIA_World_Factbook"));
        test.setLanguage("en");
        executeQuery(test);
        
        test = new FindQueryTestCase("proto*",
            Arrays.asList(
                "http://dbpedia.org/resource/Prototype",
                "http://dbpedia.org/resource/Proton",
                "http://dbpedia.org/resource/Internet_Protocol"),
            Arrays.asList(
                "http://dbpedia.org/resource/Pretoria"));
        test.setLanguage("en");
        executeQuery(test);
        //now the same search with wildcards
        test = new FindQueryTestCase("pr?to*",
            Arrays.asList(
                "http://dbpedia.org/resource/Pretoria",
                "http://dbpedia.org/resource/Prototype",
                "http://dbpedia.org/resource/Proton",
                "http://dbpedia.org/resource/Internet_Protocol"));
        test.setLanguage("en");
        executeQuery(test);
    }
    @Test
    public void testFindSpecificFieldQuery() throws IOException, JSONException {
        //TODO: there is no other text field as rdfs:label in the dbpedia 
        //default dataset :(
    }
    
    @Test
    public void testFieldQueryRangeConstraints() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label',"+
                    "'http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type',"+
                    "'http:\\/\\/dbpedia.org\\/ontology\\/birthDate',"+
                    "'http:\\/\\/dbpedia.org\\/ontology\\/deathDate'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'range', "+
                    "'field': 'http:\\/\\/dbpedia.org\\/ontology\\/birthDate', "+
                    "'lowerBound': '1946-01-01T00:00:00.000Z',"+
                    "'upperBound': '1946-12-31T23:59:59.999Z',"+
                    "'inclusive': true,"+
                    "'datatype': 'xsd:dateTime'"+
                "},{ "+
                    "'type': 'reference', "+
                    "'field': 'http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type', "+
                    "'value': 'http:\\/\\/dbpedia.org\\/ontology\\/Person', "+
                "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Bill_Clinton",
                 "http://dbpedia.org/resource/George_W._Bush",
                 "http://dbpedia.org/resource/Donald_Trump"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                "http://www.w3.org/2000/01/rdf-schema#label",
                "http://dbpedia.org/ontology/birthDate"));
        //now execute the test
        executeQuery(test);
        
        //cities with more than 1 million inhabitants and an altitude over
        //1000 meter
        test = new FieldQueryTestCase(
            "{"+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label',"+
                    "'http:\\/\\/dbpedia.org\\/ontology\\/populationTotal',"+
                    "'http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#alt'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                     "'type': 'range', "+
                     "'field': 'http:\\/\\/dbpedia.org\\/ontology\\/populationTotal', "+
                     "'lowerBound': 1000000,"+
                     "'inclusive': true,"+
                     "'datatype': 'xsd:long'"+
                 "},{ "+
                     "'type': 'range', "+
                     "'field': 'http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#alt', "+
                     "'lowerBound': 1000,"+
                     "'inclusive': false,"+
                 "},{ "+
                     "'type': 'reference', "+
                     "'field': 'http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type', "+
                     "'value': 'http:\\/\\/dbpedia.org\\/ontology\\/City', "+
                 "}]"+
            "}",
            Arrays.asList(
                "http://dbpedia.org/resource/Mexico_City",
                "http://dbpedia.org/resource/Bogot%C3%A1",
                "http://dbpedia.org/resource/Quito"),
            Arrays.asList(
                "http://www.w3.org/2000/01/rdf-schema#label",
                "http://dbpedia.org/ontology/populationTotal",
                "http://www.w3.org/2003/01/geo/wgs84_pos#alt"));
        //now execute the test
        executeQuery(test);
    }
    
    @Test
    public void testFieldQueryTextConstraints() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'text', "+
                    "'language': 'de', "+
                    "'patternType': 'wildcard', "+
                    "'text': 'Frankf*', "+
                    "'field': 'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label' "+
                "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Frankfurt",
                 "http://dbpedia.org/resource/Eintracht_Frankfurt",
                 "http://dbpedia.org/resource/Frankfort,_Kentucky"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);  

        test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'text', "+
                    "'text': ['Frankfurt','Main','Flughafen'], "+
                    "'language': 'de', "+
                    "'field': 'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label' "+
                "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Frankfurt_Airport",
                 "http://dbpedia.org/resource/Frankfurt",
                 "http://dbpedia.org/resource/Airport"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);  
    }
    @Test
    public void testFieldQueryValueConstraints() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'value',"+
                    "'value': '34',"+
                    "'field': 'http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#alt',"+
                    "'datatype': 'xsd:int'"+
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Berlin",
                 "http://dbpedia.org/resource/Baghdad",
                 "http://dbpedia.org/resource/Orlando,_Florida"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);
        
        // now the same query but with no datatype
        test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'value',"+
                    "'value': '34',"+ //NOTE this is a JSON String!
                    "'field': 'http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#alt',"+
                    "}]"+
             "}",
             false); //we expect no results, because the datatype should be xsd:string

        //a third time the same query (without a datatype), but now we parse a 
        //JSON number as value
        test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'value',"+
                    "'value': 34,"+
                    "'field': 'http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#alt',"+
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Berlin",
                 "http://dbpedia.org/resource/Baghdad",
                 "http://dbpedia.org/resource/Orlando,_Florida"));
        //now execute the test
        executeQuery(test);
    }    
    
}
