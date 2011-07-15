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
package org.apache.stanbol.ontologymanager.store.adapter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

public class SimpleContentItem implements ContentItem {

    private String id;

    private MGraph metadata;

    private String mimeType;

    private byte[] content;

    public SimpleContentItem(String id, MGraph metadata, String mimeType, byte[] content) {
        this.id = id;
        this.content = content;
        this.metadata = metadata;
        this.mimeType = mimeType;

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
        return new ByteArrayInputStream(content);
    }

    public static ContentItem create(String id, byte[] content, String contentType) {
        return new SimpleContentItem(id, new SimpleMGraph(), contentType, content);

    }

}
