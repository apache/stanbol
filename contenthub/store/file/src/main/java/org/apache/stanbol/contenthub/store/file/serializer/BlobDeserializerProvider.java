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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.impl.StreamSource;

/**
 * {@link ContentPartDeserializerProvider} for {@link Blob} objects. First a {@link StreamSource} is created
 * from the given {@link InputStream} and this source is fed to the
 * {@link ContentItemFactory#createBlob(org.apache.stanbol.enhancer.servicesapi.ContentSource)}.
 * 
 * @author suat
 * 
 */
@Component
@Service
public class BlobDeserializerProvider implements ContentPartDeserializerProvider {

    @Reference
    ContentItemFactory contentItemFactory;

    @Override
    public Set<Class<?>> getSupportedContentPartTypes() {
        Set<Class<?>> supportedClasses = new HashSet<Class<?>>();
        supportedClasses.add(Blob.class);
        return supportedClasses;
    }

    @Override
    public <T> T deserialize(InputStream inputStream) throws StoreException {
        return deserialize(inputStream, null, null);
    }

    @Override
    public <T> T deserialize(InputStream inputStream, String mediaType) throws StoreException {
        return deserialize(inputStream, mediaType, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(InputStream inputStream, String mediaType, Map<String,List<String>> headers) throws StoreException {
        Blob blob = null;
        try {
            blob = contentItemFactory.createBlob(new StreamSource(inputStream, mediaType, headers));
        } catch (IOException e) {
            throw new StoreException("Failed to serialize Blob part from InputStream", e);
        }
        return (T) blob;
    }

}
