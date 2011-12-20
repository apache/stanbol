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
package org.apache.stanbol.enhancer.servicesapi.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.Blob;

/**
 * Holds the parsed data in an byte array. Parsed byte[] are NOT copied,
 * Strings are encoded as UTF-8 and {@link InputStream} are copied by using 
 * {@link IOUtils#toByteArray(InputStream)}.<p>
 * The default mime-types (if <code>null</code> is parsed as mimeType) are for
 * Strings "text/plain" and in all other cases "application/octet-stream".
 */
public class InMemoryBlob implements Blob {
    private static final Charset UTF8 = Charset.forName("utf-8");
    public static final String DEFAULT_TEXT_MIMETYPE = "text/plain";
    public static final String DEFAULT_BINARY_MIMETYPE = "application/octet-stream";

    protected final String mimeType;
    protected final Map<String,String> parameters;
    
    private byte[] data;
	/**
	 * Creates an {@link InMemoryBlob} for the parsed String. If a "charset"
	 * parameter is present for the parsed mimeType it is replaced with "UTF-8"
	 * used to encode the Sting as byte[].
	 * @param text the text
	 * @param mimeType the mimeType. If <code>null</code> "text/plain" is used
	 * as default
	 */
	public InMemoryBlob(String text, String mimeType){
	    this(text.getBytes(UTF8),mimeType != null ? mimeType : DEFAULT_TEXT_MIMETYPE,
	            Collections.singletonMap("charset", UTF8.name()));
	}
	/**
	 * Creates an instance for the parsed {@link InputStream}. Data are copied
	 * to a byte array. The parsed stream is closed after copying the data.
	 * @param in the {@link InputStream}. MUST NOT be <code>null</code>
	 * @param mimeType the mime-type. If <code>null</code>  "application/octet-stream"
	 * is used as default.
	 * @throws IOException indicates an error while reading from the parsed stream
	 */
	public InMemoryBlob(InputStream in,String mimeType) throws IOException {
	    this(IOUtils.toByteArray(in),mimeType);
	    IOUtils.closeQuietly(in);
	}
	/**
	 * Creates an instance for the parsed byte array. The array is NOT copied
	 * therefore changes within that array will be reflected to components
	 * reading the data from this Blob.
	 * @param data the data. MIST NOT be <code>null</code>
	 * @param mimeType the mime-type. If <code>null</code>  "application/octet-stream"
     * is used as default.
	 */
	public InMemoryBlob(byte[] data, String mimeType) {
	    this(data,mimeType,null);
	}
	/**
	 * Internally used constructor that allows to parse additional parameters as
	 * required to ensure setting the 'charset' in case initialisation was done
	 * by parsing a string
	 * @param data
	 * @param mimeType
	 * @param parsedParameters
	 */
    protected InMemoryBlob(byte[] data, String mimeType,Map<String,String> parsedParameters) {
        if(data == null){
            throw new IllegalArgumentException("The parsed content MUST NOT be NULL!");
        }
        this.data = data;
        Map<String,String> parameters;
	    if(mimeType == null){
	        this.mimeType = DEFAULT_BINARY_MIMETYPE;
	        parameters = new HashMap<String,String>();
	    } else {
	        parameters = ContentItemHelper.parseMimeType(mimeType);
	        this.mimeType = parameters.remove(null);
	    }
	    if(parsedParameters != null){
	        parameters.putAll(parsedParameters);
	    }
	    this.parameters = Collections.unmodifiableMap(parameters);
	}

	@Override
	public final InputStream getStream() {
		return new ByteArrayInputStream(data);
	}
	@Override
	public final long getContentLength() {
	    return data.length;
	}
    @Override
    public final String getMimeType() {
        return mimeType;
    }
    @Override
    public final Map<String,String> getParameter() {
        return parameters;
    }
}
