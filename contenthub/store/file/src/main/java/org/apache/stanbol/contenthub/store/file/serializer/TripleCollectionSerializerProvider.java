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
import java.util.Arrays;
import java.util.List;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.store.StoreException;

/**
 * {@link ContentPartSerializerProvider} for {@link TripleCollection} objects. It serializes
 * {@link TripleCollection} using the {@link Serializer} in {@link SupportedFormat#RDF_XML} format.
 * 
 * @author suat
 * 
 */
@Component(immediate = true)
@Service
public class TripleCollectionSerializerProvider implements ContentPartSerializerProvider {

    @Reference
    private Serializer serializer;

    @Override
    public List<Class<?>> getSupportedContentPartTypes() {
        return Arrays.asList(new Class<?>[] {TripleCollection.class});
    }

    @Override
    public <T> void serialize(OutputStream outputStream, T contentPart) throws StoreException {
        try {
            serializer.serialize(outputStream, (TripleCollection) contentPart, SupportedFormat.RDF_XML);
        } catch (UnsupportedFormatException e) {
            throw new StoreException("Failed to serialized the content part", e);
        }
    }
}
