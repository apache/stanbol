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
package org.apache.stanbol.contenthub.store.file.serializer;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.semanticindex.store.StoreException;

/**
 * This {@link ContentPartDeserializerProvider} implementation supports {@link Graph} parts. As a result, an
 * {@link IndexedGraph} is returned as a specific implementation.
 * 
 * @author suat
 * 
 */
@Component
@Service
public class GraphDeserializerProvider implements ContentPartDeserializerProvider {

    @Reference
    Parser parser;

    @Override
    public Set<Class<?>> getSupportedContentPartTypes() {
        Set<Class<?>> supportedClasses = new HashSet<Class<?>>();
        supportedClasses.add(Graph.class);
        return supportedClasses;
    }

    @Override
    public <T> T deserialize(InputStream inputStream) throws StoreException {
        return deserialize(inputStream, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(InputStream inputStream, String mediaType) throws StoreException {
        mediaType = (mediaType == null || mediaType.trim().equals("")) ? SupportedFormat.RDF_XML : mediaType;
        return (T) new IndexedGraph(parser.parse(inputStream, mediaType));
    }

    @Override
    public <T> T deserialize(InputStream inputStream, String mediaType, Map<String,List<String>> headers) throws StoreException {
        throw new UnsupportedOperationException(
                "This deserializer does not support specifiying header values");
    }
}
