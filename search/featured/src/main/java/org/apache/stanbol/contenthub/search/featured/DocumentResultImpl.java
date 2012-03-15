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
package org.apache.stanbol.contenthub.search.featured;

import org.apache.stanbol.contenthub.servicesapi.search.featured.DocumentResult;
import org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer;

public class DocumentResultImpl implements DocumentResult {

    private String id;
    private String dereferencableURI;
    private String mimetype;
    private long enhancementCount;
    private String title;

    public DocumentResultImpl(String uri,
                                 String mimeType,
                                 long enhancementCount,
                                 String title) {
        this.id = ContentItemIDOrganizer.detachBaseURI(uri);
        this.mimetype = mimeType;
        this.title = (title == null || title.trim().equals("") ? id : title);
        this.enhancementCount = enhancementCount;
    }

    public DocumentResultImpl(String uri,
                                 String dereferencableURI,
                                 String mimeType,
                                 long enhancementCount,
                                 String title) {
        this.id = ContentItemIDOrganizer.detachBaseURI(uri);
        this.dereferencableURI = dereferencableURI;
        this.mimetype = mimeType;
        this.title = (title == null || title.trim().equals("") ? id : title);
        this.enhancementCount = enhancementCount;
    }
    
    @Override
    public String getLocalId() {
        return this.id;
    }
    
    @Override
    public String getDereferencableURI() {
        return this.dereferencableURI;
    }

    @Override
    public String getMimetype() {
        return this.mimetype;
    }

    @Override
    public long getEnhancementCount() {
        return this.enhancementCount;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDereferencableURI(String dereferencableURI) {
        this.dereferencableURI = dereferencableURI;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setEnhancements(long enhancementCount) {
        this.enhancementCount = enhancementCount;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
}
