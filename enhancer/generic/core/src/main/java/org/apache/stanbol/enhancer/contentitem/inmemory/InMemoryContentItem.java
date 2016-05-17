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


import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.impl.ByteArraySource;
import org.apache.stanbol.enhancer.servicesapi.impl.ContentItemImpl;
import org.apache.stanbol.enhancer.servicesapi.impl.StringSource;


/**
 * ContentItem implementation that holds a complete copy of the data in
 * memory. Internally it uses {@link InMemoryBlob} to store the content and
 * an {@link SimpleGraph} for the metadata.
 * <p>
 * This implementation can be used independently of any store implementation and
 * is suitable for stateless processing.
 */
public class InMemoryContentItem extends ContentItemImpl {

//Do not allow to create a ContentItem without a content
//    public InMemoryContentItem(String id) {
//        this(id, null, null, null);
//    }

    /**
     * 
     * @param content
     * @param mimeType
     * @deprecated use {@link InMemoryContentItemFactory#createContentItem(ContentItemFactory.ContentSource)} 
     * with a {@link ByteArraySource}
     */
    public InMemoryContentItem(byte[] content, String mimeType) {
        this((IRI)null,new InMemoryBlob(content, mimeType),null);
    }
    /**
     * 
     * @param id
     * @param content
     * @param mimeType
     * @deprecated use {@link InMemoryContentItemFactory#createContentItem(IRI, ContentSource)}
     * with a {@link StringSource} instead.
     */
    public InMemoryContentItem(String id, String content, String mimeType) {
		this(id, new InMemoryBlob(content, mimeType),null);
	}
    /**
     * 
     * @param id
     * @param content
     * @param mimetype
     * @deprecated use {@link InMemoryContentItemFactory#createContentItem(IRI, ContentSource)}
     * with a {@link ByteArraySource} instead.
     */
    public InMemoryContentItem(String id, byte[] content, String mimetype) {
        this(id,new InMemoryBlob(content, mimetype),null);
    }

    /**
     * 
     * @param id
     * @param content
     * @param mimetype
     * @deprecated use {@link InMemoryContentItemFactory#createContentItem(IRI, ContentSource,Graph)}
     * with a {@link ByteArraySource} instead.
     */
    public InMemoryContentItem(String uriString, byte[] content, String mimeType,
            Graph metadata) {
    	this(uriString != null? new IRI(uriString) : null ,
    	        new InMemoryBlob(content, mimeType),
    	        metadata);
    }
    /**
     * 
     * @param id
     * @param content
     * @param mimetype
     * @deprecated use {@link InMemoryContentItemFactory#createContentItem(IRI, ContentSource,Graph)}
     * with a {@link StringSource} instead.
     */
    public InMemoryContentItem(IRI uriRef, String content, String mimeType) {
		this(uriRef, new InMemoryBlob(content, mimeType), null);
	}
    /**
     * 
     * @param id
     * @param content
     * @param mimetype
     * @deprecated use {@link InMemoryContentItemFactory#createContentItem(IRI, ContentSource,Graph)}
     * with a {@link ByteArraySource} instead.
     */
    public InMemoryContentItem(IRI uri, byte[] content, String mimeType, Graph metadata) {
        this(uri, new InMemoryBlob(content, mimeType),metadata);
    }
    protected InMemoryContentItem(String uriString, Blob blob, Graph metadata) {
        this(uriString != null ? new IRI(uriString) : null, blob, metadata);
    }
    protected InMemoryContentItem(IRI uri, Blob blob, Graph metadata) {
        super(uri == null ? ContentItemHelper.makeDefaultUrn(blob): uri,blob,
                metadata == null ? new IndexedGraph() : metadata);
    }

    /**
     * 
     * @param id
     * @param content
     * @param mimetype
     * @deprecated use {@link InMemoryContentItemFactory#createContentItem(ContentSource)}
     * with a {@link StringSource} instead.
     */
	protected static final InMemoryContentItem fromString(String content) {
        return new InMemoryContentItem(content.getBytes(), "text/plain");
    }

}
