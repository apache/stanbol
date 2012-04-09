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
package org.apache.stanbol.enhancer.contentitem.inmemory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentReference;
import org.apache.stanbol.enhancer.servicesapi.ContentSink;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;

/**
 * Holds the parsed data in an byte array. <p>
 * In case a byte[]  is used to construct the parsed data are NOT copied. In
 * case of an {@link ByteArrayOutputStream} data are retrieved from the stream
 * on each call to {@link #getStream()} if new data where added to the output
 * stream in the meantime.<p>
 * Also NOTE that all public constructors are deprecated. Users are
 * encouraged to use the {@link InMemoryContentItemFactory} with a fitting
 * {@link ContentSource} or {@link ContentReference} to create {@link Blob}
 * instances.<p>
 * NOTES regarding the deprecated Constructors:<br>
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
    
    private ByteArrayOutputStream bao;
    private int size = -1;
    private byte[] data;
    
	/**
	 * Creates an {@link InMemoryBlob} for the parsed String. If a "charset"
	 * parameter is present for the parsed mimeType it is replaced with "UTF-8"
	 * used to encode the Sting as byte[].
	 * @param text the text
	 * @param mimeType the mimeType. If <code>null</code> "text/plain" is used
	 * as default
	 * @deprecated use {@link InMemoryContentItemFactory#createBlob(ContentSource)} 
	 * with a {@link StringSource} instead
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
     * @deprecated use {@link InMemoryContentItemFactory#createBlob(ContentSource)} with
     * a {@link StreamSource} instead
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
     * @deprecated use {@link InMemoryContentItemFactory#createBlob(ContentSource)} 
     * with a {@link ByteArraySource} instead
	 */
	public InMemoryBlob(byte[] data, String mimeType) {
	    this(data,mimeType,null);
	}
	/**
	 * Constructor that allows to create a byte array backed Blob based on a
	 * fixed set of parsed data.
	 * @param data the data (content of the Blob)
	 * @param mimeType the mimeType (<code>null</code> if not know; supports parameters)
	 * @param parsedParameters additional parameters (will override parameters parsed
	 * with the mimeType; <code>null</code> or mepty map if none)
	 */
    protected InMemoryBlob(byte[] data, String mimeType,Map<String,String> parsedParameters) {
        this(mimeType,parsedParameters);
        if(data == null){
            throw new IllegalArgumentException("The parsed content MUST NOT be NULL!");
        }
        this.data = data;
	}
    /**
     * Allows to create a in-memory {@link Blob} that represents the data as
     * written to the parsed {@link ByteArrayOutputStream}. NOTE that
     * {@link #getStream()} will return an {@link InputStream} over the 
     * {@link ByteArrayOutputStream#toByteArray() available bytes} at the
     * time of the call. Therefore it will return partial contents if not yet
     * all data where written to the parsed output stream!<p>
     * To workaround this one would need to use a pipe with an infinite buffer
     * that can be read my multiple {@link InputStream}s. However currently this
     * feature is not required by the {@link ContentSink} interface.
     * @param bao the {@link ByteArrayOutputStream}
     * @param mimeType the mimeType (<code>null</code> if not know; supports parameters)
     * @param parsedParameters additional parameters (will override parameters parsed
     * with the mimeType; <code>null</code> or mepty map if none)
     * @throws IllegalArgumentException if the parsed output stream is <code>null</code>
     */
    protected InMemoryBlob(ByteArrayOutputStream bao,String mimeType,Map<String,String> parsedParameters){
        this(mimeType,parsedParameters);
        if(bao == null){
            throw new IllegalArgumentException("The parsed ByteArrayOutputStream MUST NOT be NULL!");
        }
        this.bao = bao;
    }
    /**
     * Internally used to correctly init the parsed mimeType and parameter
     * @param mimeType
     * @param parsedParameters
     */
    private InMemoryBlob(String mimeType,Map<String,String> parsedParameters){
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
	    //if a ByteArrayOutputStream is used to stream the data to the blob,
	    //than check if we need to create a new array for creating the stream.
	    if(bao != null && bao.size() != size){
	        data = bao.toByteArray();
	        size = data.length;
	    }
		return new ByteArrayInputStream(data);
	}
	@Override
	public final long getContentLength() {
	    return bao != null ? bao.size() : data.length;
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
