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
package org.apache.stanbol.entityhub.it.ldpath;

import java.io.IOException;
import java.util.Collections;

import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.entityhub.it.ReferencedSiteTest;
import org.apache.stanbol.entityhub.it.SitesManagerTest;
import org.apache.stanbol.entityhub.test.it.EntityhubTestBase;
import org.junit.Test;
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
public class DbpediaLDPathTest extends EntityhubTestBase {

    
    public DbpediaLDPathTest() {
        super(Collections.singleton("dbpedia"));
    }
    @Test
    public void testNoContext() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "ldpath","name = rdfs:label[@en] :: xsd:string;")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testEmptyContext() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "ldpath","name = rdfs:label[@en] :: xsd:string;",
                "context","")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "ldpath","name = rdfs:label[@en] :: xsd:string;",
                "context",null)
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testNoLDPath() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testEmptyLDPath() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                "ldpath",null)
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                "ldpath","")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testIllegalLDPath() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                //missing semicolon
                "ldpath","name = rdfs:label[@en] :: xsd:string")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                //unknown namespace prefix
                "ldpath","name = nonexistendWkTzK:localName :: xsd:anyURI;")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                //unknown dataType prefix
                "ldpath","name = rdfs:label[@en] :: xsd:String;")
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testMultipleContext() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                "context","http://dbpedia.org/resource/London",
                "ldpath","name = rdfs:label[@en] :: xsd:string;")
        )
        .assertStatus(200)
        .assertContentContains(
            "\"@id\" : \"http://dbpedia.org/resource/London\"",
            "\"@value\" : \"London\"",
            "\"@id\" : \"http://dbpedia.org/resource/Paris\"",
            "\"@value\" : \"Paris\"");
    }
    @Test
    public void testUnknownContext() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/ThisEntityDoesNotExist_ForSure_49283",
                "ldpath","name = rdfs:label[@en] :: xsd:string;")
        )
        .assertStatus(200)
        .assertContentContains("[","]");
    }
    @Test
    public void testLDPath() throws IOException {
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withFormContent(
                "context","http://dbpedia.org/resource/Paris",
                "ldpath","@prefix dct : <http://purl.org/dc/terms/subject/> ;" +
                    "@prefix geo : <http://www.w3.org/2003/01/geo/wgs84_pos#> ;" +
                    "name = rdfs:label[@en] :: xsd:string;" +
                    "labels = rdfs:label :: xsd:string;" +
                    "comment = rdfs:comment[@en] :: xsd:string;" +
                    "categories = dct:subject :: xsd:anyURI;" +
                    "homepage = foaf:homepage :: xsd:anyURI;" +
                    "location = fn:concat(\"[\",geo:lat,\",\",geo:long,\"]\") :: xsd:string;")
        )
        .assertStatus(200)
        .assertContentType("application/json")
        .assertContentContains(
            "\"@id\" : \"http://dbpedia.org/resource/Paris\"",
            "\"comment\" : [ {",
            "Paris is the capital and largest city of France.",
            "\"homepage\" : [ {",
            "\"@id\" : \"http://www.paris.fr/\"",
            "\"labels\" : [ {",
            "\"@value\" : \"Parigi\"",
            "\"@value\" : \"巴黎\"",
            "\"location\" : [ {",
            "\"@value\" : \"[48.8567,2.3508]\"",
            "\"name\" : [ {",
            "\"@value\" : \"Paris\""
            );
    }
    /*
     * "/find" tests
     */
    @Test
    public void testFindInvalidLDPath() throws IOException {
        //parse some illegal LDPath
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/ldpath")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withHeader("Accept", "text/turtle")
            .withFormContent(
                "name","Vienna",
                "lang","en",
                //NOTE the missing semicolon
                "ldpath","label_de = rdfs:label[@de] :: xsd:string",
                "limit","1")
         )
         .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testFindLDPathSelectLabel() throws IOException {
        //select the German label on a query for the english one
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/find")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withHeader("Accept", "text/turtle")
            .withFormContent(
                "name","Vienna",
                "lang","en",
                "ldpath","name_de = rdfs:label[@de] :: xsd:string;",
                "limit","1")
         )
         .assertStatus(200)
         .assertContentType("text/turtle")
         .assertContentContains(
             "<http://stanbol.apache.org/ontology/entityhub/query#score>",
             "<http://dbpedia.org/resource/Vienna>")
          .assertContentRegexp("<name_de>\\s+\"Wien\"@de");
    }
    @Test
    public void testFindLDPathOnMultipleResults() throws IOException {
        //select multiple end check that LD-Path is executed on all results
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/find")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withHeader("Accept", "text/turtle")
            .withFormContent(
                "name","York",
                "ldpath","@prefix geo : <http://www.w3.org/2003/01/geo/wgs84_pos#> ;"+
                    "lat = geo:lat :: xsd:double;",
                "limit","5")
         )
         .assertStatus(200)
         .assertContentType("text/turtle")
         .assertContentRegexp(
             "<http://stanbol.apache.org/ontology/entityhub/query#score>",
             "<http://dbpedia.org/resource/New_York_City>",
             "<lat>.*\"40\\.716667\"\\^\\^<http://www\\.w3\\.org/2001/XMLSchema#double>",
             "<http://dbpedia.org/resource/New_York>",
             "<lat>.*\"43\\.0\"\\^\\^<http://www\\.w3\\.org/2001/XMLSchema#double>",
             "<http://dbpedia.org/resource/York>",
             "<lat>.*\"53\\.958332\"\\^\\^<http://www\\.w3\\.org/2001/XMLSchema#double>");
    }
    @Test
    public void testFindLDPathSelectPaths() throws IOException {
        //select the German name and the categories ond other members of the
        //same category
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/find")
            .withHeader("Content-Type", "application/x-www-form-urlencoded")
            .withHeader("Accept", "text/turtle")
            .withFormContent(
                "name","Webspinnen",
                "lang","de",
                "ldpath","@prefix dct : <http://purl.org/dc/terms/> ;"+
                    "name = rdfs:label[@en] :: xsd:string;"+
                    "category = dct:subject :: xsd:anyURI;"+
                    "others = dct:subject/^dct:subject :: xsd:anyURI;",
                "limit","1")
         )
         .assertStatus(200)
         .assertContentType("text/turtle")
         .assertContentRegexp(
             "<http://stanbol.apache.org/ontology/entityhub/query#score>",
             "<name>\\s+\"Spider\"@en ;",
             "<category>.*<http://dbpedia.org/resource/Category:Arachnids>",
             "<category>.*<http://dbpedia.org/resource/Category:Spiders>",
             "<others>.*<http://dbpedia.org/resource/Opiliones>",
             "<others>.*<http://dbpedia.org/resource/Acari>",
             "<others>.*<http://dbpedia.org/resource/Spider>",
             "<others>.*<http://dbpedia.org/resource/Scorpion>",
             "<others>.*<http://dbpedia.org/resource/Arachnid>");
    }
    @Test
    public void testQueryIllegalLDPath() throws IOException {
        //The field query as java string
        String query = "{"+
            "\"ldpath\": \"@prefix dct : <http:\\/\\/purl.org\\/dc\\/terms\\/subject\\/> ; " +
                "@prefix geo : <http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#> ; " +
                "@prefix dbp-ont : <http:\\/\\/dbpedia.org\\/ontology\\/> ; " +
                //note the missing semicolon
                "lat = geo:lat :: xsd:decimal ; long = geo:long :: xsd:decimal " +
                "type = rdf:type :: xsd:anyURI;\","+
            "\"constraints\": [{ "+
                    "\"type\": \"reference\","+ 
                    "\"field\": \"http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type\","+ 
                    "\"value\": \"http:\\/\\/dbpedia.org\\/ontology\\/Place\","+ 
                "},"+
                "{"+
                    "\"type\": \"range\","+
                    "\"field\": \"http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#lat\","+
                    "\"lowerBound\": 50,"+
                    "\"upperBound\": 51,"+
                    "\"inclusive\": true,"+
                    "\"datatype\": \"xsd:double\""+
                "},"+
                "{"+
                    "\"type\": \"range\","+
                    "\"field\": \"http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#long\","+
                    "\"lowerBound\": 6,"+
                    "\"upperBound\": 8,"+
                    "\"inclusive\": true,"+
                    "\"datatype\": \"xsd:double\""+
                "}"+
            "],"+
            "\"offset\": 0,"+
            "\"limit\": 10,"+
        "}";
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/query")
            .withHeader("Content-Type", "application/json")
            .withHeader("Accept", "text/turtle")
            .withContent(query)
        )
        .assertStatus(Status.BAD_REQUEST.getStatusCode());
    }
    @Test
    public void testQueryLDPathSelection() throws IOException {
        //The field query as java string
        String query = "{"+
            "\"ldpath\": \"@prefix dct : <http:\\/\\/purl.org\\/dc\\/terms\\/subject\\/> ; " +
                "@prefix geo : <http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#> ; " +
                "@prefix dbp-ont : <http:\\/\\/dbpedia.org\\/ontology\\/> ; " +
                "lat = geo:lat :: xsd:decimal ; long = geo:long :: xsd:decimal ; " +
                "population = dbp-ont:populationTotal :: xsd:integer ; " +
                "elevation = dbp-ont:elevation :: xsd:integer ; " +
                "name = rdfs:label[@en] :: xsd:string; " +
                "categories = dct:subject :: xsd:anyURI; " +
                "type = rdf:type :: xsd:anyURI;\","+
            "\"constraints\": [{ "+
                    "\"type\": \"reference\","+ 
                    "\"field\": \"http:\\/\\/www.w3.org\\/1999\\/02\\/22-rdf-syntax-ns#type\","+ 
                    "\"value\": \"http:\\/\\/dbpedia.org\\/ontology\\/Place\","+ 
                "},"+
                "{"+
                    "\"type\": \"range\","+
                    "\"field\": \"http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#lat\","+
                    "\"lowerBound\": 50,"+
                    "\"upperBound\": 51,"+
                    "\"inclusive\": true,"+
                    "\"datatype\": \"xsd:double\""+
                "},"+
                "{"+
                    "\"type\": \"range\","+
                    "\"field\": \"http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#long\","+
                    "\"lowerBound\": 6,"+
                    "\"upperBound\": 8,"+
                    "\"inclusive\": true,"+
                    "\"datatype\": \"xsd:double\""+
                "}"+
            "],"+
            "\"offset\": 0,"+
            "\"limit\": 10,"+
        "}";
        executor.execute(
            builder.buildPostRequest("/entityhub/site/dbpedia/query")
            .withHeader("Content-Type", "application/json")
            .withHeader("Accept", "text/turtle")
            .withContent(query)
        )
        .assertStatus(200)
        .assertContentType("text/turtle")
        .assertContentRegexp(
            //first expected entities
            "<http://dbpedia.org/resource/Bonn>",
            "<http://dbpedia.org/resource/Aachen>",
            "<http://dbpedia.org/resource/Koblenz>",
            "<http://dbpedia.org/resource/Cologne>",
            //now some values based on the LDPath
            "<name>\\s+\"Koblenz\"@en",
            "<lat>\\s+\"50.359722\"",
            "<long>\\s+\"7.597778\"",
            "<type>.*<http://www.w3.org/2002/07/owl#Thing>",
            "<type>.*<http://www.opengis.net/gml/_Feature>",
            "<type>.*<http://dbpedia.org/ontology/Town>",
            "<population>\\s+314926");
    }
    
    
}
