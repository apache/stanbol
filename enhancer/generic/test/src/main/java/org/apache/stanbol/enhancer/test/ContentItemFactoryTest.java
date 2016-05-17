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
package org.apache.stanbol.enhancer.test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentReference;
import org.apache.stanbol.enhancer.servicesapi.ContentSink;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.apache.stanbol.enhancer.servicesapi.rdf.Enhancer;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.junit.Before;
import org.junit.Test;

/**
 * This UnitTest MUST BE passed by all {@link ContentItemFactory} implementations.
 * It only checks the creation of {@link ContentItem}s and {@link Blob}s. It
 * does not actually test the ContentItem and Blob implementations as those
 * need to pass {@link ContentItemTest} and {@link BlobTest} respectively.
 * 
 * @author Rupert Westenthaler
 */
public abstract class ContentItemFactoryTest {

    /**
     * Internally used to test {@link ContentReference} constructors
     */
    private static ContentReference TEST_CR = new ContentReference() {
        ContentSource source = new StringSource("Dummy Reference Content");
        @Override
        public String getReference() {
            return "urn:dummy.reference";
        }
        
        @Override
        public ContentSource dereference() throws IOException {
            return source;
        }
    };
    /**
     * Internally used to test {@link ContentSource} constructors
     */
    private static ContentSource TEST_CS = new StringSource("Dummy Content");
    /**
     * The prefix used for testing
     */
    private static String PREFIX = "http://www.example.com/prefix#";
    /**
     * The {@link ContentItem#getUri() ID} used for testing
     */
    private static IRI ID = new IRI("http://www.example.com/content-items#12345");
    /**
     * ImmutableGraph used to test of parsed metadata are preserved
     */
    private static Graph METADATA = new SimpleGraph();
    static {
        METADATA.add(new TripleImpl(ID, Properties.RDF_TYPE, Enhancer.CONTENT_ITEM));
        METADATA.add(new TripleImpl(ID, Properties.RDFS_LABEL, new PlainLiteralImpl("Test ContentItem")));
    }
    /**
     * Member variable for the {@link ContentItemFactory} instance used for
     * testing. This is initialise {@link Before} the first unit test is
     * esecuted. 
     */
    protected ContentItemFactory contentItemFactory;
    
    /**
     * This method is assumed to return an new ContentItemFactory instance on 
     * every call. Currently it is called only once in the {@link Before} state
     * of the first test.<p>
     * Subclasses can use the {@link #contentItemFactory} member
     * variable in additional tests.
     * @return the {@link ContentItemFactory} instance used for the tests.
     */
    protected abstract ContentItemFactory createContentItemFactory() throws IOException ;
    
    /**
     * Initialises the {@link #contentItemFactory} {@link Before} the first 
     * unit test is executed. Needs not to be manually called by test methods.
     */
    @Before
    public void initFactory() throws IOException {
        if(contentItemFactory == null){
            contentItemFactory = createContentItemFactory();
            assertNotNull("Unable to create ContentItemFactory instance",
                contentItemFactory);
        }
    }

