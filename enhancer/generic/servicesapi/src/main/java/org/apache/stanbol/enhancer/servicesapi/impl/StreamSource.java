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
package org.apache.stanbol.enhancer.servicesapi.impl;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;

/**
 * Allows to use a {@link InputStream} as {@link ContentSource}. This is the
 * most common case, that all the contents passed via the RESTful interface of
 * the Stanbol Enhancer will be passed as {@link InputStream}.
 * @author Rupert Westenthaler
 *
 */
public class StreamSource implements ContentSource {
    
    private static final Map<String,List<String>> NO_HEADERS = Collections.emptyMap();
    
    private final InputStream in;
    private final String mt;
    private final String name;
    private final Map<String,List<String>> headers;
    private boolean consumed = false;
    private byte[] data;
    /**
     * Constructs a StreamSource for the passed InputStream. The mime type will
     * be set to "<code>application/octet-stream</code>"
     * @param in the stream
     */
    public StreamSource(InputStream in) {
        this(in,null,null,null);
    }
    /**
     * Constructs a stream source for the passed stream and mime type. When
     * parsing text the charset should be set as mime type parameter (e.g.
     * "<code>text/plain; charset=UTF-8</code>". UTF-8 is assumed as default if 
     * missing. 
     * @param in the stream
     * @param mt the media type or <code>null</code> if unknown
     */
    public StreamSource(InputStream in, String mt) {
        this(in,mt,null,null);
    }
    /**
     * Constructs a stream source for the passed stream and mime type. When
     * parsing text the charset should be set as mime type parameter (e.g.
     * "<code>text/plain; charset=UTF-8</code>". UTF-8 is assumed as default if 
     * missing. <p>
     * This allows in addition to pass the file name of the original file.
     * NOTE however this information is currently not used as the {@link Blob}
     * interface does not support those information
     * @param in the stream
     * @param mt the media type or <code>null</code> if unknown
     * @param fileName the file name or <code>null</code> if unknown
     */
    public StreamSource(InputStream in, String mt,String fileName) {
        this(in,mt,fileName,null);
    }
    /**
     * Constructs a stream source for the passed stream and mime type. When
     * parsing text the charset should be set as mime type parameter (e.g.
     * "<code>text/plain; charset=UTF-8</code>". UTF-8 is assumed as default if 
     * missing. <p>
     * This allows in addition to pass a map with additional header filds
     * (e.g. HTTP headers). <br>
     * NOTE however this information is currently not used 
     * as the {@link Blob} interface does not support those information
     * @param in the stream
     * @param mt the media type or <code>null</code> if unknown
     * @param headers additional headers or <code>null</code>/empty map if none.
     */
    public StreamSource(InputStream in, String mt,Map<String,List<String>> headers) {
        this(in,mt,null,headers);
    }
    /**
     * Constructs a stream source for the passed stream and mime type. When
     * parsing text the charset should be set as mime type parameter (e.g.
     * "<code>text/plain; charset=UTF-8</code>". UTF-8 is assumed as default if 
     * missing. <p>
     * This allows in addition to pass the file name and a map with 
     * additional header fields (e.g. HTTP headers). <br>
     * NOTE however this information is currently not used 
     * as the {@link Blob} interface does not support those information
     * @param in the stream
     * @param mt the media type or <code>null</code> if unknown
     * @param fileName the file name or <code>null</code> if unknown
     * @param headers additional headers or <code>null</code>/empty map if none.
     */
    public StreamSource(InputStream in, String mt,String fileName,Map<String,List<String>> headers) {
        if(in == null){
            throw new IllegalArgumentException("The passed InputStream MUST NOT be NULL!");
        }
        this.in = in;
        this.mt = mt == null ? "application/octet-stream" : mt;
        this.name = fileName;
        this.headers = headers == null ? NO_HEADERS : headers;
    }
    @Override
    public synchronized InputStream getStream() {
        if(data != null){
            return new ByteArrayInputStream(data);
        }
        if(consumed){
            throw new IllegalStateException("This InputStream of this ContentSource is already consumed!");
        }
        consumed = true;
        return new NonCloseableInputStream(in);
    }
    @Override
    public synchronized byte[] getData() throws IOException {
        if(data == null){
            data = IOUtils.toByteArray(in);
        }
        return data;
    }

    @Override
    public String getMediaType() {
        return mt;
    }

    @Override
    public String getFileName() {
        return name;
    }
    @Override
    public Map<String,List<String>> getHeaders() {
        return headers;
    }
// The parsed Stream MUST NOT be closed (STANBOL-898)
//    @Override
//    protected void finalize() throws Throwable {
//        IOUtils.closeQuietly(in);
//    }
    
    /**
     * Internally used to ensure that InputStreams returned by
     * {@link StreamSource#getStream()} can not be closed by callers (as 
     * requested by STANBOL-898)
     */
    private static class NonCloseableInputStream extends FilterInputStream {

        protected NonCloseableInputStream(InputStream in) {
            super(in);
        }
    
        @Override
        public void close() throws IOException {
            //ignore call to close
        }
    }
}