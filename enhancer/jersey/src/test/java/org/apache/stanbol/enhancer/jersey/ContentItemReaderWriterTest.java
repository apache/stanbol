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
package org.apache.stanbol.enhancer.jersey;

import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.ENHANCEMENT_PROPERTIES_URI;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.OUTPUT_CONTENT;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.OUTPUT_CONTENT_PART;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.PARSED_CONTENT_URIS;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.RDF_FORMAT;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getEnhancementProperties;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getOutputContent;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getOutputContentParts;
import static org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper.getParsedContentURIs;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.initExecutionMetadata;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.initExecutionMetadataContentPart;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.createExecutionPlan;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.writeExecutionNode;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.CHAIN_EXECUTION;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.jersey.reader.ContentItemReader;
import org.apache.stanbol.enhancer.jersey.utils.EnhancementPropertiesHelper;
import org.apache.stanbol.enhancer.jersey.writers.ContentItemWriter;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryBlob;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;

public class ContentItemReaderWriterTest {
    
    private static final Logger log = LoggerFactory.getLogger(ContentItemReaderWriterTest.class);

    private static ContentItem contentItem;
    private static ContentItemWriter ciWriter;
    private static ContentItemReader ciReader;
    /**
     * @return
     */
    @BeforeClass
    public static void createTestContentItem() {
        contentItem = new InMemoryContentItem("urn:test",
            "<html>\n" +
            "  <body>\n" +
            "    This is a <b>ContentItem</b> to <i>Mime Multipart</i> test!\n" +
            "  </body>\n" +
            "</html>","text/html");
        contentItem.addPart(new UriRef("run:text:text"), new InMemoryBlob(
            "This is a ContentItem to Mime Multipart test!", "text/plain"));
        contentItem.getMetadata().add(new TripleImpl(
            new UriRef("urn:test"), RDF.type, new UriRef("urn:types:Document")));
        //mark the main content as parsed and also that all 
        //contents and contentparts should be included
        Map<String,Object> properties = getEnhancementProperties(contentItem);
        properties.put(PARSED_CONTENT_URIS, Collections.singleton(contentItem.getPartUri(0).getUnicodeString()));
        properties.put(OUTPUT_CONTENT, Collections.singleton("*/*"));
        properties.put(OUTPUT_CONTENT_PART, Collections.singleton("*"));
        properties.put(RDF_FORMAT, "application/rdf+xml");
        MGraph em = initExecutionMetadataContentPart(contentItem);
        NonLiteral ep = createExecutionPlan(em, "testChain");
        writeExecutionNode(em, ep, "testEngine", true, null);
        initExecutionMetadata(em, em, contentItem.getUri(), "testChain", false);
        ciWriter = new ContentItemWriter(null);
        ciReader = new ContentItemReader(null);
    }
    /**
     * @param out
     * @return
     * @throws IOException
     */
    private MediaType serializeContentItem(ByteArrayOutputStream out) throws IOException {
        MultivaluedMap<String,Object> headers = new StringKeyIgnoreCaseMultivaluedMap<Object>();
        ciWriter.writeTo(contentItem, ContentItem.class, null, null, MediaType.MULTIPART_FORM_DATA_TYPE, 
            headers , out);
        //check the returned content type
        String contentTypeString = (String)headers.getFirst(HttpHeaders.CONTENT_TYPE);
        assertNotNull(contentTypeString);
        MediaType contentType = MediaType.valueOf(contentTypeString);
        return contentType;
    }
    
