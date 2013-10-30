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

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.clerezza.rdf.core.sparql.ResultSet;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.w3c.dom.Document;

/**
 * MessageBodyWriter for a ResultSet.
 * Clerezza does provide such a writer, but it seems to require
 * quite a lot of Clerezza dependencies that we don't really need.
 */
//TODO maybe more to clerezza rdf.jaxrs provider
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Provider
//@Produces({"application/sparql-results+xml", "application/xml", "text/xml"})
public class ResultSetWriter implements MessageBodyWriter<ResultSet> {
    public static final Set<String> supportedMediaTypes;
    static {
        Set<String> types = new HashSet<String>();
        types.add("application/sparql-results+xml");
        types.add("application/xml");
        types.add("text/xml");
        supportedMediaTypes = Collections.unmodifiableSet(types);
    }

    public static final String ENCODING = "UTF-8";

    @Override
    public long getSize(ResultSet resultSet, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        String mediaTypeString = mediaType.getType()+'/'+mediaType.getSubtype();
        return ResultSet.class.isAssignableFrom(type) && supportedMediaTypes.contains(mediaTypeString);
    }

    @Override
    public void writeTo(ResultSet resultSet, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        String encoding = mediaType.getParameters().get("charset");
        if(encoding == null){
            encoding = ENCODING;
        }
        try {
            Document doc = new ResultSetToXml().toDocument(resultSet);
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(entityStream);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING,encoding);
            serializer.setOutputProperty(OutputKeys.INDENT,"yes");
            serializer.transform(domSource, streamResult);
        } catch(TransformerException te) {
            throw new IOException("TransformerException in writeTo()", te);
        } catch(ParserConfigurationException pce) {
            throw new IOException("Exception in ResultSetToXml.toDocument()", pce);
        }

        entityStream.flush();
    }
}
