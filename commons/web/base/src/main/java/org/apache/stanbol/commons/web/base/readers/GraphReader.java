/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stanbol.commons.web.base.readers;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.felix.scr.annotations.Reference;

/**
 * JAX-RS provider that parses RDF by using the OSGi parsing service
 */
//TODO make it a service/component
@Provider
public class GraphReader implements MessageBodyReader<Graph> {

    // TODO: make the clerezza Parser service able to describe the supported media types instead of hard
    // coding runtime assumptions as static field.
    public static final Set<String> SUPPORTED_MEDIA_TYPES;
    static {
        Set<String> types = new HashSet<String>();
        types.add(N3);
        types.add(N_TRIPLE);
        types.add(RDF_XML);
        types.add(TURTLE);
        types.add(X_TURTLE);
        types.add(RDF_JSON);
        types.add(APPLICATION_JSON);
        SUPPORTED_MEDIA_TYPES = Collections.unmodifiableSet(types);
    }


    @Reference
    private Parser parser;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return SUPPORTED_MEDIA_TYPES.contains(mediaType.toString()) && type.isAssignableFrom(Graph.class);
    }

    @Override
    public Graph readFrom(Class<Graph> type,
                          Type genericType,
                          Annotation[] annotations,
                          MediaType mediaType,
                          MultivaluedMap<String,String> httpHeaders,
                          InputStream entityStream) throws IOException, WebApplicationException {
        return parser.parse(entityStream, mediaType.toString());
    }
}
