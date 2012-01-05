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
package org.apache.stanbol.enhancer.serviceapi.helper;

import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class intended to be extended by UnitTest classes for specific {@link Blob}
 * implementations. This class tests if parsed mime-types are handled correctly.
 * It does not test the actual handling of the data, because this is considered
 * specific for each Blob implementation.<p>
 * The {@link #getBlobToTestMimetypeHandling(String)} MUST BE implemented to use
 * the generic unit tests defined by this class.<p>
 * <b>NOTE:</b>: {@link Blob} implementation can use the 
 * {@link ContentItemHelper#parseMimeType(String)} method for parsing 
 * mime-type string.
 * @see InMemoryBlobTest
 */
public abstract class AbstractBlobTest {

    /**
     * Getter used to get the Blob to test mime-type handling. The content is
     * not used for such tests and may be set to anything.
     * @param mimeType the mimetype
     * @return
     */
    protected abstract Blob getBlobToTestMimetypeHandling(String mimeType);

    @Test
    public void testNullWildCard(){
        Blob blob;
        try {
            blob = getBlobToTestMimetypeHandling(null);
        } catch (IllegalArgumentException e) {
            //if no detection of the mimeType is supported this is expected
            return;
        }
        //if autodetection is supported, check that the mimetype is not null
        Assert.assertNotNull(blob.getMimeType());
        Assert.assertFalse(blob.getMimeType().isEmpty());
    }
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyMimeType(){
        getBlobToTestMimetypeHandling("");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testWildcardType(){
        getBlobToTestMimetypeHandling("*/*;charset=UTF-8");
    }
    @Test(expected=IllegalArgumentException.class)
    public void testWildcardSubType(){
        getBlobToTestMimetypeHandling("text/*;charset=UTF-8");
    }
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyMimetype(){
        getBlobToTestMimetypeHandling(";charset=UTF-8");
    }
    @Test(expected=IllegalArgumentException.class)
    public void testMissingSubType(){
        getBlobToTestMimetypeHandling("text;charset=UTF-8");
    }
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyType(){
        getBlobToTestMimetypeHandling("/plain;charset=UTF-8");
    }
    @Test(expected=IllegalArgumentException.class)
    public void testEmptySubType(){
        getBlobToTestMimetypeHandling("text/;charset=UTF-8");
    }
    
    @Test
    public void testMimeType(){
        Blob blob = getBlobToTestMimetypeHandling("text/plain;charset=UTF-8");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
        
        blob = getBlobToTestMimetypeHandling("text/plain;charset=UTF-8;other=test");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
        Assert.assertTrue(blob.getParameter().containsKey("other"));
        Assert.assertEquals("test", blob.getParameter().get("other"));
    }
    @Test
    public void testMultipleSeparators(){
        Blob blob = getBlobToTestMimetypeHandling("text/plain;;charset=UTF-8");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
        
        blob = getBlobToTestMimetypeHandling("text/plain;charset=UTF-8;;other=test");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
        Assert.assertTrue(blob.getParameter().containsKey("other"));
        Assert.assertEquals("test", blob.getParameter().get("other"));
    }
    @Test
    public void testIllegalFormatedParameter(){
        Blob blob = getBlobToTestMimetypeHandling("text/plain;=UTF-8");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());
        
        blob = getBlobToTestMimetypeHandling("text/plain;charset=UTF-8;=illegal");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));

        blob = getBlobToTestMimetypeHandling("text/plain;=illegal;charset=UTF-8");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));

        blob = getBlobToTestMimetypeHandling("text/plain;charset=");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());
        blob = getBlobToTestMimetypeHandling("text/plain;charset");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertTrue(blob.getParameter().isEmpty());
        
        blob = getBlobToTestMimetypeHandling("text/plain;charset=UTF-8;test=");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));

        blob = getBlobToTestMimetypeHandling("text/plain;charset=UTF-8;test");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
    
        blob = getBlobToTestMimetypeHandling("text/plain;test;charset=UTF-8;");
        Assert.assertEquals("text/plain", blob.getMimeType());
        Assert.assertEquals(blob.getParameter().size(),1);
        Assert.assertTrue(blob.getParameter().containsKey("charset"));
        Assert.assertEquals("UTF-8", blob.getParameter().get("charset"));
    }
    @Test(expected=UnsupportedOperationException.class)
    public void testReadOnlyParameter(){
        Blob blob = getBlobToTestMimetypeHandling("text/plain;test;charset=UTF-8");
        blob.getParameter().put("test", "dummy");
    }

}
