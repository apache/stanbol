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
package org.apache.stanbol.enhancer.serviceapi.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;
import org.junit.Test;

public class ContentSourceTest {

    protected static final Charset UTF8 = Charset.forName("UTF-8");
    protected static final String TEST_STRING = "Thîs áre têst dàtá!";
    protected static final String DEFAULT_MT = "application/octet-stream";
    protected static final String STRING_DEFAULT_MT = "text/plain; charset=UTF-8";
    protected static final String MT = "text/plain";
    protected static final String FILE_NAME = "test.txt";
    protected static final Map<String,List<String>> HEADERS = new HashMap<String,List<String>>();
    static {
        HEADERS.put("Accept", Arrays.asList("application/rdf+xml"));
        HEADERS.put("Accept-Language", Arrays.asList("en","de"));
    }
    protected static final String MT_WITH_PARAM = "text/plain; charset=UTF-8";
    protected static final byte[] DATA = TEST_STRING.getBytes(UTF8);
    
    /*
     * Tests ensuring the IllegalArgumentExceptions if null is parsed as stream
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingStream(){
        new StreamSource(null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingStream1(){
        new StreamSource(null,MT);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingStream2(){
        new StreamSource(null,MT,FILE_NAME);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingStream3(){
        new StreamSource(null,MT,HEADERS);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingStream4(){
        new StreamSource(null,MT,FILE_NAME,HEADERS);
    }
    /*
     * Tests ensuring the IllegalArgumentExceptions if null is parsed as
     * byte array to 
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingByteArray(){
        new ByteArraySource(null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingByteArray1(){
        new ByteArraySource(null,MT);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingByteArray2(){
        new ByteArraySource(null,MT,FILE_NAME);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingByteArray3(){
        new ByteArraySource(null,MT,FILE_NAME,HEADERS);
    }
    /*
     * Tests ensuring the IllegalArgumentExceptions if null is parsed as
     * String to 
     */
    @Test(expected=IllegalArgumentException.class)
    public void missingString(){
        new StringSource(null);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingString1(){
        new StringSource(null,MT);
    }
    @Test(expected=IllegalArgumentException.class)
    public void missingString2(){
        new StringSource(null,UTF8,MT);
    }
    
    /*
     * Tests checking correct handling of data
     */
    @Test
    public void checkStreamFromStreamSource() throws IOException {
        ContentSource source = new StreamSource(new ByteArrayInputStream(DATA));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(source.getStream(), out);
        Assert.assertTrue(Arrays.equals(DATA, out.toByteArray()));
        try {
            source.getStream();
            //multiple calls are supported -> is OK
        } catch (RuntimeException e) {
            //multiple calls are not supported -> illegal state
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }
    @Test
    public void checkDataFromStreamSource() throws IOException {
        ContentSource source = new StreamSource(new ByteArrayInputStream(DATA));
        Assert.assertTrue(Arrays.equals(DATA, source.getData()));
        //multiple calls must work
        source.getData();
    }

    @Test
    public void checkStreamFromByteArraySource() throws IOException {
        ContentSource source = new ByteArraySource(DATA);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(source.getStream(), out);
        Assert.assertTrue(Arrays.equals(DATA, out.toByteArray()));
        try {
            source.getStream();
            //multiple calls are supported -> is OK
        } catch (RuntimeException e) {
            //multiple calls are not supported -> illegal state
            Assert.assertTrue(e instanceof IllegalStateException);
        }
    }
    @Test
    public void checkDataFromByteArraySource() throws IOException {
        ContentSource source = new ByteArraySource(DATA);
        assertTrue(Arrays.equals(DATA, source.getData()));
        //also check that the array is not copied
        //Also checks multiple calls to getData MUST work
        assertSame(DATA, source.getData());
    }
    
    @Test
    public void checkStreamFromStringSource() throws IOException {
        ContentSource source = new StringSource(TEST_STRING);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(source.getStream(), out);
        Assert.assertTrue(Arrays.equals(DATA, out.toByteArray()));
        try {
            source.getStream();
            //multiple calls are supported -> is OK
        } catch (RuntimeException e) {
            //multiple calls are not supported -> illegal state
            Assert.assertTrue(e instanceof IllegalStateException);
        }
        //test different encoding
        Charset ISO8859_4 = Charset.forName("ISO-8859-4");
        byte[] iso8859_4_data = TEST_STRING.getBytes(ISO8859_4);
        source = new StringSource(TEST_STRING,ISO8859_4,null);
        out = new ByteArrayOutputStream();
        IOUtils.copy(source.getStream(), out);
        Assert.assertTrue(Arrays.equals(iso8859_4_data, out.toByteArray()));
        
    }
    @Test
    public void checkDataFromStringSource() throws IOException {
        ContentSource source = new ByteArraySource(DATA);
        Assert.assertTrue(Arrays.equals(DATA, source.getData()));
        //multiple calls must work
        source.getData();
    }

    
    /*
     * Tests checking correct handling of parameters and default values
     */
    
    @Test
    public void checkMediaTypeForStreamSource() throws IOException {
        ContentSource source = new StreamSource(new ByteArrayInputStream(DATA));
        assertEquals(DEFAULT_MT, source.getMediaType());
        source = new StreamSource(new ByteArrayInputStream(DATA),null);
        assertEquals(DEFAULT_MT, source.getMediaType());
        source = new StreamSource(new ByteArrayInputStream(DATA),null,HEADERS);
        assertEquals(DEFAULT_MT, source.getMediaType());
        source = new StreamSource(new ByteArrayInputStream(DATA),null,FILE_NAME,HEADERS);
        assertEquals(DEFAULT_MT, source.getMediaType());
        
        source = new StreamSource(new ByteArrayInputStream(DATA),MT);
        assertEquals(MT, source.getMediaType());
        source = new StreamSource(new ByteArrayInputStream(DATA),MT,HEADERS);
        assertEquals(MT, source.getMediaType());
        source = new StreamSource(new ByteArrayInputStream(DATA),MT,FILE_NAME,HEADERS);
        assertEquals(MT, source.getMediaType());
        //Parameters MUST BE preserved!
        source = new StreamSource(new ByteArrayInputStream(DATA),MT_WITH_PARAM);
        assertEquals(MT_WITH_PARAM, source.getMediaType());
        source = new StreamSource(new ByteArrayInputStream(DATA),MT_WITH_PARAM,HEADERS);
        assertEquals(MT_WITH_PARAM, source.getMediaType());
        source = new StreamSource(new ByteArrayInputStream(DATA),MT_WITH_PARAM,FILE_NAME,HEADERS);
        assertEquals(MT_WITH_PARAM, source.getMediaType());
    }
    @Test
    public void checkMediaTypeForByteArraySource() throws IOException {
        ContentSource source = new ByteArraySource(DATA);
        assertEquals(DEFAULT_MT, source.getMediaType());
        source = new ByteArraySource(DATA,null);
        assertEquals(DEFAULT_MT, source.getMediaType());
        source = new ByteArraySource(DATA,null,FILE_NAME,HEADERS);
        assertEquals(DEFAULT_MT, source.getMediaType());
        
        source = new ByteArraySource(DATA,MT);
        assertEquals(MT, source.getMediaType());
        source = new ByteArraySource(DATA,MT,FILE_NAME,HEADERS);
        assertEquals(MT, source.getMediaType());
        //Parameters MUST BE preserved!
        source = new ByteArraySource(DATA,MT_WITH_PARAM);
        assertEquals(MT_WITH_PARAM, source.getMediaType());
        source = new ByteArraySource(DATA,MT_WITH_PARAM,FILE_NAME);
        assertEquals(MT_WITH_PARAM, source.getMediaType());
        source = new ByteArraySource(DATA,MT_WITH_PARAM,FILE_NAME,HEADERS);
        assertEquals(MT_WITH_PARAM, source.getMediaType());
    }
    @Test
    public void checkMediaTypeForStringSource() throws IOException {
        ContentSource source = new StringSource(TEST_STRING);
        assertEquals(STRING_DEFAULT_MT, source.getMediaType());
        source = new StringSource(TEST_STRING,null);
        assertEquals(STRING_DEFAULT_MT, source.getMediaType());
        source = new StringSource(TEST_STRING,UTF8,null);
        assertEquals(STRING_DEFAULT_MT, source.getMediaType());
        source = new StringSource(TEST_STRING,null,null);
        assertEquals(STRING_DEFAULT_MT, source.getMediaType());
        
        //this can be used to force the system default
        source = new StringSource(TEST_STRING,Charset.defaultCharset(),null);
        Map<String,String> mt = ContentItemHelper.parseMimeType(source.getMediaType());
        assertEquals("text/plain", mt.get(null));
        assertEquals(Charset.defaultCharset().name(), mt.get("charset"));
        
        String OTHER_MT = "text/rtf";
        source = new StringSource(TEST_STRING,OTHER_MT);
        mt = ContentItemHelper.parseMimeType(source.getMediaType());
        assertEquals(OTHER_MT, mt.get(null));
        assertEquals(UTF8.name(), mt.get("charset"));
        
        source = new StringSource(TEST_STRING, null,OTHER_MT);
        mt = ContentItemHelper.parseMimeType(source.getMediaType());
        assertEquals(OTHER_MT, mt.get(null));
        assertEquals(UTF8.name(), mt.get("charset"));
        
        Charset ISO8859_4 = Charset.forName("ISO-8859-4");
        source = new StringSource(TEST_STRING, ISO8859_4,OTHER_MT);
        mt = ContentItemHelper.parseMimeType(source.getMediaType());
        assertEquals(OTHER_MT, mt.get(null));
        assertEquals(ISO8859_4.name(), mt.get("charset"));
    }

    @Test
    public void checkFileName() throws IOException{
        ContentSource source = new StreamSource(new ByteArrayInputStream(DATA),null,null,null);
        assertNull(source.getFileName());

        source = new StreamSource(new ByteArrayInputStream(DATA),null,FILE_NAME,null);
        assertEquals(FILE_NAME, source.getFileName());
        
        source = new ByteArraySource(DATA,null,FILE_NAME);
        assertEquals(FILE_NAME, source.getFileName());
        
        source = new ByteArraySource(DATA,null,FILE_NAME,null);
        assertEquals(FILE_NAME, source.getFileName());
        
    }

    @Test
    public void checkHeaders() throws IOException{
        ContentSource source = new StreamSource(new ByteArrayInputStream(DATA),null,null,null);
        assertNotNull(source.getHeaders());
        assertTrue(source.getHeaders().isEmpty());
        source = new StreamSource(new ByteArrayInputStream(DATA),null,null,HEADERS);
        assertEquals(HEADERS, source.getHeaders());
        
        source = new ByteArraySource(DATA,null,null,null);
        assertNotNull(source.getHeaders());
        assertTrue(source.getHeaders().isEmpty());
        source = new ByteArraySource(DATA,null,null,HEADERS);
        assertEquals(HEADERS, source.getHeaders());
    }
    
}
