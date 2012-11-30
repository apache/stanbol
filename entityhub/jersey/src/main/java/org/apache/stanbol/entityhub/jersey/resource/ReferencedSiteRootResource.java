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

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;
import static org.apache.stanbol.commons.web.base.utils.MediaTypeUtil.getAcceptableMediaType;
import static org.apache.stanbol.entityhub.jersey.utils.JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES;
import static org.apache.stanbol.entityhub.jersey.utils.JerseyUtils.REPRESENTATION_SUPPORTED_MEDIA_TYPES;
import static org.apache.stanbol.entityhub.jersey.utils.JerseyUtils.createFieldQueryForFindRequest;
import static org.apache.stanbol.entityhub.jersey.utils.LDPathHelper.getLDPathParseExceptionMessage;
import static org.apache.stanbol.entityhub.jersey.utils.LDPathHelper.handleLDPathRequest;
import static org.apache.stanbol.entityhub.jersey.utils.LDPathHelper.prepareQueryLDPathProgram;
import static org.apache.stanbol.entityhub.jersey.utils.LDPathHelper.transformQueryResults;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.stanbol.commons.indexedgraph.IndexedMGraph;
import org.apache.stanbol.commons.viewable.Viewable;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.jersey.parsers.FieldQueryReader;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SiteBackend;
import org.apache.stanbol.entityhub.ldpath.query.LDPathSelect;
import org.apache.stanbol.entityhub.model.clerezza.RdfRepresentation;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.License;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSiteException;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.newmedialab.ldpath.exception.LDPathParseException;
import at.newmedialab.ldpath.model.programs.Program;

import com.hp.hpl.jena.reasoner.rulesys.builtins.GE;

/**
 * Resource to provide a REST API for the {@link SiteManager}
 * <p/>
 * TODO: add description
 */
@Path("/entityhub/site/{site}")
public class ReferencedSiteRootResource extends BaseStanbolResource {
    

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(Arrays.asList(SupportedFormat.N3,
        SupportedFormat.N_TRIPLE, SupportedFormat.RDF_XML, SupportedFormat.TURTLE, SupportedFormat.X_TURTLE,
        SupportedFormat.RDF_JSON));
    /**
     * The relative path used to publish the license.
     */
    public static final String LICENSE_PATH = "license";
    /**
     * The name of the resource used for Licenses of no {@link License#getUrl()} 
     * is present
     */
    private static final String LICENSE_NAME = "LICENSE";
    
    /**
     * The Field used for find requests if not specified TODO: This will be replaced by the EntitySearch. With
     * this search the Site is responsible to decide what properties to use for label based searches.
     */
    private static final String DEFAULT_FIND_FIELD = RDFS.label.getUnicodeString();
    
    /**
     * The Field used as default as selected fields for find requests TODO: Make configurable via the
     * {@link ConfiguredSite} interface! NOTE: This feature is deactivated, because OPTIONAL selects do have
     * very weak performance when using SPARQL endpoints
     */
    // private static final Collection<String> DEFAULT_FIND_SELECTED_FIELDS =
    // Arrays.asList(RDFS.comment.getUnicodeString());
    
