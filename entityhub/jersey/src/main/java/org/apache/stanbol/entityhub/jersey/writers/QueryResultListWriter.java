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
package org.apache.stanbol.entityhub.jersey.writers;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO: Replace with Serializer infrastructure similar to {@link Serializer}
 */
@Provider
//@Produces({APPLICATION_JSON, N3, N_TRIPLE, RDF_XML, TURTLE, X_TURTLE, RDF_JSON})
public class QueryResultListWriter implements MessageBodyWriter<QueryResultList<?>> {

    protected static final Set<String> produces;
    static {
        Set<String> p = new HashSet<String>();
        p.add(APPLICATION_JSON);
        p.add(N3);
        p.add(N_TRIPLE);
        p.add(RDF_XML);
        p.add(TURTLE);
        p.add(X_TURTLE);
        p.add(RDF_JSON);
        produces = Collections.unmodifiableSet(p);
    }
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(QueryResultListWriter.class);
    
    @Context
    protected ServletContext servletContext;
    
    protected Serializer getSerializer() {
        return ContextHelper.getServiceFromContext(Serializer.class, servletContext);
    }

    @Override
    public long getSize(QueryResultList<?> result, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        String mediaTypeString = mediaType.getType()+'/'+mediaType.getSubtype();
        return QueryResultList.class.isAssignableFrom(type) &&
            produces.contains(mediaTypeString);
        
    }

    @Override
    public void writeTo(QueryResultList<?> resultList, Class<?> doNotUse, Type genericType,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException, WebApplicationException {
        String mediaTypeString = mediaType.getType()+'/'+mediaType.getSubtype();
        String encoding = mediaType.getParameters().get("charset");
        if(encoding == null){
            encoding = DEFAULT_ENCODING;
        }
        if (APPLICATION_JSON.equals(mediaTypeString)) {
            try {
                IOUtils.write(QueryResultsToJSON.toJSON(resultList).toString(4), entityStream,encoding);
            } catch (JSONException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
        } else { //RDF
            MGraph resultGraph = QueryResultsToRDF.toRDF(resultList);
            addFieldQuery(resultList.getQuery(),resultGraph);
            getSerializer().serialize(entityStream, resultGraph, mediaTypeString);
        }
    }
    private void addFieldQuery(FieldQuery query, MGraph resultGraph) {
        if(query == null){
            return;
        }
        try {
            JSONObject fieldQueryJson = FieldQueryToJSON.toJSON(query);
            if(fieldQueryJson != null){
                //add the triple with the fieldQuery
                resultGraph.add(new TripleImpl(
                    QueryResultsToRDF.QUERY_RESULT_LIST, 
                    QueryResultsToRDF.FIELD_QUERY, 
                    QueryResultsToRDF.literalFactory.createTypedLiteral(
                        fieldQueryJson.toString())));
            }
        } catch (JSONException e) {
            log.warn(String.format("Unable to serialize Fieldquery %s to JSON",
                query),e);
        }
    }

}
