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
package org.apache.stanbol.commons.web.base.writers;

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
import org.apache.clerezza.rdf.core.impl.util.W3CDateFormat;
import org.apache.commons.io.IOUtils;
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

    private String expectedW3CFormattedDate;
    
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
    public void testEmptyGraph() {
        MGraph emptyGraph = new SimpleMGraph();
        
        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setIndentation(0);
        jsonldProvider.serialize(serializedGraph, emptyGraph, formatIdentifier);
        
        String expected = "{}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testEmptyGraphWithIndent() {
        MGraph emptyGraph = new SimpleMGraph();
        
        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setIndentation(4);
        jsonldProvider.serialize(serializedGraph, emptyGraph, formatIdentifier);
        
        String expected = "{\n\n}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testSingleSubjectSerializeNoNs() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived "
            + "in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setIndentation(0);
        jsonldProvider.setUseTypeCoercion(false);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);
        
        String expected = "{\"@context\":{\"dc\":\"http://purl.org/dc/terms/\",\"enhancer\":\"http://fise.iks-project.eu/ontology/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"<enhancer:Enhancement>\",\"<enhancer:TextAnnotation>\"],\"dc:created\":{\"@literal\":\"" + this.expectedW3CFormattedDate + "\",\"@datatype\":\"xsd:dateTime\"},\"dc:creator\":{\"@literal\":\"urn:iks-project:enhancer:test:dummyEngine\",\"@datatype\":\"@iri\"},\"dc:type\":{\"@literal\":\"http://dbpedia.org/ontology/Person\",\"@datatype\":\"@iri\"},\"enhancer:end\":{\"@literal\":\"20\",\"@datatype\":\"xsd:int\"},\"enhancer:selected-text\":{\"@literal\":\"Patrick Marshall\",\"@datatype\":\"xsd:string\"},\"enhancer:selection-context\":{\"@literal\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"@datatype\":\"xsd:string\"},\"enhancer:start\":{\"@literal\":\"4\",\"@datatype\":\"xsd:int\"}}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testSingleSubjectSerializeNoNsWithCoercion() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived "
            + "in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setIndentation(0);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);
        
        String expected = "{\"@context\":{\"dbp-ont\":\"http://dbpedia.org/ontology/\",\"dc\":\"http://purl.org/dc/terms/\",\"enhancer\":\"http://fise.iks-project.eu/ontology/\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"@iri\":[\"dc:creator\",\"dc:type\"],\"xsd:dateTime\":\"dc:created\",\"xsd:int\":[\"enhancer:end\",\"enhancer:start\"],\"xsd:string\":[\"enhancer:selected-text\",\"enhancer:selection-context\"]}},\"@subject\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"<enhancer:Enhancement>\",\"<enhancer:TextAnnotation>\"],\"dc:created\":\""+this.expectedW3CFormattedDate+"\",\"dc:creator\":\"urn:iks-project:enhancer:test:dummyEngine\",\"dc:type\":\"dbp-ont:Person\",\"enhancer:end\":20,\"enhancer:selected-text\":\"Patrick Marshall\",\"enhancer:selection-context\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"enhancer:start\":4}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testSingleSubjectSerializeNoNsWithIndent() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setUseTypeCoercion(false);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\n  \"@context\": {\n    \"dc\": \"http://purl.org/dc/terms/\",\n    \"enhancer\": \"http://fise.iks-project.eu/ontology/\",\n    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\"\n  },\n  \"@subject\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n  \"@type\": [\n    \"<enhancer:Enhancement>\",\n    \"<enhancer:TextAnnotation>\"\n  ],\n  \"dc:created\": {\n    \"@literal\": \""+this.expectedW3CFormattedDate+"\",\n    \"@datatype\": \"xsd:dateTime\"\n  },\n  \"dc:creator\": {\n    \"@literal\": \"urn:iks-project:enhancer:test:dummyEngine\",\n    \"@datatype\": \"@iri\"\n  },\n  \"dc:type\": {\n    \"@literal\": \"http://dbpedia.org/ontology/Person\",\n    \"@datatype\": \"@iri\"\n  },\n  \"enhancer:end\": {\n    \"@literal\": \"20\",\n    \"@datatype\": \"xsd:int\"\n  },\n  \"enhancer:selected-text\": {\n    \"@literal\": \"Patrick Marshall\",\n    \"@datatype\": \"xsd:string\"\n  },\n  \"enhancer:selection-context\": {\n    \"@literal\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\n    \"@datatype\": \"xsd:string\"\n  },\n  \"enhancer:start\": {\n    \"@literal\": \"4\",\n    \"@datatype\": \"xsd:int\"\n  }\n}";
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
        jsonldProvider.setUseTypeCoercion(false);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\"@context\":{\"dcterms\":\"http://purl.org/dc/terms\",\"enhancer\":\"http://fise.iks-project.eu/ontology/\",\"xmlns\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"<enhancer:Enhancement>\",\"<enhancer:TextAnnotation>\"],\"dcterms:/created\":{\"@literal\":\""+this.expectedW3CFormattedDate+"\",\"@datatype\":\"xmlns:dateTime\"},\"dcterms:/creator\":{\"@literal\":\"urn:iks-project:enhancer:test:dummyEngine\",\"@datatype\":\"@iri\"},\"dcterms:/type\":{\"@literal\":\"http://dbpedia.org/ontology/Person\",\"@datatype\":\"@iri\"},\"enhancer:end\":{\"@literal\":\"20\",\"@datatype\":\"xmlns:int\"},\"enhancer:selected-text\":{\"@literal\":\"Patrick Marshall\",\"@datatype\":\"xmlns:string\"},\"enhancer:selection-context\":{\"@literal\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"@datatype\":\"xmlns:string\"},\"enhancer:start\":{\"@literal\":\"4\",\"@datatype\":\"xmlns:int\"}}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testSingleSubjectSerializeWithNsWithCoercion() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("http://fise.iks-project.eu/ontology/", "enhancer");
        nsMap.put("http://www.w3.org/2001/XMLSchema#", "xmlns");
        nsMap.put("http://dbpedia.org/ontology/", "dbpedia");
        nsMap.put("http://purl.org/dc/terms/", "dcterms");
        jsonldProvider.setIndentation(0);
        jsonldProvider.setUseTypeCoercion(true);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\"@context\":{\"dbpedia\":\"http://dbpedia.org/ontology/\",\"dcterms\":\"http://purl.org/dc/terms/\",\"enhancer\":\"http://fise.iks-project.eu/ontology/\",\"xmlns\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"@iri\":[\"dcterms:creator\",\"dcterms:type\"],\"xmlns:dateTime\":\"dcterms:created\",\"xmlns:int\":[\"enhancer:end\",\"enhancer:start\"],\"xmlns:string\":[\"enhancer:selected-text\",\"enhancer:selection-context\"]}},\"@subject\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"<enhancer:Enhancement>\",\"<enhancer:TextAnnotation>\"],\"dcterms:created\":\""+this.expectedW3CFormattedDate+"\",\"dcterms:creator\":\"urn:iks-project:enhancer:test:dummyEngine\",\"dcterms:type\":\"dbpedia:Person\",\"enhancer:end\":20,\"enhancer:selected-text\":\"Patrick Marshall\",\"enhancer:selection-context\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"enhancer:start\":4}";
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
        nsMap.put("http://purl.org/dc/terms/", "dcterms");
        jsonldProvider.setIndentation(4);
        jsonldProvider.setUseTypeCoercion(false);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\n    \"@context\": {\n        \"dcterms\": \"http://purl.org/dc/terms/\",\n        \"enhancer\": \"http://fise.iks-project.eu/ontology/\",\n        \"xmlns\": \"http://www.w3.org/2001/XMLSchema#\"\n    },\n    \"@subject\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n    \"@type\": [\n        \"<enhancer:Enhancement>\",\n        \"<enhancer:TextAnnotation>\"\n    ],\n    \"dcterms:created\": {\n        \"@literal\": \""+this.expectedW3CFormattedDate+"\",\n        \"@datatype\": \"xmlns:dateTime\"\n    },\n    \"dcterms:creator\": {\n        \"@literal\": \"urn:iks-project:enhancer:test:dummyEngine\",\n        \"@datatype\": \"@iri\"\n    },\n    \"dcterms:type\": {\n        \"@literal\": \"http://dbpedia.org/ontology/Person\",\n        \"@datatype\": \"@iri\"\n    },\n    \"enhancer:end\": {\n        \"@literal\": \"20\",\n        \"@datatype\": \"xmlns:int\"\n    },\n    \"enhancer:selected-text\": {\n        \"@literal\": \"Patrick Marshall\",\n        \"@datatype\": \"xmlns:string\"\n    },\n    \"enhancer:selection-context\": {\n        \"@literal\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\n        \"@datatype\": \"xmlns:string\"\n    },\n    \"enhancer:start\": {\n        \"@literal\": \"4\",\n        \"@datatype\": \"xmlns:int\"\n    }\n}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testSingleSubjectSerializeWithNsWithIndentWithCoercion() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        ContentItem ci = getContentItem("urn:iks-project:enhancer:test:content-item:person", context);
        getTextAnnotation(ci, "Person", "Patrick Marshall", context, OntologicalClasses.DBPEDIA_PERSON);

        OutputStream serializedGraph = new ByteArrayOutputStream();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("http://fise.iks-project.eu/ontology/", "enhancer");
        nsMap.put("http://www.w3.org/2001/XMLSchema#", "xmlns");
        nsMap.put("http://dbpedia.org/ontology/", "dbpedia");
        nsMap.put("http://purl.org/dc/terms/", "dcterms");
        jsonldProvider.setIndentation(4);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.setUseTypeCoercion(true);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);

        String expected = "{\n    \"@context\": {\n        \"dbpedia\": \"http://dbpedia.org/ontology/\",\n        \"dcterms\": \"http://purl.org/dc/terms/\",\n        \"enhancer\": \"http://fise.iks-project.eu/ontology/\",\n        \"xmlns\": \"http://www.w3.org/2001/XMLSchema#\",\n        \"@coerce\": {\n            \"@iri\": [\n                \"dcterms:creator\",\n                \"dcterms:type\"\n            ],\n            \"xmlns:dateTime\": \"dcterms:created\",\n            \"xmlns:int\": [\n                \"enhancer:end\",\n                \"enhancer:start\"\n            ],\n            \"xmlns:string\": [\n                \"enhancer:selected-text\",\n                \"enhancer:selection-context\"\n            ]\n        }\n    },\n    \"@subject\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n    \"@type\": [\n        \"<enhancer:Enhancement>\",\n        \"<enhancer:TextAnnotation>\"\n    ],\n    \"dcterms:created\": \""+this.expectedW3CFormattedDate+"\",\n    \"dcterms:creator\": \"urn:iks-project:enhancer:test:dummyEngine\",\n    \"dcterms:type\": \"dbpedia:Person\",\n    \"enhancer:end\": 20,\n    \"enhancer:selected-text\": \"Patrick Marshall\",\n    \"enhancer:selection-context\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\n    \"enhancer:start\": 4\n}";
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
            content = IOUtils.toString(ci.getStream(),"UTF-8");
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
        myCal.setTimeZone(TimeZone.getTimeZone("UTC"));
        testAnnotation.setCreated(myCal.getTime());
        
        this.expectedW3CFormattedDate = new W3CDateFormat().format(myCal.getTime());
        
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
