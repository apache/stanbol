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
package org.apache.stanbol.enhancer.engines.tika;

import static java.util.Collections.singleton;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.stanbol.enhancer.engines.tika.TikaEngine.XHTML;
import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.CANNOT_ENHANCE;
import static org.apache.tika.mime.MediaType.OCTET_STREAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryContentItem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

public class TikaEngineTest {

    private static TikaEngine engine;
    private static MockComponentContext context;
    
    @BeforeClass
    public static void setUpServices() throws IOException {
        context = new MockComponentContext();
        context.properties.put(TikaEngine.PROPERTY_NAME, "tika");
    }

    @Before
    public void bindServices() throws ConfigurationException {
        if(engine == null){
            engine = new TikaEngine();
            engine.activate(context);
        }
    }

    @Test
    public void testHtml() throws EngineException, IOException {
        ContentItem ci = createContentItem("test.html", "text/html; charset=UTF-8");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<title>The Apache Stanbol Enhancer</title>",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    @Test
    public void testPdf() throws EngineException, IOException {
        //PDF created by Apple Pages
        ContentItem ci = createContentItem("test.pdf", "application/pdf");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities ");        
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<div class=\"page\">",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
        
        //PDF created by OpenOffice
        ci = createContentItem("test2.pdf", "application/pdf");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        //validate plain text results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<div class=\"page\">",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");

    }
    @Test
    public void testMsWord() throws EngineException, IOException {
        ContentItem ci = createContentItem("test.doc", "application/msword");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<title>",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");    }
    @Test
    public void testRtf() throws EngineException, IOException {
        ContentItem ci = createContentItem("test.rtf", "application/rtf");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<title>",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    @Test
    public void testOdt() throws EngineException, IOException {
        ContentItem ci = createContentItem("test.odt", "application/vnd.oasis.opendocument.text");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities such as Paris and people such as Bob Marley.");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<title>",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    @Test
    public void testContentTypeDetection() throws EngineException, IOException {
        ContentItem ci = createContentItem("test.pdf", OCTET_STREAM.toString());
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities");
        //validate XHTML results
        contentPart = ContentItemHelper.getBlob(ci, 
            singleton("application/xhtml+xml"));
        assertNotNull(contentPart);
        Blob xhtmlBlob = contentPart.getValue();
        assertNotNull(xhtmlBlob);
        assertContentRegexp(xhtmlBlob,
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">",
            "<head>",
            "<meta name=",
            "<div class=\"page\">",
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities",
            "</body></html>");
    }
    /**
     * Tests that text is not processed
     */
    @Test
    public void testText() throws EngineException {
        byte[] data = ("The Stanbol enhancer can " +
                "detect famous cities such as Paris and people such as Bob " +
                "Marley.").getBytes(Charset.forName("UTF-8"));
        ContentItem ci = new InMemoryContentItem(data,"text/plain; charset=UTF-8");
        Assert.assertEquals(1, ContentItemHelper.getContentParts(ci, Blob.class).size());
    }
    @Test
    public void testUnsupported() throws EngineException, IOException {
        ContentItem ci = createContentItem("test.pages", "application/x-iwork-pages-sffpages");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        //it MUST NOT give an error but also not add a content part
        assertNull(contentPart);
        //only the original content
        assertEquals(1, ContentItemHelper.getContentParts(ci, Blob.class).size());
        
        
    }
    @Test
    public void testXhtml() throws EngineException, IOException {
        ContentItem ci = createContentItem("test.xhtml", XHTML.toString()+"; charset=UTF-8");
        assertFalse(engine.canEnhance(ci) == CANNOT_ENHANCE);
        engine.computeEnhancements(ci);
        Entry<UriRef,Blob> contentPart = ContentItemHelper.getBlob(ci, 
            singleton("text/plain"));
        assertNotNull(contentPart);
        Blob plainTextBlob = contentPart.getValue();
        assertNotNull(plainTextBlob);
        assertContentRegexp(plainTextBlob, 
            "The Apache Stanbol Enhancer",
            "The Stanbol enhancer can detect famous cities");
        //only the original and the plain text
        // this asserts that no xhtml is parsed from the parsed xhtml content
        assertEquals(2, ContentItemHelper.getContentParts(ci, Blob.class).size());
    }
    
    private ContentItem createContentItem(String resourceName, String contentType){
        InputStream in = TikaEngineTest.class.getClassLoader().getResourceAsStream(resourceName);
        assertNotNull(in);
        byte[] data;
        try {
            data = toByteArray(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read test data!",e);
        }
        closeQuietly(in);
        UriRef ref = new UriRef("urn:contentItem:content-"+ContentItemHelper.toHexString(data));
        return new InMemoryContentItem(data,contentType);
    }
    /**
     * Tests if the parsed regex pattern are contained in any line of the parsed
     * test
     * @throws IOException 
     */
    public void assertContentRegexp(Blob blob, String... regexp) throws IOException {
        Charset charset;
        if(blob.getParameter().containsKey("charset")){
            charset = Charset.forName(blob.getParameter().get("charset"));
        } else {
            charset = Charset.defaultCharset();
        }
        Reader reader = null;
        nextPattern:
        for (String expr : regexp) {
            if(reader != null){
                closeQuietly(reader);
            }
            final Pattern p = Pattern.compile(".*" + expr + ".*");
            reader = new InputStreamReader(blob.getStream(), charset);
            final LineIterator it = new LineIterator(reader);
            while (it.hasNext()) {
                final String line = it.nextLine();
                if (p.matcher(line).matches()) {
                    continue nextPattern;
                }
            }
            fail(this + ": no match for regexp '" + expr + "', content=\n" + 
                    IOUtils.toString(blob.getStream(), charset.toString()));
        }
    }
    @After
    public void unbindServices() {/*nothing to do */}

    @AfterClass
    public static void shutdownServices() {
        engine.deactivate(context);
        engine = null;
    }

}
