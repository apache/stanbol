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
package org.apache.stanbol.commons.web.base.writers;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO check if clerezza rdf.jaxrs prvoder fits the purpose?
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Provider
// @Produces({TEXT_PLAIN, N3, N_TRIPLE, RDF_XML, TURTLE, X_TURTLE, RDF_JSON, APPLICATION_JSON})
public class GraphWriter implements MessageBodyWriter<Graph> {

    /**
     * The media type for JSON-LD (<code>application/ld+json</code>)
     */
    private static String APPLICATION_LD_JSON = "application/ld+json";
    
    private static final Charset UTF8 = Charset.forName("UTF-8");
    
    private final Logger log = LoggerFactory.getLogger(GraphWriter.class);
    public static final Set<String> supportedMediaTypes;
    static {
        Set<String> types = new HashSet<String>();
        types.add(TEXT_PLAIN);
        types.add(N3);
        types.add(N_TRIPLE);
        types.add(RDF_XML);
        types.add(TURTLE);
        types.add(X_TURTLE);
        types.add(RDF_JSON);
        types.add(APPLICATION_JSON);
        types.add(APPLICATION_OCTET_STREAM);
        types.add(APPLICATION_LD_JSON);
        supportedMediaTypes = Collections.unmodifiableSet(types);
    }

    public static final String ENCODING = "UTF-8";

    @Reference
    private Serializer serializer;

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        String mediaTypeString = mediaType.getType() + '/' + mediaType.getSubtype();
        return Graph.class.isAssignableFrom(type) && supportedMediaTypes.contains(mediaTypeString);
    }

    public long getSize(Graph t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType) {
        return -1;
    }

    public void writeTo(Graph t,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String,Object> httpHeaders,
                        OutputStream entityStream) throws IOException, WebApplicationException {

        String mediaTypeString = mediaType.getType() + '/' + mediaType.getSubtype();
        if (mediaType.isWildcardType() || TEXT_PLAIN.equals(mediaTypeString)
            || APPLICATION_OCTET_STREAM.equals(mediaTypeString)) {
            mediaTypeString = APPLICATION_LD_JSON;
        }
        httpHeaders.putSingle(CONTENT_TYPE, mediaTypeString + ";charset=" + UTF8.name());
        
        long start = System.currentTimeMillis();
        serializer.serialize(entityStream, t, mediaTypeString);
        log.debug("Serialized {} in {}ms", t.size(), System.currentTimeMillis() - start);
    }
}
