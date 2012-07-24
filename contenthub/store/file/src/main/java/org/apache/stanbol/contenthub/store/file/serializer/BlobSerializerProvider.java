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
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.semanticindex.store.StoreException;
import org.apache.stanbol.enhancer.servicesapi.Blob;

/**
 * {@link ContentPartSerializerProvider} for {@link Blob} objects. It basically copies the stream obtained
 * through the {@link Blob#getStream()} method to the given {@link OutputStream}.
 * 
 * @author suat
 * 
 */
@Component
@Service
public class BlobSerializerProvider implements ContentPartSerializerProvider {

    @Override
    public List<Class<?>> getSupportedContentPartTypes() {
        return Arrays.asList(new Class<?>[] {Blob.class});
    }

    @Override
    public <T> void serialize(OutputStream outputStream, T contentPart) throws StoreException {
        Blob blobPart = (Blob) contentPart;
        try {
            IOUtils.copy(blobPart.getStream(), outputStream);
        } catch (IOException e) {
            throw new StoreException("Failed to write Blob part to OutputStream", e);
        }
    }
}
