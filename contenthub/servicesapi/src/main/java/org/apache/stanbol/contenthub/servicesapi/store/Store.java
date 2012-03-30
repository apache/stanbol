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
package org.apache.stanbol.contenthub.servicesapi.store;

import java.io.InputStream;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;

/**
 * Store and retrieve ContentItem instances.
 * 
 * Incomplete CRUD for now, we don't need it for our initial use cases.
 */
public interface Store {

//    /**
//     * Creates a {@link ContentItem} item based on supplied data, using an implementation that suits this
//     * store.
//     * <p>
//     * Calling this method creates an empty data transfer object in memory suitable for later saving using the
//     * {@link Store#put(ContentItem)} method. The Store state is unchanged by the call to the
//     * {@link #create(String, byte[], String)} method.
//     * 
//     * @param id
//     *            The value to use {@link ContentItem#getId}. If <code>null</code> is parsed as id, an id need
//     *            to be computed based on the parsed content ( e.g. calculating the stream digest (see also
//     *            {@link ContentItemHelper#streamDigest(java.io.InputStream, java.io.OutputStream, String)})
//     * @param content
//     *            the binary content
//     * @param contentType
//     *            The Mime-Type of the binary data
//     * @return the {@link ContentItem} that was created
//     */
//    ContentItem create(String id, byte[] content, String contentType) throws StoreException;
//
//    ContentItem create(String id, InputStream is, String contentType) throws StoreException;

    ContentItem remove(UriRef id) throws StoreException;

    /**
     * Store supplied {@link ContentItem} and return its id, which is assigned by the store if not defined
     * yet.
     * 
     * If the {@link ContentItem} already exists, it is overwritten.
     */
    UriRef put(ContentItem ci) throws StoreException;

    /** Get a {@link ContentItem} by id, null if non-existing */
    ContentItem get(UriRef id) throws StoreException;
    /**
     * Requests the next <code>batchSize</code> changes starting from
     * <code>revision</code>. If there are no more revisions that an
     * {@link ChangeSet} with an empty {@link ChangeSet#changed()} set.
     * @param revision the starting revision number for the returned {@link ChangeSet}
     * @return the {@link ChangeSet} with a maximum of <code>batchSize</code> changes
     * @throws StoreException on any error while accessing the store.
     */
    ChangeSet changes(long revision, int batchSize) throws StoreException;
}
