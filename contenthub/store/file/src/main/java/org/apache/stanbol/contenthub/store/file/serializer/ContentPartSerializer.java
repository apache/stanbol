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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;

/**
 * This is the manager class delegating the content part serialize requests to the suitable
 * {@link ContentPartSerializerProvider}s instances that are already registered in the OSGi environment.
 * 
 * @author suat
 * 
 */
@Component
@Service(value = ContentPartSerializer.class)
public class ContentPartSerializer {
    private Map<Class<?>,ContentPartSerializerProvider> serializerMap = new HashMap<Class<?>,ContentPartSerializerProvider>();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = ContentPartSerializerProvider.class, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, bind = "bindContentPartSerializerProvider", unbind = "unbindContentPartSerializerProvider")
    private List<ContentPartSerializerProvider> serializerList = new ArrayList<ContentPartSerializerProvider>();

    /**
     * Serializes the content part to the given {@link OutputStream} using a
     * {@link ContentPartSerializerProvider} which will be obtained by the class of the content part. The
     * first serializer compatible with the class of the content part is used. The types of supported by a
     * serializer is compared with the class of the given content part through the
     * {@link Class#isAssignableFrom(Class)} method. This means that a serializer is compatible with the given
     * content part if one of the supported types by serializer either is a superclass or superinterface of
     * the class of the content part or same with the class of the content part.
     * 
     * @param <T>
     *            Generic type representing content part to be serialized
     * @param os
     *            {@link OutputStream} to which the content part will be serialized
     * @param contentPart
     *            Content part to be serialized
     * @throws StoreException
     */
    public <T> void serializeContentPart(OutputStream os, T contentPart) throws StoreException {
        ContentPartSerializerProvider serializer = null;
        synchronized (serializerList) {
            for (Entry<Class<?>,ContentPartSerializerProvider> e : serializerMap.entrySet()) {
                if (e.getKey().isAssignableFrom(contentPart.getClass())) {
                    serializer = e.getValue();
                    break;
                }
            }
        }
        if (serializer == null) {
            throw new StoreException(String.format(
                "Failed to obtain serializer for the content part having type: %s", contentPart.getClass()
                        .getName()));
        }
        serializer.serialize(os, contentPart);
    }

    /**
     * Returns the set of classes that are serializable by the registered
     * {@link ContentPartSerializerProvider}s.
     * 
     * @return {@link Set} of serializable classes.
     */
    public Set<Class<?>> getSerializableTypes() {
        return serializerMap.keySet();
    }

    protected void bindContentPartSerializerProvider(ContentPartSerializerProvider serializerProvider) {
        synchronized (serializerList) {
            serializerList.add(serializerProvider);
            refreshSerializerMap();
        }
    }

    protected void unbindContentPartSerializerProvider(ContentPartSerializerProvider serializerProvider) {
        synchronized (serializerList) {
            serializerList.remove(serializerProvider);
            refreshSerializerMap();
        }
    }

    private <T> void refreshSerializerMap() {
        Map<Class<?>,ContentPartSerializerProvider> newSerializerMap = new HashMap<Class<?>,ContentPartSerializerProvider>();
        for (ContentPartSerializerProvider serializer : serializerList) {
            List<Class<?>> classes = serializer.getSupportedContentPartTypes();
            for (Class<?> c : classes) {
                newSerializerMap.put(c, serializer);
            }
        }
        serializerMap = newSerializerMap;
    }
}