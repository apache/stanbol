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
package org.apache.stanbol.entityhub.jersey.utils;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TEXT_RDF_NT;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.commons.io.IOUtils;
import org.apache.stanbol.commons.web.base.utils.MediaTypeUtil;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.ldpath.query.LDPathFieldQueryImpl;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods used by several of the RESTful service endpoints of the
 * Entityhub.
 * @author Rupert Westenthaler
 *
 */
public final class JerseyUtils {
    
    private static Logger log = LoggerFactory.getLogger(JerseyUtils.class);
    /**
     * Unmodifiable Set with the Media Types supported for {@link Representation}
     */
    public static final Set<String> REPRESENTATION_SUPPORTED_MEDIA_TYPES = 
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            APPLICATION_JSON,RDF_XML,N3,TURTLE,X_TURTLE,RDF_JSON,N_TRIPLE, TEXT_RDF_NT,
            MediaTypeUtil.JSON_LD)));
    /**
     * Unmodifiable Set with the Media Types supported for {@link Entity}
     */
    public static final Set<String> ENTITY_SUPPORTED_MEDIA_TYPES = 
        REPRESENTATION_SUPPORTED_MEDIA_TYPES;
    
    /**
     * Unmodifiable Set with the Media Types supported for {@link QueryResultList}
     */
    public static final Set<String> QUERY_RESULT_SUPPORTED_MEDIA_TYPES =
        REPRESENTATION_SUPPORTED_MEDIA_TYPES;
    /**
     * This utility class used the {@link DefaultQueryFactory} as
     * {@link FieldQueryFactory} instance. 
     */
    private static FieldQueryFactory queryFactory = DefaultQueryFactory.getInstance();

    private JerseyUtils() {/* do not create instance of Util Classes */}


//    /**
//     * Returns the {@link FieldQuery} based on the JSON formatted String (in case
//     * of "application/x-www-form-urlencoded" requests) or file (in case of
//     * "multipart/form-data" requests).<p>
//     * @param query the string containing the JSON serialised FieldQuery or
//     * <code>null</code> in case of a "multipart/form-data" request
//     * @param file the temporary file holding the data parsed by the request to
//     * the web server in case of a "multipart/form-data" request or <code>null</code>
//     * in case of the "application/x-www-form-urlencoded" request.
//     * @return the FieldQuery parsed from the string provided by one of the two
//     * parameters
//     * @throws WebApplicationException if both parameter are <code>null</code> or
//     * if the string provided by both parameters could not be used to parse a
//     * {@link FieldQuery} instance.
//     */
//    public static FieldQuery parseFieldQuery(String query, File file) throws WebApplicationException {
//        if(query == null && file == null) {
//            throw new WebApplicationException(new IllegalArgumentException("Query Requests MUST define the \"query\" parameter"), Response.Status.BAD_REQUEST);
//        }
//        FieldQuery fieldQuery = null;
//        JSONException exception = null;
//        if(query != null){
//            try {
//                fieldQuery = JSONToFieldQuery.fromJSON(queryFactory,query);
//            } catch (JSONException e) {
//                log.warn("unable to parse FieldQuery from \"application/x-www-form-urlencoded\" encoded query string "+query,e);
//                fieldQuery = null;
//                exception = e;
//            }
//        } //else no query via application/x-www-form-urlencoded parsed
//        if(fieldQuery == null && file != null){
//            try {
//                query = FileUtils.readFileToString(file);
//                fieldQuery = JSONToFieldQuery.fromJSON(queryFactory,query);
//            } catch (IOException e) {
//                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
//            } catch (JSONException e) {
//                log.warn("unable to parse FieldQuery from \"multipart/form-data\" encoded query string "+query,e);
//                exception = e;
//            }
//        }//fieldquery already initialised or no query via multipart/form-data parsed
//        if(fieldQuery == null){
//            throw new WebApplicationException(new IllegalArgumentException("Unable to parse FieldQuery form the parsed query String:"+query, exception),Response.Status.BAD_REQUEST);
//        }
//        return fieldQuery;
//    }
    /**
     * Creates an {@link FieldQuery} for parameters parsed by the /find requests
     * supported by the /symbol, /sites and {siteId} RESTful endpoints.
     * TODO: This has to be refactored to use "EntityQuery" as soon as Multiple
     *       query types are implemented for the Entityhub.
     * @param name the name pattern to search entities for (required)
     * @param field the field used to search for entities (required)
     * @param language the language of the parsed name pattern (optional)
     * @param limit the maximum number of result (optional)
     * @param offset the offset of the first result (optional)
     * @return the {@link FieldQuery} representing the parsed parameter
     * @throws WebApplicationException in case the parsed name pattern is invalid.
     * The validation of this required parameter provided by the Request is done
     * by this method.
     * @throws IllegalArgumentException in case the parsed field is invalid. Callers
     * of this method need to ensure that this parameter is set to an valid value.
     */
    public static FieldQuery createFieldQueryForFindRequest(String name, String field, 
                                                            String language, Integer limit, 
                                                            Integer offset, String ldpath) throws WebApplicationException, IllegalArgumentException{
        if(name == null || name.trim().isEmpty()){
            // This throws an WebApplicationException, because the search name is
            // provided by the caller. So an empty or missing name is interpreted
            // as an bad Requested sent by the user
            throw new WebApplicationException(
                new IllegalArgumentException(
                    "The parsed \"name\" pattern to search entities for MUST NOT be NULL nor EMPTY"),
                    Response.Status.BAD_REQUEST);
        } else {
            name = name.trim();
        }
        if(field == null || field.trim().isEmpty()){
            // This throws no WebApplicationException, because "field" is an 
            // optional parameter and callers of this method MUST provide an
            // valid default value in case the request does not provide any or
            // valid data. 
            throw new IllegalArgumentException("The parsed search \"field\" MUST NOT be NULL nor EMPTY");
        } else {
            field = field.trim();
        }
        log.debug("Process Find Request:");
        log.debug("  > name  : " + name);
        log.debug("  > field  : " + field);
        log.debug("  > lang  : " + language);
        log.debug("  > limit : " + limit);
        log.debug("  > offset: " + offset);
        log.debug("  > ldpath: " + ldpath);
        FieldQuery query;
        if(ldpath != null && !ldpath.isEmpty()){ //STANBOL-417 
            query = new LDPathFieldQueryImpl();
            ((LDPathFieldQueryImpl)query).setLDPathSelect(ldpath);
        } else { //if no LDPath is parsed select the default field
            query = queryFactory.createFieldQuery();
            Collection<String> selectedFields = new ArrayList<String>();
            selectedFields.add(field); //select also the field used to find entities
            query.addSelectedFields(selectedFields);
        }
        if (language == null || language.trim().isEmpty()) {
            query.setConstraint(field, new TextConstraint(name, PatternType.wildcard, false));
        } else {
            query.setConstraint(field, new TextConstraint(name, PatternType.wildcard, false, language));
        }
        if (limit != null && limit > 0) {
            query.setLimit(limit);
        }
        if(offset != null && offset > 0) {
            query.setOffset(offset);
        }
        return query;
    }
