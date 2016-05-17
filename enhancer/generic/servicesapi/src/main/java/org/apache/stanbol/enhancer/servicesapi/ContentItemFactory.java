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
import java.io.OutputStream;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;

/**
 * OSGI service to be used to create {@link ContentItem}s and Blobs.
 * 
 * @since 0.9.1-incubating
 */
public interface ContentItemFactory {

    /**
     * Creates a new ContentItem for the passed {@link ContentSource} and
     * generates as unique ID from the passed content.
     * Implementors might want to use
     * {@link ContentItemHelper#streamDigest(InputStream, java.io.OutputStream, String)
     * for generating an ID while reading the data from the ContentSource.<p>
     * The content provided by the {@link ContentSource} is consumed by the
     * this method and stored as {@link Blob} with the returned {@link ContentItem}.
     * In addition implementors need to ensure that this {@link Blob} is also
     * used by the {@link ContentItem#getBlob()}, {@link ContentItem#getStream()},
     * and {@link ContentItem#getMimeType()} methods.<p>
     * Callers can safely close any resource related to the parsed {@link ContentSource}
     * method after this method returns.
     * @param source The content source
     * @return the {@link ContentItem} with a generated id and the passed
     * content as content-part of type {@link Blob} at index <code>0</code>
     * @throws IllegalArgumentException if <code>null</code> is passed as content
     * source or the passed content source is already consumed.
     * @throws IOException on any error while reading the content from the 
     * content source.
     */
    ContentItem createContentItem(ContentSource source) throws IOException;
    /**
     * Creates a new ContentItem for the passed content source and an
     * ID relative to the passed prefix.
     * @param prefix the URI prefix used generate the URI of the content item.
     * Note the only a generated ID will be added to the passed prefix. So passed
     * values should typically end with an separator char (e.g. '/', '#', ':').
     * Implementors might want to use
     * {@link org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper#streamDigest(InputStream, java.io.OutputStream, String)
     * for generating an ID while reading the data from the ContentSource.<p>
     * The content provided by the {@link ContentSource} is consumed by the
     * this method and stored as {@link Blob} with the returned {@link ContentItem}.
     * In addition implementors need to ensure that this {@link Blob} is also
     * used by the {@link ContentItem#getBlob()}, {@link ContentItem#getStream()},
     * and {@link ContentItem#getMimeType()} methods.<p>
     * Callers can safely close any resource related to the parsed {@link ContentSource}
     * method after this method returns.
     * @param source The content source
     * @return the {@link ContentItem} with a generated id and the passed
     * content as content-part of type {@link Blob} at index <code>0</code>
     * @throws IllegalArgumentException if <code>null</code> is passed as content
     * source, the content source is already consumed or the passed prefix is
     * <code>null</code>
     * @throws IOException on any error while reading the content from the 
     * content source.
     */
    ContentItem createContentItem(String prefix, ContentSource source) throws IOException;
    /**
     * Creates a new ContentItem for the passed id and content source.
     * @param id the id for the ContentItem or <code>null</code> to generate an id.
     * If <code>null</code> is passed as ID, implementors might want to use
     * {@link ContentItemHelper#streamDigest(InputStream, java.io.OutputStream, String)
     * for generating an ID while reading the data from the ContentSource.<p>
     * The content provided by the {@link ContentSource} is consumed by the
     * this method and stored as {@link Blob} with the returned {@link ContentItem}.
     * In addition implementors need to ensure that this {@link Blob} is also
     * used by the {@link ContentItem#getBlob()}, {@link ContentItem#getStream()},
     * and {@link ContentItem#getMimeType()} methods.<p>
     * Callers can safely close any resource related to the parsed {@link ContentSource}
     * method after this method returns.
     * @param source The content source
     * @return the {@link ContentItem} with a passed/generated id and the passed
     * content as content-part of type {@link Blob} at index <code>0</code>
     * @throws IllegalArgumentException if <code>null</code> is passed as content
     * source, the content source is already consumed or the
     * passed id is not <code>null</code> but empty.
     * @throws IOException on any error while reading the content from the 
     * content source.
     */
    ContentItem createContentItem(IRI id, ContentSource source) throws IOException;
    /**
     * Creates a new ContentItem for the passed id and content source.
     * @param prefix the URI prefix used generate the URI of the content item.
     * Note the only a generated ID will be added to the passed prefix. So passed
     * values should typically end with an separator char (e.g. '/', '#', ':').
     * Implementors might want to use
     * {@link ContentItemHelper#streamDigest(InputStream, java.io.OutputStream, String)
     * for generating an ID while reading the data from the ContentSource.<p>
     * The content provided by the {@link ContentSource} is consumed by the
     * this method and stored as {@link Blob} with the returned {@link ContentItem}.
     * In addition implementors need to ensure that this {@link Blob} is also
     * used by the {@link ContentItem#getBlob()}, {@link ContentItem#getStream()},
     * and {@link ContentItem#getMimeType()} methods.<p>
     * Callers can safely close any resource related to the parsed {@link ContentSource}
     * method after this method returns.
     * @param source The content source
     * @param metadata an {@link Graph} with the metadata or <code>null</code>
     * if none. Implementation are free to use the passed instance or to generate 
     * a new one. However they MUST ensure that all {@link Triple}s contained by 
     * the passed graph are also added to the {@link ContentItem#getMetadata() 
     * metadata} of the returned ContentItem.
     * @return the {@link ContentItem} with a passed/generated id and the passed
     * content as content-part of type {@link Blob} at index <code>0</code>
     * @throws IllegalArgumentException if <code>null</code> is passed as content
     * source, the content source is already consumed or the
     * passed prefix is <code>null</code>.
     * @throws IOException on any error while reading the content from the 
     * content source.
     */
    ContentItem createContentItem(String prefix, ContentSource source, Graph metadata) throws IOException;
    /**
     * Creates a new ContentItem for the passed id and content source.
     * @param id the id for the ContentItem or <code>null</code> to generate an id.
     * If <code>null</code> is passed as ID, implementors might want to use
     * {@link ContentItemHelper#streamDigest(InputStream, java.io.OutputStream, String)
     * for generating an ID while reading the data from the ContentSource.<p>
     * The content provided by the {@link ContentSource} is consumed by the
     * this method and stored as {@link Blob} with the returned {@link ContentItem}.
     * In addition implementors need to ensure that this {@link Blob} is also
     * used by the {@link ContentItem#getBlob()}, {@link ContentItem#getStream()},
     * and {@link ContentItem#getMimeType()} methods.<p>
     * Callers can safely close any resource related to the parsed {@link ContentSource}
     * method after this method returns.
     * @param source The content source
     * @param metadata an {@link Graph} with the metadata or <code>null</code>
     * if none. Implementation are free to use the passed instance or to generate 
     * a new one. However they MUST ensure that all {@link Triple}s contained by 
     * the passed graph are also added to the {@link ContentItem#getMetadata() 
     * metadata} of the returned ContentItem.
     * @return the {@link ContentItem} with a passed/generated id and the passed
     * content as content-part of type {@link Blob} at index <code>0</code>
     * @throws IllegalArgumentException if <code>null</code> is passed as content
     * source, the content source is already consumed or the
     * passed id is not <code>null</code> but empty.
     * @throws IOException on any error while reading the content from the 
     * content source.
     */
    ContentItem createContentItem(IRI id, ContentSource source, Graph metadata) throws IOException;
    /**
     * Creates a new ContentItem for the passed {@link ContentReference}. The
     * {@link ContentReference#getReference()} is used as ID for the content
     * item. Implementations might choose to {@link ContentReference#dereference()
     * dereference}
     * the reference at creation if needed.
     * @param reference the reference to the content
     * @return the {@link ContentItem} with the {@link ContentReference#getReference()}
     * as ID.
     * @throws IOException if the implementation {@link ContentReference#dereference()
     * dereferences} the {@link ContentReference} during creation and this action
     * fails.
     * @throws IllegalArgumentException if the passed {@link ContentReference}
     * is <code>null</code>.
     */
    ContentItem createContentItem(ContentReference reference) throws IOException;
    /**
     * Creates a new ContentItem for the passed {@link ContentReference}. The
     * {@link ContentReference#getReference()} is used as ID for the content
     * item. Implementations might choose to {@link ContentReference#dereference()
     * dereference}
     * the reference at creation if needed.
     * @param reference the reference to the content
     * @param metadata an {@link Graph} with the metadata or <code>null</code>
     * if none. Implementation are free to use the passed instance or to generate 
     * a new one. However they MUST ensure that all {@link Triple}s contained by 
     * the passed graph are also added to the {@link ContentItem#getMetadata() 
     * metadata} of the returned ContentItem.
     * @return the {@link ContentItem} with the {@link ContentReference#getReference()}
     * as ID.
     * @throws IOException if the implementation {@link ContentReference#dereference()
     * dereferences} the {@link ContentReference} during creation and this action
     * fails.
     * @throws IllegalArgumentException if the passed {@link ContentReference}
     * is <code>null</code>.
     */
    ContentItem createContentItem(ContentReference reference, Graph metadata) throws IOException;
    /**
     * Creates a new Blob based on the passed {@link ContentSource}<p>
     * The content provided by the {@link ContentSource} is consumed by the
     * this method and stored in the returned {@link Blob}. Callers can safely 
     * close any resource related to the parsed {@link ContentSource}
     * method after this method returns.
     * @param source The source 
     * @return the Blob
     * @throws IllegalArgumentException of the passed source is <code>null</code>
     * @throws IllegalStateException if the passed source is already consumed
     * @throws IOException on any error while reading the content from the source.
     */
    Blob createBlob(ContentSource source) throws IOException;
    /**
     * Creates a new Blob based on the passed {@link ContentReference}. If the
     * content reference is {@link ContentReference#dereference() dereferenced}
     * during the construction or at an later point in time is implementation
     * dependent.
     * @param reference The reference to the content 
     * @return the Blob
     * @throws IllegalArgumentException of the passed reference is <code>null</code>
     * @throws IOException on any error while dereferencing the passed reference.
     */
    Blob createBlob(ContentReference reference) throws IOException;
    
    /**
     * Creates a {@link ContentSink} that allows to "stream" content to an
     * newly created {@link Blob}. This is intended to be used by
     * {@link EnhancementEngine}s that transform the parsed content and want
     * to store "stream" the transformation result to a new {@link Blob} that
     * can be later added to the {@link ContentItem}. <p>
     * <b>IMPORTANT NOTE:</b> Do not parse the {@link Blob} of a {@link ContentSink}
     * to any other components until all the data are written to the 
     * {@link OutputStream} (see {@link ContentSink} for details).
     * @param mediaType the mediaType for the created blob. "application/octet-stream" is
     * used as default if <code>null</code>. For textual content the charset should be
     * added as parameter (e.g. "text/plain; charset=UTF-8"). If no charset is
     * present Stanbol will assume that the text is encoded using <code>UTF-8</code>.
     * @return The {@link ContentSink} providing both the {@link Blob} and an
     * {@link OutputStream} allowing to write the data for this {@link Blob}.
     * @throws IOException On any error while creating the Blob.
     */
    ContentSink createContentSink(String mediaType) throws IOException;

}
