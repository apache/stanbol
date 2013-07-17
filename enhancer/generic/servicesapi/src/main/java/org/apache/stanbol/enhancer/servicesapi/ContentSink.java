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

import java.io.OutputStream;

/**
 * A {@link ContentSink} allows to stream content to a {@link Blob}. This
 * allows to "stream" content that is created by the Stanbol Enhancer (e.g.
 * the plain text extracted from an PDF) directly to the Blob and therefore
 * greatly reduces the memory footprint if dealing with very large 
 * content.<p>
 * <b>IMPORTANT NOTE:</b> Do not parse the {@link Blob} of a {@link ContentSink}
 * to any other components until all the data are written to the 
 * {@link OutputStream}, because this may cause that other components to read
 * partial data when calling {@link Blob#getStream()}!<br>
 * This feature is intended to reduce the memory footprint and not to support
 * concurrent writing and reading of data as supported by pipes. However
 * if you can come-up with a nice use case that would require this ability
 * let us know on the stanbol-dev mailing list or by creating an 
 * <a href=https://issues.apache.org/jira/browse/STANBOL>issue</a>. 
 */
public interface ContentSink {
    /**
     * The output stream - sink - for the content. Multiple calls to 
     * this method will return the same {@link OutputStream}.<p>
     * Users need to ensure that the provided stream is correctly closed
     * after all the content was written to it.
     * @return the output stream used to stream the content to the
     * {@link Blob}
     */
    OutputStream getOutputStream();
    /**
     * The - initially empty - Blob. if this Blob is shared with other 
     * components (e.g. added to an {@link ContentItem}) before all data are 
     * written to the sink, than other engines will be able to access it
     * even while data are still added to blob.
     * @return The blob.
     */
    Blob getBlob();
}