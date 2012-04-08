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

import static org.apache.stanbol.enhancer.servicesapi.impl.StringSource.UTF8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class intended to be extended by UnitTest classes for specific {@link Blob}
 * implementations. This class tests if parsed mime-types are handled correctly.
 * It does not test the actual handling of the data, because this is considered
 * specific for each Blob implementation.<p>
 * The {@link #createBlob(String)} MUST BE implemented to use
 * the generic unit tests defined by this class.<p>
 * <b>NOTE:</b>: {@link Blob} implementation can use the 
 * {@link ContentItemHelper#parseMimeType(String)} method for parsing 
 * mime-type string.
 */
public abstract class BlobTest {

    private static byte[] content = "This is a test".getBytes(Charset.forName("UTF-8"));
    
    /**
     * Getter used to get the Blob to test mime-type handling. The content is
     * not used for such tests and may be set to anything.
     * @param mimeType the mimetype
     * @return
     */
    protected abstract Blob createBlob(ContentSource cs) throws IOException;
    /**
     * Internally used to create {@link ContentSource} instanced for the
     * MimeType parsing tests
     */
    private static ContentSource createContentSource(String mimeType){
        return new ByteArraySource(content, mimeType);
    }
    

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyMimeType() throws IOException {
        createBlob(createContentSource(""));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testWildcardType() throws IOException {
        createBlob(createContentSource("*/*;charset=UTF-8"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testWildcardSubType() throws IOException {
        createBlob(createContentSource("text/*;charset=UTF-8"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyMimetype() throws IOException {
        createBlob(createContentSource(";charset=UTF-8"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testMissingSubType() throws IOException {
        createBlob(createContentSource("text;charset=UTF-8"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyType() throws IOException {
        createBlob(createContentSource("/plain;charset=UTF-8"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void testEmptySubType() throws IOException {
        createBlob(createContentSource("text/;charset=UTF-8"));
    }
    
    @Test
    public void testMimeType() throws IOException {
        Blob blob = createBlob(createContentSource("text/plain;charset=UTF-8"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
        
        blob = createBlob(createContentSource("text/plain;charset=UTF-8;other=test"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
        Assert.assertTrue(blob.getParameter().containsKey("other"));
        Assert.assertEquals("test", blob.getParameter().get("other"));
    }
    @Test
    public void testMultipleSeparators() throws IOException {
        Blob blob = createBlob(createContentSource("text/plain;;charset=UTF-8"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
        
        blob = createBlob(createContentSource("text/plain;charset=UTF-8;;other=test"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
        Assert.assertTrue(blob.getParameter().containsKey("other"));
        Assert.assertEquals("test", blob.getParameter().get("other"));
    }
    @Test
    public void testIllegalFormatedParameter() throws IOException {
        Blob blob = createBlob(createContentSource("text/plain;=UTF-8"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());
        
        blob = createBlob(createContentSource("text/plain;charset=UTF-8;=illegal"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));

        blob = createBlob(createContentSource("text/plain;=illegal;charset=UTF-8"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));

        blob = createBlob(createContentSource("text/plain;charset="));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());
        blob = createBlob(createContentSource("text/plain;charset"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());
        
        blob = createBlob(createContentSource("text/plain;charset=UTF-8;test="));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));

        blob = createBlob(createContentSource("text/plain;charset=UTF-8;test"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
    
        blob = createBlob(createContentSource("text/plain;test;charset=UTF-8;"));
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
    }
    @Test(expected=UnsupportedOperationException.class)
    public void testReadOnlyParameter() throws IOException {
        Blob blob = createBlob(createContentSource("text/plain;test;charset=UTF-8"));
        blob.getParameter().put("test", "dummy");
    }
    /**
     * Tests correct handling of  UTF-8 as default charset
     * @throws IOException
     */
    @Test
    public void testString() throws IOException{
        String test = "Exámplê";

        //first via a StringSource
        ContentSource cs = new StringSource(test);
        Blob blob = createBlob(cs);
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals(UTF8.name(), blob.getParameter().get("charset"));

        String value = new String(IOUtils.toByteArray(blob.getStream()),UTF8);
        Assert.assertEquals(test, value);
    }
    /**
     * This tests that texts with custom charsets are converted to UTF-8. 
     * @throws IOException
     */
    @Test
    public void testStringWithCustomCharset() throws IOException{
        String test = "Exámplê";
        Charset ISO8859_4 = Charset.forName("ISO-8859-4");
        //first via a StringSource
        ContentSource cs = new StringSource(test,ISO8859_4,"text/plain");
        Blob blob = createBlob(cs);
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals(ISO8859_4.name(), blob.getParameter().get("charset"));
        //2nd via a ByteArray
        byte[] data = test.getBytes(ISO8859_4);
        cs = new ByteArraySource(data,"text/plain; charset="+ISO8859_4.name());
        blob = createBlob(cs);
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals(ISO8859_4.name(), blob.getParameter().get("charset"));
        //3rd as Stream
        cs = new StreamSource(new ByteArrayInputStream(data), "text/plain; charset="+ISO8859_4.name());
        blob = createBlob(cs);
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals(ISO8859_4.name(), blob.getParameter().get("charset"));
        cs = new StreamSource(new ByteArrayInputStream(data), "text/plain; "+ISO8859_4.name());
    }
    /**
     * Tests the default mimeType "application/octet-stream" for binary data.
     * @throws IOException
     */
    @Test
    public void testDefaultBinaryMimeType() throws IOException {
        Blob blob = createBlob(new ByteArraySource("dummy".getBytes(UTF8)));
        Assert.assertEquals("application/octet-stream", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());

        blob = createBlob(new StreamSource(new ByteArrayInputStream("dummy".getBytes(UTF8))));
        Assert.assertEquals("application/octet-stream", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());
    }
}
