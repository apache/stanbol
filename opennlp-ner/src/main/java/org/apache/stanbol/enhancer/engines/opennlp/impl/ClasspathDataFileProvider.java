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
package org.apache.stanbol.enhancer.engines.opennlp.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;

/** DataFileProvider that looks in our class resources */
public class ClasspathDataFileProvider implements DataFileProvider {

    @Override
    public InputStream getInputStream(String bundleSymbolicName,
            String filename, Map<String, String> comments) 
    throws IOException {
        // load default OpenNLP models from classpath (embedded in the defaultdata bundle)
        final String resourcePath = "org/apache/stanbol/defaultdata/opennlp/" + filename;
        final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Resource not found in my classpath: " + resourcePath);
        }
        return in;
    }
}
