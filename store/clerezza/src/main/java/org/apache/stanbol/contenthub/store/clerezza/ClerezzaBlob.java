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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.apache.clerezza.platform.content.DiscobitsHandler;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.stanbol.enhancer.servicesapi.Blob;

public class ClerezzaBlob implements Blob {

    private final GraphNode idNode;
    private final DiscobitsHandler handler;

    protected ClerezzaBlob(DiscobitsHandler handler, GraphNode idNode){
        this.handler = handler;
        this.idNode = idNode;
    }
    @Override
    public InputStream getStream() {
        return new ByteArrayInputStream(handler.getData((UriRef) idNode.getNode()));
    }
    @Override
    public String getMimeType() {
        return handler.getMediaType((UriRef) idNode.getNode()).toString();
    }
    @Override
    public Map<String,String> getParameter() {
        return Collections.emptyMap();
    }
    @Override
    public long getContentLength() {
        return -1;
    }

}
