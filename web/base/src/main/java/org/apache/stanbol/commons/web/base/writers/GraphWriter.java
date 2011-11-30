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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Provider
//@Produces({TEXT_PLAIN, N3, N_TRIPLE, RDF_XML, TURTLE, X_TURTLE, RDF_JSON, APPLICATION_JSON})
public class GraphWriter implements MessageBodyWriter<TripleCollection> {
    
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
        supportedMediaTypes = Collections.unmodifiableSet(types);
    }

    public static final String ENCODING = "UTF-8";
    
    @Context
    protected ServletContext servletContext;

    protected Serializer getSerializer() {
        return ContextHelper.getServiceFromContext(Serializer.class, servletContext);
    }

    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        String mediaTypeString = mediaType.getType()+'/'+mediaType.getSubtype();
        return TripleCollection.class.isAssignableFrom(type) && supportedMediaTypes.contains(mediaTypeString);
    }

    public long getSize(TripleCollection t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    public void writeTo(TripleCollection t, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        
        long start = System.currentTimeMillis();
        String mediaTypeString = mediaType.getType()+'/'+mediaType.getSubtype();
        if (mediaType.isWildcardType() || TEXT_PLAIN.equals(mediaTypeString) || APPLICATION_OCTET_STREAM.equals(mediaTypeString)) {
            httpHeaders.putSingle("Content-Type", APPLICATION_JSON);
            getSerializer().serialize(entityStream, t, APPLICATION_JSON);
        } else {
            getSerializer().serialize(entityStream, t, mediaTypeString);
        }
        
        log.debug("Serialized {} in {}ms",t.size(),System.currentTimeMillis()-start);
    }
}
