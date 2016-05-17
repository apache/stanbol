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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.ContentReference;
import org.apache.stanbol.enhancer.servicesapi.ContentSink;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;

/**
 * Abstract implementation of the {@link ContentItemFactory} that requires only
 * the three abstract methods <ul>
 * <li> {@link #createBlob(ContentSource)}
 * <li> {@link #createContentItem(String, Blob, Graph)}
 * <li> {@link #createContentItem(IRI, Blob, Graph)}
 * </ul> to be overridden.<p>
 * Implementers should NOTE that {@link #createBlob(ContentSource)} will be
 * called to create the main {@link Blob} instance for a contentItem before
 * the {@link ContentItem} itself is instantiated. If this is a problem, than
 * this abstract super class can not be used.
 * 
 * @author Rupert Westenthaler
 * @since 0.9.1-incubating
 */
@Component(componentAbstract=true,immediate=true,policy=ConfigurationPolicy.OPTIONAL)
@Service(value=ContentItemFactory.class)
public abstract class AbstractContentItemFactory implements ContentItemFactory {

    /**
     * State if {@link LazyDereferencingBlob}s are used for {@link Blob}s
     * created for {@link ContentReference}s
     */
    private final boolean lazyLoadingBlobsEnabled;
    /**
     * Default constructor setting {@link #isLazyDereferenceing()} to <code>false</code>
     */
    protected AbstractContentItemFactory(){
        this(false);
    }
    /**
     * Creates a AbstractContentItemFactory and sets the state for the usage
     * of {@link LazyDereferencingBlob}s.
     * @param enableLazyDereferencingBlobs if {@link Blob}s generated for
     * {@link ContentReference}s should dereference the content during the
     * construction or lazily on the first access to the content.
     */
    protected AbstractContentItemFactory(boolean enableLazyDereferencingBlobs){
        this.lazyLoadingBlobsEnabled = enableLazyDereferencingBlobs;
    }
    
    @Override
    public final ContentItem createContentItem(ContentSource source) throws IOException {
        return createContentItem((IRI)null, source, null);
    }

    @Override
    public final ContentItem createContentItem(String prefix, ContentSource source) throws IOException {
        return createContentItem(prefix,source,null);
    }

    @Override
    public final ContentItem createContentItem(IRI id, ContentSource source) throws IOException {
        return createContentItem(id, source, null);
    }

    @Override
    public final ContentItem createContentItem(ContentReference reference) throws IOException {
        return createContentItem(reference, null);
    }

    @Override
    public final ContentItem createContentItem(ContentReference reference, Graph metadata) throws IOException {
        if(reference == null){
            throw new IllegalArgumentException("The parsed ContentReference MUST NOT be NULL!");
        }
        return createContentItem(new IRI(reference.getReference()),createBlob(reference),metadata);
    }
    @Override
    public final ContentItem createContentItem(String prefix, ContentSource source,Graph metadata) throws IOException {
        if(prefix == null){
            throw new IllegalArgumentException("The parsed prefix MUST NOT be NULL!");
        }
        if(source == null){
            throw new IllegalArgumentException("The parsed ContentSource MUST NOT be NULL!");
        }
        return createContentItem(prefix, createBlob(source), metadata);
    }

    @Override
    public final ContentItem createContentItem(IRI id, ContentSource source, Graph metadata) throws IOException {
        if(source == null){
            throw new IllegalArgumentException("The parsed ContentSource MUST NOT be NULL!");
        }
        return createContentItem(id, createBlob(source), metadata);
    }

    
    
    /**
     * Creates a ContentItem for the parsed parameters
     * @param id the ID or <code>null</code>. Implementors might want to use
     * {@link ContentItemHelper#streamDigest(InputStream, java.io.OutputStream, String)
     * for generating an ID while reading the data from the ContentSource
     * @param blob the Blob
     * @param metadata the metadata or <code>null</code> if none. Implementation 
     * are free to use the passed instance or to generate a new one. However 
     * they MUST ensure that all {@link Triple}s contained by the passed graph 
     * are also added to the {@link ContentItem#getMetadata() metadata} of the 
     * returned ContentItem.
     * @return the created content item
     */
    protected abstract ContentItem createContentItem(IRI id, Blob blob, Graph metadata);
    
    /**
     * Creates a ContentItem for the parsed parameters
     * @param prefix the prefix for the ID of the contentItem. Guaranteed to be
     * NOT <code>null</code>. Implementors might want to use
     * {@link ContentItemHelper#streamDigest(InputStream, java.io.OutputStream, String)
     * for generating an ID while reading the data from the ContentSource
     * @param blob the Blob
     * @param metadata the metadata or <code>null</code> if none. Implementation 
     * are free to use the passed instance or to generate a new one. However 
     * they MUST ensure that all {@link Triple}s contained by the passed graph 
     * are also added to the {@link ContentItem#getMetadata() metadata} of the 
     * returned ContentItem.
     * @return the created content item
     */
    protected abstract ContentItem createContentItem(String prefix, Blob blob, Graph metadata);

    @Override
    public abstract Blob createBlob(ContentSource source) throws IOException;
    
    @Override
    public abstract ContentSink createContentSink(String mediaType) throws IOException;
    /**
     * Getter for the state if the content from {@link ContentReference} used
     * to create {@link ContentItem}s and Blobs is dereferenced immediately or
     * - lazily - on the first usage.
     * @return the lazy dereferencing state.
     */
    public boolean isLazyDereferenceing(){
        return lazyLoadingBlobsEnabled;
    }
    
    @Override
    public final Blob createBlob(ContentReference reference) throws IOException {
        if(reference == null){
            throw new IllegalArgumentException("The passed ContentReference MUST NOT be NULL!");
        }
        if(lazyLoadingBlobsEnabled){
            return new LazyDereferencingBlob(reference);
        } else {
            return createBlob(reference.dereference());
        }
    }

    /**
     * If {@link AbstractContentItemFactory#isLazyDereferenceing}
     * @author westei
     *
     */
    protected class LazyDereferencingBlob implements Blob {
        
        private final ContentReference contentReference;
        private Blob _blob;

        protected LazyDereferencingBlob(ContentReference contentReference){
            this.contentReference = contentReference;
        }

        @Override
        public String getMimeType() {
            return getLazy().getMimeType();
        }

        @Override
        public InputStream getStream() {
            return getLazy().getStream();
        }

        @Override
        public Map<String,String> getParameter() {
            return getLazy().getParameter();
        }

        @Override
        public long getContentLength() {
            if(_blob == null){ //do not dereference for calls on getContentLength
                return -1;
            } else {
                return _blob.getContentLength();
            }
        }
        public Blob getLazy() {
            if(_blob == null){
                try {
                    _blob = createBlob(contentReference.dereference());
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to derefernece content reference '"
                        + contentReference.getReference()+" (Message: "+e.getMessage()+")!",e);
                }
            }
            return _blob;
        }
        
    }
}
