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
package org.apache.stanbol.commons.jsonld;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Fabian Christ
 *
 */
public class JsonLdTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSpecExample1() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        jsonLd.addNamespacePrefix("http://example.org/myvocab#", "myvocab");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://sioc.org/vocab/1/", "sioc");

        JsonLdResource jsonLdResource = new JsonLdResource();
        jsonLdResource.addType("foaf:Person");
        jsonLdResource.putProperty("foaf:name", "Manu Sporny");
        jsonLdResource.putProperty("foaf:homepage", "<http://manu.sporny.org/>");
        jsonLdResource.putProperty("sioc:avatar", "<http://twitter.com/account/profile_image/manusporny>");
        jsonLdResource.putProperty("myvocab:credits", 500);
        jsonLd.put(jsonLdResource);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"myvocab\":\"http://example.org/myvocab#\",\"sioc\":\"http://sioc.org/vocab/1/\"},\"@type\":\"foaf:Person\",\"foaf:homepage\":\"<http://manu.sporny.org/>\",\"foaf:name\":\"Manu Sporny\",\"myvocab:credits\":500,\"sioc:avatar\":\"<http://twitter.com/account/profile_image/manusporny>\"}";
        assertEquals(expected, actual);
        
        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"@context\": {\n        \"foaf\": \"http://xmlns.com/foaf/0.1/\",\n        \"myvocab\": \"http://example.org/myvocab#\",\n        \"sioc\": \"http://sioc.org/vocab/1/\"\n    },\n    \"@type\": \"foaf:Person\",\n    \"foaf:homepage\": \"<http://manu.sporny.org/>\",\n    \"foaf:name\": \"Manu Sporny\",\n    \"myvocab:credits\": 500,\n    \"sioc:avatar\": \"<http://twitter.com/account/profile_image/manusporny>\"\n}";
        assertEquals(expectedIndent, actualIndent);
    }
    
    @Test
    public void testSpecExample1NoCuries() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(false);
        jsonLd.addNamespacePrefix("http://example.org/myvocab#", "myvocab");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://sioc.org/vocab/1/", "sioc");

        JsonLdResource jsonLdResource = new JsonLdResource();
        jsonLdResource.addType("foaf:Person");
        jsonLdResource.putProperty("foaf:name", "Manu Sporny");
        jsonLdResource.putProperty("foaf:homepage", "http://manu.sporny.org/");
        jsonLdResource.putProperty("sioc:avatar", "http://twitter.com/account/profile_image/manusporny");
        jsonLdResource.putProperty("myvocab:credits", 500);
        jsonLd.put(jsonLdResource);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"avatar\":\"http://sioc.org/vocab/1/avatar\",\"credits\":\"http://example.org/myvocab#credits\",\"homepage\":\"http://xmlns.com/foaf/0.1/homepage\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"Person\":\"http://xmlns.com/foaf/0.1/Person\"},\"@type\":\"Person\",\"avatar\":\"http://twitter.com/account/profile_image/manusporny\",\"credits\":500,\"homepage\":\"http://manu.sporny.org/\",\"name\":\"Manu Sporny\"}";
        assertEquals(expected, actual);        
    }

    @Test
    public void testSpecExample2_JointGraph() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseJointGraphs(true);
        jsonLd.setUseCuries(true);
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("foaf:Person");
        r1.putProperty("foaf:homepage", "<http://example.com/bob>");
        r1.putProperty("foaf:name", "Bob");
        jsonLd.put(r1.getSubject(), r1);

        JsonLdResource r2 = new JsonLdResource();
        r2.setSubject("_:bnode2");
        r2.addType("foaf:Person");
        r2.putProperty("foaf:homepage", "<http://example.com/eve>");
        r2.putProperty("foaf:name", "Eve");
        jsonLd.put(r2.getSubject(), r2);

        JsonLdResource r3 = new JsonLdResource();
        r3.setSubject("_:bnode3");
        r3.addType("foaf:Person");
        r3.putProperty("foaf:homepage", "<http://example.com/bert>");
        r3.putProperty("foaf:name", "Bert");
        jsonLd.put(r3.getSubject(), r3);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\"},\"@subject\":[{\"@subject\":\"_:bnode1\",\"@type\":\"foaf:Person\",\"foaf:homepage\":\"<http://example.com/bob>\",\"foaf:name\":\"Bob\"},{\"@subject\":\"_:bnode2\",\"@type\":\"foaf:Person\",\"foaf:homepage\":\"<http://example.com/eve>\",\"foaf:name\":\"Eve\"},{\"@subject\":\"_:bnode3\",\"@type\":\"foaf:Person\",\"foaf:homepage\":\"<http://example.com/bert>\",\"foaf:name\":\"Bert\"}]}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"@context\": {\n        \"foaf\": \"http://xmlns.com/foaf/0.1/\"\n    },\n    \"@subject\": [\n        {\n            \"@subject\": \"_:bnode1\",\n            \"@type\": \"foaf:Person\",\n            \"foaf:homepage\": \"<http://example.com/bob>\",\n            \"foaf:name\": \"Bob\"\n        },\n        {\n            \"@subject\": \"_:bnode2\",\n            \"@type\": \"foaf:Person\",\n            \"foaf:homepage\": \"<http://example.com/eve>\",\n            \"foaf:name\": \"Eve\"\n        },\n        {\n            \"@subject\": \"_:bnode3\",\n            \"@type\": \"foaf:Person\",\n            \"foaf:homepage\": \"<http://example.com/bert>\",\n            \"foaf:name\": \"Bert\"\n        }\n    ]\n}";
        assertEquals(expectedIndent, actualIndent);
    }
    
    @Test
    public void testSpecExample2_JointGraphNoCuries() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseJointGraphs(true);
        jsonLd.setUseCuries(false);
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("http://xmlns.com/foaf/0.1/Person");
        r1.putProperty("http://xmlns.com/foaf/0.1/homepage", "<http://example.com/bob>");
        r1.putProperty("http://xmlns.com/foaf/0.1/name", "Bob");
        jsonLd.put(r1.getSubject(), r1);

        JsonLdResource r2 = new JsonLdResource();
        r2.setSubject("_:bnode2");
        r2.addType("http://xmlns.com/foaf/0.1/Person");
        r2.putProperty("http://xmlns.com/foaf/0.1/homepage", "<http://example.com/eve>");
        r2.putProperty("http://xmlns.com/foaf/0.1/name", "Eve");
        jsonLd.put(r2.getSubject(), r2);

        JsonLdResource r3 = new JsonLdResource();
        r3.setSubject("_:bnode3");
        r3.addType("http://xmlns.com/foaf/0.1/Person");
        r3.putProperty("http://xmlns.com/foaf/0.1/homepage", "<http://example.com/bert>");
        r3.putProperty("http://xmlns.com/foaf/0.1/name", "Bert");
        jsonLd.put(r3.getSubject(), r3);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"homepage\":\"http://xmlns.com/foaf/0.1/homepage\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"Person\":\"http://xmlns.com/foaf/0.1/Person\"},\"@subject\":[{\"@subject\":\"_:bnode1\",\"@type\":\"Person\",\"homepage\":\"<http://example.com/bob>\",\"name\":\"Bob\"},{\"@subject\":\"_:bnode2\",\"@type\":\"Person\",\"homepage\":\"<http://example.com/eve>\",\"name\":\"Eve\"},{\"@subject\":\"_:bnode3\",\"@type\":\"Person\",\"homepage\":\"<http://example.com/bert>\",\"name\":\"Bert\"}]}";
        assertEquals(expected, actual);
    }

    @Test
    public void testSpecExample2_DisjointGraph() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        jsonLd.setUseJointGraphs(false);
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("foaf:Person");
        r1.putProperty("foaf:homepage", "<http://example.com/bob>");
        r1.putProperty("foaf:name", "Bob");
        jsonLd.put(r1.getSubject(), r1);

        JsonLdResource r2 = new JsonLdResource();
        r2.setSubject("_:bnode2");
        r2.addType("foaf:Person");
        r2.putProperty("foaf:homepage", "<http://example.com/eve>");
        r2.putProperty("foaf:name", "Eve");
        jsonLd.put(r2.getSubject(), r2);

        JsonLdResource r3 = new JsonLdResource();
        r3.setSubject("_:bnode3");
        r3.addType("foaf:Person");
        r3.putProperty("foaf:homepage", "<http://example.com/eve>");
        r3.putProperty("foaf:name", "Eve");
        jsonLd.put(r3.getSubject(), r3);

        String actual = jsonLd.toString();
        String expected = "[{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\"},\"@subject\":\"_:bnode1\",\"@type\":\"foaf:Person\",\"foaf:homepage\":\"<http://example.com/bob>\",\"foaf:name\":\"Bob\"},{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\"},\"@subject\":\"_:bnode2\",\"@type\":\"foaf:Person\",\"foaf:homepage\":\"<http://example.com/eve>\",\"foaf:name\":\"Eve\"},{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\"},\"@subject\":\"_:bnode3\",\"@type\":\"foaf:Person\",\"foaf:homepage\":\"<http://example.com/eve>\",\"foaf:name\":\"Eve\"}]";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "[\n    {\n        \"@context\": {\n            \"foaf\": \"http://xmlns.com/foaf/0.1/\"\n        },\n        \"@subject\": \"_:bnode1\",\n        \"@type\": \"foaf:Person\",\n        \"foaf:homepage\": \"<http://example.com/bob>\",\n        \"foaf:name\": \"Bob\"\n    },\n    {\n        \"@context\": {\n            \"foaf\": \"http://xmlns.com/foaf/0.1/\"\n        },\n        \"@subject\": \"_:bnode2\",\n        \"@type\": \"foaf:Person\",\n        \"foaf:homepage\": \"<http://example.com/eve>\",\n        \"foaf:name\": \"Eve\"\n    },\n    {\n        \"@context\": {\n            \"foaf\": \"http://xmlns.com/foaf/0.1/\"\n        },\n        \"@subject\": \"_:bnode3\",\n        \"@type\": \"foaf:Person\",\n        \"foaf:homepage\": \"<http://example.com/eve>\",\n        \"foaf:name\": \"Eve\"\n    }\n]";
        assertEquals(expectedIndent, actualIndent);
    }
    
    @Test
    public void testSpecExample2_DisjointGraphNoCuries() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(false);
        jsonLd.setUseJointGraphs(false);
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("foaf:Person");
        r1.putProperty("foaf:homepage", "<http://example.com/bob>");
        r1.putProperty("foaf:name", "Bob");
        jsonLd.put(r1.getSubject(), r1);

        JsonLdResource r2 = new JsonLdResource();
        r2.setSubject("_:bnode2");
        r2.addType("foaf:Person");
        r2.putProperty("foaf:homepage", "<http://example.com/eve>");
        r2.putProperty("foaf:name", "Eve");
        jsonLd.put(r2.getSubject(), r2);

        JsonLdResource r3 = new JsonLdResource();
        r3.setSubject("_:bnode3");
        r3.addType("foaf:Person");
        r3.putProperty("foaf:homepage", "<http://example.com/eve>");
        r3.putProperty("foaf:name", "Eve");
        jsonLd.put(r3.getSubject(), r3);

        String actual = jsonLd.toString();
        String expected = "[{\"@context\":{\"homepage\":\"http://xmlns.com/foaf/0.1/homepage\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"Person\":\"http://xmlns.com/foaf/0.1/Person\"},\"@subject\":\"_:bnode1\",\"@type\":\"Person\",\"homepage\":\"<http://example.com/bob>\",\"name\":\"Bob\"},{\"@context\":{\"homepage\":\"http://xmlns.com/foaf/0.1/homepage\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"Person\":\"http://xmlns.com/foaf/0.1/Person\"},\"@subject\":\"_:bnode2\",\"@type\":\"Person\",\"homepage\":\"<http://example.com/eve>\",\"name\":\"Eve\"},{\"@context\":{\"homepage\":\"http://xmlns.com/foaf/0.1/homepage\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"Person\":\"http://xmlns.com/foaf/0.1/Person\"},\"@subject\":\"_:bnode3\",\"@type\":\"Person\",\"homepage\":\"<http://example.com/eve>\",\"name\":\"Eve\"}]";
        assertEquals(expected, actual);
    }

    @Test
    public void testSpecExample3() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        jsonLd.addNamespacePrefix("http://microformats.org/profile/hcard#vcard", "vcard");
        jsonLd.addNamespacePrefix("http://microformats.org/profile/hcard#url", "url");
        jsonLd.addNamespacePrefix("http://microformats.org/profile/hcard#fn", "fn");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("vcard");
        r1.putProperty("url", "<http://tantek.com/>");
        r1.putProperty("fn", "Tantek Celik");
        jsonLd.put(r1.getSubject(), r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"fn\":\"http://microformats.org/profile/hcard#fn\",\"url\":\"http://microformats.org/profile/hcard#url\",\"vcard\":\"http://microformats.org/profile/hcard#vcard\"},\"@subject\":\"_:bnode1\",\"@type\":\"vcard\",\"fn\":\"Tantek Celik\",\"url\":\"<http://tantek.com/>\"}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"@context\": {\n        \"fn\": \"http://microformats.org/profile/hcard#fn\",\n        \"url\": \"http://microformats.org/profile/hcard#url\",\n        \"vcard\": \"http://microformats.org/profile/hcard#vcard\"\n    },\n    \"@subject\": \"_:bnode1\",\n    \"@type\": \"vcard\",\n    \"fn\": \"Tantek Celik\",\n    \"url\": \"<http://tantek.com/>\"\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample3DefaultContext() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://purl.org/dc/terms/", "dc");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://rdfs.org/sioc/ns#", "sioc");
        jsonLd.addNamespacePrefix("http://creativecommons.org/ns#", "cc");
        jsonLd.addNamespacePrefix("http://www.w3.org/2003/01/geo/wgs84_pos#", "geo");
        jsonLd.addNamespacePrefix("http://www.w3.org/2006/vcard/ns#", "vcard");
        jsonLd.addNamespacePrefix("http://www.w3.org/2002/12/cal/ical#", "cal");
        jsonLd.addNamespacePrefix("http://usefulinc.com/ns/doap#", "doap");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/Person", "Person");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/name", "name");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/homepage", "homepage");

        String actual = jsonLd.toString();
        String expected = "{}";
        assertEquals(expected, actual);
    }

    @Test
    public void testSpecExample4Microformats() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        jsonLd.setUseJointGraphs(false);

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://purl.oreilly.com/works/45U8QJGZSQKDH8N>");
        r1.addType("http://purl.org/vocab/frbr/core#Work");
        r1.putProperty("http://purl.org/dc/terms/title", "Just a Geek");
        r1.putProperty("http://purl.org/dc/terms/creator", "Whil Wheaton");
        r1.putProperty("http://purl.org/vocab/frbr/core#realization", "<http://purl.oreilly.com/products/9780596007683.BOOK>");
        r1.putProperty("http://purl.org/vocab/frbr/core#realization", "<http://purl.oreilly.com/products/9780596802189.EBOOK>");
        
        jsonLd.put(r1.getSubject(), r1);

        JsonLdResource r2 = new JsonLdResource();
        r2.setSubject("<http://purl.oreilly.com/products/9780596007683.BOOK>");
        r2.addType("<http://purl.org/vocab/frbr/core#Expression>");
        r2.putProperty("http://purl.org/dc/terms/type", "<http://purl.oreilly.com/product-types/BOOK>");
        jsonLd.put(r2.getSubject(), r2);

        JsonLdResource r3 = new JsonLdResource();
        r3.setSubject("<http://purl.oreilly.com/products/9780596802189.EBOOK>");
        r3.addType("http://purl.org/vocab/frbr/core#Expression");
        r3.putProperty("http://purl.org/dc/terms/type", "<http://purl.oreilly.com/product-types/BOOK>");
        jsonLd.put(r3.getSubject(), r3);

        String actual = jsonLd.toString();
        String expected = "[{\"@subject\":\"<http://purl.oreilly.com/products/9780596007683.BOOK>\",\"@type\":\"<http://purl.org/vocab/frbr/core#Expression>\",\"http://purl.org/dc/terms/type\":\"<http://purl.oreilly.com/product-types/BOOK>\"},{\"@subject\":\"<http://purl.oreilly.com/products/9780596802189.EBOOK>\",\"@type\":\"http://purl.org/vocab/frbr/core#Expression\",\"http://purl.org/dc/terms/type\":\"<http://purl.oreilly.com/product-types/BOOK>\"},{\"@subject\":\"<http://purl.oreilly.com/works/45U8QJGZSQKDH8N>\",\"@type\":\"http://purl.org/vocab/frbr/core#Work\",\"http://purl.org/dc/terms/creator\":\"Whil Wheaton\",\"http://purl.org/dc/terms/title\":\"Just a Geek\",\"http://purl.org/vocab/frbr/core#realization\":[\"<http://purl.oreilly.com/products/9780596007683.BOOK>\",\"<http://purl.oreilly.com/products/9780596802189.EBOOK>\"]}]";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "[\n    {\n        \"@subject\": \"<http://purl.oreilly.com/products/9780596007683.BOOK>\",\n        \"@type\": \"<http://purl.org/vocab/frbr/core#Expression>\",\n        \"http://purl.org/dc/terms/type\": \"<http://purl.oreilly.com/product-types/BOOK>\"\n    },\n    {\n        \"@subject\": \"<http://purl.oreilly.com/products/9780596802189.EBOOK>\",\n        \"@type\": \"http://purl.org/vocab/frbr/core#Expression\",\n        \"http://purl.org/dc/terms/type\": \"<http://purl.oreilly.com/product-types/BOOK>\"\n    },\n    {\n        \"@subject\": \"<http://purl.oreilly.com/works/45U8QJGZSQKDH8N>\",\n        \"@type\": \"http://purl.org/vocab/frbr/core#Work\",\n        \"http://purl.org/dc/terms/creator\": \"Whil Wheaton\",\n        \"http://purl.org/dc/terms/title\": \"Just a Geek\",\n        \"http://purl.org/vocab/frbr/core#realization\": [\n            \"<http://purl.oreilly.com/products/9780596007683.BOOK>\",\n            \"<http://purl.oreilly.com/products/9780596802189.EBOOK>\"\n        ]\n    }\n]";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample5TypedLiterals() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://purl.org/dc/terms/", "dc");

        JsonLdResource r1 = new JsonLdResource();
        r1.putProperty("http://purl.org/dc/terms/modified", "2010-05-29T14:17:39+02:00^^http://www.w3.org/2001/XMLSchema#dateTime");
        jsonLd.put("r1", r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"dc\":\"http://purl.org/dc/terms/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"dc:modified\":\"2010-05-29T14:17:39+02:00^^xsd:dateTime\"}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testSpecExample5TypedLiteralsCoercion() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(false);
        jsonLd.setUseTypeCoercion(true);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        String nick = "\"stu\"^^http://www.w3.org/2001/XMLSchema#string";
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/nick", "xsd:string");
        jsonLd.put("r1", r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"@coerce\":{\"http://www.w3.org/2001/XMLSchema#string\":\"http://xmlns.com/foaf/0.1/nick\"}},\"@subject\":\"<http://example.org/people#joebob>\",\"http://xmlns.com/foaf/0.1/nick\":\"stu\"}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testSpecExample5TypedLiteralsNsCoercion() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(true);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        String nick = "\"stu\"^^http://www.w3.org/2001/XMLSchema#string";
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/nick", "xsd:string");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:string\":\"foaf:nick\"}},\"@subject\":\"<http://example.org/people#joebob>\",\"foaf:nick\":\"stu\"}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testSpecExample5TypedLiteralsNsCoercionNoCuries() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(false);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("http://example.org/people#joebob");
        String nick = "\"stu\"^^http://www.w3.org/2001/XMLSchema#string";
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/nick", "xsd:string");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"nick\":\"http://xmlns.com/foaf/0.1/nick\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:string\":\"nick\"}},\"@subject\":\"http://example.org/people#joebob\",\"nick\":\"stu\"}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testSpecExample5TypedLiteralsNoCoercion() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(false);
        jsonLd.setUseTypeCoercion(false);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", "stu");
        r1.putPropertyType("http://xmlns.com/foaf/0.1/nick", "xsd:string");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@subject\":\"<http://example.org/people#joebob>\",\"http://xmlns.com/foaf/0.1/nick\":{\"@literal\":\"stu\",\"@datatype\":\"http://www.w3.org/2001/XMLSchema#string\"}}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testSpecExample5TypedLiteralsNsNoCoercion() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", "stu");
        r1.putPropertyType("http://xmlns.com/foaf/0.1/nick", "xsd:string");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"<http://example.org/people#joebob>\",\"foaf:nick\":{\"@literal\":\"stu\",\"@datatype\":\"xsd:string\"}}";
        assertEquals(expected, actual);
    }

    @Test
    public void testSpecExample6MultipleObjects() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        r1.putProperty("foaf:nick", "stu");
        r1.putProperty("foaf:nick", "groknar");
        r1.putProperty("foaf:nick", "radface");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\"},\"@subject\":\"<http://example.org/people#joebob>\",\"foaf:nick\":[\"stu\",\"groknar\",\"radface\"]}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testSpecExample6MultipleTypedObjects() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        r1.putProperty("foaf:nick", "stu");
        r1.putProperty("foaf:nick", "groknar");
        r1.putProperty("foaf:nick", "radface");
        r1.putPropertyType("foaf:nick", "xsd:string");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"<http://example.org/people#joebob>\",\"foaf:nick\":[{\"@literal\":\"stu\",\"@datatype\":\"xsd:string\"},{\"@literal\":\"groknar\",\"@datatype\":\"xsd:string\"},{\"@literal\":\"radface\",\"@datatype\":\"xsd:string\"}]}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"@context\": {\n        \"foaf\": \"http://xmlns.com/foaf/0.1/\",\n        \"xsd\": \"http://www.w3.org/2001/XMLSchema#\"\n    },\n    \"@subject\": \"<http://example.org/people#joebob>\",\n    \"foaf:nick\": [\n        {\n            \"@literal\": \"stu\",\n            \"@datatype\": \"xsd:string\"\n        },\n        {\n            \"@literal\": \"groknar\",\n            \"@datatype\": \"xsd:string\"\n        },\n        {\n            \"@literal\": \"radface\",\n            \"@datatype\": \"xsd:string\"\n        }\n    ]\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample6MultipleObjectsCoerce() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(false);
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(true);
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        r1.putProperty("foaf:nick", "stu");
        r1.putProperty("foaf:nick", "groknar");
        r1.putProperty("foaf:nick", "radface");
        r1.putPropertyType("foaf:nick", "xsd:string");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"@coerce\":{\"http://www.w3.org/2001/XMLSchema#string\":\"http://xmlns.com/foaf/0.1/nick\"}},\"@subject\":\"<http://example.org/people#joebob>\",\"http://xmlns.com/foaf/0.1/nick\":[\"stu\",\"groknar\",\"radface\"]}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testSpecExample6MultipleObjectsCoerceNoCuries() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(false);
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        r1.putProperty("foaf:nick", "stu");
        r1.putProperty("foaf:nick", "groknar");
        r1.putProperty("foaf:nick", "radface");
        r1.putPropertyType("foaf:nick", "xsd:string");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"nick\":\"http://xmlns.com/foaf/0.1/nick\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:string\":\"nick\"}},\"@subject\":\"<http://example.org/people#joebob>\",\"nick\":[\"stu\",\"groknar\",\"radface\"]}";
        assertEquals(expected, actual);
    }

    @Test
    public void testSpecExample6MultipleObjectsNsCoerce() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(true);
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        r1.putProperty("foaf:nick", "stu");
        r1.putProperty("foaf:nick", "groknar");
        r1.putProperty("foaf:nick", "radface");
        r1.putPropertyType("foaf:nick", "xsd:string");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:string\":\"foaf:nick\"}},\"@subject\":\"<http://example.org/people#joebob>\",\"foaf:nick\":[\"stu\",\"groknar\",\"radface\"]}";
        assertEquals(expected, actual);
    }

    @Test
    public void testMultipleIRIsNoNsCoerce() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setApplyNamespaces(false);

        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://nickworld.com/nicks/", "nick");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", "nick:stu");
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", "nick:pet");
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", "nick:flo");
        r1.putPropertyType("http://xmlns.com/foaf/0.1/nick", "@iri");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"@coerce\":{\"@iri\":\"http://xmlns.com/foaf/0.1/nick\"}},\"@subject\":\"<http://example.org/people#joebob>\",\"http://xmlns.com/foaf/0.1/nick\":[\"http://nickworld.com/nicks/stu\",\"http://nickworld.com/nicks/pet\",\"http://nickworld.com/nicks/flo\"]}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"@context\": {\n        \"@coerce\": {\n            \"@iri\": \"http://xmlns.com/foaf/0.1/nick\"\n        }\n    },\n    \"@subject\": \"<http://example.org/people#joebob>\",\n    \"http://xmlns.com/foaf/0.1/nick\": [\n        \"http://nickworld.com/nicks/stu\",\n        \"http://nickworld.com/nicks/pet\",\n        \"http://nickworld.com/nicks/flo\"\n    ]\n}";
        assertEquals(expectedIndent, actualIndent);
    }
    
    @Test
    public void testMultipleIRIs() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setApplyNamespaces(false);

        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://nickworld.com/nicks/", "nick");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        
        JsonLdIRI nick1 = new JsonLdIRI("nick:stu");
        JsonLdIRI nick2 = new JsonLdIRI("nick:pet");
        JsonLdIRI nick3 = new JsonLdIRI("nick:flo");
        
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick1);
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick2);
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick3);
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@subject\":\"<http://example.org/people#joebob>\",\"http://xmlns.com/foaf/0.1/nick\":[{\"@iri\":\"http://nickworld.com/nicks/stu\"},{\"@iri\":\"http://nickworld.com/nicks/pet\"},{\"@iri\":\"http://nickworld.com/nicks/flo\"}]}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testMultipleIRIsWithNS() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseCuries(true);

        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://nickworld.com/nicks/", "nick");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        
        JsonLdIRI nick1 = new JsonLdIRI("nick:stu");
        JsonLdIRI nick2 = new JsonLdIRI("nick:pet");
        JsonLdIRI nick3 = new JsonLdIRI("nick:flo");
        
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick1);
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick2);
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick3);
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"nick\":\"http://nickworld.com/nicks/\"},\"@subject\":\"<http://example.org/people#joebob>\",\"foaf:nick\":[{\"@iri\":\"nick:stu\"},{\"@iri\":\"nick:pet\"},{\"@iri\":\"nick:flo\"}]}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testMultipleIRIsWithNSCoerce() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseCuries(true);

        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://nickworld.com/nicks/", "nickw");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        
        JsonLdIRI nick1 = new JsonLdIRI("nick:stu");
        JsonLdIRI nick2 = new JsonLdIRI("nick:pet");
        JsonLdIRI nick3 = new JsonLdIRI("nick:flo");
        
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick1);
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick2);
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nick3);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/nick", "@iri");
        jsonLd.put(r1);

        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"@coerce\":{\"@iri\":\"foaf:nick\"}},\"@subject\":\"<http://example.org/people#joebob>\",\"foaf:nick\":[\"nick:stu\",\"nick:pet\",\"nick:flo\"]}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testUseProfile() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseCuries(true);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setProfile("testprofile");
        r1.setSubject("_:bnode1");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@subject\":\"_:bnode1\",\"@profile\":\"testprofile\"}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testIntegerValue() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(true);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.putProperty("foaf:age", 31);
        r1.putPropertyType("foaf:age", "http://www.w3.org/2001/XMLSchema#int");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:int\":\"foaf:age\"}},\"@subject\":\"_:bnode1\",\"foaf:age\":31}";
        assertEquals(expected, actual);
        
        String actualIndented = jsonLd.toString(2);
        String expectedIndented = "{\n  \"@context\": {\n    \"foaf\": \"http://xmlns.com/foaf/0.1/\",\n    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\",\n    \"@coerce\": {\n      \"xsd:int\": \"foaf:age\"\n    }\n  },\n  \"@subject\": \"_:bnode1\",\n  \"foaf:age\": 31\n}";
        assertEquals(expectedIndented, actualIndented);
    }
    
    @Test
    public void testIntegerValueNoCoerce() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.putProperty("foaf:age", 31);
        r1.putPropertyType("foaf:age", "http://www.w3.org/2001/XMLSchema#int");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"_:bnode1\",\"foaf:age\":{\"@literal\":\"31\",\"@datatype\":\"xsd:int\"}}";
        assertEquals(expected, actual);
    }
    
    @Test
    public void testFloatValue() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(true);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.putProperty("foaf:age", 31.533567);
        r1.putPropertyType("foaf:age", "http://www.w3.org/2001/XMLSchema#int");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:int\":\"foaf:age\"}},\"@subject\":\"_:bnode1\",\"foaf:age\":31.533567}";
        assertEquals(expected, actual);        
    }
    
    @Test
    public void testFloatValueNoCuries() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(true);
        jsonLd.setUseCuries(false);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.putProperty("http://xmlns.com/foaf/0.1/age", 31.533567);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/age", "http://www.w3.org/2001/XMLSchema#int");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"age\":\"http://xmlns.com/foaf/0.1/age\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:int\":\"age\"}},\"@subject\":\"_:bnode1\",\"age\":31.533567}";
        assertEquals(expected, actual);        
    }
    
    @Test
    public void testFloatValueNoCoerce() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(true);
        jsonLd.setUseTypeCoercion(false);
        jsonLd.setUseCuries(true);
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.putProperty("foaf:age", 31.533567);
        r1.putPropertyType("foaf:age", "http://www.w3.org/2001/XMLSchema#int");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"_:bnode1\",\"foaf:age\":{\"@literal\":\"31.533567\",\"@datatype\":\"xsd:int\"}}";
        assertEquals(expected, actual);        
    }
    
    @Test
    public void testDuplicateProperties() {
        JsonLd jsonLd = new JsonLd();
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://onto.test.org/", "onto");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.putProperty("http://xmlns.com/foaf/0.1/age", 31.533567);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/age", "http://www.w3.org/2001/XMLSchema#float");
        r1.putProperty("http://onto.test.org/age", 456);
        r1.putPropertyType("http://onto.test.org/age", "http://www.w3.org/2001/XMLSchema#int");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"onto\":\"http://onto.test.org/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:float\":\"foaf:age\",\"xsd:int\":\"onto:age\"}},\"@subject\":\"_:bnode1\",\"foaf:age\":31.533567,\"onto:age\":456}";
        assertEquals(expected, actual); 
    }
    
    @Test
    public void testSubjectWithSeveralTypes() {
        JsonLd jsonLd = new JsonLd();
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://onto.test.org/", "onto");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("xsd:String");
        r1.addType("foaf:name");
        r1.putProperty("http://xmlns.com/foaf/0.1/age", 31.533567);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/age", "http://www.w3.org/2001/XMLSchema#float");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"age\":\"http://xmlns.com/foaf/0.1/age\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"String\":\"http://www.w3.org/2001/XMLSchema#String\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:float\":\"age\"}},\"@subject\":\"_:bnode1\",\"@type\":[\"String\",\"name\"],\"age\":31.533567}";
        assertEquals(expected, actual); 
    }
    
    @Test
    public void testSubjectWithSeveralSameTypes() {
        JsonLd jsonLd = new JsonLd();
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://onto.test.org/", "onto");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("xsd:String");
        r1.addType("foaf:String");
        r1.putProperty("http://xmlns.com/foaf/0.1/age", 31.533567);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/age", "http://www.w3.org/2001/XMLSchema#float");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"foaf\":\"http://xmlns.com/foaf/0.1/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:float\":\"foaf:age\"}},\"@subject\":\"_:bnode1\",\"@type\":[\"foaf:String\",\"xsd:String\"],\"foaf:age\":31.533567}";
        assertEquals(expected, actual); 
    }
    
    @Test
    public void testSubjectsWithSeveralTypes() {
        JsonLd jsonLd = new JsonLd();
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://onto.test.org/", "onto");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("xsd:String");
        r1.addType("foaf:name");
        r1.putProperty("http://xmlns.com/foaf/0.1/age", 31.533567);
        r1.putPropertyType("http://xmlns.com/foaf/0.1/age", "http://www.w3.org/2001/XMLSchema#float");
        jsonLd.put(r1);
        
        JsonLdResource r2 = new JsonLdResource();
        r2.setSubject("_:bnode2");
        r2.addType("xsd:String");
        r2.addType("foaf:name");
        r2.putProperty("http://xmlns.com/foaf/0.1/age", 31.533567);
        r2.putPropertyType("http://xmlns.com/foaf/0.1/age", "http://www.w3.org/2001/XMLSchema#float");
        jsonLd.put(r2);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"age\":\"http://xmlns.com/foaf/0.1/age\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"String\":\"http://www.w3.org/2001/XMLSchema#String\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"xsd:float\":\"age\"}},\"@subject\":[{\"@subject\":\"_:bnode1\",\"@type\":[\"String\",\"name\"],\"age\":31.533567},{\"@subject\":\"_:bnode2\",\"@type\":[\"String\",\"name\"],\"age\":31.533567}]}";
        assertEquals(expected, actual); 
    }
    
    @Test
    public void testMultivaluedProperty() {
        JsonLd jsonLd = new JsonLd();
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://onto.test.org/", "onto");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("xsd:String");
        r1.addType("foaf:name");
        r1.putProperty("http://xmlns.com/foaf/0.1/age", 31.533567);
        r1.putProperty("http://xmlns.com/foaf/0.1/age", "de");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"age\":\"http://xmlns.com/foaf/0.1/age\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"String\":\"http://www.w3.org/2001/XMLSchema#String\"},\"@subject\":\"_:bnode1\",\"@type\":[\"String\",\"name\"],\"age\":[31.533567,\"de\"]}";
        assertEquals(expected, actual); 
    }

    @Test
    public void testMultivaluedTypedProperty() {
        JsonLd jsonLd = new JsonLd();
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://onto.test.org/", "onto");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("xsd:String");
        r1.addType("foaf:name");
        
        JsonLdProperty ageProperty = new JsonLdProperty("http://xmlns.com/foaf/0.1/age");
        
        JsonLdPropertyValue v1 = new JsonLdPropertyValue(31.533567);
        v1.setType("xsd:Float");
        ageProperty.addValue(v1);

        JsonLdPropertyValue v2 = new JsonLdPropertyValue("test");
        v2.setType("xsd:String");
        ageProperty.addValue(v2);

        r1.putProperty(ageProperty);
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"age\":\"http://xmlns.com/foaf/0.1/age\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"String\":\"http://www.w3.org/2001/XMLSchema#String\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"_:bnode1\",\"@type\":[\"String\",\"name\"],\"age\":[{\"@literal\":\"31.533567\",\"@datatype\":\"xsd:Float\"},{\"@literal\":\"test\",\"@datatype\":\"xsd:String\"}]}";
        assertEquals(expected, actual); 
    }
    
    @Test
    public void testMultivaluedTypedLangProperty() {
        JsonLd jsonLd = new JsonLd();
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://onto.test.org/", "onto");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("xsd:String");
        r1.addType("foaf:name");
        
        JsonLdProperty ageProperty = new JsonLdProperty("http://xmlns.com/foaf/0.1/age");
        JsonLdPropertyValue v1 = new JsonLdPropertyValue(31.533567);
        v1.setType("xsd:Float");
        ageProperty.addValue(v1);

        JsonLdPropertyValue v2 = new JsonLdPropertyValue("test");
        v2.setType("xsd:String");
        v2.setLanguage("en");
        ageProperty.addValue(v2);

        r1.putProperty(ageProperty);
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"age\":\"http://xmlns.com/foaf/0.1/age\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"String\":\"http://www.w3.org/2001/XMLSchema#String\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"_:bnode1\",\"@type\":[\"String\",\"name\"],\"age\":[{\"@literal\":\"31.533567\",\"@datatype\":\"xsd:Float\"},{\"@literal\":\"test\",\"@language\":\"en\",\"@datatype\":\"xsd:String\"}]}";
        assertEquals(expected, actual); 
    }
    
    
    @Test
    public void testMultivaluedMixedTypedLangProperty() {
        JsonLd jsonLd = new JsonLd();
        
        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");
        jsonLd.addNamespacePrefix("http://onto.test.org/", "onto");
        
        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("_:bnode1");
        r1.addType("xsd:String");
        r1.addType("foaf:name");
        
        JsonLdProperty ageProperty = new JsonLdProperty("http://xmlns.com/foaf/0.1/age");
        JsonLdPropertyValue v1 = new JsonLdPropertyValue(31.533567);
        v1.setType("xsd:Float");
        ageProperty.addValue(v1);

        JsonLdPropertyValue v2 = new JsonLdPropertyValue("test");
        v2.setType("xsd:String");
        v2.setLanguage("en");
        ageProperty.addValue(v2);

        r1.putProperty(ageProperty);
        r1.putProperty("http://xmlns.com/foaf/0.1/age", "On more untyped value");
        jsonLd.put(r1);
        
        String actual = jsonLd.toString();
        String expected = "{\"@context\":{\"age\":\"http://xmlns.com/foaf/0.1/age\",\"name\":\"http://xmlns.com/foaf/0.1/name\",\"String\":\"http://www.w3.org/2001/XMLSchema#String\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"_:bnode1\",\"@type\":[\"String\",\"name\"],\"age\":[{\"@literal\":\"31.533567\",\"@datatype\":\"xsd:Float\"},{\"@literal\":\"test\",\"@language\":\"en\",\"@datatype\":\"xsd:String\"},\"On more untyped value\"]}";
        assertEquals(expected, actual); 
    }

    @SuppressWarnings("unused")
    private void toConsole(String actual) {
        System.out.println(actual);
        String s = actual;
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("\n", "\\n");
        System.out.println(s);
    }
}
