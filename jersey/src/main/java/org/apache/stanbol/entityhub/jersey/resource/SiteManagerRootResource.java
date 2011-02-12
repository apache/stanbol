package org.apache.stanbol.entityhub.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.apache.commons.io.FileUtils;
import org.apache.stanbol.entityhub.core.query.FieldQueryImpl;
import org.apache.stanbol.entityhub.jersey.parsers.JSONToFieldQuery;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteManager;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Resource to provide a REST API for the {@link ReferencedSiteManager}.
 *
 * TODO: add description
 */
@Path("/sites")
public class SiteManagerRootResource extends NavigationMixin {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(
            Arrays.asList(N3, N_TRIPLE, RDF_XML, TURTLE, X_TURTLE, RDF_JSON));

    /**
     * The Field used for find requests if not specified
     * TODO: Will be depreciated as soon as EntityQuery is implemented
     */
    private static final String DEFAULT_FIND_FIELD = RDFS.label.getUnicodeString();

    /**
     * The default number of maximal results of searched sites.
     */
    private static final int DEFAULT_FIND_RESULT_LIMIT = 5;

    protected Serializer serializer;

    private ReferencedSiteManager referencedSiteManager;

    public SiteManagerRootResource(@Context ServletContext context) {
        super();
        log.info("... init SiteManagerRootResource");
        referencedSiteManager = (ReferencedSiteManager) context.getAttribute(ReferencedSiteManager.class.getName());
        serializer = (Serializer) context.getAttribute(Serializer.class.getName());
        if (referencedSiteManager == null) {
            log.error("Missing referencedSiteManager={}", referencedSiteManager);
            throw new WebApplicationException(NOT_FOUND);
        }
    }

    /**
     * Getter for the id's of all referenced sites
     *
     * @return the id's of all referenced sites.
     */
    @GET
    @Path(value = "/referenced")
    @Produces(APPLICATION_JSON)
    public JSONArray getReferencedSites(@Context UriInfo uriInfo) {
        log.info("sites/referenced Request");
        JSONArray referencedSites = new JSONArray();
        for (String site : referencedSiteManager.getReferencedSiteIds()) {
            referencedSites.put(String.format("%ssite/%s/", uriInfo.getBaseUri(), site));
        }
        log.info("  ... return " + referencedSites.toString());
        return referencedSites;
    }

    /**
     * Cool URI handler for Signs.
     *
     * @param id The id of the entity (required)
     * @param headers the request headers used to get the requested {@link MediaType}
     * @return a redirection to either a browser view, the RDF meta data or the
     *         raw binary content
     */
    @GET
    @Path("/entity")
    public Response getSignById(@QueryParam(value = "id") String id, @Context HttpHeaders headers) {
        log.info("sites/entity Request");
        log.info("  > id       : " + id);
        log.info("  > accept   : " + headers.getAcceptableMediaTypes());
        log.info("  > mediaType: " + headers.getMediaType());
        if (id == null || id.isEmpty()) {
            log.error("No or emptpy ID was parsed as query parameter (id={})", id);
            throw new WebApplicationException(BAD_REQUEST);
        }
        Sign sign;
//        try {
        sign = referencedSiteManager.getSign(id);
//        } catch (IOException e) {
//            log.error("IOException while accessing ReferencedSiteManager",e);
//            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
//        }
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, APPLICATION_JSON_TYPE);
        if (sign != null) {
            return Response.ok(sign, acceptedMediaType).build();
        } else {
            //TODO: How to parse an ErrorMessage?
            // create an Response with the the Error?
            log.info(" ... Entity {} not found on any referenced site");
            throw new WebApplicationException(NOT_FOUND);
        }
    }

    @GET
    @Path("/find")
    public Response findEntityfromGet(@QueryParam(value = "name") String name,
            @FormParam(value="field") String field,
            @QueryParam(value = "lang") String language,
            //@FormParam(value="select") String select,
            @QueryParam(value = "limit") @DefaultValue(value = "-1") int limit,
            @QueryParam(value = "offset") @DefaultValue(value = "0") int offset,
            @Context HttpHeaders headers) {
        return findEntity(name, field,language, limit, offset, headers);
    }

    @POST
    @Path("/find")
    public Response findEntity(@FormParam(value = "name") String name,
            @FormParam(value="field") String field,
            @FormParam(value = "lang") String language,
            //@FormParam(value="select") String select,
            @FormParam(value = "limit") @DefaultValue(value = "-1") int limit,
            @FormParam(value = "offset") @DefaultValue(value = "0") int offset,
            @Context HttpHeaders headers) {
        log.info("sites/find Request");
        log.info("  > name  : " + name);
        log.info("  > lang  : " + language);
        log.info("  > limit : " + limit);
        log.info("  > offset: " + offset);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        if (name == null || name.trim().isEmpty()) {
            log.error("/find Request with invalied name={}!", name);
        } else {
            name = name.trim();
        }
        if(field == null){
            field = DEFAULT_FIND_FIELD;
        } else {
            field = field.trim();
            if(field.isEmpty()){
                field = DEFAULT_FIND_FIELD;
            }
        }
        FieldQuery query = new FieldQueryImpl();
        if (language == null) {
            query.setConstraint(field, new TextConstraint(name, PatternType.wildcard, false));
        } else {
            query.setConstraint(field, new TextConstraint(name, PatternType.wildcard, false, language));
        }
        Collection<String> selectedFields = new ArrayList<String>();
        selectedFields.add(field); //select also the field used to find entities
//        if(select == null ||select.isEmpty()){
//            selectedFields.addAll(DEFAULT_FIND_SELECTED_FIELDS);
//        } else {
//            for(String selected : select.trim().split(" ")){
//                if(selected != null && !selected.isEmpty()){
//                    selectedFields.add(selected);
//                }
//            }
//        }
        query.addSelectedFields(selectedFields);
        if (limit < 1) {
            limit = DEFAULT_FIND_RESULT_LIMIT;
        }
        query.setLimit(limit);
        query.setOffset(offset);
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, APPLICATION_JSON_TYPE);
        return Response.ok(referencedSiteManager.find(query), acceptedMediaType).build();
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
        if(query == null && file == null) {
            throw new WebApplicationException(new IllegalArgumentException("Query Requests MUST define the \"query\" parameter"), Response.Status.BAD_REQUEST);
        }
        FieldQuery fieldQuery = null;
        JSONException exception = null;
        if(query != null){
            try {
                fieldQuery = JSONToFieldQuery.fromJSON(query);
            } catch (JSONException e) {
                log.warn("unable to parse FieldQuery from \"application/x-www-form-urlencoded\" encoded query string "+query);
                fieldQuery = null;
                exception = e;
            }
        } //else no query via application/x-www-form-urlencoded parsed
        if(fieldQuery == null && file != null){
            try {
                query = FileUtils.readFileToString(file);
                fieldQuery = JSONToFieldQuery.fromJSON(query);
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
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, MediaType.APPLICATION_JSON_TYPE);
        return Response.ok(referencedSiteManager.find(fieldQuery), acceptedMediaType).build();
    }
    
}
