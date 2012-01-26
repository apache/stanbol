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

package org.apache.stanbol.contenthub.store.solr;

import static org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer.CONTENT_ITEM_URI_PREFIX;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.contenthub.servicesapi.store.solr.SolrContentItem;
import org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemImpl;
import org.apache.stanbol.enhancer.servicesapi.helper.InMemoryBlob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author meric
 * @author anil.sinaci
 * 
 */
public class SolrContentItemImpl extends ContentItemImpl implements SolrContentItem {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(SolrContentItem.class);

    /**
     * TODO: sync access to {@link #constraints} using}
     */
    private final Map<String,List<Object>> constraints;
    private String title;

    public SolrContentItemImpl(byte[] content, String mimetype) {
        this(null, content, mimetype, null, null);
    }

    public SolrContentItemImpl(String id, byte[] content, String mimeType) {
        this(id, content, mimeType, null, null);
    }

    public SolrContentItemImpl(String id,
                               byte[] content,
                               String mimeType,
                               MGraph metadata,
                               Map<String,List<Object>> constraints) {
        this(id, "", content, mimeType, metadata, constraints);
    }

    public SolrContentItemImpl(String id,
                               String title,
                               byte[] content,
                               String mimeType,
                               MGraph metadata,
                               Map<String,List<Object>> constraints) {
        super(id == null ? ContentItemHelper.makeDefaultUri(CONTENT_ITEM_URI_PREFIX, content) : new UriRef(
                ContentItemIDOrganizer.attachBaseURI(id)), new InMemoryBlob(content, mimeType),
                metadata == null ? new SimpleMGraph() : metadata);

        if (metadata == null) {
            metadata = new SimpleMGraph();
        }
        if (constraints == null) {
            constraints = new HashMap<String,List<Object>>();
        }
        this.title = title;
        this.constraints = constraints;
    }

    public Map<String,List<Object>> getConstraints() {
        // TODO: sync access to constraints via #readLock and #writeLocck
        return constraints;
    }

    public String getTitle() {
        if (title != null && !title.trim().equals("")) {
            return title;
        }
        return getUri().getUnicodeString();
    }
}