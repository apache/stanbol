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


import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.enhancer.servicesapi.Blob;


/**
 * ContentItem implementation that holds a complete copy of the data in
 * memory. Internally it uses {@link InMemoryBlob} to store the content and
 * an {@link SimpleMGraph} for the metadata.
 * <p>
 * This implementation can be used independently of any store implementation and
 * is suitable for stateless processing.
 */
public class InMemoryContentItem extends ContentItemImpl {

//Do not allow to create a ContentItem without a content
//    public InMemoryContentItem(String id) {
//        this(id, null, null, null);
//    }

    public InMemoryContentItem(byte[] content, String mimeType) {
        this((UriRef)null,new InMemoryBlob(content, mimeType),null);
    }
    
    public InMemoryContentItem(String id, String content, String mimeType) {
		this(id, new InMemoryBlob(content, mimeType),null);
	}

    public InMemoryContentItem(String id, byte[] content, String mimetype) {
        this(id,new InMemoryBlob(content, mimetype),null);
    }

    public InMemoryContentItem(String uriString, byte[] content, String mimeType,
            MGraph metadata) {
    	this(uriString != null? new UriRef(uriString) : null ,
    	        new InMemoryBlob(content, mimeType),
    	        metadata);
    }
    public InMemoryContentItem(UriRef uriRef, String content, String mimeType) {
		this(uriRef, new InMemoryBlob(content, mimeType), null);
	}
    public InMemoryContentItem(UriRef uri, byte[] content, String mimeType, MGraph metadata) {
        this(uri, new InMemoryBlob(content, mimeType),metadata);
    }
    protected InMemoryContentItem(String uriString, Blob blob, MGraph metadata) {
        this(uriString != null ? new UriRef(uriString) : null, blob, metadata);
    }
    protected InMemoryContentItem(UriRef uri, Blob blob, MGraph metadata) {
        super(uri == null ? ContentItemHelper.makeDefaultUrn(blob): uri,blob,
                metadata == null ? new IndexedMGraph() : metadata);
    }

	protected static final InMemoryContentItem fromString(String content) {
        return new InMemoryContentItem(content.getBytes(), "text/plain");
    }

}