    /*
     * Set of tests to test that IllegalArgumentExceptions are
     * thrown if null is parsed as ContentSource to the various
     * createContentItem methods
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingCiContentSource() throws IOException{
        contentItemFactory.createContentItem((ContentSource)null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingCiContentSource2() throws IOException{
        contentItemFactory.createContentItem(PREFIX,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingCiContentSource3() throws IOException{
        contentItemFactory.createContentItem(ID,null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingCiContentSource4() throws IOException{
        contentItemFactory.createContentItem(ID,null,new SimpleGraph());
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingCiContentSource5() throws IOException{
        contentItemFactory.createContentItem(PREFIX,null,new SimpleGraph());
    }
    /*
     * Set of tests to test that IllegalArgumentExceptions are
     * thrown if null is parsed as ContentReference to the various
     * createContentItem methods
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingCiContentReference() throws IOException{
        contentItemFactory.createContentItem((ContentReference)null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingCiContentReference2() throws IOException{
        contentItemFactory.createContentItem(null,new SimpleGraph());
    }
    /*
     * Set of tests to test that IllegalArgumentExceptions are
     * thrown if null is parsed to the createBlob methods.
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingBlobContentReference() throws IOException{
        contentItemFactory.createBlob((ContentReference)null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingBlobContentSource() throws IOException{
        contentItemFactory.createBlob((ContentSource)null);
    }
    /*
     * Set of tests to test that IllegalArgumentExceptions if
     * null is parsed as prefix
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingCiPrefix() throws IOException{
        contentItemFactory.createContentItem((String)null,TEST_CS);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingCiPrefix2() throws IOException{
        contentItemFactory.createContentItem((String)null,TEST_CS,new SimpleGraph());
    }
    /**
     * Test that the generated ID starts with the parsed prefix
     */
    @Test
    public void testPrefix() throws IOException{
        ContentItem ci = contentItemFactory.createContentItem(PREFIX, TEST_CS);
        assertNotNull(ci);
        assertNotNull(ci.getUri());
        assertTrue("The ID of the created ContentItem MUST start with the parsed prefix", 
            ci.getUri().getUnicodeString().startsWith(PREFIX));
        
        ci = contentItemFactory.createContentItem(PREFIX, TEST_CS,new SimpleGraph());
        assertNotNull(ci);
        assertNotNull(ci.getUri());
        assertTrue("The ID of the created ContentItem MUST start with the parsed prefix", 
            ci.getUri().getUnicodeString().startsWith(PREFIX));
    }
    /**
     * Test that the parsed URI is used as ID of the ContentItem
     */
    @Test
    public void testURI() throws IOException {
        ContentItem ci = contentItemFactory.createContentItem(ID, TEST_CS);
        assertNotNull(ci);
        assertNotNull(ci.getUri());
        assertTrue("The ID of the created ContentItem MUST be equals to the parsed ID", 
            ci.getUri().equals(ID));
        
        ci = contentItemFactory.createContentItem(ID, TEST_CS,new SimpleGraph());
        assertNotNull(ci);
        assertNotNull(ci.getUri());
        assertTrue("The ID of the created ContentItem MUST be equals to the parsed ID", 
            ci.getUri().equals(ID));
    }
    /**
     * Test the generation of valid IDs if no or <code>null</code> is parsed
     * as id
     */
    @Test
    public void testDefaultId() throws IOException {
        ContentItem ci = contentItemFactory.createContentItem(TEST_CS);
        assertNotNull(ci);
        assertNotNull(ci.getUri());
        ci = contentItemFactory.createContentItem((IRI)null,TEST_CS);
        assertNotNull(ci);
        assertNotNull(ci.getUri());
        ci = contentItemFactory.createContentItem((IRI)null,TEST_CS, new SimpleGraph());
        assertNotNull(ci);
        assertNotNull(ci.getUri());
    }
    /**
     * Tests if the {@link ContentReference#getReference()} is used as ID for
     * the contentItem
     */
    @Test
    public void testContentReferenceId() throws IOException {
        ContentItem ci = contentItemFactory.createContentItem(TEST_CR);
        assertNotNull(ci);
        assertNotNull(ci.getUri());
        assertEquals(TEST_CR.getReference(),ci.getUri().getUnicodeString());
        
        contentItemFactory.createContentItem(TEST_CR, new SimpleGraph());
        assertNotNull(ci);
        assertNotNull(ci.getUri());
        assertEquals(TEST_CR.getReference(),ci.getUri().getUnicodeString());
    }
    /**
     * Tests if triples contained in parsed Metadata are also present within 
     * the {@link ContentItem#getMetadata()} graph
     */
    @Test
    public void testParsedMetadata() throws IOException {
        ContentItem ci = contentItemFactory.createContentItem(TEST_CR, METADATA);
        assertNotNull(ci);
        assertEquals("The created ContentItem MUST contain parsed metadata",
            METADATA.size(), ci.getMetadata().size());
        
        ci = contentItemFactory.createContentItem(ID,TEST_CS, METADATA);
        assertNotNull(ci);
        assertEquals("The created ContentItem MUST contain parsed metadata",
            METADATA.size(), ci.getMetadata().size());

        ci = contentItemFactory.createContentItem(PREFIX,TEST_CS, METADATA);
        assertNotNull(ci);
        assertEquals("The created ContentItem MUST contain parsed metadata",
            METADATA.size(), ci.getMetadata().size());
    }
    
    @Test
    public void testContentSink() throws IOException {
        String mt = "text/plain";
        Charset ISO8859_4 = Charset.forName("ISO-8859-4");
        ContentSink cs = contentItemFactory.createContentSink(mt+"; charset="+ISO8859_4.name());
        assertNotNull(cs);
        assertNotNull(cs.getBlob());
        OutputStream out = cs.getOutputStream();
        assertNotNull(cs);
        // multiple calls MUST return the same OutputStream!
        assertSame(out, cs.getOutputStream());
        //test mime type
        assertNotNull(cs.getBlob().getMimeType());
        //get MimeType MUST return the simple mime type
        assertEquals(mt, cs.getBlob().getMimeType());
        String charsetParam = cs.getBlob().getParameter().get("charset");
        assertNotNull("expected charset parameter is missing!",charsetParam);
        assertEquals(ISO8859_4.name(), charsetParam);
        
        //now write some data to the sink
        String TEST_CONTENT = "Thîs tésts wrîtîng to â ConténtSînk!";
        //note this uses the same charset as parsed as charset in the
        //constructor!
        IOUtils.write(TEST_CONTENT, cs.getOutputStream(),ISO8859_4.name());
        IOUtils.closeQuietly(cs.getOutputStream());
        //now read the data from the blob
        String content = IOUtils.toString(
            cs.getBlob().getStream(),
            charsetParam);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testContentSinkDefaultMimeType() throws IOException {
        String DEFAULT = "application/octet-stream";
        ContentSink cs = contentItemFactory.createContentSink(null);
        assertNotNull(cs);
        assertNotNull(cs.getBlob());
        assertNotNull(cs.getBlob().getMimeType());
        //get MimeType MUST return the simple mime type
        assertEquals(DEFAULT, cs.getBlob().getMimeType());
        assertNull(cs.getBlob().getParameter().get("charset"));
    }

    
}
