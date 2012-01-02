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
package org.apache.stanbol.contenthub.store.clerezza;

import javax.ws.rs.core.MediaType;

import org.apache.clerezza.platform.content.DiscobitsHandler;
import org.apache.clerezza.platform.graphprovider.content.ContentGraphProvider;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.NoSuchEntityException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.store.Store;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;


/**
 * test
 *
 * @author andreas
 */
@Component
@Service(Store.class)
public class ClerezzaStore implements Store {

    @Reference
    DiscobitsHandler handler;

    @Reference
    ContentGraphProvider cgProvider;

    @Reference
    TcManager tcManager;

    public ContentItem create(String id, byte[] content, String contentType) {

        // TODO: the semantics of this implementation are wrong: the creation of
        // a new content item should not touch the persistence backends. The write
        // operation to the backend should only be performed when calling the
        // {@link put} method.

        UriRef uriRef = new UriRef(id);
        MGraph metadataGraph;
        try {
            metadataGraph = tcManager.createMGraph(uriRef);
        } catch (EntityAlreadyExistsException ex) {
            return null;
        }
        handler.put(new UriRef(id), MediaType.valueOf(contentType), content);
        ContentItem contentItem = new ClerezzaContentItem(new GraphNode(uriRef,
                cgProvider.getContentGraph()), new SimpleMGraph(metadataGraph), handler);
        return contentItem;
    }

    public String put(ContentItem ci) {
        MGraph metadataGraph = tcManager.getMGraph(new UriRef(ci.getUri().getUnicodeString()));
        metadataGraph.clear();
        metadataGraph.addAll(ci.getMetadata());
        return ci.getUri().getUnicodeString();
    }

    public ContentItem get(String id) {
        UriRef uriRef = new UriRef(id);
        MGraph metadataGraph;
        try {
            metadataGraph = tcManager.getMGraph(uriRef);
        } catch (NoSuchEntityException ex) {
            throw new IllegalArgumentException("Is not a content item");
        }
        ContentItem contentItem = new ClerezzaContentItem(new GraphNode(uriRef,
                cgProvider.getContentGraph()), metadataGraph, handler);
        return contentItem;
    }

    public MGraph getEnhancementGraph() {
        // TODO: implement me: this should return an aggregate graph with all
        // the triples of all the content item of this store
        return new SimpleMGraph();
    }
}
