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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.jersey.parsers.JSONToFieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.codehaus.jettison.json.JSONException;
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
     * This utility class used the {@link DefaultQueryFactory} as
     * {@link FieldQueryFactory} instance. 
     */
    private static FieldQueryFactory queryFactory = DefaultQueryFactory.getInstance();

    private JerseyUtils() {/* do not create instance of Util Classes */}

    /**
     * Searches the Header for acceptable media types and returns the first found
     * that is not the wildcard type. If no one is found the parsed default type
     * is returned.
     *
     * @param headers the request headers
     * @param defaultType the default type if no or only the wildcard type was found in
     * the header
     * @return the acceptable media type
     */
    public static MediaType getAcceptableMediaType(HttpHeaders headers, MediaType defaultType) {
        MediaType acceptedMediaType = null;
        if (!headers.getAcceptableMediaTypes().isEmpty()) {
            for (MediaType accepted : headers.getAcceptableMediaTypes()) {
                if (!accepted.isWildcardType()) {
                    acceptedMediaType = accepted;
                    break;
                }
            }
        }
        if (acceptedMediaType == null) {
            acceptedMediaType = defaultType;
        }
        return acceptedMediaType;
    }
    /**
     * Returns the {@link FieldQuery} based on the JSON formatted String (in case
     * of "application/x-www-form-urlencoded" requests) or file (in case of
     * "multipart/form-data" requests).<p>
     * @param query the string containing the JSON serialised FieldQuery or
     * <code>null</code> in case of a "multipart/form-data" request
     * @param file the temporary file holding the data parsed by the request to
     * the web server in case of a "multipart/form-data" request or <code>null</code>
     * in case of the "application/x-www-form-urlencoded" request.
     * @return the FieldQuery parsed from the string provided by one of the two
     * parameters
     * @throws WebApplicationException if both parameter are <code>null</code> or
     * if the string provided by both parameters could not be used to parse a
     * {@link FieldQuery} instance.
     */
    public static FieldQuery parseFieldQuery(String query, File file) throws WebApplicationException {
        if(query == null && file == null) {
            throw new WebApplicationException(new IllegalArgumentException("Query Requests MUST define the \"query\" parameter"), Response.Status.BAD_REQUEST);
        }
        FieldQuery fieldQuery = null;
        JSONException exception = null;
        if(query != null){
            try {
                fieldQuery = JSONToFieldQuery.fromJSON(queryFactory,query);
            } catch (JSONException e) {
                log.warn("unable to parse FieldQuery from \"application/x-www-form-urlencoded\" encoded query string "+query);
                fieldQuery = null;
                exception = e;
            }
        } //else no query via application/x-www-form-urlencoded parsed
        if(fieldQuery == null && file != null){
            try {
                query = FileUtils.readFileToString(file);
                fieldQuery = JSONToFieldQuery.fromJSON(queryFactory,query);
            } catch (IOException e) {
                throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
            } catch (JSONException e) {
                log.warn("unable to parse FieldQuery from \"multipart/form-data\" encoded query string "+query);
                exception = e;
            }
        }//fieldquery already initialised or no query via multipart/form-data parsed
        if(fieldQuery == null){
            throw new WebApplicationException(new IllegalArgumentException("Unable to parse FieldQuery for the parsed query\n"+query, exception),Response.Status.BAD_REQUEST);
        }
        return fieldQuery;
    }
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
    public static FieldQuery createFieldQueryForFindRequest(String name, String field, String language, Integer limit, Integer offset) throws WebApplicationException, IllegalArgumentException{
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
            new IllegalArgumentException("The parsed search \"field\" MUST NOT be NULL nor EMPTY");
        } else {
            field = field.trim();
        }
        log.debug("Process Find Request:");
        log.debug("  > name  : " + name);
        log.debug("  > field  : " + field);
        log.debug("  > lang  : " + language);
        log.debug("  > limit : " + limit);
        log.debug("  > offset: " + offset);
        FieldQuery query = queryFactory.createFieldQuery();
        if (language == null) {
            query.setConstraint(field, new TextConstraint(name, PatternType.wildcard, false));
        } else {
            query.setConstraint(field, new TextConstraint(name, PatternType.wildcard, false, language));
        }
        Collection<String> selectedFields = new ArrayList<String>();
        selectedFields.add(field); //select also the field used to find entities
        query.addSelectedFields(selectedFields);
        if (limit != null && limit > 0) {
            query.setLimit(limit);
        }
        if(offset != null && offset > 0) {
            query.setOffset(offset);
        }
        return query;
    }
    /**
     * Getter for a Service from the {@link ServletContext} by using the
     * {@link Class#getName()} as key for {@link ServletContext#getAttribute(String)}.
     * In case the Service can not be found a {@link WebApplicationException} is
     * thrown with the message that the Service is currently not available.
     * @param <T> The type of the Service
     * @param service the Service interface
     * @param context the context used to search the service
     * @return the Service instance
     * @throws WebApplicationException in case the service instance was not found 
     * in the parsed servlet context
     * @throws IllegalArgumentException if <code>null</code> is parsed as
     * service or context
     */
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
}
