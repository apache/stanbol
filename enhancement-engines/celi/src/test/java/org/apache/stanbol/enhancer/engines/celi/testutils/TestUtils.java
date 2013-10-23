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
package org.apache.stanbol.enhancer.engines.celi.testutils;

import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;

import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.apache.clerezza.rdf.jena.serializer.JenaSerializerProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestUtils {
    
    private static final Logger log = LoggerFactory.getLogger(TestUtils.class);
    
    private TestUtils(){}
    
    public static void logEnhancements(ContentItem ci) {
        JenaSerializerProvider serializer = new JenaSerializerProvider();
        ByteArrayOutputStream logOut = new ByteArrayOutputStream();
        serializer.serialize(logOut, ci.getMetadata(), TURTLE);
        log.info("Enhancements: \n{}",new String(logOut.toByteArray(),Charset.forName("UTF-8")));
    }

}
