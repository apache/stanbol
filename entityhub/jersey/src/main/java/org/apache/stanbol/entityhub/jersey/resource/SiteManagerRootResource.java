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

import static javax.ws.rs.core.MediaType.TEXT_HTML;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
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
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.programs.Program;
import org.apache.stanbol.commons.indexedgraph.IndexedGraph;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.commons.web.viewable.Viewable;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.ldpath.EntityhubLDPath;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.ldpath.query.LDPathSelect;
import org.apache.stanbol.entityhub.model.clerezza.RdfValueFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.apache.stanbol.entityhub.servicesapi.util.AdaptingIterator;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

/**
 * RDFTerm to provide a REST API for the {@link SiteManager}.
 * 
 * TODO: add description
 */
@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/entityhub/sites")
public class SiteManagerRootResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    private NamespacePrefixService nsPrefixService;
    
    @Reference
    private SiteManager referencedSiteManager;

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(Arrays.asList(N3, N_TRIPLE,
        RDF_XML, TURTLE, X_TURTLE, RDF_JSON));

    /**
     * The Field used for find requests if not specified TODO: Will be depreciated as soon as EntityQuery is
     * implemented
     */
    private static final String DEFAULT_FIND_FIELD = RDFS.label.getUnicodeString();

    /**
     * The default number of maximal results of searched sites.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;

    

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers);
        return res.build();
    }
    @OPTIONS
    @Path("/find")
    public Response handleCorsPreflightFind(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers);
        return res.build();
    }
    
    @OPTIONS
    @Path("/query")
    public Response handleCorsPreflightQuery(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers);
        return res.build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getSitesPage(@Context HttpHeaders headers) {
        ResponseBuilder rb =  Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

// removed to allow request with Accept headers other than text/html to return
// the JSON array
//    @GET
//    @Path("/referenced")
//    @Produces(MediaType.TEXT_HTML)
//    public Response getReferencedSitesPage() {
//        return Response.ok(new Viewable("referenced", this))
//        .header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8").build();
//    }
    
    /**
     * Getter for the id's of all referenced sites
     * 
     * @return the id's of all referenced sites.
     */
    @GET
    @Path(value = "/referenced")
    @Produces({MediaType.APPLICATION_JSON,MediaType.TEXT_HTML})
    public Response getReferencedSites(@Context UriInfo uriInfo,
                                        @Context HttpHeaders headers) {
        MediaType acceptable = getAcceptableMediaType(headers,
           Arrays.asList(MediaType.APPLICATION_JSON,MediaType.TEXT_HTML) ,
           MediaType.APPLICATION_JSON_TYPE);
        if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptable)){
            ResponseBuilder rb =  Response.ok(new Viewable("referenced", this));
            rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } else {
            JSONArray referencedSites = new JSONArray();
            for (String site : referencedSiteManager.getSiteIds()) {
                referencedSites.put(String.format("%sentityhub/site/%s/", uriInfo.getBaseUri(), site));
            }
            ResponseBuilder rb =  Response.ok(referencedSites.toString());
            rb.header(HttpHeaders.CONTENT_TYPE, acceptable+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        }
    }
    
    @OPTIONS
    @Path("/entity")
    public Response handleCorsPreflightEntity(@Context HttpHeaders headers){
        ResponseBuilder res = Response.ok();
        //enableCORS(servletContext, res, headers,OPTIONS,GET);
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
        log.debug("getSignById() request\n\t> id       : {}\n\t> accept   : {}\n\t> mediaType: {}",
            new Object[] {id, headers.getAcceptableMediaTypes(), headers.getMediaType()});

        Collection<String> supported = new HashSet<String>(JerseyUtils.ENTITY_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = getAcceptableMediaType(
            headers, supported, MediaType.APPLICATION_JSON_TYPE);
        if (id == null || id.isEmpty()) {
            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                ResponseBuilder rb =  Response.ok(new Viewable("entity", this));
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
                //addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } else {
                return Response.status(Status.BAD_REQUEST)
                    .entity("No or empty ID was parsed. Missing parameter id.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
        Entity sign = referencedSiteManager.getEntity(id);
        if (sign != null) {
            ResponseBuilder rb = Response.ok(sign);
            rb.header(HttpHeaders.CONTENT_TYPE, acceptedMediaType+"; charset=utf-8");
            //addCORSOrigin(servletContext, rb, headers);
            return rb.build();
        } else {
            // TODO: How to parse an ErrorMessage?
            // create an Response with the the Error?
            log.info("getSignById() entity {} not found on any referenced site");
            return Response.status(Status.NOT_FOUND)
                .entity("Entity with ID '"+id+"' not found an any referenced site\n")
                .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
        }
    }

//    @GET
//    @Path("/find")
//    @Produces(MediaType.TEXT_HTML)
//    public Response getFindPage() {
//        return Response.ok(new Viewable("find", this))
//        .header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8").build();
//    }
    
    @GET
    @Path("/find")
    public Response findEntityfromGet(@QueryParam(value = "name") String name,
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
                               @FormParam(value = "field") String parsedField,
                               @FormParam(value = "lang") String language,
                               // @FormParam(value="select") String select,
                               @FormParam(value = "limit") Integer limit,
                               @FormParam(value = "offset") Integer offset,
                               @FormParam(value = "ldpath") String ldpath,
                               @Context HttpHeaders headers) {
        log.debug("findEntity() Request");
        Collection<String> supported = new HashSet<String>(JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = getAcceptableMediaType(
            headers, supported, MediaType.APPLICATION_JSON_TYPE);
        if(name == null || name.isEmpty()){
            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                ResponseBuilder rb =  Response.ok(new Viewable("find", this));
                rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML+"; charset=utf-8");
                //addCORSOrigin(servletContext, rb, headers);
                return rb.build();
            } else {
                return Response.status(Status.BAD_REQUEST)
                    .entity("The name must not be null nor empty for find requests. Missing parameter name.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        }
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
        return executeQuery(referencedSiteManager, query, acceptedMediaType, headers);
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
     * @param query
     *            The field query in JSON format
     * @param headers
     *            the header information of the request
     * @return the results of the query
     */
    @POST
    @Path("/query")
    @Consumes( {MediaType.APPLICATION_JSON})
    public Response queryEntities(FieldQuery query,
                                  @Context HttpHeaders headers) {
        Collection<String> supported = new HashSet<String>(JerseyUtils.QUERY_RESULT_SUPPORTED_MEDIA_TYPES);
        supported.add(TEXT_HTML);
        final MediaType acceptedMediaType = getAcceptableMediaType(
            headers, supported, MediaType.APPLICATION_JSON_TYPE);
        if(query == null){
            //if query is null nd the mediaType is HTML we need to print the
            //Documentation of the RESTful API
            if(MediaType.TEXT_HTML_TYPE.isCompatible(acceptedMediaType)){
                return getQueryDocumentation(headers);        
            } else {
                return Response.status(Status.BAD_REQUEST)
                    .entity("The query must not be null nor empty for query requests. Missing parameter query.\n")
                    .header(HttpHeaders.ACCEPT, acceptedMediaType).build();
            }
        } else {
            return executeQuery(referencedSiteManager, query, acceptedMediaType, headers);
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
        return handleLDPathRequest(this,new SiteManagerBackend(referencedSiteManager), 
            ldpath, contexts, headers);
    }
    /**
     * Executes the query parsed by {@link #queryEntities(String, File, HttpHeaders)} or created based
     * {@link #findEntity(String, String, String, int, int, HttpHeaders)}
     * 
     * @param manager The {@link SiteManager}
     * @param query
     *            The query to execute
     * @param headers the request headers
     * @return the response (results of error)
     */
    private Response executeQuery(SiteManager manager,
                                  FieldQuery query, MediaType mediaType, 
                                  HttpHeaders headers) throws WebApplicationException {
        if(query instanceof LDPathSelect && ((LDPathSelect)query).getLDPathSelect() != null){
            //use the LDPath variant to process this query
            return executeLDPathQuery(manager, query, ((LDPathSelect)query).getLDPathSelect(),
                mediaType, headers);
        } else { //use the default query execution
            QueryResultList<Representation> result = manager.find(query);
            ResponseBuilder rb = Response.ok(result);
            rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
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
    private Response executeLDPathQuery(SiteManager manager,FieldQuery query, String ldpathProgramString, MediaType mediaType, HttpHeaders headers) {
        QueryResultList<Representation> result;
        ValueFactory vf = new RdfValueFactory(new IndexedGraph());
        SiteManagerBackend backend = new SiteManagerBackend(manager);
        EntityhubLDPath ldPath = new EntityhubLDPath(backend,vf);
        //copy the selected fields, because we might need to delete some during
        //the preparation phase
        Set<String> selectedFields = new HashSet<String>(query.getSelectedFields());
        //first prepare (only execute the query if the parameters are valid)
        Program<Object> program;
        try {
            program = prepareQueryLDPathProgram(ldpathProgramString, selectedFields, backend, ldPath);
        } catch (LDPathParseException e) {
            log.warn("Unable to parse LDPath program used as select for a Query to the '/sites' endpoint:");
            log.warn("FieldQuery: \n {}",query);
            log.warn("LDPath: \n {}",((LDPathSelect)query).getLDPathSelect());
            log.warn("Exception:",e);
            return Response.status(Status.BAD_REQUEST)
            .entity(("Unable to parse LDPath program (Messages: "+
                    getLDPathParseExceptionMessage(e)+")!\n"))
            .header(HttpHeaders.ACCEPT, mediaType).build();
        } catch (IllegalStateException e) {
            log.warn("parsed LDPath program is not compatible with the Query " +
            		"parsed to the '/sites' endpoint!",e);
            return Response.status(Status.BAD_REQUEST)
            .entity(e.getMessage())
            .header(HttpHeaders.ACCEPT, mediaType).build();
        }
        //2. execute the query
        // we need to adapt from Entity to Representation
        //TODO: should we add the metadata to the result?
        Iterator<Representation> resultIt = new AdaptingIterator<Entity,Representation>(manager.findEntities(query).iterator(),
            new AdaptingIterator.Adapter<Entity,Representation>() {
                @Override
                public Representation adapt(Entity value, Class<Representation> type) {
                    return value.getRepresentation();
                }},Representation.class);
        //process the results
        Collection<Representation> transformedResults = transformQueryResults(resultIt, program,
            selectedFields, ldPath, backend, vf);
        result = new QueryResultListImpl<Representation>(query, transformedResults, Representation.class);
        ResponseBuilder rb = Response.ok(result);
        rb.header(HttpHeaders.CONTENT_TYPE, mediaType+"; charset=utf-8");
        //addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }
}