    @Test
    public void testWriter() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MediaType contentType = serializeContentItem(out);
        assertTrue(MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(contentType));
        assertNotNull(contentType.getParameters().get("boundary"));
        assertEquals(contentType.getParameters().get("boundary"),"contentItem");
        assertNotNull(contentType.getParameters().get("charset"));
        assertEquals(contentType.getParameters().get("charset"),"UTF-8");
        //check the serialised multipart MIME
        String multipartMime = new String(out.toByteArray(),Charset.forName(contentType.getParameters().get("charset")));
        log.info("Multipart MIME content:\n{}\n",multipartMime);
        String[] tests = new String[]{
            "--"+contentType.getParameters().get("boundary"),
            "Content-Disposition: form-data; name=\"metadata\"; filename=\"urn:test\"",
            "Content-Type: application/rdf+xml; charset=UTF-8",
            "<rdf:type rdf:resource=\"urn:types:Document\"/>",
            "--"+contentType.getParameters().get("boundary"),
            "Content-Disposition: form-data; name=\"content\"",
            "Content-Type: multipart/alternate; boundary=contentParts; charset=UTF-8",
            "--contentParts",
            "Content-Disposition: form-data; name=\"urn:test_main\"",
            "Content-Type: text/html; charset=UTF-8",
            "This is a <b>ContentItem</b> to <i>Mime Multipart</i> test!",
            "--contentParts",
            "Content-Disposition: form-data; name=\"run:text:text\"",
            "Content-Type: text/plain; charset=UTF-8",
            "This is a ContentItem to Mime Multipart test!",
            "--contentParts--",
            "--"+contentType.getParameters().get("boundary"),
            "Content-Disposition: form-data; name=\""+ENHANCEMENT_PROPERTIES_URI.getUnicodeString()+"\"",
            "Content-Type: application/json; charset=UTF-8",
            "--"+contentType.getParameters().get("boundary"),
            "Content-Disposition: form-data; name=\""+CHAIN_EXECUTION.getUnicodeString()+"\"",
            "Content-Type: application/rdf+xml; charset=UTF-8",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionplan#ExecutionNode\"/>",
            "--"+contentType.getParameters().get("boundary")+"--"
        };
        for(String test : tests){
            int index = multipartMime.indexOf(test);
            assertTrue(index >=0);
            multipartMime = multipartMime.substring(index);
        }
    }

    @Test
    public void testReader() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MediaType contentType = serializeContentItem(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ContentItem ci = ciReader.readFrom(ContentItem.class, null, null, contentType, null, in);
        //assert ID
        assertEquals(contentItem.getUri(), ci.getUri());
        //assert metadata
        MGraph copy = new SimpleMGraph();
        copy.addAll(contentItem.getMetadata());
        assertTrue(copy.removeAll(ci.getMetadata()));
        assertTrue(copy.isEmpty());
        //assert Blob
        assertEquals(contentItem.getBlob().getMimeType(), ci.getBlob().getMimeType());
        String content = IOUtils.toString(contentItem.getStream(),"UTF-8");
        String readContent = IOUtils.toString(ci.getStream(), "UTF-8");
        assertEquals(content, readContent);
        Iterator<Entry<UriRef,Blob>> contentItemBlobsIt = ContentItemHelper.getContentParts(contentItem, Blob.class).entrySet().iterator();
        Iterator<Entry<UriRef,Blob>> ciBlobsIt = ContentItemHelper.getContentParts(ci, Blob.class).entrySet().iterator();
        Set<String> expectedParsedContentIds = new HashSet<String>(); //later used to validate enhancementMetadata
        while(contentItemBlobsIt.hasNext() && ciBlobsIt.hasNext()){
            Entry<UriRef,Blob> contentItemBlobPart = contentItemBlobsIt.next();
            Entry<UriRef,Blob> ciBlobPart = ciBlobsIt.next();
            expectedParsedContentIds.add(ciBlobPart.getKey().getUnicodeString());
            assertEquals(contentItemBlobPart.getKey(), ciBlobPart.getKey());
            String partContentType = contentItemBlobPart.getValue().getMimeType();
            String readPartContentType = ciBlobPart.getValue().getMimeType();
            assertEquals(partContentType, readPartContentType);
            String partContent = IOUtils.toString(contentItemBlobPart.getValue().getStream(),"UTF-8");
            String readPartContent = IOUtils.toString(ciBlobPart.getValue().getStream(), "UTF-8");
            assertEquals(partContent, readPartContent);
        }
        //validate ExecutionMetadata
        MGraph executionMetadata = contentItem.getPart(ExecutionMetadata.CHAIN_EXECUTION, MGraph.class);
        MGraph readExecutionMetadata = ci.getPart(ExecutionMetadata.CHAIN_EXECUTION, MGraph.class);
        assertNotNull(executionMetadata);
        assertNotNull(readExecutionMetadata);
        assertEquals(executionMetadata.size(), readExecutionMetadata.size());
        //validate EnhancemetnProperties
        Map<String,Object> properties = getEnhancementProperties(ci);
        //the parsed value MUST BE overridden by the two content parts parsed
        assertEquals(expectedParsedContentIds, getParsedContentURIs(properties));
        Collection<String> outputContent = getOutputContent(properties);
        assertEquals(1, outputContent.size());
        assertEquals(outputContent.iterator().next(), "*/*");
        Collection<String> outputContentPart = Collections.singleton("*");
        assertEquals(1, outputContentPart.size());
        assertEquals(outputContentPart.iterator().next(), "*");
    }

}
