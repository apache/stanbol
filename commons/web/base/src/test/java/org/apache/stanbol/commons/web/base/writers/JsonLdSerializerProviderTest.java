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
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.impl.util.W3CDateFormat;
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
        MGraph graph = new SimpleMGraph();
        getTextAnnotation(graph, "Person", "Patrick Marshall", context, new UriRef(DBPEDIA+"Person"));

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.serialize(serializedGraph, graph, "application/format+notsupported");
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

        MGraph graph = new SimpleMGraph();
        getTextAnnotation(graph, "Person", "Patrick Marshall", context, new UriRef(DBPEDIA+"Person"));

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setIndentation(0);
        jsonldProvider.setUseTypeCoercion(false);
        jsonldProvider.serialize(serializedGraph, graph, formatIdentifier);
        
        String expected = "{\"@context\":{\"created\":\"http://purl.org/dc/terms/created\",\"creator\":\"http://purl.org/dc/terms/creator\",\"end\":\"http://fise.iks-project.eu/ontology/end\",\"Enhancement\":\"http://fise.iks-project.eu/ontology/Enhancement\",\"Person\":\"http://dbpedia.org/ontology/Person\",\"selected-text\":\"http://fise.iks-project.eu/ontology/selected-text\",\"selection-context\":\"http://fise.iks-project.eu/ontology/selection-context\",\"start\":\"http://fise.iks-project.eu/ontology/start\",\"TextAnnotation\":\"http://fise.iks-project.eu/ontology/TextAnnotation\",\"type\":\"http://purl.org/dc/terms/type\",\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@id\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"Enhancement\",\"TextAnnotation\"],\"created\":{\"@type\":\"xsd:dateTime\",\"@value\":\"" + this.expectedW3CFormattedDate + "\"},\"creator\":{\"@id\":\"urn:iks-project:enhancer:test:dummyEngine\"},\"end\":{\"@type\":\"xsd:int\",\"@value\":\"20\"},\"selected-text\":{\"@type\":\"xsd:string\",\"@value\":\"Patrick Marshall\"},\"selection-context\":{\"@type\":\"xsd:string\",\"@value\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\"},\"start\":{\"@type\":\"xsd:int\",\"@value\":\"4\"},\"type\":{\"@id\":\"Person\"}}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testSingleSubjectSerializeNoNsWithCoercion() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived "
            + "in New Zealand and worked at the University of Otago.";

        MGraph graph = new SimpleMGraph();
        getTextAnnotation(graph, "Person", "Patrick Marshall", context, new UriRef(DBPEDIA+"Person"));

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setIndentation(0);
        jsonldProvider.setUseTypeCoercion(true);
        jsonldProvider.serialize(serializedGraph, graph, formatIdentifier);
        
        String expected = "{\"@context\":{\"created\":{\"@id\":\"http://purl.org/dc/terms/created\",\"@type\":\"http://www.w3.org/2001/XMLSchema#dateTime\"},\"creator\":{\"@id\":\"http://purl.org/dc/terms/creator\",\"@type\":\"@id\"},\"end\":{\"@id\":\"http://fise.iks-project.eu/ontology/end\",\"@type\":\"http://www.w3.org/2001/XMLSchema#int\"},\"Enhancement\":\"http://fise.iks-project.eu/ontology/Enhancement\",\"Person\":\"http://dbpedia.org/ontology/Person\",\"selected-text\":{\"@id\":\"http://fise.iks-project.eu/ontology/selected-text\",\"@type\":\"http://www.w3.org/2001/XMLSchema#string\"},\"selection-context\":{\"@id\":\"http://fise.iks-project.eu/ontology/selection-context\",\"@type\":\"http://www.w3.org/2001/XMLSchema#string\"},\"start\":{\"@id\":\"http://fise.iks-project.eu/ontology/start\",\"@type\":\"http://www.w3.org/2001/XMLSchema#int\"},\"TextAnnotation\":\"http://fise.iks-project.eu/ontology/TextAnnotation\",\"type\":{\"@id\":\"http://purl.org/dc/terms/type\",\"@type\":\"@id\"},\"xsd\":\"http://www.w3.org/2001/XMLSchema#\"},\"@id\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"Enhancement\",\"TextAnnotation\"],\"created\":\"2010-10-27T12:00:00Z\",\"creator\":\"urn:iks-project:enhancer:test:dummyEngine\",\"end\":20,\"selected-text\":\"Patrick Marshall\",\"selection-context\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"start\":4,\"type\":\"Person\"}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testSingleSubjectSerializeNoNsWithIndent() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        MGraph graph = new SimpleMGraph();
        getTextAnnotation(graph, "Person", "Patrick Marshall", context, new UriRef(DBPEDIA+"Person"));

        OutputStream serializedGraph = new ByteArrayOutputStream();
        jsonldProvider.setUseTypeCoercion(false);
        jsonldProvider.serialize(serializedGraph, graph, formatIdentifier);

        String expected = "{\n  \"@context\": {\n    \"created\": \"http://purl.org/dc/terms/created\",\n    \"creator\": \"http://purl.org/dc/terms/creator\",\n    \"end\": \"http://fise.iks-project.eu/ontology/end\",\n    \"Enhancement\": \"http://fise.iks-project.eu/ontology/Enhancement\",\n    \"Person\": \"http://dbpedia.org/ontology/Person\",\n    \"selected-text\": \"http://fise.iks-project.eu/ontology/selected-text\",\n    \"selection-context\": \"http://fise.iks-project.eu/ontology/selection-context\",\n    \"start\": \"http://fise.iks-project.eu/ontology/start\",\n    \"TextAnnotation\": \"http://fise.iks-project.eu/ontology/TextAnnotation\",\n    \"type\": \"http://purl.org/dc/terms/type\",\n    \"xsd\": \"http://www.w3.org/2001/XMLSchema#\"\n  },\n  \"@id\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n  \"@type\": [\n    \"Enhancement\",\n    \"TextAnnotation\"\n  ],\n  \"created\": {\n    \"@type\": \"xsd:dateTime\",\n    \"@value\": \"" + this.expectedW3CFormattedDate + "\"\n  },\n  \"creator\": {\n    \"@id\": \"urn:iks-project:enhancer:test:dummyEngine\"\n  },\n  \"end\": {\n    \"@type\": \"xsd:int\",\n    \"@value\": \"20\"\n  },\n  \"selected-text\": {\n    \"@type\": \"xsd:string\",\n    \"@value\": \"Patrick Marshall\"\n  },\n  \"selection-context\": {\n    \"@type\": \"xsd:string\",\n    \"@value\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\"\n  },\n  \"start\": {\n    \"@type\": \"xsd:int\",\n    \"@value\": \"4\"\n  },\n  \"type\": {\n    \"@id\": \"Person\"\n  }\n}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testSingleSubjectSerializeWithNs() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        MGraph graph = new SimpleMGraph();
        getTextAnnotation(graph, "Person", "Patrick Marshall", context, new UriRef(DBPEDIA+"Person"));

        OutputStream serializedGraph = new ByteArrayOutputStream();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("http://fise.iks-project.eu/ontology/", "enhancer");
        nsMap.put("http://www.w3.org/2001/XMLSchema#", "xmlns");
        nsMap.put("http://dbpedia.org/ontology/", "dbpedia");
        nsMap.put("http://purl.org/dc/terms", "dcterms");
        jsonldProvider.setIndentation(0);
        jsonldProvider.setUseTypeCoercion(false);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.serialize(serializedGraph, graph, formatIdentifier);

        String expected = "{\"@context\":{\"/created\":\"http://purl.org/dc/terms/created\",\"/creator\":\"http://purl.org/dc/terms/creator\",\"/type\":\"http://purl.org/dc/terms/type\",\"end\":\"http://fise.iks-project.eu/ontology/end\",\"Enhancement\":\"http://fise.iks-project.eu/ontology/Enhancement\",\"Person\":\"http://dbpedia.org/ontology/Person\",\"selected-text\":\"http://fise.iks-project.eu/ontology/selected-text\",\"selection-context\":\"http://fise.iks-project.eu/ontology/selection-context\",\"start\":\"http://fise.iks-project.eu/ontology/start\",\"TextAnnotation\":\"http://fise.iks-project.eu/ontology/TextAnnotation\",\"xmlns\":\"http://www.w3.org/2001/XMLSchema#\"},\"@id\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"Enhancement\",\"TextAnnotation\"],\"/created\":{\"@type\":\"xmlns:dateTime\",\"@value\":\"" + this.expectedW3CFormattedDate + "\"},\"/creator\":{\"@id\":\"urn:iks-project:enhancer:test:dummyEngine\"},\"/type\":{\"@id\":\"Person\"},\"end\":{\"@type\":\"xmlns:int\",\"@value\":\"20\"},\"selected-text\":{\"@type\":\"xmlns:string\",\"@value\":\"Patrick Marshall\"},\"selection-context\":{\"@type\":\"xmlns:string\",\"@value\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\"},\"start\":{\"@type\":\"xmlns:int\",\"@value\":\"4\"}}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testSingleSubjectSerializeWithNsWithCoercion() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        MGraph graph = new SimpleMGraph();
        getTextAnnotation(graph, "Person", "Patrick Marshall", context, new UriRef(DBPEDIA+"Person"));

        OutputStream serializedGraph = new ByteArrayOutputStream();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("http://fise.iks-project.eu/ontology/", "enhancer");
        nsMap.put("http://www.w3.org/2001/XMLSchema#", "xmlns");
        nsMap.put("http://dbpedia.org/ontology/", "dbpedia");
        nsMap.put("http://purl.org/dc/terms/", "dcterms");
        jsonldProvider.setIndentation(0);
        jsonldProvider.setUseTypeCoercion(true);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.serialize(serializedGraph, graph, formatIdentifier);

        String expected = "{\"@context\":{\"created\":{\"@id\":\"http://purl.org/dc/terms/created\",\"@type\":\"http://www.w3.org/2001/XMLSchema#dateTime\"},\"creator\":{\"@id\":\"http://purl.org/dc/terms/creator\",\"@type\":\"@id\"},\"end\":{\"@id\":\"http://fise.iks-project.eu/ontology/end\",\"@type\":\"http://www.w3.org/2001/XMLSchema#int\"},\"Enhancement\":\"http://fise.iks-project.eu/ontology/Enhancement\",\"Person\":\"http://dbpedia.org/ontology/Person\",\"selected-text\":{\"@id\":\"http://fise.iks-project.eu/ontology/selected-text\",\"@type\":\"http://www.w3.org/2001/XMLSchema#string\"},\"selection-context\":{\"@id\":\"http://fise.iks-project.eu/ontology/selection-context\",\"@type\":\"http://www.w3.org/2001/XMLSchema#string\"},\"start\":{\"@id\":\"http://fise.iks-project.eu/ontology/start\",\"@type\":\"http://www.w3.org/2001/XMLSchema#int\"},\"TextAnnotation\":\"http://fise.iks-project.eu/ontology/TextAnnotation\",\"type\":{\"@id\":\"http://purl.org/dc/terms/type\",\"@type\":\"@id\"},\"xmlns\":\"http://www.w3.org/2001/XMLSchema#\"},\"@id\":\"urn:iks-project:enhancer:test:text-annotation:Person\",\"@type\":[\"Enhancement\",\"TextAnnotation\"],\"created\":\"2010-10-27T12:00:00Z\",\"creator\":\"urn:iks-project:enhancer:test:dummyEngine\",\"end\":20,\"selected-text\":\"Patrick Marshall\",\"selection-context\":\"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\"start\":4,\"type\":\"Person\"}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }    

    @Test
    public void testSingleSubjectSerializeWithNsWithIndent() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        MGraph graph = new SimpleMGraph();
        getTextAnnotation(graph, "Person", "Patrick Marshall", context, new UriRef(DBPEDIA+"Person"));

        OutputStream serializedGraph = new ByteArrayOutputStream();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("http://fise.iks-project.eu/ontology/", "enhancer");
        nsMap.put("http://www.w3.org/2001/XMLSchema#", "xmlns");
        nsMap.put("http://dbpedia.org/ontology/", "dbpedia");
        nsMap.put("http://purl.org/dc/terms/", "dcterms");
        jsonldProvider.setIndentation(4);
        jsonldProvider.setUseTypeCoercion(false);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.serialize(serializedGraph, graph, formatIdentifier);

        String expected = "{\n    \"@context\": {\n        \"created\": \"http://purl.org/dc/terms/created\",\n        \"creator\": \"http://purl.org/dc/terms/creator\",\n        \"end\": \"http://fise.iks-project.eu/ontology/end\",\n        \"Enhancement\": \"http://fise.iks-project.eu/ontology/Enhancement\",\n        \"Person\": \"http://dbpedia.org/ontology/Person\",\n        \"selected-text\": \"http://fise.iks-project.eu/ontology/selected-text\",\n        \"selection-context\": \"http://fise.iks-project.eu/ontology/selection-context\",\n        \"start\": \"http://fise.iks-project.eu/ontology/start\",\n        \"TextAnnotation\": \"http://fise.iks-project.eu/ontology/TextAnnotation\",\n        \"type\": \"http://purl.org/dc/terms/type\",\n        \"xmlns\": \"http://www.w3.org/2001/XMLSchema#\"\n    },\n    \"@id\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n    \"@type\": [\n        \"Enhancement\",\n        \"TextAnnotation\"\n    ],\n    \"created\": {\n        \"@type\": \"xmlns:dateTime\",\n        \"@value\": \"2010-10-27T12:00:00Z\"\n    },\n    \"creator\": {\n        \"@id\": \"urn:iks-project:enhancer:test:dummyEngine\"\n    },\n    \"end\": {\n        \"@type\": \"xmlns:int\",\n        \"@value\": \"20\"\n    },\n    \"selected-text\": {\n        \"@type\": \"xmlns:string\",\n        \"@value\": \"Patrick Marshall\"\n    },\n    \"selection-context\": {\n        \"@type\": \"xmlns:string\",\n        \"@value\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\"\n    },\n    \"start\": {\n        \"@type\": \"xmlns:int\",\n        \"@value\": \"4\"\n    },\n    \"type\": {\n        \"@id\": \"Person\"\n    }\n}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    @Test
    public void testSingleSubjectSerializeWithNsWithIndentWithCoercion() {
        String context = "Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.";

        MGraph graph = new SimpleMGraph();
        getTextAnnotation(graph, "Person", "Patrick Marshall", context, new UriRef(DBPEDIA+"Person"));

        OutputStream serializedGraph = new ByteArrayOutputStream();
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("http://fise.iks-project.eu/ontology/", "enhancer");
        nsMap.put("http://www.w3.org/2001/XMLSchema#", "xmlns");
        nsMap.put("http://dbpedia.org/ontology/", "dbpedia");
        nsMap.put("http://purl.org/dc/terms/", "dcterms");
        jsonldProvider.setIndentation(4);
        jsonldProvider.setNamespacePrefixMap(nsMap);
        jsonldProvider.setUseTypeCoercion(true);
        jsonldProvider.serialize(serializedGraph, graph, formatIdentifier);

        String expected = "{\n    \"@context\": {\n        \"created\": {\n            \"@id\": \"http://purl.org/dc/terms/created\",\n            \"@type\": \"http://www.w3.org/2001/XMLSchema#dateTime\"\n        },\n        \"creator\": {\n            \"@id\": \"http://purl.org/dc/terms/creator\",\n            \"@type\": \"@id\"\n        },\n        \"end\": {\n            \"@id\": \"http://fise.iks-project.eu/ontology/end\",\n            \"@type\": \"http://www.w3.org/2001/XMLSchema#int\"\n        },\n        \"Enhancement\": \"http://fise.iks-project.eu/ontology/Enhancement\",\n        \"Person\": \"http://dbpedia.org/ontology/Person\",\n        \"selected-text\": {\n            \"@id\": \"http://fise.iks-project.eu/ontology/selected-text\",\n            \"@type\": \"http://www.w3.org/2001/XMLSchema#string\"\n        },\n        \"selection-context\": {\n            \"@id\": \"http://fise.iks-project.eu/ontology/selection-context\",\n            \"@type\": \"http://www.w3.org/2001/XMLSchema#string\"\n        },\n        \"start\": {\n            \"@id\": \"http://fise.iks-project.eu/ontology/start\",\n            \"@type\": \"http://www.w3.org/2001/XMLSchema#int\"\n        },\n        \"TextAnnotation\": \"http://fise.iks-project.eu/ontology/TextAnnotation\",\n        \"type\": {\n            \"@id\": \"http://purl.org/dc/terms/type\",\n            \"@type\": \"@id\"\n        },\n        \"xmlns\": \"http://www.w3.org/2001/XMLSchema#\"\n    },\n    \"@id\": \"urn:iks-project:enhancer:test:text-annotation:Person\",\n    \"@type\": [\n        \"Enhancement\",\n        \"TextAnnotation\"\n    ],\n    \"created\": \"" + this.expectedW3CFormattedDate + "\",\n    \"creator\": \"urn:iks-project:enhancer:test:dummyEngine\",\n    \"end\": 20,\n    \"selected-text\": \"Patrick Marshall\",\n    \"selection-context\": \"Dr. Patrick Marshall (1869 - November 1950) was a geologist who lived in New Zealand and worked at the University of Otago.\",\n    \"start\": 4,\n    \"type\": \"Person\"\n}";
        String result = serializedGraph.toString();
        Assert.assertEquals(expected, result);
    }
    
    private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String DC = "http://purl.org/dc/terms/";
    private static final String FISE = "http://fise.iks-project.eu/ontology/";
    private static final String DBPEDIA = "http://dbpedia.org/ontology/";
    
    private void getTextAnnotation(MGraph graph, String annotationURNExtension, String namedEntity, String context, UriRef type) {
        UriRef annotation = new UriRef("urn:iks-project:enhancer:test:text-annotation:" + annotationURNExtension);
        graph.add(new TripleImpl(annotation, new UriRef(RDF+"type"), new UriRef(FISE+"Enhancement")));
        graph.add(new TripleImpl(annotation, new UriRef(RDF+"type"), new UriRef(FISE+"TextAnnotation")));
        graph.add(new TripleImpl(annotation, new UriRef(DC+"creator"), new UriRef("urn:iks-project:enhancer:test:dummyEngine")));
        
        Calendar myCal = Calendar.getInstance();
        myCal.set(2010, 9, 27, 12, 0, 0);
        myCal.set(Calendar.MILLISECOND, 0);
        myCal.setTimeZone(TimeZone.getTimeZone("UTC"));
        graph.add(new TripleImpl(annotation, new UriRef(DC+"created"), LiteralFactory.getInstance().createTypedLiteral(myCal.getTime())));
        
        this.expectedW3CFormattedDate = new W3CDateFormat().format(myCal.getTime());
        
        graph.add(new TripleImpl(annotation, new UriRef(FISE+"selected-text"), LiteralFactory.getInstance().createTypedLiteral(namedEntity)));
        graph.add(new TripleImpl(annotation, new UriRef(FISE+"selection-context"), LiteralFactory.getInstance().createTypedLiteral(context)));
        graph.add(new TripleImpl(annotation, new UriRef(DC+"type"), type));
        Integer start = context.indexOf(namedEntity);
        if (start < 0) { // if not found in the content set start to 42
            start = 42;
        }
        graph.add(new TripleImpl(annotation, new UriRef(FISE+"start"), LiteralFactory.getInstance().createTypedLiteral(start)));
        Integer end = start + namedEntity.length();
        graph.add(new TripleImpl(annotation, new UriRef(FISE+"end"), LiteralFactory.getInstance().createTypedLiteral(end)));
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
