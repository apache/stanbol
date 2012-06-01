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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.contenthub.servicesapi.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.ContentSource;

/**
 * Deserializer for {@link ContentItem} parts. This interface is expected to be used while a content item is
 * deserialized from a persistent storage. Different deserialization methods aim to provide a compatible usage
 * with the {@link ContentSource} interface which is used during the content part creation process.
 * 
 * @author suat
 * 
 */
public interface ContentPartDeserializerProvider {
    /**
     * Returns the {@link Set} of supported classes.
     * 
     * @return
     */
    Set<Class<?>> getSupportedContentPartTypes();

    /**
     * Serializes the content part to be retrieved from the given {@link InputStream}.
     * 
     * @param <T>
     *            Generic type representing content part to be returned
     * @param inputStream
     *            {@link InputStream} providing the raw content part
     * @return deserialized content part
     * @throws StoreException
     */
    <T> T deserialize(InputStream inputStream) throws StoreException;

    /**
     * Serializes the content part to be retrieved from the given {@link InputStream}.
     * 
     * @param <T>
     *            Generic type representing content part to be returned
     * @param inputStream
     *            {@link InputStream} providing the raw content part
     * @param mediaType
     *            Media type of the content part
     * 
     * @return deserialized content part
     * @throws StoreException
     */
    <T> T deserialize(InputStream inputStream, String mediaType) throws StoreException;

    /**
     * Serialized the content part to be retrieved from the given {@link InputStream}.
     * 
     * @param <T>
     *            Generic type representing content part to be returned
     * @param inputStream
     *            {@link InputStream} providing the raw content part
     * @param mediaType
     *            Media type of the content part
     * @param headers
     *            Optional headers for the the content part
     * @return
     * @throws StoreException
     */
    <T> T deserialize(InputStream inputStream, String mediaType, Map<String,List<String>> headers) throws StoreException;
}
