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

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.site.ConfiguredSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteException;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Resource to provide a REST API for the {@link ReferencedSiteManager}
 * <p/>
 * TODO: add description
 */
@Path("/site/{site}")
public class ReferencedSiteRootResource extends NavigationMixin {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(Arrays.asList(SupportedFormat.N3, SupportedFormat.N_TRIPLE, SupportedFormat.RDF_XML, SupportedFormat.TURTLE, SupportedFormat.X_TURTLE, SupportedFormat.RDF_JSON));

    /**
     * The Field used for find requests if not specified
     * TODO: This will be replaced by the EntitySearch. With this search the
     *       Site is responsible to decide what properties to use for label
     *       based searches.
     */
    private static final String DEFAULT_FIND_FIELD = RDFS.label.getUnicodeString();
    /**
     * The Field used as default as selected fields for find requests
     * TODO: Make configurable via the {@link ConfiguredSite} interface!
     */
    private static final Collection<String> DEFAULT_FIND_SELECTED_FIELDS = Arrays.asList(RDFS.comment.getUnicodeString());
    /**
     * The default number of maximal results.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;
    protected Serializer serializer;

    protected ReferencedSite site;

    public ReferencedSiteRootResource(@Context ServletContext context, @PathParam(value = "site") String siteId) {
        super();
        log.info("... init ReferencedSiteRootResource for Site {}", siteId);
        ReferencedSiteManager referencedSiteManager = (ReferencedSiteManager) context.getAttribute(ReferencedSiteManager.class.getName());
        serializer = (Serializer) context.getAttribute(Serializer.class.getName());
        if (referencedSiteManager == null) {
            log.error("Missing referencedSiteManager={}", referencedSiteManager);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        if (siteId == null || siteId.isEmpty()) {
            log.error("Missing path parameter site={}", siteId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        site = referencedSiteManager.getReferencedSite(siteId);
        if (site == null) {
            log.error("Site {} not found (No referenced site with that ID is present within the Entityhub", siteId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @GET
    @Path(value = "/")
    @Produces(MediaType.TEXT_HTML)
    public String getInfo() {
        return "<html><head>" + site.getName() + "</head><body>" + "<h1>Referenced Site " + site.getName() + ":</h1></body></html>";
    }

    /**
     * Cool URI handler for Signs.
     *
     * @param siteId A specific {@link ReferencedSite} to search the parsed id or
     * <code>null</code> to search all referenced sites for the requested
     * entity id. The {@link ReferencedSite#getId()} property is used to map
     * the path to the site!
     * @param id The id of the entity (required)
     * @param headers the request headers used to get the requested {@link MediaType}
     * @return a redirection to either a browser view, the RDF meta data or the
     *         raw binary content
     */
    @GET
    @Path("/entity")
    public Response getSignById(@QueryParam(value = "id") String id, @Context HttpHeaders headers) {
        log.info("site/" + site.getId() + "/entity Request");
        log.info("  > id       : " + id);
        log.info("  > accept   : " + headers.getAcceptableMediaTypes());
        log.info("  > mediaType: " + headers.getMediaType());
        if (id == null || id.isEmpty()) {
            log.error("No or emptpy ID was parsd as query parameter (id={})", id);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        log.info("handle Request for Entity {} of Site {}", id, site.getId());
        Sign sign;
        try {
            sign = site.getSign(id);
        } catch (ReferencedSiteException e) {
            log.error("ReferencedSiteException while accessing Site " + site.getName() + " (id=" + site.getId() + ")", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, MediaType.APPLICATION_JSON_TYPE);
        if (sign != null) {
            return Response.ok(sign, acceptedMediaType).build();
        } else {
            //TODO: How to parse an ErrorMessage?
            // create an Response with the the Error?
            log.info(" ... Entity {} not found on referenced site {}", id, site.getId());
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @GET
    @Path("/find")
    public Response findEntitybyGet(@QueryParam(value = "name") String name,
            @QueryParam(value="field") String field,
            @QueryParam(value = "lang") String language,
            //@QueryParam(value="select") String select,
            @QueryParam(value = "limit") @DefaultValue(value = "-1") int limit, @QueryParam(value = "offset") @DefaultValue(value = "0") int offset, @Context HttpHeaders headers) {
        return findEntity(name, field,language, limit, offset, headers);
    }

    @POST
    @Path("/find")
    public Response findEntity(@FormParam(value = "name") String name,
            @FormParam(value="field") String field,
            @FormParam(value = "lang") String language,
            //@FormParam(value="select") String select,
            @FormParam(value = "limit") Integer limit, 
            @FormParam(value = "offset") Integer offset, 
            @Context HttpHeaders headers) {
        log.debug("site/" + site.getId() + "/find Request");
        //process the optional search field parameter
        if(field == null){
            field = DEFAULT_FIND_FIELD;
        } else {
            field = field.trim();
            if(field.isEmpty()){
                field = DEFAULT_FIND_FIELD;
            }
        }
        return executeQuery(
            JerseyUtils.createFieldQueryForFindRequest(name, field, language, 
                limit == null || limit < 1 ? DEFAULT_FIND_RESULT_LIMIT : limit, offset),
            headers);
    }
    /**
     * Allows to parse any kind of {@link FieldQuery} in its JSON Representation.
     * Note that the maximum number of results (limit) and the offset of the
     * first result (offset) are parsed as seperate parameters and are not
     * part of the field query as in the java API.<p>
     * TODO: as soon as the entityhub supports multiple query types this need
     *       to be refactored. The idea is that this dynamically detects query
     *       types and than redirects them to the referenced site implementation.
     * @param query The field query in JSON format
     * @param limit the maximum number of results starting at offset
     * @param offset the offset of the first result
     * @param headers the header information of the request
     * @return the results of the query
     */
    @POST
    @Path("/query")
    @Consumes( { APPLICATION_FORM_URLENCODED + ";qs=1.0",
            MULTIPART_FORM_DATA + ";qs=0.9" })
    public Response queryEntities(
            @FormParam("query") String query,
            @FormParam("query") File file,
            @Context HttpHeaders headers) {
        return executeQuery(JerseyUtils.parseFieldQuery(query, file), headers);
    }
    /**
     * Executes the query parsed by {@link #queryEntities(String, File, HttpHeaders)}
     * or created based {@link #findEntity(String, String, String, int, int, HttpHeaders)}
     * @param query The query to execute
     * @param headers The headers used to determine the media types
     * @return the response (results of error)
     */
    private Response executeQuery(FieldQuery query,HttpHeaders headers) throws WebApplicationException {
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, MediaType.APPLICATION_JSON_TYPE);
        try {
            return Response.ok(site.find(query), acceptedMediaType).build();
        } catch (ReferencedSiteException e) {
            log.error("ReferencedSiteException while accessing Site " + site.getName() + " (id=" + site.getId() + ")", e);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        
    }

}
