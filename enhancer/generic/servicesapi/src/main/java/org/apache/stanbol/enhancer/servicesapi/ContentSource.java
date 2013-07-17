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
package org.apache.stanbol.enhancer.servicesapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The content source representing the data and optionally the media type
 * and file name. This interface is only used to parse the content
 * when creating a {@link ContentItem}. To obtain the content of a
 * ContentItem the {@link Blob} interface is used<p>
 * NOTE that {@link #getStream()} can typically only be called a single time.
 * Multiple calls will throw an {@link IllegalArgumentException}.
 * @see Blob
 */
public interface ContentSource {
    /**
     * Getter for the data as stream. This method might only work a single time so
     * multiple calls might result in {@link IllegalStateException}s.<p>
     * {@link ContentItem}/{@link Blob} implementations that keep the
     * content in memory should preferable use {@link #getData()} to
     * obtain the content from the source.
     * @return the data.
     * @throws IllegalStateException if the stream is already consumed and
     * can not be re-created.
     * @see #getStream()
     */
    InputStream getStream();
    /**
     * Getter for the data as byte array. <p>
     * NOTE that his method will load
     * the content in-memory. However using this method instead of
     * {@link #getStream()} might preserve holding multiple in-memory version
     * of the same content in cases where both the {@link ContentSource}
     * and the {@link ContentItem} are internally using an byte array to
     * hold the content. <p> 
     * As a rule of thumb this method should only be
     * used by in-memory {@link ContentItem}/{@link Blob} implementations.
     * @return the content as byte array.
     * @throws IOException On any error while reading the data from the source.
     * @throws IllegalStateException If the {@link #getStream()} was already
     * consumed when calling this method.
     * @see #getStream()
     */
    byte[] getData() throws IOException;
    /**
     * An valid media type as defined by 
     * <a href="http://tools.ietf.org/html/rfc2046">RFC2046</a>.
     * "application/octet-stream" if unknown
     * @return The media type or <code>null</code> if unknown
     */
    String getMediaType();
    /**
     * The original file name.
     * @return the name of the file or <code>null</code> if not known
     */
    String getFileName();
    /**
     * Getter for additional header information about the ContentSource. The
     * returned Map MUST NOT be <code>null</code> and MAY be read-only.
     * @return additional header information.
     */
    Map<String,List<String>> getHeaders();
}