package org.apache.stanbol.enhancer.jersey.writers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.jersey.writers.JsonLdSerializerProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.TextAnnotation;
import org.apache.stanbol.enhancer.servicesapi.helper.RdfEntityFactory;
import org.apache.stanbol.enhancer.servicesapi.rdf.OntologicalClasses;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class JsonLdSerializerProviderTest {

    private JsonLdSerializerProvider jsonldProvider;
    private String formatIdentifier = "application/json";

    @Before
    public void setup() {
        jsonldProvider = new JsonLdSerializerProvider();
    }

    @Test
    public void testWrongFormat() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived "
            + "in New Zealand and worked at the University of Otago.";
        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), "application/format+notsupported");
    }

    @Test
    public void testSingleSubjectSerializeNoNs() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived "
            + "in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setIndentation(0);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\"@\":\"<urn:iks-project:enhancer:test:text-annotation:Person>\",\"a\":[\"<http:\\/\\/fise.iks-project.eu\\/ontology\\/Enhancement>\",\"<http:\\/\\/fise.iks-project.eu\\/ontology\\/TextAnnotation>\"],\"http:\\/\\/fise.iks-project.eu\\/ontology\\/end\":\"\\\"20\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#int>\",\"http:\\/\\/fise.iks-project.eu\\/ontology\\/selected-text\":\"\\\"Patrick Marshall\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string>\",\"http:\\/\\/fise.iks-project.eu\\/ontology\\/selection-context\":\"\\\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string>\",\"http:\\/\\/fise.iks-project.eu\\/ontology\\/start\":\"\\\"4\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#int>\",\"http:\\/\\/purl.org\\/dc\\/terms\\/created\":\"\\\"2010-10-27T14:00:00+02:00\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#dateTime>\",\"http:\\/\\/purl.org\\/dc\\/terms\\/creator\":\"<urn:iks-project:enhancer:test:dummyEngine>\",\"http:\\/\\/purl.org\\/dc\\/terms\\/type\":\"<http:\\/\\/dbpedia.org\\/ontology\\/Person>\"}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testSingleSubjectSerializeNoNsWithIndent() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\n  \"@\": \"<urn:iks-project:enhancer:test:text-annotation:Person>\",\n  \"a\": [\n    \"<http:\\/\\/fise.iks-project.eu\\/ontology\\/Enhancement>\",\n    \"<http:\\/\\/fise.iks-project.eu\\/ontology\\/TextAnnotation>\"\n  ],\n  \"http:\\/\\/fise.iks-project.eu\\/ontology\\/end\": \"\\\"20\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#int>\",\n  \"http:\\/\\/fise.iks-project.eu\\/ontology\\/selected-text\": \"\\\"Patrick Marshall\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string>\",\n  \"http:\\/\\/fise.iks-project.eu\\/ontology\\/selection-context\": \"\\\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#string>\",\n  \"http:\\/\\/fise.iks-project.eu\\/ontology\\/start\": \"\\\"4\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#int>\",\n  \"http:\\/\\/purl.org\\/dc\\/terms\\/created\": \"\\\"2010-10-27T14:00:00+02:00\\\"^^<http:\\/\\/www.w3.org\\/2001\\/XMLSchema#dateTime>\",\n  \"http:\\/\\/purl.org\\/dc\\/terms\\/creator\": \"<urn:iks-project:enhancer:test:dummyEngine>\",\n  \"http:\\/\\/purl.org\\/dc\\/terms\\/type\": \"<http:\\/\\/dbpedia.org\\/ontology\\/Person>\"\n}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testSingleSubjectSerializeWithNs() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("http://fise.iks-project.eu/ontology/", "enhancer");
        nsMap.put("http://www.w3.org/2001/XMLSchema#", "xmlns");
        nsMap.put("http://dbpedia.org/ontology/", "dbpedia");
        nsMap.put("http://purl.org/dc/terms", "dcterms");
        jsonldProvider.setIndentation(0);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\"#\":{\"dbpedia\":\"http:\\/\\/dbpedia.org\\/ontology\\/\",\"dcterms\":\"http:\\/\\/purl.org\\/dc\\/terms\",\"enhancer\":\"http:\\/\\/fise.iks-project.eu\\/ontology\\/\",\"xmlns\":\"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"},\"@\":\"<urn:iks-project:enhancer:test:text-annotation:Person>\",\"a\":[\"<enhancer:Enhancement>\",\"<enhancer:TextAnnotation>\"],\"dcterms:\\/created\":\"\\\"2010-10-27T14:00:00+02:00\\\"^^<xmlns:dateTime>\",\"dcterms:\\/creator\":\"<urn:iks-project:enhancer:test:dummyEngine>\",\"dcterms:\\/type\":\"<dbpedia:Person>\",\"enhancer:end\":\"\\\"20\\\"^^<xmlns:int>\",\"enhancer:selected-text\":\"\\\"Patrick Marshall\\\"^^<xmlns:string>\",\"enhancer:selection-context\":\"\\\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\\\"^^<xmlns:string>\",\"enhancer:start\":\"\\\"4\\\"^^<xmlns:int>\"}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testSingleSubjectSerializeWithNsWithIndent() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("http://fise.iks-project.eu/ontology/", "enhancer");
        nsMap.put("http://www.w3.org/2001/XMLSchema#", "xmlns");
        nsMap.put("http://dbpedia.org/ontology/", "dbpedia");
        nsMap.put("http://purl.org/dc/terms", "dcterms");
        jsonldProvider.setIndentation(4);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\n    \"#\": {\n        \"dbpedia\": \"http:\\/\\/dbpedia.org\\/ontology\\/\",\n        \"dcterms\": \"http:\\/\\/purl.org\\/dc\\/terms\",\n        \"enhancer\": \"http:\\/\\/fise.iks-project.eu\\/ontology\\/\",\n        \"xmlns\": \"http:\\/\\/www.w3.org\\/2001\\/XMLSchema#\"\n    },\n    \"@\": \"<urn:iks-project:enhancer:test:text-annotation:Person>\",\n    \"a\": [\n        \"<enhancer:Enhancement>\",\n        \"<enhancer:TextAnnotation>\"\n    ],\n    \"dcterms:\\/created\": \"\\\"2010-10-27T14:00:00+02:00\\\"^^<xmlns:dateTime>\",\n    \"dcterms:\\/creator\": \"<urn:iks-project:enhancer:test:dummyEngine>\",\n    \"dcterms:\\/type\": \"<dbpedia:Person>\",\n    \"enhancer:end\": \"\\\"20\\\"^^<xmlns:int>\",\n    \"enhancer:selected-text\": \"\\\"Patrick Marshall\\\"^^<xmlns:string>\",\n    \"enhancer:selection-context\": \"\\\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\\\"^^<xmlns:string>\",\n    \"enhancer:start\": \"\\\"4\\\"^^<xmlns:int>\"\n}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }

    private ContentItem getContentItem(final String id, final String text) {
        return new ContentItem() {

            SimpleMGraph metadata = new SimpleMGraph();

            public InputStream getStream() {
                return new ByteArrayInputStream(text.getBytes());
            }

            public String getMimeType() {
                return "text/plain";
            }

            public MGraph getMetadata() {
                return metadata;
            }

            public String getId() {
                return id;
            }
        };
    }

    private void getTextAnnotation(ContentItem ci, String annotationURNExtension, String namedEntity, String context, UriRef type) {
        String content;
        try {
            content = IOUtils.toString(ci.getStream());
        } catch (IOException e) {
            // should never happen anyway!
            content = "";
        }
        RdfEntityFactory factory = RdfEntityFactory.createInstance(ci.getMetadata());
        TextAnnotation testAnnotation = factory.getProxy(new UriRef("urn:iks-project:enhancer:test:text-annotation:" + annotationURNExtension), TextAnnotation.class);
        testAnnotation.setCreator(new UriRef("urn:iks-project:enhancer:test:dummyEngine"));

        Calendar myCal = Calendar.getInstance();
        myCal.set(2010, 9, 27, 12, 0, 0);
        myCal.set(Calendar.MILLISECOND, 0);
        myCal.setTimeZone(TimeZone.getTimeZone("Germany/Berlin"));
        testAnnotation.setCreated(myCal.getTime());

        testAnnotation.setSelectedText(namedEntity);
        testAnnotation.setSelectionContext(context);
        testAnnotation.getDcType().add(type);
        Integer start = content.indexOf(namedEntity);
        if (start < 0) { // if not found in the content set start to 42
            start = 42;
        }
        testAnnotation.setStart(start);
        testAnnotation.setEnd(start + namedEntity.length());
    }

    @SuppressWarnings("unused")
    private void toConsole(String result) {
        System.out.println(result);
        String s = result;
        s = s.replaceAll("\\\\", "\\\\\\\\");
        s = s.replace("\"", "\\\"");
        s = s.replace("\n", "\\n");
        System.out.println(s);
    }

}
