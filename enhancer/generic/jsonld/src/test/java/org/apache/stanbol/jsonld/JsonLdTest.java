package org.apache.stanbol.jsonld;

import static org.junit.Assert.assertEquals;

import org.apache.stanbol.jsonld.JsonLd;
import org.apache.stanbol.jsonld.JsonLdResource;
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

        jsonLd.addNamespacePrefix("http://example.org/myvocab#", "myvocab");

        JsonLdResource jsonLdResource = new JsonLdResource();
        jsonLdResource.addType("foaf:Person");
        jsonLdResource.putProperty("foaf:name", "Manu Sporny");
        jsonLdResource.putProperty("foaf:homepage", "<http://manu.sporny.org/>");
        jsonLdResource.putProperty("sioc:avatar", "<http://twitter.com/account/profile_image/manusporny>");
        jsonLdResource.putProperty("myvocab:credits", 500);
        jsonLd.put("dummy", jsonLdResource);

        String actual = jsonLd.toString();
        String expected = "{\"#\":{\"myvocab\":\"http:\\/\\/example.org\\/myvocab#\"},\"a\":\"foaf:Person\",\"foaf:homepage\":\"<http:\\/\\/manu.sporny.org\\/>\",\"foaf:name\":\"Manu Sporny\",\"myvocab:credits\":500,\"sioc:avatar\":\"<http:\\/\\/twitter.com\\/account\\/profile_image\\/manusporny>\"}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"#\": {\n        \"myvocab\": \"http:\\/\\/example.org\\/myvocab#\"\n    },\n    \"a\": \"foaf:Person\",\n    \"foaf:homepage\": \"<http:\\/\\/manu.sporny.org\\/>\",\n    \"foaf:name\": \"Manu Sporny\",\n    \"myvocab:credits\": 500,    \"sioc:avatar\": \"<http:\\/\\/twitter.com\\/account\\/profile_image\\/manusporny>\"\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample2_JointGraph() {
        JsonLd jsonLd = new JsonLd();

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
        String expected = "{\"#\":{\"foaf\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\"},\"@\":[{\"@\":\"_:bnode1\",\"a\":\"foaf:Person\",\"foaf:homepage\":\"<http:\\/\\/example.com\\/bob>\",\"foaf:name\":\"Bob\"},{\"@\":\"_:bnode2\",\"a\":\"foaf:Person\",\"foaf:homepage\":\"<http:\\/\\/example.com\\/eve>\",\"foaf:name\":\"Eve\"},{\"@\":\"_:bnode3\",\"a\":\"foaf:Person\",\"foaf:homepage\":\"<http:\\/\\/example.com\\/bert>\",\"foaf:name\":\"Bert\"}]}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"#\": {\n        \"foaf\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\"\n    },\n    \"@\": [\n        {\n            \"@\": \"_:bnode1\",\n            \"a\": \"foaf:Person\",\n            \"foaf:homepage\": \"<http:\\/\\/example.com\\/bob>\",\n            \"foaf:name\": \"Bob\"\n        },\n        {\n            \"@\": \"_:bnode2\",\n            \"a\": \"foaf:Person\",\n            \"foaf:homepage\": \"<http:\\/\\/example.com\\/eve>\",\n            \"foaf:name\": \"Eve\"\n        },\n        {\n            \"@\": \"_:bnode3\",\n            \"a\": \"foaf:Person\",\n            \"foaf:homepage\": \"<http:\\/\\/example.com\\/bert>\",\n            \"foaf:name\": \"Bert\"\n        }\n    ]\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample2_DisjointGraph() {
        JsonLd jsonLd = new JsonLd();
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
        String expected = "[{\"#\":{\"foaf\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\"},\"@\":\"_:bnode1\",\"a\":\"foaf:Person\",\"foaf:homepage\":\"<http:\\/\\/example.com\\/bob>\",\"foaf:name\":\"Bob\"},{\"#\":{\"foaf\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\"},\"@\":\"_:bnode2\",\"a\":\"foaf:Person\",\"foaf:homepage\":\"<http:\\/\\/example.com\\/eve>\",\"foaf:name\":\"Eve\"},{\"#\":{\"foaf\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\"},\"@\":\"_:bnode3\",\"a\":\"foaf:Person\",\"foaf:homepage\":\"<http:\\/\\/example.com\\/eve>\",\"foaf:name\":\"Eve\"}]";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "[\n    {\n        \"#\": {\n            \"foaf\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\"\n        },\n        \"@\": \"_:bnode1\",\n        \"a\": \"foaf:Person\",\n        \"foaf:homepage\": \"<http:\\/\\/example.com\\/bob>\",\n        \"foaf:name\": \"Bob\"\n    },\n    {\n        \"#\": {\n            \"foaf\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\"\n        },\n        \"@\": \"_:bnode2\",\n        \"a\": \"foaf:Person\",\n        \"foaf:homepage\": \"<http:\\/\\/example.com\\/eve>\",\n        \"foaf:name\": \"Eve\"\n    },\n    {\n        \"#\": {\n            \"foaf\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\"\n        },\n        \"@\": \"_:bnode3\",\n        \"a\": \"foaf:Person\",\n        \"foaf:homepage\": \"<http:\\/\\/example.com\\/eve>\",\n        \"foaf:name\": \"Eve\"\n    }\n]";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample3() {
        JsonLd jsonLd = new JsonLd();

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
        String expected = "{\"#\":{\"fn\":\"http:\\/\\/microformats.org\\/profile\\/hcard#fn\",\"url\":\"http:\\/\\/microformats.org\\/profile\\/hcard#url\",\"vcard\":\"http:\\/\\/microformats.org\\/profile\\/hcard#vcard\"},\"@\":\"_:bnode1\",\"a\":\"vcard\",\"fn\":\"Tantek Celik\",\"url\":\"<http:\\/\\/tantek.com\\/>\"}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"#\": {\n        \"fn\": \"http:\\/\\/microformats.org\\/profile\\/hcard#fn\",\n        \"url\": \"http:\\/\\/microformats.org\\/profile\\/hcard#url\",\n        \"vcard\": \"http:\\/\\/microformats.org\\/profile\\/hcard#vcard\"\n    },\n    \"@\": \"_:bnode1\",\n    \"a\": \"vcard\",\n    \"fn\": \"Tantek Celik\",\n    \"url\": \"<http:\\/\\/tantek.com\\/>\"\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample3DefaultContext() {
        JsonLd jsonLd = new JsonLd();

        jsonLd.addNamespacePrefix("http://example.org/default-vocab#","#vocab");
        jsonLd.addNamespacePrefix("http://example.org/baseurl/","#base");
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
        String expected = "{\"#\":{\"#base\":\"http:\\/\\/example.org\\/baseurl\\/\",\"#vocab\":\"http:\\/\\/example.org\\/default-vocab#\",\"cal\":\"http:\\/\\/www.w3.org\\/2002\\/12\\/cal\\/ical#\",\"cc\":\"http:\\/\\/creativecommons.org\\/ns#\",\"dc\":\"http:\\/\\/purl.org\\/dc\\/terms\\/\",\"doap\":\"http:\\/\\/usefulinc.com\\/ns\\/doap#\",\"foaf\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\",\"geo\":\"http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#\",\"homepage\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/homepage\",\"name\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/name\",\"Person\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/Person\",\"sioc\":\"http:\\/\\/rdfs.org\\/sioc\\/ns#\",\"vcard\":\"http:\\/\\/www.w3.org\\/2006\\/vcard\\/ns#\",\"xsd\":\"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"}}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"#\": {\n        \"#base\": \"http:\\/\\/example.org\\/baseurl\\/\",\n        \"#vocab\": \"http:\\/\\/example.org\\/default-vocab#\",\n        \"cal\": \"http:\\/\\/www.w3.org\\/2002\\/12\\/cal\\/ical#\",\n        \"cc\": \"http:\\/\\/creativecommons.org\\/ns#\",\n        \"dc\": \"http:\\/\\/purl.org\\/dc\\/terms\\/\",\n        \"doap\": \"http:\\/\\/usefulinc.com\\/ns\\/doap#\",\n        \"foaf\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\",\n        \"geo\": \"http:\\/\\/www.w3.org\\/2003\\/01\\/geo\\/wgs84_pos#\",\n        \"homepage\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/homepage\",\n        \"name\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/name\",\n        \"Person\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/Person\",\n        \"sioc\": \"http:\\/\\/rdfs.org\\/sioc\\/ns#\",\n        \"vcard\": \"http:\\/\\/www.w3.org\\/2006\\/vcard\\/ns#\",\n        \"xsd\": \"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"\n    }\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample4Microformats() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setUseJointGraphs(false);

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://purl.oreilly.com/works/45U8QJGZSQKDH8N>");
        r1.addType("http://purl.org/vocab/frbr/core#Work");
        r1.putProperty("http://purl.org/dc/terms/title", "Just a Geek");
        r1.putProperty("http://purl.org/dc/terms/creator", "Whil Wheaton");
        String [] realizationValues = {"<http://purl.oreilly.com/products/9780596007683.BOOK>", "<http://purl.oreilly.com/products/9780596802189.EBOOK>"};
        r1.putProperty("http://purl.org/vocab/frbr/core#realization", realizationValues);
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
        String expected = "[{\"@\":\"<http:\\/\\/purl.oreilly.com\\/products\\/9780596007683.BOOK>\",\"a\":\"<http:\\/\\/purl.org\\/vocab\\/frbr\\/core#Expression>\",\"http:\\/\\/purl.org\\/dc\\/terms\\/type\":\"<http:\\/\\/purl.oreilly.com\\/product-types\\/BOOK>\"},{\"@\":\"<http:\\/\\/purl.oreilly.com\\/products\\/9780596802189.EBOOK>\",\"a\":\"http:\\/\\/purl.org\\/vocab\\/frbr\\/core#Expression\",\"http:\\/\\/purl.org\\/dc\\/terms\\/type\":\"<http:\\/\\/purl.oreilly.com\\/product-types\\/BOOK>\"},{\"@\":\"<http:\\/\\/purl.oreilly.com\\/works\\/45U8QJGZSQKDH8N>\",\"a\":\"http:\\/\\/purl.org\\/vocab\\/frbr\\/core#Work\",\"http:\\/\\/purl.org\\/dc\\/terms\\/creator\":\"Whil Wheaton\",\"http:\\/\\/purl.org\\/dc\\/terms\\/title\":\"Just a Geek\",\"http:\\/\\/purl.org\\/vocab\\/frbr\\/core#realization\":[\"<http:\\/\\/purl.oreilly.com\\/products\\/9780596007683.BOOK>\",\"<http:\\/\\/purl.oreilly.com\\/products\\/9780596802189.EBOOK>\"]}]";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "[\n    {\n        \"@\": \"<http:\\/\\/purl.oreilly.com\\/products\\/9780596007683.BOOK>\",\n        \"a\": \"<http:\\/\\/purl.org\\/vocab\\/frbr\\/core#Expression>\",\n        \"http:\\/\\/purl.org\\/dc\\/terms\\/type\": \"<http:\\/\\/purl.oreilly.com\\/product-types\\/BOOK>\"\n    },\n    {\n        \"@\": \"<http:\\/\\/purl.oreilly.com\\/products\\/9780596802189.EBOOK>\",\n        \"a\": \"http:\\/\\/purl.org\\/vocab\\/frbr\\/core#Expression\",\n        \"http:\\/\\/purl.org\\/dc\\/terms\\/type\": \"<http:\\/\\/purl.oreilly.com\\/product-types\\/BOOK>\"\n    },\n    {\n        \"@\": \"<http:\\/\\/purl.oreilly.com\\/works\\/45U8QJGZSQKDH8N>\",\n        \"a\": \"http:\\/\\/purl.org\\/vocab\\/frbr\\/core#Work\",\n        \"http:\\/\\/purl.org\\/dc\\/terms\\/creator\": \"Whil Wheaton\",\n        \"http:\\/\\/purl.org\\/dc\\/terms\\/title\": \"Just a Geek\",\n        \"http:\\/\\/purl.org\\/vocab\\/frbr\\/core#realization\": [\n            \"<http:\\/\\/purl.oreilly.com\\/products\\/9780596007683.BOOK>\",\n            \"<http:\\/\\/purl.oreilly.com\\/products\\/9780596802189.EBOOK>\"\n        ]\n    }\n]";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample5TypedLiterals() {
        JsonLd jsonLd = new JsonLd();

        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://purl.org/dc/terms/", "dc");

        JsonLdResource r1 = new JsonLdResource();
        r1.putProperty("http://purl.org/dc/terms/modified", "2010-05-29T14:17:39+02:00^^http://www.w3.org/2001/XMLSchema#dateTime");
        jsonLd.put("r1", r1);

        String actual = jsonLd.toString();
        String expected = "{\"#\":{\"dc\":\"http:\\/\\/purl.org\\/dc\\/terms\\/\",\"xsd\":\"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"},\"dc:modified\":\"2010-05-29T14:17:39+02:00^^xsd:dateTime\"}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"#\": {\n        \"dc\": \"http:\\/\\/purl.org\\/dc\\/terms\\/\",\n        \"xsd\": \"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"\n    },\n    \"dc:modified\": \"2010-05-29T14:17:39+02:00^^xsd:dateTime\"\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample6MultipleObjects() {
        JsonLd jsonLd = new JsonLd();

        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        String [] nicks = new String [] {"\"stu\"^^http://www.w3.org/2001/XMLSchema#string", "\"groknar\"^^http://www.w3.org/2001/XMLSchema#string", "\"radface\"^^http://www.w3.org/2001/XMLSchema#string"};
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nicks);
        jsonLd.put("r1", r1);

        String actual = jsonLd.toString();
        String expected = "{\"#\":{\"foaf\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\",\"xsd\":\"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"},\"@\":\"<http:\\/\\/example.org\\/people#joebob>\",\"foaf:nick\":[\"\\\"stu\\\"^^xsd:string\",\"\\\"groknar\\\"^^xsd:string\",\"\\\"radface\\\"^^xsd:string\"]}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"#\": {\n        \"foaf\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\",\n        \"xsd\": \"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"\n    },\n    \"@\": \"<http:\\/\\/example.org\\/people#joebob>\",\n    \"foaf:nick\": [\n        \"\\\"stu\\\"^^xsd:string\",\n        \"\\\"groknar\\\"^^xsd:string\",\n        \"\\\"radface\\\"^^xsd:string\"\n    ]\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample7NoNSApply() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(false);

        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        String [] nicks = new String [] {"\"stu\"^^http://www.w3.org/2001/XMLSchema#string", "\"groknar\"^^http://www.w3.org/2001/XMLSchema#string", "\"radface\"^^http://www.w3.org/2001/XMLSchema#string"};
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nicks);
        jsonLd.put("r1", r1);

        String actual = jsonLd.toString();
        String expected = "{\"#\":{\"foaf\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\",\"xsd\":\"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"},\"@\":\"<http:\\/\\/example.org\\/people#joebob>\",\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/nick\":[\"\\\"stu\\\"^^http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string\",\"\\\"groknar\\\"^^http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string\",\"\\\"radface\\\"^^http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string\"]}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"#\": {\n        \"foaf\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\",\n        \"xsd\": \"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"\n    },\n    \"@\": \"<http:\\/\\/example.org\\/people#joebob>\",\n    \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/nick\": [\n        \"\\\"stu\\\"^^http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string\",\n        \"\\\"groknar\\\"^^http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string\",\n        \"\\\"radface\\\"^^http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string\"\n    ]\n}";
        assertEquals(expectedIndent, actualIndent);
    }

    @Test
    public void testSpecExample8NoNSApply() {
        JsonLd jsonLd = new JsonLd();
        jsonLd.setApplyNamespaces(false);

        jsonLd.addNamespacePrefix("http://www.w3.org/2001/XMLSchema#", "xsd");
        jsonLd.addNamespacePrefix("http://xmlns.com/foaf/0.1/", "foaf");

        JsonLdResource r1 = new JsonLdResource();
        r1.setSubject("<http://example.org/people#joebob>");
        String [] nicks = new String [] {"\"stu\"^^xsd:string", "\"groknar\"^^xsd:string", "\"radface\"^^xsd:string"};
        r1.putProperty("http://xmlns.com/foaf/0.1/nick", nicks);
        jsonLd.put("r1", r1);

        String actual = jsonLd.toString();
        String expected = "{\"#\":{\"foaf\":\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\",\"xsd\":\"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"},\"@\":\"<http:\\/\\/example.org\\/people#joebob>\",\"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/nick\":[\"\\\"stu\\\"^^xsd:string\",\"\\\"groknar\\\"^^xsd:string\",\"\\\"radface\\\"^^xsd:string\"]}";
        assertEquals(expected, actual);

        String actualIndent = jsonLd.toString(4);
        String expectedIndent = "{\n    \"#\": {\n        \"foaf\": \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/\",\n        \"xsd\": \"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"\n    },\n    \"@\": \"<http:\\/\\/example.org\\/people#joebob>\",\n    \"http:\\/\\/xmlns.com\\/foaf\\/0.1\\/nick\": [\n        \"\\\"stu\\\"^^xsd:string\",\n        \"\\\"groknar\\\"^^xsd:string\",\n        \"\\\"radface\\\"^^xsd:string\"\n    ]\n}";
        assertEquals(expectedIndent, actualIndent);
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
