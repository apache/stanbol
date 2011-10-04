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

package org.apache.stanbol.contenthub.core.store;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.contenthub.core.utils.ContentItemIDOrganizer;
import org.apache.stanbol.contenthub.servicesapi.store.SolrContentItem;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author meric
 * 
 */
public class SolrContentItemImpl implements SolrContentItem {

    private static final Logger log = LoggerFactory.getLogger(SolrContentItem.class);

    private final MGraph metadata;
    private final String id;
    private final String mimeType;
    private final byte[] data;
    private final Map<String,List<Object>> constraints;

    public SolrContentItemImpl(String id) {
        this(id, null, null, null, null);
    }

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
        if (id == null) {
            id = ContentItemHelper.makeDefaultUri(ContentItemIDOrganizer.CONTENT_ITEM_URI_PREFIX, content)
                    .getUnicodeString();
        } else {
            id = ContentItemIDOrganizer.attachBaseURI(id);
        }

        if (metadata == null) {
            metadata = new SimpleMGraph();
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        } else {
            // Keep only first part of content-types like text/plain ; charset=UTF-8
            try {
                mimeType = mimeType.split(";")[0].trim();
            } catch (Exception ex) {
                log.error("mimeType of SolrContentItem cannot be parsed", ex);
            }
        }
        if (content == null) {
            content = new byte[0];
        }

        this.id = id;
        this.data = content;
        this.mimeType = mimeType;
        this.metadata = metadata;
        this.constraints = constraints;
    }

    public String getId() {
        return id;
    }

    public MGraph getMetadata() {
        return metadata;
    }

    public String getMimeType() {
        return mimeType;
    }

    public InputStream getStream() {
        return new ByteArrayInputStream(data);
    }

    public Map<String,List<Object>> getConstraints() {
        return constraints;
    }
}