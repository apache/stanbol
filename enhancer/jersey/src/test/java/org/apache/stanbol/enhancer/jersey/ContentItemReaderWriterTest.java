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

import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.REQUEST_PROPERTIES_URI;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.OUTPUT_CONTENT;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.OUTPUT_CONTENT_PART;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.PARSED_CONTENT_URIS;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.RDF_FORMAT;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.getOutputContent;
import static org.apache.stanbol.enhancer.jersey.utils.RequestPropertiesHelper.getParsedContentURIs;
import static org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper.initRequestPropertiesContentPart;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.initExecutionMetadata;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionMetadataHelper.initExecutionMetadataContentPart;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.createExecutionPlan;
import static org.apache.stanbol.enhancer.servicesapi.helper.ExecutionPlanHelper.writeExecutionNode;
import static org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata.CHAIN_EXECUTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.BlankNodeOrIRI;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.contentitem.inmemory.InMemoryContentItemFactory;
import org.apache.stanbol.enhancer.jersey.reader.ContentItemReader;
import org.apache.stanbol.enhancer.jersey.writers.ContentItemWriter;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.ExecutionMetadata;
import org.glassfish.jersey.internal.RuntimeDelegateImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentItemReaderWriterTest {
    
    private static final Logger log = LoggerFactory.getLogger(ContentItemReaderWriterTest.class);

    private static ContentItem contentItem;
    private static ContentItemWriter ciWriter;
    private static ContentItemReader ciReader;

    private static ContentItemFactory ciFactory = InMemoryContentItemFactory.getInstance();
    
    
    /**
     * @return
     */
    @BeforeClass
    public static void createTestContentItem() throws IOException {
        contentItem = ciFactory.createContentItem(new IRI("urn:test"),
            new StringSource(
                "<html>\n" +
                "  <body>\n" +
                "    This is a <b>ContentItem</b> to <i>Mime Multipart</i> test!\n" +
                "  </body>\n" +
                "</html>","text/html"));
        RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
        contentItem.addPart(new IRI("run:text:text"), 
            ciFactory.createBlob(new StringSource(
            "This is a ContentItem to Mime Multipart test!")));
        contentItem.getMetadata().add(new TripleImpl(
            new IRI("urn:test"), RDF.type, new IRI("urn:types:Document")));
        //mark the main content as parsed and also that all 
        //contents and contentparts should be included
        Map<String,Object> properties = initRequestPropertiesContentPart(contentItem);
        properties.put(PARSED_CONTENT_URIS, Collections.singleton(contentItem.getPartUri(0).getUnicodeString()));
        properties.put(OUTPUT_CONTENT, Collections.singleton("*/*"));
        properties.put(OUTPUT_CONTENT_PART, Collections.singleton("*"));
        properties.put(RDF_FORMAT, "application/rdf+xml");
        Graph em = initExecutionMetadataContentPart(contentItem);
        BlankNodeOrIRI ep = createExecutionPlan(em, "testChain",null);
        writeExecutionNode(em, ep, "testEngine", true, null,null);
        initExecutionMetadata(em, em, contentItem.getUri(), "testChain", false);

        ciWriter = new ContentItemWriter(Serializer.getInstance());

        ciReader = new ContentItemReader(){
            @Override
            protected Parser getParser() {
                return Parser.getInstance();
            }
            @Override
            protected ContentItemFactory getContentItemFactory() {
                return ciFactory;
            }
        };
    }
    /**
     * @param out
     * @return
     * @throws IOException
     */
    private MediaType serializeContentItem(ByteArrayOutputStream out) throws IOException {
        MultivaluedMap<String,Object> headers = new MultivaluedHashMap<String, Object>();
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
        assertEquals(contentType.getParameters().get("boundary"),ContentItemWriter.CONTENT_ITEM_BOUNDARY);
        assertNotNull(contentType.getParameters().get("charset"));
        assertEquals(contentType.getParameters().get("charset"),"UTF-8");
        //check the serialised multipart MIME
        String multipartMime = new String(out.toByteArray(),Charset.forName(contentType.getParameters().get("charset")));
        log.info("Multipart MIME content:\n{}\n",multipartMime);
        String[] tests = new String[]{
            "--"+ContentItemWriter.CONTENT_ITEM_BOUNDARY,
            "Content-Disposition: form-data; name=\"metadata\"; filename=\"urn:test\"",
            "Content-Type: application/rdf+xml; charset=UTF-8",
            "<rdf:type rdf:resource=\"urn:types:Document\"/>",
            "--"+ContentItemWriter.CONTENT_ITEM_BOUNDARY,
            "Content-Disposition: form-data; name=\"content\"",
            "Content-Type: multipart/alternate; boundary="+ContentItemWriter.CONTENT_PARTS_BOUNDERY,
            "--"+ContentItemWriter.CONTENT_PARTS_BOUNDERY,
            "Content-Disposition: form-data; name=\"urn:test_main\"",
            "Content-Type: text/html; charset=UTF-8",
            "This is a <b>ContentItem</b> to <i>Mime Multipart</i> test!",
            "--"+ContentItemWriter.CONTENT_PARTS_BOUNDERY,
            "Content-Disposition: form-data; name=\"run:text:text\"",
            "Content-Type: text/plain; charset=UTF-8",
            "This is a ContentItem to Mime Multipart test!",
            "--"+ContentItemWriter.CONTENT_PARTS_BOUNDERY+"--",
            "--"+ContentItemWriter.CONTENT_ITEM_BOUNDARY,
            "Content-Disposition: form-data; name=\""+REQUEST_PROPERTIES_URI.getUnicodeString()+"\"",
            "Content-Type: application/json; charset=UTF-8",
            "--"+ContentItemWriter.CONTENT_ITEM_BOUNDARY,
            "Content-Disposition: form-data; name=\""+CHAIN_EXECUTION.getUnicodeString()+"\"",
            "Content-Type: application/rdf+xml; charset=UTF-8",
            "<rdf:type rdf:resource=\"http://stanbol.apache.org/ontology/enhancer/executionplan#ExecutionNode\"/>",
            "--"+ContentItemWriter.CONTENT_ITEM_BOUNDARY+"--"
        };
        log.debug("> Validate Multipart Mime:");
        for(String test : tests){
            int index = multipartMime.indexOf(test);
            assertTrue("Unable to find: '"+test+"' in multipart mime!",index >=0);
            log.debug(" - found '{}'",test);
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
        Graph copy = new SimpleGraph();
        copy.addAll(contentItem.getMetadata());
        assertTrue(copy.removeAll(ci.getMetadata()));
        assertTrue(copy.isEmpty());
        //assert Blob
        assertEquals(contentItem.getBlob().getMimeType(), ci.getBlob().getMimeType());
        String content = IOUtils.toString(contentItem.getStream(),"UTF-8");
        String readContent = IOUtils.toString(ci.getStream(), "UTF-8");
        assertEquals(content, readContent);
        Iterator<Entry<IRI,Blob>> contentItemBlobsIt = ContentItemHelper.getContentParts(contentItem, Blob.class).entrySet().iterator();
        Iterator<Entry<IRI,Blob>> ciBlobsIt = ContentItemHelper.getContentParts(ci, Blob.class).entrySet().iterator();
        Set<String> expectedParsedContentIds = new HashSet<String>(); //later used to validate enhancementMetadata
        while(contentItemBlobsIt.hasNext() && ciBlobsIt.hasNext()){
            Entry<IRI,Blob> contentItemBlobPart = contentItemBlobsIt.next();
            Entry<IRI,Blob> ciBlobPart = ciBlobsIt.next();
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
        Graph executionMetadata = contentItem.getPart(ExecutionMetadata.CHAIN_EXECUTION, Graph.class);
        Graph readExecutionMetadata = ci.getPart(ExecutionMetadata.CHAIN_EXECUTION, Graph.class);
        assertNotNull(executionMetadata);
        assertNotNull(readExecutionMetadata);
        assertEquals(executionMetadata.size(), readExecutionMetadata.size());
        //validate EnhancemetnProperties
        Map<String,Object> reqProp = ContentItemHelper.getRequestPropertiesContentPart(ci);
        assertNotNull(reqProp);
        //the parsed value MUST BE overridden by the two content parts parsed
        assertEquals(expectedParsedContentIds, getParsedContentURIs(reqProp));
        Collection<String> outputContent = getOutputContent(reqProp);
        assertEquals(1, outputContent.size());
        assertEquals(outputContent.iterator().next(), "*/*");
        Collection<String> outputContentPart = Collections.singleton("*");
        assertEquals(1, outputContentPart.size());
        assertEquals(outputContentPart.iterator().next(), "*");
    }

}
