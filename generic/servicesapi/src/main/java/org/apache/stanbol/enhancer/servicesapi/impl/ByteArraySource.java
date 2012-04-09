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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;

/**
 * Allows to use a byte array as {@link ContentSource}. If the content is
 * already stored in an byte array this implementation is preferable over the
 * {@link StreamSource} as it does not require to copy the content to yet an
 * other byte array when creating an in-memory content item when using the
 * {@link #getData()} method.
 * 
 * @author Rupert Westenthaler
 *
 */
public class ByteArraySource implements ContentSource {

    private static final Map<String,List<String>> NO_HEADERS = Collections.emptyMap();
    
    private byte[] content;
    private String mt;
    private String name;
    private Map<String,List<String>> headers;

    /**
     * Constructs a {@link ContentSource} for the passed byte array. The mime type will
     * be set to "<code>application/octet-stream</code>"
     * @param content the array containing the content
     */
    public ByteArraySource(byte[] content) {
        this(content,null,null,null);
    }
    /**
     * Constructs a {@link ContentSource} for the passed byte array and mime type. 
     * When parsing text the charset should be set as mime type parameter (e.g.
     * "<code>text/plain; charset=UTF-8</code>". UTF-8 is assumed as default if 
     * missing.
     * @param content the array containing the content
     * @param mt the media type or <code>null</code> if unknown
     */
    public ByteArraySource(byte[] content,String mt) {
        this(content,mt,null,null);
    }
    /**
     * Constructs a {@link ContentSource} for the passed byte array and mime type. 
     * When parsing text the charset should be set as mime type parameter (e.g.
     * "<code>text/plain; charset=UTF-8</code>". UTF-8 is assumed as default if 
     * missing. <p>
     * This allows in addition to pass the file name or the original file.<br>
     * NOTE however this information is currently not used 
     * as the {@link Blob} interface does not support those information
     * @param content the array containing the content
     * @param mt the media type or <code>null</code> if unknown
     * @param fileName the file name or <code>null</code> if unknown
     */
    public ByteArraySource(byte[] content,String mt, String fileName) {
        this(content,mt,fileName,null);
    }
    /**
     * Constructs a {@link ContentSource} for the passed byte array and mime type. 
     * When parsing text the charset should be set as mime type parameter (e.g.
     * "<code>text/plain; charset=UTF-8</code>". UTF-8 is assumed as default if 
     * missing. <p>
     * This allows in addition to pass the file name and a map with 
     * additional header fields (e.g. HTTP headers). <br>
     * NOTE however this information is currently not used 
     * as the {@link Blob} interface does not support those information
     * @param content the array containing the content
     * @param mt the media type or <code>null</code> if unknown
     * @param fileName the file name or <code>null</code> if unknown
     * @param headers additional headers or <code>null</code>/empty map if none.
     */
    public ByteArraySource(byte[] content,String mt, String fileName,Map<String,List<String>> headers) {
        if(content == null){
            throw new IllegalArgumentException("The parsed byte array MUST NOT be NULL!");
        }
        this.content = content;
        this.mt = mt == null ? "application/octet-stream" : mt;
        this.name = fileName;
        this.headers = headers == null ? NO_HEADERS : headers;
    }

    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(content);
    }
    @Override
    public byte[] getData() throws IOException {
        return content;
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
    
}