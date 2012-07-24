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
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the manager class delegating the content part deserialize requests to the suitable
 * {@link ContentPartDeserializerProvider}s instances that are already registered in the OSGi environment.
 * 
 * @author suat
 * 
 */
@Component
@Service(value = ContentPartDeserializer.class)
public class ContentPartDeserializer {
    private static final Logger log = LoggerFactory.getLogger(ContentPartDeserializer.class);

    private Map<Class<?>,ContentPartDeserializerProvider> deserializerMap = new HashMap<Class<?>,ContentPartDeserializerProvider>();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = ContentPartDeserializerProvider.class, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, bind = "bindContentPartDeserializerProvider", unbind = "unbindContentPartDeserializerProvider")
    private List<ContentPartDeserializerProvider> deserializerList = new ArrayList<ContentPartDeserializerProvider>();

    /**
     * Deserializes the content part which will be read from the given {@link InputStream} using a
     * {@link ContentPartDeserializerProvider} which will be obtained by the given {@link Class}. The first
     * deserializer compatible with the given class is used. The types supported by a deserializer are
     * compared with the given class parameter through the {@link Class#isAssignableFrom(Class)} method. This
     * means that a deserializer is compatible with the given class if one of the supported types by
     * deserializer either is a superclass or superinterface of the given class or same with the given class.
     * 
     * @param <T>
     *            Generic type representing the content part to be returned
     * @param is
     *            {@link InputStream} from which the content part data will be read
     * @param klass
     *            Type for which a deserializer instance will be obtained.
     * @return the deserialized content part
     * @throws StoreException
     */
    public <T> T deserializeContentPart(InputStream is, Class<?> klass) throws StoreException {
        return deserializeContentPart(is, klass, null);
    }

    /**
     * Deserializes the content part which will be read from the given {@link InputStream} using a
     * {@link ContentPartDeserializerProvider} which will be obtained by the given {@link Class}. The first
     * deserializer compatible with the given class is used. The types supported by a deserializer are
     * compared with the given class parameter through the {@link Class#isAssignableFrom(Class)} method. This
     * means that a deserializer is compatible with the given class if one of the supported types by
     * deserializer either is a superclass or superinterface of the given class or same with the given class.
     * 
     * @param <T>
     *            Generic type representing the content part to be returned
     * @param is
     *            {@link InputStream} from which the content part data will be read
     * @param klass
     *            Type for which a deserializer instance will be obtained.
     * @param mimeType
     *            Mime type of the content part to be serialized
     * @return the deserialized content part
     * @throws StoreException
     */
    public <T> T deserializeContentPart(InputStream is, Class<?> klass, String mimeType) throws StoreException {
        ContentPartDeserializerProvider deserializer = null;
        synchronized (deserializerList) {
            for (Entry<Class<?>,ContentPartDeserializerProvider> e : deserializerMap.entrySet()) {
                if (e.getKey().isAssignableFrom(klass)) {
                    deserializer = e.getValue();
                    break;
                }
            }
        }
        if (deserializer == null) {
            throw new StoreException(String.format(
                "Failed to obtain serializer for the content part having type: %s", klass.getName()));
        }
        return deserializer.deserialize(is, mimeType);
    }

    /**
     * Deserializes the content part which will be read from the given {@link InputStream} using a
     * {@link ContentPartDeserializerProvider} which will be obtained by the given class name. In the first
     * step, to be able to get a dedicated deserializer for the given <code>className</code>, this method
     * checks a suitable deserializers using this name. This is done by comparing the name parameter with the
     * name of the supported types of registered deserializers. If this attempt is unsuccessful, a serializer
     * is tried to be obtained through the {@link #deserializeContentPart(InputStream, Class)} method after
     * getting a {@link Class} from the given class name by {@link Class#forName(String)}.
     * 
     * @param <T>
     *            Generic type of representing content part to be returned
     * @param is
     *            {@link InputStream} from which the content part data will be read
     * @param className
     *            Name of the class for which a deserializer instance will be obtained.
     * @return the deserialized content part
     * @throws StoreException
     */
    public <T> T deserializeContentPart(InputStream is, String className) throws StoreException {
        return deserializeContentPart(is, className, null);
    }

    /**
     * Deserializes the content part which will be read from the given {@link InputStream} using a
     * {@link ContentPartDeserializerProvider} which will be obtained by the given class name. In the first
     * step, to be able to get a dedicated deserializer for the given <code>className</code>, this method
     * checks a suitable deserializers using this name. This is done by comparing the name parameter with the
     * name of the supported types of registered deserializers. If this attempt is unsuccessful, a serializer
     * is tried to be obtained through the {@link #deserializeContentPart(InputStream, Class)} method after
     * getting a {@link Class} from the given class name by {@link Class#forName(String)}.
     * 
     * @param <T>
     *            Generic type of representing content part to be returned
     * @param is
     *            {@link InputStream} from which the content part data will be read
     * @param className
     *            Name of the class for which a deserializer instance will be obtained.
     * @param mimeType
     *            Mime type of the content part to be serialized
     * @return the deserialized content part
     * @throws StoreException
     */
    public <T> T deserializeContentPart(InputStream is, String className, String mimeType) throws StoreException {
        ContentPartDeserializerProvider deserializer = null;
        synchronized (deserializerList) {
            for (Entry<Class<?>,ContentPartDeserializerProvider> e : deserializerMap.entrySet()) {
                if (e.getKey().getName().equals(className)) {
                    deserializer = e.getValue();
                    break;
                }
            }
        }
        if (deserializer == null) {
            log.info("No deserializer supporting directly the class: {}", className);
            try {
                Class<?> klass = Class.forName(className);
                return deserializeContentPart(is, klass, mimeType);
            } catch (ClassNotFoundException e) {
                throw new StoreException(String.format("Failed to load class: %s", className));
            }
        }
        return deserializer.deserialize(is);
    }

    /**
     * Returns the set of classes that are deserializable by the registered
     * {@link ContentPartDeserializerProvider}s.
     * 
     * @return {@link Set} of deserializable classes.
     */
    public Set<Class<?>> getSerializableTypes() {
        return deserializerMap.keySet();
    }

    protected void bindContentPartDeserializerProvider(ContentPartDeserializerProvider deserializerProvider) {
        synchronized (deserializerList) {
            deserializerList.add(deserializerProvider);
            refreshDeserializerMap();
        }
    }

    protected void unbindContentPartDeserializerProvider(ContentPartDeserializerProvider deserializerProvider) {
        synchronized (deserializerList) {
            deserializerList.remove(deserializerProvider);
            refreshDeserializerMap();
        }
    }

    private void refreshDeserializerMap() {
        Map<Class<?>,ContentPartDeserializerProvider> newSerializerMap = new HashMap<Class<?>,ContentPartDeserializerProvider>();
        for (ContentPartDeserializerProvider deserializer : deserializerList) {
            Set<Class<?>> classes = deserializer.getSupportedContentPartTypes();
            for (Class<?> c : classes) {
                newSerializerMap.put(c, deserializer);
            }
        }
        deserializerMap = newSerializerMap;
    }
}
