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
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.util.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.Viewable;

@Path("/entityhub")
public class EntityhubRootResource extends BaseStanbolResource {
    
    private static Logger log = LoggerFactory.getLogger(EntityhubRootResource.class);
    /**
     * used to extract the mediaType for the response based on the Accept
     * header of the request.
     */
    private static Collection<String> ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML;
    static {
        ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML = new HashSet<String>(
                JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES);
        ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML.add(TEXT_HTML);
    }
    /**
     * The default search field for /find queries is the entityhub-maodel:label
     */
    private static final String DEFAULT_FIND_FIELD = RdfResourceEnum.label.getUri();
    /**
     * The default number of maximal results of searched sites.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;
    /**
     * The default result fields for /find queries is the entityhub-maodel:label and the
     * entityhub-maodel:description.
     */
    private static final Collection<? extends String> DEFAULT_FIND_SELECTED_FIELDS = Arrays.asList(
        RdfResourceEnum.label.getUri(), RdfResourceEnum.description.getUri());

    private ServletContext context;
    // bind the job manager by looking it up from the servlet request context
    public EntityhubRootResource(@Context ServletContext context) {
        super();
        this.context = context;
    }
    @GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }

    @GET
    @Path("entity")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response getSymbol(@QueryParam("id") String symbolId, @Context HttpHeaders headers) throws WebApplicationException {
        log.info("GET /entity Request");
        log.info("  > id: " + symbolId);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, 
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            APPLICATION_JSON_TYPE);
        if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE) && symbolId == null){
            //return HTML docu
            return Response.ok(new Viewable("entity", this), TEXT_HTML).build();
        }
        if (symbolId == null || symbolId.isEmpty()) {
            // TODO: how to parse an error message
            throw new WebApplicationException(BAD_REQUEST);
        }
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        Entity entity;
        try {
            entity = entityhub.getEntity(symbolId);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (entity == null) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            return Response.ok(entity, acceptedMediaType).build();
        }
    }
        
    @GET
    @Path("lookup")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response lookupSymbol(@QueryParam("id") String reference,
                                 @QueryParam("create") boolean create,
                                 @Context HttpHeaders headers) throws WebApplicationException {
        log.info("GET /lookup Request");
        log.info("  > id: " + reference);
        log.info("  > create   : " + create);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, 
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            APPLICATION_JSON_TYPE);
        if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE) && reference == null){
            //return docu
            return Response.ok(new Viewable("lookup", this), TEXT_HTML).build();
        } else {
            if (reference == null || reference.isEmpty()) {
                // TODO: how to parse an error message
                throw new WebApplicationException(BAD_REQUEST);
            }
            Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
            Entity entity;
            try {
                entity = entityhub.lookupLocalEntity(reference, create);
            } catch (EntityhubException e) {
                throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
            }
            if (entity == null) {
                return Response.status(Status.NOT_FOUND).entity("No symbol found for '" + reference + "'.")
                        .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            } else {
                return Response.ok(entity, acceptedMediaType).build();
            }
        }
    }

    @POST
    @Path("entity/")
    @Consumes(MediaType.WILDCARD)
    public Response createEntity(@QueryParam(value = "id") String id, 
                               Set<Representation> parsed,
                               @Context HttpHeaders headers){
        //Set<Representation> representations = Collections.emptySet();
        //log.info("Test: "+test);
        log.info("Headers: "+headers.getRequestHeaders());
        log.info("Entity: "+id);
        log.info("Representations : "+parsed);
        return updateOrCreateEntity(id, parsed, true, 
            JerseyUtils.getAcceptableMediaType(headers,
                JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES, 
                MediaType.APPLICATION_JSON_TYPE));
    }

    @PUT
    @Path("entity/")
    @Consumes(MediaType.WILDCARD)
    public Response updateEntity(@QueryParam(value = "id") String id, 
                               Set<Representation> parsed,
                               @Context HttpHeaders headers){
        //Set<Representation> representations = Collections.emptySet();
        //log.info("Test: "+test);
        log.info("Headers: "+headers.getRequestHeaders());
        log.info("Entity: "+id);
        log.info("Representations : "+parsed);
        return updateOrCreateEntity(id, parsed, false, 
            JerseyUtils.getAcceptableMediaType(headers,
                JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES, 
                MediaType.APPLICATION_JSON_TYPE));
    }
    
    @DELETE
    @Path("entity")
    public Response deleteEntity(@QueryParam(value="id") String id,
                                 @Context HttpHeaders headers){
        MediaType accepted = JerseyUtils.getAcceptableMediaType(headers,
            JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES, 
            MediaType.APPLICATION_JSON_TYPE);
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        if(id == null || id.isEmpty()){
            return Response.status(Status.BAD_REQUEST).entity("The Request does" +
                    "not provide the id of the Entity to delete (parameter 'id').")
                    .header(HttpHeaders.ACCEPT, accepted).build();
        }
        Entity entity;
        try {
            entity = entityhub.delete(id);
        } catch (EntityhubException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
            .header(HttpHeaders.ACCEPT, accepted).build();
        }
        if(entity == null){
            return Response.status(Status.NOT_FOUND).entity("An Entity with the" +
                    "parsed id "+id+" is not managed by the Entityhub")
                    .header(HttpHeaders.ACCEPT, accepted).build();
        } else {
            return Response.ok(entity,accepted).build();
        }
    }
    /**
     * Implements the creation/update of Representations within the Entityhub.
     * @param id the id of the resource to create or update. If not 
     * <code>null</code> all parsed Representations with other
     * ids will be ignored.
     * @param parsed the parsed representation(s)
     * @param createState create or update request
     * @param accepted the accepted media type for the response
     * @return the created/updated representation as response
     */
    private Response updateOrCreateEntity(String id,Set<Representation> parsed, 
                                          boolean createState, MediaType accepted){
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        if(entityhub == null){
            return Response.status(Status.INTERNAL_SERVER_ERROR).
                entity("The Entityhub is currently unavailable.")
                .header(HttpHeaders.ACCEPT, accepted).build();
        }
        //(1) if an id is parsed we need to filter parsed Representations
        if(id != null){
            for(Iterator<Representation> it = parsed.iterator(); it.hasNext();){
                Representation rep = it.next();
                String aboutId = ModelUtils.getAboutRepresentation(rep);
                if(!(id.equals(rep.getId()) ||
                        id.equals(aboutId))){
                    it.remove(); //not the Entity nor the metadata of the parsed ID
                }
            }
        }
        //First check if all parsed Representation can be created/updated
        for(Representation representation : parsed){
            boolean exists;
            try {
                exists = entityhub.isRepresentation(representation.getId());
            } catch (EntityhubException e) {
                log.error(String.format("Exception while checking the existance " +
                    "of an Entity with id  %s in the Entityhub.",
                    representation.getId()),e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(String.format("Unable to %s Entity %s because of" +
                        "an Error while checking the current version of that" +
                        "Entity within the Entityhub (Message: %s)",
                        createState?"create":"update", representation.getId(),e.getMessage()))
                        .header(HttpHeaders.ACCEPT, accepted).build();
            }
            if(createState == exists){
                return Response.status(Status.BAD_REQUEST).entity(String.format(
                    "Unable to %s an Entity that %s exist",
                    createState?"create":"update",
                    exists?"does already":"does not"))
                    .header(HttpHeaders.ACCEPT, accepted).build();
            }
        }
        //store the Representations
        //If someone parses data for more than a single Entity, but does not
        //provide an ID for the Entity to update, this will update/create all
        //the parsed entity. However the response can only return a single
        //Entity!
        //This can not be changed easily as long as there are no local URIs 
        //for remote Entiteis as suggested by
        // http://incubator.apache.org/stanbol/docs/trunk/entityhub/entityhubandlinkeddata.html
        Map<String,Entity> updated = new HashMap<String,Entity>();
        for(Representation representation : parsed){
            try {
                Entity entity = entityhub.store(representation);
                updated.put(entity.getId(), entity);
            }catch (EntityhubException e) {
                log.error(String.format("Exception while %s representation %s" +
                        "in the Entityhub.",
                        createState?"create":"update",representation),e);
            }
        }
        //create the response for the Entity
        // for now directly return the added entity. one could also
        // consider returning a seeOther (303) with the get URI for the
        // created/updated entity
        if(updated.isEmpty()){
            // No (valid) data parsed
            return Response.status(Status.NOT_MODIFIED).build();
        } else {
            Entity entity = updated.values().iterator().next();
            if(createState){
                return Response.created(uriInfo.getAbsolutePathBuilder()
                    .queryParam("id", "{entityId}")
                    .build(entity.getId())).build();
            } else {
                //return Response.noContent().build();
                //As alternative return the modified entity
                return Response.ok(entity, accepted).build();
                
            }
//            if (updated.size() == 1){
//                return Response.status(createState? Status.CREATED:Status.OK)
//                .entity(updated.values().iterator().next())
//                .header(HttpHeaders.ACCEPT, accepted).build();
//            } else { //implement serializer for list of Entitis!
//                return Response.status(createState? Status.CREATED:Status.OK)
//                .entity(updated.values())
//                .header(HttpHeaders.ACCEPT, accepted).build();
//            }
        }
        //return Response.seeOther(uriInfo.getAbsolutePath()).build();
    }
    
    @GET
    @Path("/find")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response findEntityByGet(@QueryParam(value = "name") String name,
                                    @QueryParam(value = "field") String field,
                                    @QueryParam(value = "lang") String language,
                                    @QueryParam(value = "limit") Integer limit,
                                    @QueryParam(value = "offset") Integer offset,
                                    // TODO: Jersey supports parsing multiple values in Collections.
                                    // Use this feature here instead of using this hand crafted
                                    // solution!
                                    @QueryParam(value = "select") String select,
                                    @Context HttpHeaders headers) {
        return findEntity(name, field, language, limit, offset, select, headers);
    }
    
    @POST
    @Path("/find")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response findEntity(@FormParam(value = "name") String name,
                               @FormParam(value = "field") String field,
                               @FormParam(value = "lang") String language,
                               @FormParam(value = "limit") Integer limit,
                               @FormParam(value = "offset") Integer offset,
                               // TODO: Jersey supports parsing multiple values in Collections.
                               // Use this feature here instead of using this hand crafted
                               // solution!
                               @FormParam(value = "select") String select,
                               @Context HttpHeaders headers) {
        log.debug("/find Request");
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            MediaType.APPLICATION_JSON_TYPE);
        if(name == null || name.isEmpty()){
            if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE)){
                //return HTML docu
                return Response.ok(new Viewable("find", this), TEXT_HTML).build();
            } else {
                return Response.status(Status.BAD_REQUEST)
                    .entity("The name must not be null nor empty for find requests. Missing parameter name.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        } else {
            if (field == null || field.trim().isEmpty()) {
                field = DEFAULT_FIND_FIELD;
            } else {
                field = field.trim();
            }
            FieldQuery query = JerseyUtils.createFieldQueryForFindRequest(name, field, language,
                limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, offset);
            
            // For the Entityhub we support to select additional fields for results
            // of find requests. For the Sites and {site} endpoint this is currently
            // deactivated because of very bad performance with OPTIONAL graph patterns
            // in SPARQL queries.
            Collection<String> additionalSelectedFields = new ArrayList<String>();
            if (select == null || select.isEmpty()) {
                additionalSelectedFields.addAll(DEFAULT_FIND_SELECTED_FIELDS);
            } else {
                for (String selected : select.trim().split(" ")) {
                    if (selected != null && !selected.isEmpty()) {
                        additionalSelectedFields.add(selected);
                    }
                }
            }
            query.addSelectedFields(additionalSelectedFields);
            return executeQuery(query, acceptedMediaType);
        }
    }
        
    @GET
    @Path("/query")
    public Response getQueryDocumentation(){
        return Response.ok(new Viewable("query", this), TEXT_HTML).build();        
    }
    /**
     * Allows to parse any kind of {@link FieldQuery} in its JSON Representation.
     * <p>
     * TODO: as soon as the entityhub supports multiple query types this need to be refactored. The idea is
     * that this dynamically detects query types and than redirects them to the referenced site
     * implementation.
     * 
     * @param query The field query in JSON format
     * @param headers the header information of the request
     * @return the results of the query
     */
    @POST
    @Path("/query")
    //@Consumes( {APPLICATION_FORM_URLENCODED + ";qs=1.0", MULTIPART_FORM_DATA + ";qs=0.9"})
    @Consumes( {MediaType.APPLICATION_JSON})
    public Response queryEntities(/*@FormParam("query")*/ FieldQuery query,
                                  @Context HttpHeaders headers) {
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,
            JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES,
            MediaType.APPLICATION_JSON_TYPE);
        if(query == null && MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
            return getQueryDocumentation();
        }
        return executeQuery(query, acceptedMediaType);
    }
    
    /**
     * Executes the query parsed by {@link #queryEntities(String, File, HttpHeaders)}
     * or created based {@link #findEntity(String, String, String, String, HttpHeaders)
     * @param query The query to execute
     * @param headers The headers used to determine the media types
     * @return the response (results of error)
     */
    private Response executeQuery(FieldQuery query, MediaType acceptedMediaType) throws WebApplicationException {
        Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, context);
        try {
            return Response.ok(entityhub.find(query), acceptedMediaType).build();
        } catch (EntityhubException e) {
            log.error("Exception while performing the FieldQuery on the EntityHub", e);
            log.error("Query:\n" + query);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    /*--------------------------------------------------------------------------
     * Methods for EntityMappings
     *--------------------------------------------------------------------------
     */
    
    @GET
    @Path("mapping")
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
              return Response.ok(new Viewable("mapping", this), TEXT_HTML).build();
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
    @Path("mapping/entity")
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
                return Response.ok(new Viewable("mapping_entity", this), TEXT_HTML).build();
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
//    @Path("mapping/symbol")
//    @Produces(MediaType.TEXT_HTML)
//    public Response getSymbolMappingPage() {
//        return Response.ok(new Viewable("symbol", this), TEXT_HTML).build();
//    }
    
    @GET
    @Path("mapping/symbol")
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
                return Response.ok(new Viewable("mapping_symbol", this), TEXT_HTML).build();
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
