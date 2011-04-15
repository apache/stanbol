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
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.util.Collection;

import javax.servlet.ServletContext;
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
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.EntityMapping;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

/**
 * RESTful interface for the {@link EntityMapping}s defined by the {@link Entityhub}.
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

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getEntityMappingPage() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }
    
    @GET
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE})
    public Response getMapping(@QueryParam("id") String reference, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("get mapping for request > id : {} > accept: {}",
            reference, headers.getAcceptableMediaTypes());

        MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, APPLICATION_JSON_TYPE);
        
        if (reference == null || reference.isEmpty()) {
            return Response.status(Status.BAD_REQUEST).entity("The mapping ID (URI) is missing.").header(
                HttpHeaders.ACCEPT, acceptedMediaType).build();
        }
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        EntityMapping mapping;
        try {
            mapping = entityhub.getMappingById(reference);
        } catch (EntityhubException e) {
            log.error("error while getting the mapping for {}", reference, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        if (mapping == null) {
            return Response.status(Status.NOT_FOUND).entity("No mapping found for '" + reference + "'.")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        } else {
            return Response.ok(mapping, acceptedMediaType).build();
        }
    }

    @GET
    @Path("/entity")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE})
    public Response getEntityMapping(@QueryParam("id") String entity, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("getEntityMapping() POST Request > entity: {} > accept: {}",
            entity, headers.getAcceptableMediaTypes());

        if (entity == null || entity.isEmpty()) {
            // TODO: how to parse an error message
            throw new WebApplicationException(BAD_REQUEST);
        }
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        EntityMapping mapping;
        try {
            mapping = entityhub.getMappingByEntity(entity);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (mapping == null) {
            throw new WebApplicationException(404);
        } else {
            MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, APPLICATION_JSON_TYPE);
            return Response.ok(mapping, acceptedMediaType).build();
        }
    }

    @GET
    @Path("/symbol")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE})
    public Response getSymbolMappings(@QueryParam("id") String symbol, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("getSymbolMappings() POST Request > symbol: {} > accept: {}",
            symbol, headers.getAcceptableMediaTypes());

        if (symbol == null || symbol.isEmpty()) {
            // TODO: how to parse an error message
            throw new WebApplicationException(BAD_REQUEST);
        }
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        Collection<EntityMapping> mappings;
        try {
            mappings = entityhub.getMappingsBySymbol(symbol);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (mappings == null || mappings.isEmpty()) {
            throw new WebApplicationException(404);
        } else {
            MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, APPLICATION_JSON_TYPE);
            // TODO: Implement Support for list of Signs, Representations and Strings
            // For now use a pseudo QueryResultList
            QueryResultList<EntityMapping> mappingResultList = new QueryResultListImpl<EntityMapping>(null,
                    mappings, EntityMapping.class);
            return Response.ok(mappingResultList, acceptedMediaType).build();
        }
    }
}
