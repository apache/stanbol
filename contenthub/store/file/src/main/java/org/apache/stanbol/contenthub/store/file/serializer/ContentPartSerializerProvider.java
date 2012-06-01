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

import java.io.OutputStream;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

/**
 * Serializer for {@link ContentItem} parts. This interface is expected to be used while a {@link ContentItem}
 * is serialized into a persistent storage.
 * 
 * @author suat
 * 
 */
public interface ContentPartSerializerProvider {
    /**
     * Returns the {@link Set} of classes which are supported by a specific instance of
     * {@link ContentPartSerializerProvider}.
     * 
     * @return
     */
    List<Class<?>> getSupportedContentPartTypes();

    /**
     * Serializes the provided content part to the provided {@link OutputStream}.
     * 
     * @param <T>
     *            Generic type representing content part to be serialized
     * @param outputStream
     *            {@link OutputStream} into which the content part will be serialized
     * @param contentPart
     *            Content part to be serialized
     * @throws StoreException
     */
    <T> void serialize(OutputStream outputStream, T contentPart) throws StoreException;
}
