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
package org.apache.stanbol.entityhub.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.entityhub.core.impl.EntityMapping;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * RESTful interface for the {@link EntityMapping}s defined by the {@link Entityhub}.
 * 
 * NOTE to RESTful Service Documentation in the header:
 *   Removed all Methods used to provide the RESTful Service documentation and
 *   incorporated them to the methods using the same path but with the id
 *   parameter. The reason for that was that the documentation methods where
 *   called even if an id parameter was provided if the "Accept:" header was
 *   not specified in requests.
 * 
 * @author Rupert Westenthaler
 */
@Path("/entityhub/mapping")
public class EntityMappingResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ServletContext context;
    
    // bind the job manager by looking it up from the servlet request context
    public EntityMappingResource(@Context ServletContext context) {
        super();
        this.context = context;
    }

// see NOTE to RESTful Service Documentation in the header
//    @GET
//    @Path("/")
//    @Produces(MediaType.TEXT_HTML)
//    public Response getMappingPage() {
//        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
//    }
    
    @GET
    @Path("/")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE,TEXT_HTML})
    public Response getMapping(@QueryParam("id") String reference, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("get mapping for request > id : {} > accept: {}",
            reference, headers.getAcceptableMediaTypes());
        Set<String> supported = new HashSet<String>(JerseyUtils.REPRESENTATION_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(
            headers,supported, APPLICATION_JSON_TYPE);
        
        if (reference == null || reference.isEmpty()) {
            //if HTML -> print the docu of the restfull service
            if(TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
              return Response.ok(new Viewable("index", this), TEXT_HTML).build();
            } else {
                return Response.status(Status.BAD_REQUEST).entity("The mapping id (URI) is missing.\n").header(
                    HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        Entity mapping;
        try {
            mapping = entityhub.getMappingById(reference);
        } catch (EntityhubException e) {
            log.error("error while getting the mapping for {}", reference, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        if (mapping == null) {
            return Response.status(Status.NOT_FOUND).entity("No mapping found for '" + reference + "'.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        } else {
            return Response.ok(mapping, acceptedMediaType).build();
        }
    }
// see NOTE to RESTful Service Documentation in the header
//    @GET
//    @Path("/entity")
//    @Produces(MediaType.TEXT_HTML)
//    public Response getEntityMappingPage() {
//        return Response.ok(new Viewable("entity", this), TEXT_HTML).build();
//    }

    @GET
    @Path("/entity")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE,TEXT_HTML})
    public Response getEntityMapping(@QueryParam("id") String entity, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("getEntityMapping() POST Request > entity: {} > accept: {}",
            entity, headers.getAcceptableMediaTypes());
        
        Set<String> supported = new HashSet<String>(JerseyUtils.REPRESENTATION_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(
            headers,supported, APPLICATION_JSON_TYPE);

        if (entity == null || entity.isEmpty()) {
            //if HTML -> print the docu of the restfull service
            if(TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                return Response.ok(new Viewable("entity", this), TEXT_HTML).build();
            } else {
                return Response.status(Status.BAD_REQUEST).entity("No entity given. Missing parameter id.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        Entity mapping;
        try {
            mapping = entityhub.getMappingBySource(entity);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (mapping == null) {
            return Response.status(Status.NOT_FOUND).entity("No mapping found for entity '" + entity + "'.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        } else {
            return Response.ok(mapping, acceptedMediaType).build();
        }
    }
 // see NOTE to RESTful Service Documentation in the header
//    @GET
//    @Path("/symbol")
//    @Produces(MediaType.TEXT_HTML)
//    public Response getSymbolMappingPage() {
//        return Response.ok(new Viewable("symbol", this), TEXT_HTML).build();
//    }
    
    @GET
    @Path("/symbol")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE,TEXT_HTML})
    public Response getSymbolMappings(@QueryParam("id") String symbol, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("getSymbolMappings() POST Request > symbol: {} > accept: {}",
            symbol, headers.getAcceptableMediaTypes());
        
        Set<String> supported = new HashSet<String>(JerseyUtils.REPRESENTATION_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(
            headers,supported, APPLICATION_JSON_TYPE);

        if (symbol == null || symbol.isEmpty()) {
            //if HTML -> print the docu of the restfull service
            if(TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                return Response.ok(new Viewable("symbol", this), TEXT_HTML).build();
            } else {
                return Response.status(Status.BAD_REQUEST).entity("No symbol given. Missing parameter id.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        Collection<Entity> mappings;
        try {
            mappings = entityhub.getMappingsByTarget(symbol);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (mappings == null || mappings.isEmpty()) {
            return Response.status(Status.NOT_FOUND).entity("No mapping found for symbol '" + symbol + "'.\n")
            .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        } else {
            // TODO: Implement Support for list of Signs, Representations and Strings
            // For now use a pseudo QueryResultList
            QueryResultList<Entity> mappingResultList = new QueryResultListImpl<Entity>(null,
                    mappings, Entity.class);
            return Response.ok(mappingResultList, acceptedMediaType).build();
        }
    }
}