//    /**
//     * Getter for a Service from the {@link ServletContext} by using the
//     * {@link Class#getName()} as key for {@link ServletContext#getAttribute(String)}.
//     * In case the Service can not be found a {@link WebApplicationException} is
//     * thrown with the message that the Service is currently not available.
//     * @param <T> The type of the Service
//     * @param service the Service interface
//     * @param context the context used to search the service
//     * @return the Service instance
//     * @throws WebApplicationException in case the service instance was not found 
//     * in the parsed servlet context
//     * @throws IllegalArgumentException if <code>null</code> is parsed as
//     * service or context
//     */
//    @SuppressWarnings("unchecked")
//    public static <T> T getService(Class<T> service, ServletContext context) throws WebApplicationException, IllegalArgumentException {
//        if(service == null){
//            throw new IllegalArgumentException("The parsed ServiceInterface MUST NOT be NULL!");
//        }
//        if(context == null){
//            throw new IllegalArgumentException("The parsed ServletContext MUST NOT be NULL");
//        }
//        T serviceInstance = (T) context.getAttribute(service.getName());
//        if(serviceInstance == null){
//            throw new WebApplicationException(new IllegalStateException(
//                "The "+service.getSimpleName()+" Service is currently not available " +
//                		"(full name= "+service+"| " +
//                				"servlet context name = "+context.getServletContextName()+")"), 
//                Response.Status.INTERNAL_SERVER_ERROR);
//        }
//        return serviceInstance;
//    }
    /**
     * Tests if a generic type (may be &lt;?&gt;, &lt;? extends {required}&gt; 
     * or &lt;? super {required}&gt;) is compatible with the required one.
     * TODO: Should be moved to an utility class
     * @param required the required class the generic type MUST BE compatible with
     * @param genericType the required class
     * @return if the generic type is compatible with the required class
     */
    public static boolean testType(Class<?> required, Type type) {
        //for the examples let assume that a Set is the raw type and the
        //requested generic type is a Representation with the following class
        //hierarchy:
        // Object
        //     -> Representation
        //         -> RdfRepresentation
        //         -> InMemoryRepresentation
        //     -> InputStream
        //     -> Collection<T>
        boolean typeOK = false;
//        while(type != null && !typeOK){
//            types.add(type);
        if(type instanceof Class<?>){
            typeOK = required.isAssignableFrom((Class<?>) type);
            type = ((Class<?>)type).getGenericSuperclass();
        } else if(type instanceof WildcardType){
            //In cases <? super {class}>, <? extends {class}, <?>
            WildcardType wildcardSetType = (WildcardType) type;
            if(wildcardSetType.getLowerBounds().length > 0){
                Type lowerBound = wildcardSetType.getLowerBounds()[0];
                //OK
                //  Set<? super RdfRepresentation>
                //  Set<? super Representation>
                //NOT OK
                //  Set<? super InputStream>
                //  Set<? super Collection<Representation>>
                typeOK = lowerBound instanceof Class<?> &&
                    required.isAssignableFrom((Class<?>)lowerBound);
            } else if (wildcardSetType.getUpperBounds().length > 0){
                Type upperBound = wildcardSetType.getUpperBounds()[0];
                //OK
                //  Set<? extends Representation>
                //  Set<? extends Object>
                //NOT OK
                //  Set<? extends RdfRepresentation>
                //  Set<? extends InputStream>
                //  Set<? extends Collection<Representation>
                typeOK = upperBound instanceof Class<?> &&
                    ((Class<?>)upperBound).isAssignableFrom(required); 
            } else { //no upper nor lower bound
                // Set<?>
                typeOK = true;
            }
        } else if(required.isArray() && type instanceof GenericArrayType){
            //In case the required type is an array we need also to support 
            //possible generic Array specifications
            GenericArrayType arrayType = (GenericArrayType)type;
            typeOK = testType(required.getComponentType(), arrayType.getGenericComponentType());
        } else if(type instanceof ParameterizedType){
            ParameterizedType pType = ((ParameterizedType)type);
            typeOK = pType.getRawType() instanceof Class<?> && 
                    required.isAssignableFrom((Class<?>)pType.getRawType());
            type = null;
        } else {
            //GenericArrayType but !required.isArray() -> incompatible
            //TypeVariable -> no variables define -> incompatible
            typeOK = false;
//                type = null; //end
        }
//        }
        return typeOK;
    }
    /**
     * Tests the parsed type against the raw type and parsed Type parameters.
     * This allows e.g. to check for <code>Map&lt;String,Number&gt</code> but
     * also works with classes that extend generic types such as
     * <code>Dummy extends {@link HashMap}&lt;String,String&gt</code>.
     * @param rawType the raw type to test against
     * @param parameterTypes the types of the parameters
     * @param type the type to test
     * @return if the type is compatible or not
     */
    public static boolean testParameterizedType(Class<?> rawType, Class<?>[] parameterTypes, Type type) {
        // first check the raw type
        if (!testType(rawType, type)) {
            return false;
        }
        while (type != null) {
            // types.add(type);
            Type[] parameters = null;
            if (type instanceof ParameterizedType) {
                parameters = ((ParameterizedType) type).getActualTypeArguments();
                // the number of type arguments MUST BE the same as parameter types
                if (parameters.length == parameterTypes.length) {
                    boolean compatible = true;
                    // All parameters MUST BE compatible!
                    for (int i = 0; compatible && i < parameters.length; i++) {
                        compatible = testType(parameterTypes[i], parameters[i]);
                    }
                    if (compatible) {
                        return true;
                    }
                } // else check parent types

            } // else not parameterised
            if (type instanceof Class<?>) {
                type = ((Class<?>) type).getGenericSuperclass();
            } else {
                return false;
            }
        }
        return false;
    }
    
    /**
     * This Method is intended to parse form data from 
     * {@link MediaType#APPLICATION_FORM_URLENCODED} requests. This functionality
     * us usually needed when writing a {@link MessageBodyReader} to get the
     * data from the "{@link InputStream} entityStream" parameter of the 
     * {@link MessageBodyReader#readFrom(Class, Type, java.lang.annotation.Annotation[], MediaType, javax.ws.rs.core.MultivaluedMap, InputStream)}
     * method.
     * @param entityStream the stream with the form data
     * @param charset The charset used for the request (if <code>null</code> or
     * empty UTF-8 is used as default.
     * @return the parsed form data as key value map
     * @throws IOException on any exception while reading the data form the stream
     */
    public static Map<String,String> parseForm(InputStream entityStream,String charset) throws IOException {
        /* TODO: Question: 
         * If I get an Post Request with "application/x-www-form-urlencoded" 
         * and a charset (lets assume "iso-2022-kr") do I need to use the 
         * charset to read the String from the Stream, or to URL decode the 
         * String or both?
         * 
         * This code assumes that it needs to be used for both, but this needs
         * validation!
         */
        if(charset == null || charset.isEmpty()){
            charset = "UTF-8";
        }
        String data;
        try {
             data = IOUtils.toString(entityStream,charset);
        } catch (UnsupportedCharsetException e) {
            throw new IOException(e.getMessage(),e);
        }
        Map<String, String> form = new HashMap<String, String>();
        StringTokenizer tokenizer = new StringTokenizer(data, "&");
        String token;
        try {
            while (tokenizer.hasMoreTokens()) {
                token = tokenizer.nextToken();
                int index = token.indexOf('=');
                if (index < 0) {
                    form.put(URLDecoder.decode(token,charset), null);
                } else if (index > 0) {
                    form.put(URLDecoder.decode(token.substring(0, index),charset), 
                        URLDecoder.decode(token.substring(index+1),charset));
                }
            }
        } catch (UnsupportedCharsetException e) {
            throw new IOException(e.getMessage(),e);
        }
        return form;
    }
    
}