    /**
     * The default number of maximal results.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;
    
    private Site site;
    
    public ReferencedSiteRootResource(@PathParam(value = "site") String siteId,
                                      @Context ServletContext servletContext) {
        super();
        log.debug("<init> with site {}", siteId);
        SiteManager referencedSiteManager = ContextHelper.getServiceFromContext(
            SiteManager.class, servletContext);
        if (siteId == null || siteId.isEmpty()) {
            log.error("Missing path parameter site={}", siteId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        site = referencedSiteManager.getSite(siteId);
        if (site == null) {
            log.error("Site {} not found (no referenced site with that ID is present within the Entityhub",
                siteId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        if(site instanceof ManagedSite){
            log.debug("   ... init ManagedSite");
        }
    }

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }
    
    @GET
    @Produces(value=MediaType.TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers){
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
    /**
     * Provides metadata about this referenced site as representation
     * @param headers the request headers used to get the requested {@link MediaType}
     * @param uriInfo used to get the URI of the current request
     * @return the response
     */
    @GET
    @Produces({APPLICATION_JSON,RDF_XML,N3,TURTLE,X_TURTLE,RDF_JSON,N_TRIPLE})
    public Response getInfo(@Context HttpHeaders headers,
                            @Context UriInfo uriInfo) {
        MediaType acceptedMediaType = getAcceptableMediaType(headers, REPRESENTATION_SUPPORTED_MEDIA_TYPES,MediaType.APPLICATION_JSON_TYPE);
        ResponseBuilder rb =  Response.ok(site2Representation(uriInfo.getAbsolutePath().toString()));
        rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
    @GET
    @Path(value=ReferencedSiteRootResource.LICENSE_PATH+"/{name}")
    public Response getLicenseInfo(@Context HttpHeaders headers,
                                   @Context UriInfo uriInfo,
                                   @PathParam(value = "name") String name) {
        MediaType acceptedMediaType = getAcceptableMediaType(headers, MediaType.APPLICATION_JSON_TYPE);
        if(name == null || name.isEmpty()){
            //return all
        } else if(name.startsWith(LICENSE_NAME)){
            try {
                String numberString = name.substring(LICENSE_NAME.length());
                if(numberString.isEmpty()){
                    numberString = "0";
                }
                int count = -1; //license0 is the first one
                if(site.getConfiguration().getLicenses() != null){
                    for(License license : site.getConfiguration().getLicenses()){
                        if(license.getUrl() == null){
                            count++;
                        }
                        if(Integer.toString(count).equals(numberString)){
                            ResponseBuilder rb = Response.ok(
                                license2Representation(uriInfo.getAbsolutePath().toString(),license));
                            rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
                            addCORSOrigin(servletContext, rb, headers);
                            return rb.build();
                        }
                    }
                }
            }catch (NumberFormatException e) {
                return Response.status(Status.NOT_FOUND).
                entity("No License found.\n")
                .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
    @OPTIONS
    @Path("/entity")
    public Response handleCorsPreflightEntity(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        if(site instanceof ManagedSite){
            enableCORS(servletContext, res, headers, OPTIONS,GET,POST,PUT,DELETE);
        } else {
            enableCORS(servletContext, res, headers,OPTIONS,GET);
        }
        return res.build();
    }
    
    /**
     * Cool URI handler for Signs.
     * 
     * @param id
     *            The id of the entity (required)
     * @param headers
     *            the request headers used to get the requested {@link MediaType}
     * @return a redirection to either a browser view, the RDF meta data or the raw binary content
     */
    @GET
    @Path("/entity")
    public Response getEntityById(@QueryParam(value = "id") String id, @Context HttpHeaders headers) {
        log.debug("site/{}/entity Request",site.getId());
        log.debug("  > id       : " + id);
        log.debug("  > accept   : " + headers.getAcceptableMediaTypes());
        log.debug("  > mediaType: " + headers.getMediaType());
        Collection<String> supported = new HashSet<String>(JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = getAcceptableMediaType(headers,
            supported, MediaType.APPLICATION_JSON_TYPE);
        if (id == null || id.isEmpty()) {
            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                ResponseBuilder rb = Response.ok(new Viewable("entity", this));
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } else {
                return Response.status(Status.BAD_REQUEST)
                    .entity("No or empty ID was parsed. Missing parameter id.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        log.debug("handle Request for Entity {} of Site {}", id, site.getId());
        Entity entity;
        try {
            entity = site.getEntity(id);
        } catch (SiteException e) {
            log.error("ReferencedSiteException while accessing Site " + site.getConfiguration().getName() + 
                " (id=" + site.getId() + ")", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        if (entity != null) {
            ResponseBuilder rb =  Response.ok(entity);
            rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
            addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } else {
            // TODO: How to parse an ErrorMessage?
            // create an Response with the the Error?
            log.debug(" ... Entity {} not found on referenced site {}", 
                id, site.getId());
            return Response.status(Status.NOT_FOUND).
            entity("Entity '"+id+"' not found on referenced site '"+site.getId()+"'\n")
            .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        }
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
        log.info("Representations : {} parsed",parsed.size());
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
        log.info("Representations : {} parsed", parsed.size());
        return updateOrCreateEntity(id, parsed, HttpMethod.PUT, allowCreate, true, headers);
    }
    
    private Response updateOrCreateEntity(String id,
                                          Map<String,Representation> parsed,
                                          String requestMethod,
                                          boolean create,
                                          boolean update,
                                          HttpHeaders headers) {
        long start = System.currentTimeMillis();
        MediaType accepted = getAcceptableMediaType(headers,
            JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES, 
            MediaType.APPLICATION_JSON_TYPE);
        ManagedSite managedSite;
        if(site instanceof ManagedSite){
            managedSite = (ManagedSite)site;
        } else {
            ResponseBuilder builder =  Response.status(Status.FORBIDDEN).entity(
                String.format("The Site '%s' is not managed and does not support "
                    +"create/update nor delete operations",site.getId()))
            .header(HttpHeaders.ACCEPT, accepted);
            addCORSOrigin(servletContext, builder, headers);
            return builder.build();
        }
        //(1) if an id is parsed we need to ignore all other representations
        if(id != null && !"*".equals(id)){
            Representation r = parsed.get(id);
            if(r == null){
                ResponseBuilder builder = Response.status(Status.BAD_REQUEST)
                .entity(String.format("Parsed RDF data do not contain any "
                    + "Information about the parsed id '%s'",id))
                    .header(HttpHeaders.ACCEPT, accepted);
                addCORSOrigin(servletContext, builder, headers);
                return builder.build();
            } else {
                parsed = Collections.singletonMap(id, r);
            }
        }
        //First check if all parsed Representation can be created/updated
        if(!(create && update)){ //if both create and update are enabled skip this
            log.debug("   ... validate parsed Representation state (create: {}| update: {})",
                create,update);
            for(Entry<String,Representation> entry : parsed.entrySet()){
                boolean exists;
                try {
                    exists = managedSite.getEntity(entry.getKey()) != null;
                } catch (SiteException e) {
                    log.error(String.format("Exception while checking the existance " +
                        "of an Entity with id  %s in the Entityhub.",
                        entry.getKey()),e);
                    ResponseBuilder builder =  Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(String.format("Unable to process Entity %s because of" +
                            "an Error while checking the current version of that" +
                            "Entity within the Entityhub (Message: %s)",
                            entry.getKey(),e.getMessage()))
                            .header(HttpHeaders.ACCEPT, accepted);
                    addCORSOrigin(servletContext, builder, headers);
                    return builder.build();
                }
                if((exists && !update) || (!exists && !create)){
                    ResponseBuilder builder = Response.status(Status.BAD_REQUEST).entity(String.format(
                        "Unable to %s an Entity %s becuase it %s and %s is deactivated. " +
                        " You might want to set the '%s' parameter to TRUE in your Request",
                        exists ? "update" : "create", entry.getKey(),
                        exists ? "does already exists " : "does not",
                        exists ? "updateing existing" : "creating new",
                        exists ? "does already" : "does not exists",
                        exists ? "update" : "create"))
                        .header(HttpHeaders.ACCEPT, accepted);
                    addCORSOrigin(servletContext, builder, headers);
                    return builder.build();

                }
            }
        }
        long validateCompleted = System.currentTimeMillis();
        log.info("   ... validate request data {}ms",
            validateCompleted-start);
        try {
            managedSite.store(parsed.values());
        } catch (ManagedSiteException e) {
            log.error(String.format("Exception while storing parsed Representations "
                + "in the ManagedSite %s",managedSite.getId()),e);
            ResponseBuilder builder = Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity("Unable to store parsed Entities to ManagedSite "
                + managedSite.getId() +" because of an error (Message: "
                + e.getMessage()+")")
            .header(HttpHeaders.ACCEPT, accepted);
            addCORSOrigin(servletContext, builder, headers);
            return builder.build();
        }
        ResponseBuilder builder;
        if(create && parsed.size() == 1){
            String createdId =  parsed.keySet().iterator().next();
            URI created = uriInfo.getRequestUriBuilder().queryParam("id",createdId).build();
            builder = Response.created(created);
            builder.header(HttpHeaders.ACCEPT, accepted);
        } else {
            builder = Response.noContent();
        }
        log.info("   ... create/update {} entities in {}ms",
            parsed.size(),System.currentTimeMillis()-validateCompleted);
        addCORSOrigin(servletContext, builder, headers);
        return builder.build();
    }

    @DELETE
    @Path("entity/")
    public Response deleteEntity(@QueryParam(value="id") String id,
                                 @Context HttpHeaders headers){
        MediaType accepted = getAcceptableMediaType(headers,
            JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES, 
            MediaType.APPLICATION_JSON_TYPE);
        ManagedSite managedSite;
        if(site instanceof ManagedSite){
            managedSite = (ManagedSite)site;
        } else {
            ResponseBuilder builder =  Response.status(Status.FORBIDDEN).entity(
                String.format("The Site '%s' is not managed and does not support "
                    +"create/update nor delete operations",site.getId()))
            .header(HttpHeaders.ACCEPT, accepted);
            addCORSOrigin(servletContext, builder, headers);
            return builder.build();
        }
        if(id == null || id.isEmpty()){
            ResponseBuilder builder =  Response.status(Status.BAD_REQUEST).entity("The Request does" +
                    "not provide the id of the Entity to delete (parameter 'id').")
                    .header(HttpHeaders.ACCEPT, accepted);
            addCORSOrigin(servletContext, builder, headers);
            return builder.build();
        }
        ResponseBuilder builder;
        try {
            if("*".equals(id)){
                managedSite.deleteAll();
                builder = Response.noContent();
            } else if(managedSite.getEntity(id) != null){
                managedSite.delete(id);
                builder = Response.noContent();
            } else {
                builder = Response.status(Status.NOT_FOUND).entity(
                    "No Entity with the parsed Id '"+id+"' is present on the ManagedSite '"
                    + managedSite.getId()+"'!")
                .header(HttpHeaders.ACCEPT, accepted);
            }
        } catch (SiteException e) {
            String message = "Exception while deleting '"+id+"' from ManagedSite '"
                    + managedSite.getId()+"'!";
            log.error(message,e);
            builder = Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(message + ' '+ e.getClass().getSimpleName()+": "+ e.getMessage())
            .header(HttpHeaders.ACCEPT, accepted);
        }
        addCORSOrigin(servletContext, builder, headers);
        return builder.build();
    }    
    @OPTIONS
    @Path("/find")
    public Response handleCorsPreflightFind(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }
    
    @GET
    @Path("/find")
    public Response findEntitybyGet(@QueryParam(value = "name") String name,
                                    @QueryParam(value = "field") String field,
                                    @QueryParam(value = "lang") String language,
                                    // @QueryParam(value="select") String select,
                                    @QueryParam(value = "limit") @DefaultValue(value = "-1") int limit,
                                    @QueryParam(value = "offset") @DefaultValue(value = "0") int offset,
                                    @QueryParam(value = "ldpath") String ldpath,
                                    @Context HttpHeaders headers) {
        return findEntity(name, field, language, limit, offset, ldpath, headers);
    }
    
    @POST
    @Path("/find")
    public Response findEntity(@FormParam(value = "name") String name,
                               @FormParam(value = "field") String field,
                               @FormParam(value = "lang") String language,
                               // @FormParam(value="select") String select,
                               @FormParam(value = "limit") Integer limit,
                               @FormParam(value = "offset") Integer offset,
                               @FormParam(value = "ldpath") String ldpath,
                               @Context HttpHeaders headers) {
        log.debug("site/{}/find Request",site.getId());
        Collection<String> supported = new HashSet<String>(JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = getAcceptableMediaType(
            headers, supported, MediaType.APPLICATION_JSON_TYPE);
        if(name == null || name.isEmpty()){
            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                ResponseBuilder rb = Response.ok(new Viewable("find", this));
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
                addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } else {
                return Response.status(Status.BAD_REQUEST)
                    .entity("The name must not be null nor empty for find requests. Missing parameter name.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        if (field == null) {
            field = DEFAULT_FIND_FIELD;
        } else {
            field = field.trim();
            if (field.isEmpty()) {
                field = DEFAULT_FIND_FIELD;
            }
        }
        return executeQuery(createFieldQueryForFindRequest(
                name, field, language,
                limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, 
                offset,ldpath),
            headers);
    }
    
    @OPTIONS
    @Path("/query")
    public Response handleCorsPreflightQuery(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }
    /**
     * Allows to parse any kind of {@link FieldQuery} in its JSON Representation. 
     * Note that the maximum number of results (limit) and the offset of the 
     * <p>
     * TODO: as soon as the entityhub supports multiple query types this need to be refactored. The idea is
     * that this dynamically detects query types and than redirects them to the referenced site
     * implementation.
     * @param query The field query as parsed by {@link FieldQueryReader}
     * @param headers the header information of the request
     * @return the results of the query
     */
    @POST
    @Path("/query")
    @Consumes( {APPLICATION_JSON})
    public Response queryEntities(FieldQuery query,
                                  @Context HttpHeaders headers) {
        return executeQuery(query,headers);
    }
    @GET
    @Path("/query")
    @Produces(TEXT_HTML)
    public Response getQueryDocumentation(@Context HttpHeaders headers){
        ResponseBuilder rb = Response.ok(new Viewable("query", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
    
    /**
     * Executes the query parsed by {@link #queryEntities(String, File, HttpHeaders)} or created based
     * {@link #findEntity(String, String, String, int, int, HttpHeaders)}
     * 
     * @param query
     *            The query to execute
     * @param headers the request headers
     * @return the response (results of error)
     */
    private Response executeQuery(FieldQuery query, HttpHeaders headers) throws WebApplicationException {
        MediaType mediaType = getAcceptableMediaType(headers, ENTITY_SUPPORTED_MEDIA_TYPES, 
            APPLICATION_JSON_TYPE);
        if(query instanceof LDPathSelect && ((LDPathSelect)query).getLDPathSelect() != null){
            //use the LDPath variant to process this query
            return executeLDPathQuery(query, ((LDPathSelect)query).getLDPathSelect(),
                mediaType, headers);
        } else { //use the default query execution
            QueryResultList<Representation> result;
            try {
                result = site.find(query);
            } catch (SiteException e) {
                String message = String.format("Unable to Query Site '%s' (message: %s)",
                    site.getId(),e.getMessage());
                log.error(message, e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(message)
                .header(HttpHeaders.ACCEPT, mediaType).build();
            }
            ResponseBuilder rb = Response.ok(result);
            rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
            addCORSOrigin(servletContext, rb, headers);
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
    private Response executeLDPathQuery(FieldQuery query, String ldpathProgramString, MediaType mediaType, HttpHeaders headers) {
        QueryResultList<Representation> result;
        ValueFactory vf = new RdfValueFactory(new IndexedMGraph());
        SiteBackend backend = new SiteBackend(site,vf);
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
        try { // we need to adapt from Entity to Representation
            resultIt = new AdaptingIterator<Entity,Representation>(site.findEntities(query).iterator(),
                    new AdaptingIterator.Adapter<Entity,Representation>() {
                        @Override
                        public Representation adapt(Entity value, Class<Representation> type) {
                            return value.getRepresentation();
                        }},Representation.class);
        } catch (SiteException e) {
            String message = String.format("Unable to Query Site '%s' (message: %s)",
                site.getId(),e.getMessage());
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
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }


    /*
     * LDPath support
     */
    @OPTIONS
    @Path("/ldpath")
    public Response handleCorsPreflightLDPath(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers,OPTIONS,GET,POST);
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
        return handleLDPathRequest(this,new SiteBackend(site), 
            ldpath, contexts, headers, servletContext);
    }
    
    /*
     * Referenced Site Metadata
     */
    /**
     * Transforms a site to a Representation that can be serialised 
     * @param context
     * @return
     */
    private Representation site2Representation(String id){
        RdfValueFactory valueFactory = RdfValueFactory.getInstance();
        RdfRepresentation rep = valueFactory.createRepresentation(id);
        String namespace = NamespaceEnum.entityhub.getNamespace();
        rep.add(namespace+"localMode", site.supportsLocalMode());
        rep.add(namespace+"supportsSearch", site.supportsSearch());
        SiteConfiguration config = site.getConfiguration();
        rep.add(NamespaceEnum.rdfs+"label", config.getName());
        rep.add(NamespaceEnum.rdf+"type", valueFactory.createReference(namespace+"ReferencedSite"));
        if(config.getDescription() != null){
            rep.add(NamespaceEnum.rdfs+"description", config.getDescription());
        }
        if(config.getAttribution() != null){
            rep.add(NamespaceEnum.cc.getNamespace()+"attributionName", config.getAttribution());
        }
        if(config.getAttributionUrl() != null){
            rep.add(NamespaceEnum.cc.getNamespace()+"attributionURL", config.getAttributionUrl());
        }
        //add the licenses
        if(config.getLicenses() != null){
            int count = 0;
            for(License license : config.getLicenses()){
                String licenseUrl;
                if(license.getUrl() != null){
                    licenseUrl = license.getUrl();
                } else {
                    
                    licenseUrl = id+(!id.endsWith("/")?"/":"")+
                        LICENSE_PATH+'/'+LICENSE_NAME+(count>0?count:"");
                    count++;
                }
                //if defined add the name to dc:license
                if(license.getName() != null){
                    rep.add(NamespaceEnum.dcTerms.getNamespace()+"license", licenseUrl);
                }
                //link to the license via cc:license
                rep.add(NamespaceEnum.cc.getNamespace()+"license", licenseUrl);
            }
        }
        if(config.getEntityPrefixes() != null){
            for(String prefix : config.getEntityPrefixes()){
                rep.add(namespace+"entityPrefix", prefix);
            }
        } else { //all entities are allowed/processed
            rep.add(namespace+"entityPrefix", "*");
        }
        if(config instanceof ReferencedSiteConfiguration){
            ReferencedSiteConfiguration refConfig = (ReferencedSiteConfiguration)config;
            if(refConfig.getCacheStrategy() != null){
                rep.add(namespace+"cacheStrategy", valueFactory.createReference(namespace+"cacheStrategy-"+refConfig.getCacheStrategy().name()));
            }
            //add the accessUri and queryUri
            if(refConfig.getAccessUri() != null){
                rep.add(namespace+"accessUri", valueFactory.createReference(refConfig.getAccessUri()));
            }
            if(refConfig.getQueryUri() != null){
                rep.add(namespace+"queryUri", valueFactory.createReference(refConfig.getQueryUri()));
            }
        }
        return rep;
    }
    private Representation license2Representation(String id, License license) {
        RdfValueFactory valueFactory = RdfValueFactory.getInstance();
        RdfRepresentation rep = valueFactory.createRepresentation(id);
        
        if(license.getName() != null){
            rep.add(NamespaceEnum.dcTerms.getNamespace()+"license", license.getName());
            rep.add(NamespaceEnum.rdfs+"label", license.getName());
            rep.add(NamespaceEnum.dcTerms+"title", license.getName());
        }
        if(license.getText() != null){
            rep.add(NamespaceEnum.rdfs+"description", license.getText());
            
        }
        rep.add(NamespaceEnum.cc.getNamespace()+"licenseUrl", 
            license.getUrl() == null ? id:license.getUrl());
        return rep;
    }
    
    public boolean isManagedSite(){
        return site instanceof ManagedSite;
    }
    public Site getSite(){
        return site;
    }
}
