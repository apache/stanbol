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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
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
        
        String expected = "{\"@context\":{\"created\":\"http://purl.org/dc/terms/created\",\"creator\":\"http://purl.org/dc/terms/creator\",\"end\":\"http://fise.iks-project.eu/ontology/end\",\"Enhancement\":\"http://fise.iks-project.eu/ontology/Enhancement\",\"Person\":\"http://dbpedia.org/ontology/Person\",\"selected-text\":\"http://fise.iks-project.eu/ontology/selected-text\",\"selection-context\":\"http://fise.iks-project.eu/ontology/selection-context\",\"start\":\"http://fise.iks-project.eu/ontology/start\",\"TextAnnotation\":\"http://fise.iks-project.eu/ontology/TextAnnotation\",\"type\":\"http://purl.org/dc/terms/type\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"Enhancement\",\"TextAnnotation\"],\"created\":{\"@literal\":\""+this.expectedW3CFormattedDate+"\",\"@datatype\":\"xsd:dateTime\"},\"creator\":{\"@iri\":\"urn:iks-project:enhancer:test:dummyEngine\"},\"end\":{\"@literal\":\"20\",\"@datatype\":\"xsd:int\"},\"selected-text\":{\"@literal\":\"Patrick Marshall\",\"@datatype\":\"xsd:string\"},\"selection-context\":{\"@literal\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"@datatype\":\"xsd:string\"},\"start\":{\"@literal\":\"4\",\"@datatype\":\"xsd:int\"},\"type\":{\"@iri\":\"Person\"}}";
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
        jsonldProvider.setUseTypeCoercion(true);
        jsonldProvider.serialize(serializedGraph, ci.getMetadata(), formatIdentifier);
        
        String expected = "{\"@context\":{\"created\":\"http://purl.org/dc/terms/created\",\"creator\":\"http://purl.org/dc/terms/creator\",\"end\":\"http://fise.iks-project.eu/ontology/end\",\"Enhancement\":\"http://fise.iks-project.eu/ontology/Enhancement\",\"Person\":\"http://dbpedia.org/ontology/Person\",\"selected-text\":\"http://fise.iks-project.eu/ontology/selected-text\",\"selection-context\":\"http://fise.iks-project.eu/ontology/selection-context\",\"start\":\"http://fise.iks-project.eu/ontology/start\",\"TextAnnotation\":\"http://fise.iks-project.eu/ontology/TextAnnotation\",\"type\":\"http://purl.org/dc/terms/type\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"@iri\":[\"creator\",\"type\"],\"xsd:dateTime\":\"created\",\"xsd:int\":[\"end\",\"start\"],\"xsd:string\":[\"selected-text\",\"selection-context\"]}},\"@subject\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"Enhancement\",\"TextAnnotation\"],\"created\":\""+this.expectedW3CFormattedDate+"\",\"creator\":\"urn:iks-project:enhancer:test:dummyEngine\",\"end\":20,\"selected-text\":\"Patrick Marshall\",\"selection-context\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"start\":4,\"type\":\"Person\"}";
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

        String expected = "{\n  \"@context\": {\n    \"created\": \"http://purl.org/dc/terms/created\",\n    \"creator\": \"http://purl.org/dc/terms/creator\",\n    \"end\": \"http://fise.iks-project.eu/ontology/end\",\n    \"Enhancement\": \"http://fise.iks-project.eu/ontology/Enhancement\",\n    \"Person\": \"http://dbpedia.org/ontology/Person\",\n    \"selected-text\": \"http://fise.iks-project.eu/ontology/selected-text\",\n    \"selection-context\": \"http://fise.iks-project.eu/ontology/selection-context\",\n    \"start\": \"http://fise.iks-project.eu/ontology/start\",\n    \"TextAnnotation\": \"http://fise.iks-project.eu/ontology/TextAnnotation\",\n    \"type\": \"http://purl.org/dc/terms/type\",\n    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\"\n  },\n  \"@subject\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n  \"@type\": [\n    \"Enhancement\",\n    \"TextAnnotation\"\n  ],\n  \"created\": {\n    \"@literal\": \""+this.expectedW3CFormattedDate+"\",\n    \"@datatype\": \"xsd:dateTime\"\n  },\n  \"creator\": {\n    \"@iri\": \"urn:iks-project:enhancer:test:dummyEngine\"\n  },\n  \"end\": {\n    \"@literal\": \"20\",\n    \"@datatype\": \"xsd:int\"\n  },\n  \"selected-text\": {\n    \"@literal\": \"Patrick Marshall\",\n    \"@datatype\": \"xsd:string\"\n  },\n  \"selection-context\": {\n    \"@literal\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\n    \"@datatype\": \"xsd:string\"\n  },\n  \"start\": {\n    \"@literal\": \"4\",\n    \"@datatype\": \"xsd:int\"\n  },\n  \"type\": {\n    \"@iri\": \"Person\"\n  }\n}";
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

        String expected = "{\"@context\":{\"/created\":\"http://purl.org/dc/terms/created\",\"/creator\":\"http://purl.org/dc/terms/creator\",\"/type\":\"http://purl.org/dc/terms/type\",\"end\":\"http://fise.iks-project.eu/ontology/end\",\"Enhancement\":\"http://fise.iks-project.eu/ontology/Enhancement\",\"Person\":\"http://dbpedia.org/ontology/Person\",\"selected-text\":\"http://fise.iks-project.eu/ontology/selected-text\",\"selection-context\":\"http://fise.iks-project.eu/ontology/selection-context\",\"start\":\"http://fise.iks-project.eu/ontology/start\",\"TextAnnotation\":\"http://fise.iks-project.eu/ontology/TextAnnotation\",\"xmlns\":\"http://www.w3.org/2001/XMLSchema#\"},\"@subject\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"Enhancement\",\"TextAnnotation\"],\"/created\":{\"@literal\":\""+this.expectedW3CFormattedDate+"\",\"@datatype\":\"xmlns:dateTime\"},\"/creator\":{\"@iri\":\"urn:iks-project:enhancer:test:dummyEngine\"},\"/type\":{\"@iri\":\"Person\"},\"end\":{\"@literal\":\"20\",\"@datatype\":\"xmlns:int\"},\"selected-text\":{\"@literal\":\"Patrick Marshall\",\"@datatype\":\"xmlns:string\"},\"selection-context\":{\"@literal\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"@datatype\":\"xmlns:string\"},\"start\":{\"@literal\":\"4\",\"@datatype\":\"xmlns:int\"}}";
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

        String expected = "{\"@context\":{\"created\":\"http://purl.org/dc/terms/created\",\"creator\":\"http://purl.org/dc/terms/creator\",\"end\":\"http://fise.iks-project.eu/ontology/end\",\"Enhancement\":\"http://fise.iks-project.eu/ontology/Enhancement\",\"Person\":\"http://dbpedia.org/ontology/Person\",\"selected-text\":\"http://fise.iks-project.eu/ontology/selected-text\",\"selection-context\":\"http://fise.iks-project.eu/ontology/selection-context\",\"start\":\"http://fise.iks-project.eu/ontology/start\",\"TextAnnotation\":\"http://fise.iks-project.eu/ontology/TextAnnotation\",\"type\":\"http://purl.org/dc/terms/type\",\"xmlns\":\"http://www.w3.org/2001/XMLSchema#\",\"@coerce\":{\"@iri\":[\"creator\",\"type\"],\"xmlns:dateTime\":\"created\",\"xmlns:int\":[\"end\",\"start\"],\"xmlns:string\":[\"selected-text\",\"selection-context\"]}},\"@subject\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"Enhancement\",\"TextAnnotation\"],\"created\":\""+this.expectedW3CFormattedDate+"\",\"creator\":\"urn:iks-project:enhancer:test:dummyEngine\",\"end\":20,\"selected-text\":\"Patrick Marshall\",\"selection-context\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"start\":4,\"type\":\"Person\"}";
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

        String expected = "{\n    \"@context\": {\n        \"created\": \"http://purl.org/dc/terms/created\",\n        \"creator\": \"http://purl.org/dc/terms/creator\",\n        \"end\": \"http://fise.iks-project.eu/ontology/end\",\n        \"Enhancement\": \"http://fise.iks-project.eu/ontology/Enhancement\",\n        \"Person\": \"http://dbpedia.org/ontology/Person\",\n        \"selected-text\": \"http://fise.iks-project.eu/ontology/selected-text\",\n        \"selection-context\": \"http://fise.iks-project.eu/ontology/selection-context\",\n        \"start\": \"http://fise.iks-project.eu/ontology/start\",\n        \"TextAnnotation\": \"http://fise.iks-project.eu/ontology/TextAnnotation\",\n        \"type\": \"http://purl.org/dc/terms/type\",\n        \"xmlns\": \"http://www.w3.org/2001/XMLSchema#\"\n    },\n    \"@subject\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n    \"@type\": [\n        \"Enhancement\",\n        \"TextAnnotation\"\n    ],\n    \"created\": {\n        \"@literal\": \""+this.expectedW3CFormattedDate+"\",\n        \"@datatype\": \"xmlns:dateTime\"\n    },\n    \"creator\": {\n        \"@iri\": \"urn:iks-project:enhancer:test:dummyEngine\"\n    },\n    \"end\": {\n        \"@literal\": \"20\",\n        \"@datatype\": \"xmlns:int\"\n    },\n    \"selected-text\": {\n        \"@literal\": \"Patrick Marshall\",\n        \"@datatype\": \"xmlns:string\"\n    },\n    \"selection-context\": {\n        \"@literal\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\n        \"@datatype\": \"xmlns:string\"\n    },\n    \"start\": {\n        \"@literal\": \"4\",\n        \"@datatype\": \"xmlns:int\"\n    },\n    \"type\": {\n        \"@iri\": \"Person\"\n    }\n}";
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

        String expected = "{\n    \"@context\": {\n        \"created\": \"http://purl.org/dc/terms/created\",\n        \"creator\": \"http://purl.org/dc/terms/creator\",\n        \"end\": \"http://fise.iks-project.eu/ontology/end\",\n        \"Enhancement\": \"http://fise.iks-project.eu/ontology/Enhancement\",\n        \"Person\": \"http://dbpedia.org/ontology/Person\",\n        \"selected-text\": \"http://fise.iks-project.eu/ontology/selected-text\",\n        \"selection-context\": \"http://fise.iks-project.eu/ontology/selection-context\",\n        \"start\": \"http://fise.iks-project.eu/ontology/start\",\n        \"TextAnnotation\": \"http://fise.iks-project.eu/ontology/TextAnnotation\",\n        \"type\": \"http://purl.org/dc/terms/type\",\n        \"xmlns\": \"http://www.w3.org/2001/XMLSchema#\",\n        \"@coerce\": {\n            \"@iri\": [\n                \"creator\",\n                \"type\"\n            ],\n            \"xmlns:dateTime\": \"created\",\n            \"xmlns:int\": [\n                \"end\",\n                \"start\"\n            ],\n            \"xmlns:string\": [\n                \"selected-text\",\n                \"selection-context\"\n            ]\n        }\n    },\n    \"@subject\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n    \"@type\": [\n        \"Enhancement\",\n        \"TextAnnotation\"\n    ],\n    \"created\": \""+this.expectedW3CFormattedDate+"\",\n    \"creator\": \"urn:iks-project:enhancer:test:dummyEngine\",\n    \"end\": 20,\n    \"selected-text\": \"Patrick Marshall\",\n    \"selection-context\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\n    \"start\": 4,\n    \"type\": \"Person\"\n}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }

    private ContentItem getContentItem(final String id, final String text) {
        return new InMemoryContentItem(id, text, "text/plain");
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
