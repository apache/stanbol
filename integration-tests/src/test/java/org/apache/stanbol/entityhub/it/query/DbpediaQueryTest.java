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
import org.apache.stanbol.entityhub.servicesapi.defaults.SpecialFieldEnum;
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
    public void testMultiWordWildcardTextConstraints() throws IOException, JSONException {
        //this is specially for issue described in the first comment of
        //STANBOL-607
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
                    "'text': 'Frankf* am Main', "+
                    "'field': 'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label' "+
                "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Frankfurt"),
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
    @Test
    public void testFieldQueryMultiReferenceConstraints() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'rdfs:label',"+
                    "'rdf:type'],"+
                "'offset': '0',"+ 
                "'limit': '5',"+ 
                "'constraints': ["+
                    "{"+ 
                        "'type': 'text',"+ 
                        "'patternType': 'wildcard',"+
                        "'text': ['ford'],"+ 
                        "'field': 'rdfs:label',"+
                     "},{"+ 
                        "'type': 'reference',"+ 
                        "'value': ['dbp-ont:Organisation','dbp-ont:OfficeHolder'],"+ 
                        "'field': 'rdf:type',"+
                     "}"+
                 "]"+ 
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Ford_Motor_Company",
                 "http://dbpedia.org/resource/Gerald_Ford",
                 //this third result is important, as we would get different
                 //without the reference constraint
                 "http://dbpedia.org/resource/Ford_Foundation"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
        //now execute the test
        executeQuery(test);
    }
    
    @Test
    public void testFieldQueryMultipleValueConstraints() throws IOException, JSONException {
        //munich is on geo:alt 519 (will change to 518 on dbpedia 3.7)
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'value',"+
                    "'value': ['34','519'],"+
                    "'field': 'http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#alt',"+
                    "'datatype': 'xsd:int'"+
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Munich",
                 "http://dbpedia.org/resource/Berlin",
                 "http://dbpedia.org/resource/Baghdad"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);
        
        //a 2nd time the same query (without a datatype), but now we parse a 
        //JSON number as value
        test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'value',"+
                    "'value': [34,519],"+
                    "'field': 'http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#alt',"+
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Munich",
                 "http://dbpedia.org/resource/Berlin",
                 "http://dbpedia.org/resource/Baghdad"));
        //now execute the test
        executeQuery(test);
    }    
    /**
     * Tests that full text queries are possible by using the 
     * {@link SpecialFieldEnum#fullText} field (STANBOL-596) 
     */
    @Test
    public void testFullTextQuery() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'text',"+ 
                    "'patternType': 'wildcard',"+ 
                    "'text': ['microbiologist'],"+  
                    "'field': 'entityhub-query:fullText'"+ 
                 "},{"+ 
                    "'type': 'reference',"+  
                    "'value': ['dbp-ont:Person'],"+  
                    "'field': 'rdf:type',"+ 
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Louis_Pasteur"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);
        
    }
    /**
     * Tests searches for references in the semantic context field (the
     * field containing all references to other entities (STANBOL-597) 
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testSemanticContextQuery() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '5',"+
                "'constraints': [{ "+
                    "'type': 'reference',"+ 
                    "'value': ["+
                        "'http://dbpedia.org/resource/Category:Capitals_in_Europe',"+
                        "'http://dbpedia.org/resource/Category:Host_cities_of_the_Summer_Olympic_Games'"+
                    "],"+ 
                    "'field': 'entityhub-query:references',"+
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/London",
                 "http://dbpedia.org/resource/Paris",
                 "http://dbpedia.org/resource/Moscow",
                 "http://dbpedia.org/resource/Rome",
                 "http://dbpedia.org/resource/Berlin"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);        
    }
    
    /**
     * Tests ValueConstraint MODE "any" and "all" queries (STANBOL-595) 
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testConstraintValueModeQuery() throws IOException, JSONException {
        //First with mode = 'any' -> combine Entity Ranking with types
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '5',"+
                "'constraints': [{ "+
                    "'type': 'reference',"+ 
                    "'value': ["+
                        "'http:\\/\\/dbpedia.org\\/resource\\/Category:Capitals_in_Europe',"+
                        "'http:\\/\\/dbpedia.org\\/resource\\/Category:Host_cities_of_the_Summer_Olympic_Games',"+
                        "'http:\\/\\/dbpedia.org\\/ontology\\/City'"+
                    "],"+ 
                    "'field': 'entityhub-query:references',"+
                    "'mode': 'any'"+
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/London",
                 "http://dbpedia.org/resource/Paris",
                 "http://dbpedia.org/resource/Moscow",
                 "http://dbpedia.org/resource/Rome",
                 //now we get Los_Angeles because it has the dbp-ont:City type
                 "http://dbpedia.org/resource/Los_Angeles"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);
        
        //Second query for Entities that do have relations to all three
        //Entities (NOTE: the dbp-ont:City type is missing for most of the
        //members of the two categories used in this example!)
        test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '5',"+
                "'constraints': [{ "+
                    "'type': 'reference',"+ 
                    "'value': ["+
                        "'http:\\/\\/dbpedia.org\\/resource\\/Category:Capitals_in_Europe',"+
                        "'http:\\/\\/dbpedia.org\\/resource\\/Category:Host_cities_of_the_Summer_Olympic_Games',"+
                        "'http:\\/\\/dbpedia.org\\/ontology\\/City'"+
                    "],"+ 
                    "'field': 'entityhub-query:references',"+
                    "'mode': 'all'"+
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Berlin",
                 "http://dbpedia.org/resource/Amsterdam"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);        
    }
    /**
     * Tests (1) similarity searches and (2) that the full text field is supported
     * for those (STANBOL-589 and STANBOL-596)
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testSimilaritySearch() throws IOException, JSONException {
        
        //searches Places with "Wolfgang Amadeus Mozart" as context
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                "'offset': '0',"+
                "'limit': '5',"+
                "'constraints': [{ " +
                        "'type': 'reference'," +
                        "'value': 'http:\\/\\/dbpedia.org\\/ontology\\/Place'," +
                        "'field': 'http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type'," +
                    "},{"+ 
                        "'type': 'similarity'," + 
                        "'context': 'Wolfgang Amadeus Mozart'," + 
                        "'field': 'http:\\/\\/stanbol.apache.org\\/ontology\\/entityhub\\/query#fullText'," +
                    "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://dbpedia.org/resource/Salzburg"),
             Arrays.asList( //list of required fields for results
                "http://www.w3.org/2000/01/rdf-schema#label"));
        //now execute the test
        executeQuery(test);        
        
    }
    @Test
    public void testBoostAndProximityRanking() throws IOException, JSONException {
            //test features added with STANBOL-1105, STANBOL-1106
            FieldQueryTestCase test = new FieldQueryTestCase(
                "{ "+
                    "'selected': ["+
                        "'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label'],"+
                    "'offset': '0',"+
                    "'limit': '3',"+
                    "'constraints': [{ "+
                        "'type': 'text', "+
                        "'text': ['Frankfurt','Main','Flughafen'], "+
                        "'language': ['de', 'en'], "+
                        "'field': 'http:\\/\\/www.w3.org\\/2000\\/01\\/rdf-schema#label', "+
                        "'boost': 12.34," +
                        "'proximityRanking': true"+
                    "}]"+
                 "}",
                 Arrays.asList( //list of expected results
                     "http://dbpedia.org/resource/Frankfurt_Airport",
                     "http://dbpedia.org/resource/Frankfurt",
// this query selects Main instead of Airport, as Main has the same label in German and English
//                     "http://dbpedia.org/resource/Airport"),
                     "http://dbpedia.org/resource/Main"), 
                 Arrays.asList( //list of required fields for results
                    "http://www.w3.org/2000/01/rdf-schema#label"));
            //now execute the test
            executeQuery(test);        
    }
}
