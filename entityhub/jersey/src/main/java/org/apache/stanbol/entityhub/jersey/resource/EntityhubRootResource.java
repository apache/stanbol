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
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.getAcceptableMediaType;
import static org.apache.stanbol.entityhub.jersey.utils.LDPathHelper.getLDPathParseExceptionMessage;
import static org.apache.stanbol.entityhub.jersey.utils.LDPathHelper.handleLDPathRequest;
import static org.apache.stanbol.entityhub.jersey.utils.LDPathHelper.prepareQueryLDPathProgram;
import static org.apache.stanbol.entityhub.jersey.utils.LDPathHelper.transformQueryResults;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.EntityhubBackend;
import org.apache.stanbol.entityhub.ldpath.backend.YardBackend;
import org.apache.stanbol.entityhub.ldpath.query.LDPathSelect;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
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
    
    @Reference
    private NamespacePrefixService nsPrefixService;
    
    @Reference
    private Entityhub entityhub;

    public EntityhubRootResource() {
        super();
    }
    
    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers);
        return res.build();
    }
    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @GET
    @Path("entity")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response getSymbol(@QueryParam("id") String symbolId, @Context HttpHeaders headers) throws WebApplicationException {
        log.info("GET /entity Request");
        log.info("  > id: " + symbolId);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        MediaType acceptedMediaType = getAcceptableMediaType(headers, 
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            APPLICATION_JSON_TYPE);
        if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE) && symbolId == null){
            //return HTML docu
            ResponseBuilder rb = Response.ok(new Viewable("entity", this));
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
        if (symbolId == null || symbolId.isEmpty()) {
            // TODO: how to parse an error message
            throw new WebApplicationException(BAD_REQUEST);
        }
        //Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, servletContext);
        Entity entity;
        try {
            entity = entityhub.getEntity(symbolId);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (entity == null) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            ResponseBuilder rb = Response.ok(entity);
            rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
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
        MediaType acceptedMediaType = getAcceptableMediaType(headers, 
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            APPLICATION_JSON_TYPE);
        if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE) && reference == null){
            //return docu
            ResponseBuilder rb = Response.ok(new Viewable("lookup", this));
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } else {
            if (reference == null || reference.isEmpty()) {
                // TODO: how to parse an error message
                throw new WebApplicationException(BAD_REQUEST);
            }
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
                ResponseBuilder rb = Response.ok(entity);
                rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
                //addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            }
        }
    }
    @OPTIONS
    @Path("entity/")
    public Response handleCorsPreflightEntity(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //we need also PUT and DELETE because /entity has full CRUD
        //enableCORS(servletContext, res, headers,GET,POST,PUT,DELETE,OPTIONS);
        return res.build();
    }

    @POST
    @Path("entity/")
    @Consumes(MediaType.WILDCARD)
    public Response createEntity(@QueryParam(value = "id") String id,
                                 @QueryParam(value = "update") boolean allowUpdate,
                                 Map<String,Representation> parsed,
                                 @Context HttpHeaders headers){
        //Set<Representation> representations = Collections.emptySet();
        //log.info("Test: "+test);
        log.info("Headers: "+headers.getRequestHeaders());
        log.info("Entity: "+id);
        log.info("Representations : "+parsed);
        return updateOrCreateEntity(id, parsed, HttpMethod.POST, true,allowUpdate,headers);
    }

    @PUT
    @Path("entity/")
    @Consumes(MediaType.WILDCARD)
    public Response updateEntity(@QueryParam(value = "id") String id, 
                                 @QueryParam(value = "create") @DefaultValue("true") boolean allowCreate,
                                 Map<String,Representation> parsed,
                                 @Context HttpHeaders headers){
        //Set<Representation> representations = Collections.emptySet();
        //log.info("Test: "+test);
        log.info("Headers: "+headers.getRequestHeaders());
        log.info("Entity: "+id);
        log.info("Representations : "+parsed);
        return updateOrCreateEntity(id, parsed, HttpMethod.PUT, allowCreate, true, headers);
    }
    
    @DELETE
    @Path("entity")
    public Response deleteEntity(@QueryParam(value="id") String id,
                                 @Context HttpHeaders headers){
        MediaType accepted = getAcceptableMediaType(headers,
            JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES, 
            MediaType.APPLICATION_JSON_TYPE);
        if(id == null || id.isEmpty()){
            return Response.status(Status.BAD_REQUEST).entity("The Request does" +
                    "not provide the id of the Entity to delete (parameter 'id').")
                    .header(HttpHeaders.ACCEPT, accepted).build();
        }
        //Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, servletContext);
        Entity entity;
        ResponseBuilder rb;
        try {
            if(id.equals("*")){
                log.info("Deleting all Entities form the Entityhub");
                entityhub.deleteAll();
                rb = Response.status(Response.Status.OK);
            } else {
                entity = entityhub.delete(id);
                if(entity == null){
                    rb = Response.status(Status.NOT_FOUND).entity("An Entity with the" +
                            "parsed id "+id+" is not managed by the Entityhub")
                            .header(HttpHeaders.ACCEPT, accepted);
                } else {
                    rb =  Response.ok(entity);
                    rb.header(HttpHeaders.CONTENT_TYPE, accepted+"; charset=utf-8")
                        .header(HttpHeaders.ACCEPT, accepted);
                }
            }
        } catch (EntityhubException e) {
            rb = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
            .header(HttpHeaders.ACCEPT, accepted);
        }
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
    /**
     * Implements the creation/update of Representations within the Entityhub.
     * @param id the id of the resource to create or update. If not 
     * <code>null</code> all parsed Representations with other
     * ids will be ignored.
     * @param parsed the parsed representation(s)
     * @param method the {@link HttpMethod} used by the reuqest. Needed to create
     * the correct response.
     * @param create allow to create new Entities
     * @param update allow to update existing Entities
     * @param headers the HTTP headers of the request
     * @return the created/updated representation as response
     */
    private Response updateOrCreateEntity(String id,Map<String,Representation> parsed, 
                                          String method,
                                          boolean create, 
                                          boolean update,
                                          HttpHeaders headers){
        //Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, servletContext);
        MediaType accepted = getAcceptableMediaType(headers,
            JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES, 
            MediaType.APPLICATION_JSON_TYPE);
        if(entityhub == null){
            return Response.status(Status.INTERNAL_SERVER_ERROR).
                entity("The Entityhub is currently unavailable.")
                .header(HttpHeaders.ACCEPT, accepted).build();
        }
        //(1) if an id is parsed we need to ignore all other representations
        if(id != null && !"*".equals(id)){
            Representation r = parsed.get(id);
            if(r == null){
                return Response.status(Status.BAD_REQUEST)
                .entity(String.format("Parsed RDF data do not contain any "
                    + "Information about the parsed id '%s'",id))
                    .header(HttpHeaders.ACCEPT, accepted).build();
            } else {
                parsed = Collections.singletonMap(id, r);
            }
        }
        //First check if all parsed Representation can be created/updated
        if(!(create && update)){ //if both create and update are enabled skip this
            long start = System.currentTimeMillis();
            log.debug("   ... validate parsed Representation state (create: {}| update: {})",
                create,update);
            for(Entry<String,Representation> entry : parsed.entrySet()){
                boolean exists;
                try {
                    exists = entityhub.isRepresentation(entry.getKey());
                } catch (EntityhubException e) {
                    log.error(String.format("Exception while checking the existance " +
                        "of an Entity with id  %s in the Entityhub.",
                        entry.getKey()),e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(String.format("Unable to process Entity %s because of" +
                            "an Error while checking the current version of that" +
                            "Entity within the Entityhub (Message: %s)",
                            entry.getKey(),e.getMessage()))
                            .header(HttpHeaders.ACCEPT, accepted).build();
                }
                if((exists && !update) || (!exists && !create)){
                    return Response.status(Status.BAD_REQUEST).entity(String.format(
                        "Unable to %s an Entity '%s' becuase it %s and request parameter '%s' is set. " +
                        " To allow both creating and updating of Entities you need to set "+
                        "'%s=true' in the request",
                        exists ? "update" : "create", entry.getKey(),
                        exists ? "exists " : "does not exist",
                        exists ? "update=false" : "create=false",
                        exists ? "update" : "create"))
                        .header(HttpHeaders.ACCEPT, accepted).build();
                }
            }
            log.debug("      > checked {} entities in {}ms",
                parsed.size(),System.currentTimeMillis()-start);
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
        for(Representation representation : parsed.values()){
            try {
                Entity entity = entityhub.store(representation);
                updated.put(entity.getId(), entity);
            }catch (EntityhubException e) {
                log.error(String.format("Exception while storing Entity %s" +
                        "in the Entityhub.",representation),e);
            }
        }
        //create the response for the Entity
        // for now directly return the added entity. one could also
        // consider returning a seeOther (303) with the get URI for the
        // created/updated entity
        if(updated.isEmpty()){
            // No (valid) data parsed
            ResponseBuilder rb = Response.status(Status.NOT_MODIFIED);
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } else {
            Entity entity = updated.values().iterator().next();
            if(method.equals(HttpMethod.POST)){
                ResponseBuilder rb = Response.created(uriInfo.getAbsolutePathBuilder()
                    .queryParam("id", "{entityId}")
                    .build(entity.getId()));
                //addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } else {
                //return Response.noContent().build();
                //As alternative return the modified entity
                ResponseBuilder rb =  Response.ok(entity);
                rb.header(HttpHeaders.CONTENT_TYPE, accepted+"; charset=utf-8");
                //addCORSOrigin(servletContext, rb, headers);
                return rb.build();
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
    @OPTIONS
    @Path("/find")
    public Response handleCorsPreflightFind(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers);
        return res.build();
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
                                    @QueryParam(value = "ldpath") String ldpath,
                                    @Context HttpHeaders headers) {
        return findEntity(name, field, language, limit, offset, select, ldpath, headers);
    }
    
    @POST
    @Path("/find")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE, TEXT_HTML})
    public Response findEntity(@FormParam(value = "name") String name,
                               @FormParam(value = "field") String parsedField,
                               @FormParam(value = "lang") String language,
                               @FormParam(value = "limit") Integer limit,
                               @FormParam(value = "offset") Integer offset,
                               // TODO: Jersey supports parsing multiple values in Collections.
                               // Use this feature here instead of using this hand crafted
                               // solution!
                               @FormParam(value = "select") String select,
                               @FormParam(value = "ldpath") String ldpath,
                               @Context HttpHeaders headers) {
        log.debug("/find Request");
        final MediaType acceptedMediaType = getAcceptableMediaType(headers,
            ENTITY_SUPPORTED_MEDIA_TYPE_INCL_HTML,
            MediaType.APPLICATION_JSON_TYPE);
        if(name == null || name.isEmpty()){
            if(acceptedMediaType.isCompatible(TEXT_HTML_TYPE)){
                //return HTML docu
                ResponseBuilder rb = Response.ok(new Viewable("find", this));
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
                //addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } else {
                return Response.status(Status.BAD_REQUEST)
                    .entity("The name must not be null nor empty for find requests. Missing parameter name.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        } else {
            final String property;
            if (parsedField == null) {
                property = DEFAULT_FIND_FIELD;
            } else {
                parsedField = parsedField.trim();
                if (parsedField.isEmpty()) {
                    property = DEFAULT_FIND_FIELD;
                } else {
                    property = nsPrefixService.getFullName(parsedField);
                    if(property == null){
                        String messsage = String.format("The prefix '%s' of the parsed field '%' is not "
                            + "mapped to any namespace. Please parse the full URI instead!\n",
                            NamespaceMappingUtils.getPrefix(parsedField),parsedField);
                        return Response.status(Status.BAD_REQUEST)
                                .entity(messsage)
                                .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
                    }
                }
            }
            FieldQuery query = JerseyUtils.createFieldQueryForFindRequest(name, property, language,
                limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, offset,ldpath);
            
            // For the Entityhub we support to select additional fields for results
            // of find requests. For the Sites and {site} endpoint this is currently
            // deactivated because of very bad performance with OPTIONAL graph patterns
            // in SPARQL queries.
            Collection<String> additionalSelectedFields = new ArrayList<String>();
            if (select != null && !select.isEmpty()) {
                for (String selected : select.trim().split(" ")) {
                    if (selected != null && !selected.isEmpty()) {
                        additionalSelectedFields.add(selected);
                    }
                }
            }
            query.addSelectedFields(additionalSelectedFields);
            return executeQuery(query, headers, acceptedMediaType);
        }
    }

    @OPTIONS
    @Path("/query")
    public Response handleCorsPreflightQuery(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers);
        return res.build();
    }
    
    @GET
    @Path("/query")
    public Response getQueryDocumentation(@Context HttpHeaders headers){
        ResponseBuilder rb = Response.ok(new Viewable("query", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
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
        final MediaType acceptedMediaType = getAcceptableMediaType(headers,
            JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES,
            MediaType.APPLICATION_JSON_TYPE);
        if(query == null && MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
            return getQueryDocumentation(headers);
        }
        return executeQuery(query, headers, acceptedMediaType);
    }
    
    /**
     * Executes the query parsed by {@link #queryEntities(String, File, HttpHeaders)}
     * or created based {@link #findEntity(String, String, String, String, HttpHeaders)
     * @param query The query to execute
     * @param headers The headers used to determine the media types
     * @return the response (results of error)
     */
    private Response executeQuery(FieldQuery query, HttpHeaders headers, MediaType acceptedMediaType) throws WebApplicationException {
        //Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, servletContext);
        if(query instanceof LDPathSelect && ((LDPathSelect)query).getLDPathSelect() != null){
            //use the LDPath variant to process this query
            return executeLDPathQuery(entityhub,query, ((LDPathSelect)query).getLDPathSelect(),
                acceptedMediaType, headers);
        } else { //use the default query execution
            QueryResultList<Representation> result;
            try {
                result = entityhub.find(query);
            } catch (EntityhubException e) {
                String message = String.format("Exception while performing the " +
                		"FieldQuery on the EntityHub (message: %s)", e.getMessage());
                log.error(message, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(message)
                .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
            ResponseBuilder rb = Response.ok(result);
            rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }
    /**
     * Execute a Query that uses LDPath to process results.
     * @param query the query
     * @param mediaType the mediaType for the response
     * @param headers the http headers of the request
     * @return the response
     */
    private Response executeLDPathQuery(Entityhub entityhub,FieldQuery query, String ldpathProgramString, MediaType mediaType, HttpHeaders headers) {
        QueryResultList<Representation> result;
        ValueFactory vf = new RdfValueFactory(new IndexedGraph());
        EntityhubBackend backend = new EntityhubBackend(entityhub);
        EntityhubLDPath ldPath = new EntityhubLDPath(backend,vf);
        //copy the selected fields, because we might need to delete some during
        //the preparation phase
        Set<String> selectedFields = new HashSet<String>(query.getSelectedFields());
        //first prepare (only execute the query if the parameters are valid)
        Program<Object> program;
        try {
            program = prepareQueryLDPathProgram(ldpathProgramString, selectedFields, backend, ldPath);
        } catch (LDPathParseException e) {
            log.warn("Unable to parse LDPath program used as select for Query:");
            log.warn("FieldQuery: \n {}",query);
            log.warn("LDPath: \n {}",((LDPathSelect)query).getLDPathSelect());
            log.warn("Exception:",e);
            return Response.status(Status.BAD_REQUEST)
            .entity(("Unable to parse LDPath program (Messages: "+
                    getLDPathParseExceptionMessage(e)+")!\n"))
            .header(HttpHeaders.ACCEPT, mediaType).build();
        } catch (IllegalStateException e) {
            log.warn("parsed LDPath program is not compatible with parsed Query!",e);
            return Response.status(Status.BAD_REQUEST)
            .entity(e.getMessage())
            .header(HttpHeaders.ACCEPT, mediaType).build();
        }
        //2. execute the query
        Iterator<Representation> resultIt;
        try { // go directly to the yard and query there for Representations
            resultIt = entityhub.getYard().findRepresentation(query).iterator();
        } catch (EntityhubException e) {
            String message = String.format("Exception while performing the " +
                "FieldQuery on the EntityHub (message: %s)", e.getMessage());
            log.error(message, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(message)
            .header(HttpHeaders.ACCEPT, mediaType).build();
        }
        //process the results
        Collection<Representation> transformedResults = transformQueryResults(resultIt, program,
            selectedFields, ldPath, backend, vf);
        result = new QueryResultListImpl<Representation>(query, transformedResults, Representation.class);
        ResponseBuilder rb = Response.ok(result);
        rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }    
    /*--------------------------------------------------------------------------
     * Methods for EntityMappings
     *--------------------------------------------------------------------------
     */
    @OPTIONS
    @Path("/mapping")
    public Response handleCorsPreflightMapping(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers,GET,OPTIONS);
        return res.build();
    }
    
    @GET
    @Path("mapping")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE,TEXT_HTML})
    public Response getMapping(@QueryParam("id") String reference, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("get mapping for request > id : {} > accept: {}",
            reference, headers.getAcceptableMediaTypes());
        Set<String> supported = new HashSet<String>(JerseyUtils.REPRESENTATION_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        MediaType acceptedMediaType = getAcceptableMediaType(
            headers,supported, APPLICATION_JSON_TYPE);
        
        if (reference == null || reference.isEmpty()) {
            //if HTML -> print the docu of the restfull service
            if(TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
              ResponseBuilder rb = Response.ok(new Viewable("mapping", this));
              rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
              //addCORSOrigin(servletContext, rb, headers);
            } else {
                return Response.status(Status.BAD_REQUEST).entity("The mapping id (URI) is missing.\n").header(
                    HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        //Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, servletContext);
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
            ResponseBuilder rb = Response.ok(mapping);
            rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }

    @OPTIONS
    @Path("/mapping/entity")
    public Response handleCorsPreflightMappingEntity(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers,GET,OPTIONS);
        return res.build();
    }

    @GET
    @Path("mapping/entity")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE,TEXT_HTML})
    public Response getEntityMapping(@QueryParam("id") String entity, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("getEntityMapping() POST Request > entity: {} > accept: {}",
            entity, headers.getAcceptableMediaTypes());
        
        Set<String> supported = new HashSet<String>(JerseyUtils.REPRESENTATION_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        MediaType acceptedMediaType = getAcceptableMediaType(
            headers,supported, APPLICATION_JSON_TYPE);

        if (entity == null || entity.isEmpty()) {
            //if HTML -> print the docu of the restfull service
            if(TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                ResponseBuilder rb = Response.ok(new Viewable("mapping_entity", this));
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
                //addCORSOrigin(servletContext, rb, headers);
            } else {
                return Response.status(Status.BAD_REQUEST).entity("No entity given. Missing parameter id.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        
        //Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, servletContext);
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
            ResponseBuilder rb = Response.ok(mapping);
            rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }

    @OPTIONS
    @Path("/mapping/symbol")
    public Response handleCorsPreflightMappingSymbol(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers,GET,OPTIONS);
        return res.build();
    }
    
    @GET
    @Path("mapping/symbol")
    @Produces( {APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE,TEXT_HTML})
    public Response getSymbolMappings(@QueryParam("id") String symbol, @Context HttpHeaders headers)
                                                                            throws WebApplicationException {
        log.debug("getSymbolMappings() POST Request > symbol: {} > accept: {}",
            symbol, headers.getAcceptableMediaTypes());
        
        Set<String> supported = new HashSet<String>(JerseyUtils.REPRESENTATION_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        MediaType acceptedMediaType = getAcceptableMediaType(
            headers,supported, APPLICATION_JSON_TYPE);

        if (symbol == null || symbol.isEmpty()) {
            //if HTML -> print the docu of the restfull service
            if(TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                ResponseBuilder rb = Response.ok(new Viewable("mapping_symbol", this));
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
                //addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } else {
                return Response.status(Status.BAD_REQUEST).entity("No symbol given. Missing parameter id.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        //Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, servletContext);
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
            ResponseBuilder rb = Response.ok(mappingResultList);
            rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }
    /*
     * LDPath support
     */
    @OPTIONS
    @Path("/ldpath")
    public Response handleCorsPreflightLDPath(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers,OPTIONS,GET,POST);
        return res.build();
    }
    @GET
    @Path("/ldpath")
    public Response handleLDPathGet(
            @QueryParam(value = "context")Set<String> contexts,
            @QueryParam(value = "ldpath")String ldpath,
            @Context HttpHeaders headers){
        return handleLDPathPost(contexts, ldpath, headers);
    }
    @POST
    @Path("/ldpath")
    public Response handleLDPathPost(
             @FormParam(value = "context")Set<String> contexts,
             @FormParam(value = "ldpath")String ldpath,
             @Context HttpHeaders headers){
        //Entityhub entityhub = ContextHelper.getServiceFromContext(Entityhub.class, servletContext);
        return handleLDPathRequest(this,new YardBackend(entityhub.getYard()), 
            ldpath, contexts, headers);
    }

}
