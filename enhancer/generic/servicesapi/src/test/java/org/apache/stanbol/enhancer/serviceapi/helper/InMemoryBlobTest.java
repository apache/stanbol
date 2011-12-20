package org.apache.stanbol.enhancer.serviceapi.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryBlob;
import org.junit.Assert;
import org.junit.Test;

public class InMemoryBlobTest extends AbstractBlobTest {
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    /*
     * Override to test InMemoryBlob instead of AbstractBlob
     * @see org.apache.stanbol.enhancer.serviceapi.helper.BlobMimeTypeHandlingTest#getBlobToTestMimetypeHandling(java.lang.String)
     */
    @Override
    protected Blob getBlobToTestMimetypeHandling(String mimeType) {
        return new InMemoryBlob("dummy".getBytes(UTF8), mimeType);
    }
    /**
     * Tests correct handling of strings and the DEFAULT mimeType for strings
     * "text/plain"
     * @throws IOException
     */
    @Test
    public void testString() throws IOException{
        String test = "Exámplê";
        Blob blob = new InMemoryBlob(test, null);
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals(UTF8.name(), blob.getParameter().get("charset"));
        
        String value = new String(IOUtils.toByteArray(blob.getStream()),UTF8);
        Assert.assertEquals(test, value);
    }
    /**
     * Tests that any parsed Charset is replaced by UTF-8 actually used to
     * convert the String into bytes.
     * @throws IOException
     */
    @Test
    public void testStringWithCharset() throws IOException{
        String test = "Exámplê";
        Blob blob = new InMemoryBlob(test, "text/plain;charset=ISO-8859-4");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals(UTF8.name(), blob.getParameter().get("charset"));
    }
    /**
     * Tests the default mimeType "application/octet-stream" for binary data.
     * @throws IOException
     */
    @Test
    public void testDefaultBinaryMimeType() throws IOException {
        Blob blob = new InMemoryBlob("dummy".getBytes(UTF8), null);
        Assert.assertEquals("application/octet-stream", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());

        blob = new InMemoryBlob(new ByteArrayInputStream("dummy".getBytes(UTF8)), null);
        Assert.assertEquals("application/octet-stream", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());
    }
}
