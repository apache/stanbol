package org.apache.stanbol.enhancer.jersey;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

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

import com.sun.jersey.core.util.StringKeyIgnoreCaseMultivaluedMap;

public class ContentItemReaderWriterTest {

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
        MGraph em =ExecutionMetadataHelper.initExecutionMetadataContentPart(contentItem);
        NonLiteral ep = ExecutionPlanHelper.createExecutionPlan(em, "testChain");
        ExecutionPlanHelper.writeExecutionNode(em, ep, "testEngine", true, null);
        ExecutionMetadataHelper.initExecutionMetadata(em, em, contentItem.getUri(), "testChain", false);
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
            "Content-Disposition: form-data; name=\"http://stanbol.apache.org/ontology/enhancer/executionMetadata#ChainExecution\"",
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
        ContentItemReader cir = new ContentItemReader(null);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ContentItem ci = cir.readFrom(ContentItem.class, null, null, contentType, null, in);
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
        while(contentItemBlobsIt.hasNext() && ciBlobsIt.hasNext()){
            Entry<UriRef,Blob> contentItemBlobPart = contentItemBlobsIt.next();
            Entry<UriRef,Blob> ciBlobPart = ciBlobsIt.next();
            assertEquals(contentItemBlobPart.getKey(), ciBlobPart.getKey());
            String partContentType = contentItemBlobPart.getValue().getMimeType();
            String readPartContentType = ciBlobPart.getValue().getMimeType();
            assertEquals(partContentType, readPartContentType);
            String partContent = IOUtils.toString(contentItemBlobPart.getValue().getStream(),"UTF-8");
            String readPartContent = IOUtils.toString(ciBlobPart.getValue().getStream(), "UTF-8");
            assertEquals(partContent, readPartContent);
        }
        MGraph executionMetadata = contentItem.getPart(ExecutionMetadata.CHAIN_EXECUTION, MGraph.class);
        MGraph readExecutionMetadata = ci.getPart(ExecutionMetadata.CHAIN_EXECUTION, MGraph.class);
        assertNotNull(executionMetadata);
        assertNotNull(readExecutionMetadata);
        assertEquals(executionMetadata.size(), readExecutionMetadata.size());
    }

}
