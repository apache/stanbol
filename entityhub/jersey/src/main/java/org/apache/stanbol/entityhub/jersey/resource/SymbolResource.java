package org.apache.stanbol.entityhub.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.commons.io.FileUtils;
import org.apache.stanbol.entityhub.core.query.FieldQueryImpl;
import org.apache.stanbol.entityhub.jersey.parsers.JSONToFieldQuery;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * RESTful interface for The {@link Entityhub}. To access referenced sites directly
 * see {@link ReferencedSiteRootResource}.
 */
@Path("/symbol")
//@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class SymbolResource extends NavigationMixin {
    /**
     * The default search field for /find queries is the entityhub-maodel:label
     */
    private static final String DEFAULT_FIND_FIELD = RdfResourceEnum.label.getUri();

    /**
     * The default result fields for /find queries is the entityhub-maodel:label and the
     * entityhub-maodel:description.
     */
    private static final Collection<? extends String> DEFAULT_FIND_SELECTED_FIELDS = Arrays.asList(RdfResourceEnum.label.getUri(), RdfResourceEnum.description.getUri());

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Entityhub entityhub;

    protected TcManager tcManager;

    protected Serializer serializer;

    protected TripleCollection entityCache;

    // bind the job manager by looking it up from the servlet request context
    public SymbolResource(@Context ServletContext context) {
        super();
        entityhub = (Entityhub) context.getAttribute(Entityhub.class.getName());
        tcManager = (TcManager) context.getAttribute(TcManager.class.getName());
        serializer = (Serializer) context.getAttribute(Serializer.class.getName());
    }

    @GET
    @Path("/")
    @Produces({APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE})
    public Response getSymbol(@QueryParam("id") String symbolId, @Context HttpHeaders headers)
            throws WebApplicationException {
        log.info("/symbol/lookup Request");
        log.info("  > id: " + symbolId);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        if (symbolId == null || symbolId.isEmpty()) {
            //TODO: how to parse an error message
            throw new WebApplicationException(BAD_REQUEST);
        }
        Symbol symbol;
        try {
            symbol = entityhub.getSymbol(symbolId);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (symbol == null) {
            throw new WebApplicationException(NOT_FOUND);
        } else {
            MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, APPLICATION_JSON_TYPE);
            return Response.ok(symbol, acceptedMediaType).build();
        }
    }

    @GET
    @Path("/lookup")
    @Produces({APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE})
    public Response lookupSymbol(@QueryParam("id") String reference, @QueryParam("create") boolean create,
            @Context HttpHeaders headers) throws WebApplicationException {
        log.info("/symbol/lookup Request");
        log.info("  > id: " + reference);
        log.info("  > create   : " + create);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        if (reference == null || reference.isEmpty()) {
            //TODO: how to parse an error message
            throw new WebApplicationException(BAD_REQUEST);
        }
        Symbol symbol;
        try {
            symbol = entityhub.lookupSymbol(reference, create);
        } catch (EntityhubException e) {
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
        if (symbol == null) {
            throw new WebApplicationException(404);
        } else {
            MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, APPLICATION_JSON_TYPE);
            return Response.ok(symbol, acceptedMediaType).build();
        }
    }

    @POST
    @Path("/find")
    @Produces({APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE})
    public Response findEntity(@FormParam(value = "name") String name, 
            @FormParam(value = "field") String field,
            @FormParam(value = "lang") String language, 
            @FormParam(value = "select") String select, 
            @Context HttpHeaders headers) {
        log.info("symbol/find Request");
        log.info("  > name  : " + name);
        log.info("  > field : " + field);
        log.info("  > lang  : " + language);
        log.info("  > select: " + select);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        //TODO: Implement by using EntityQuery as soon as implemented
        if (name == null || name.trim().isEmpty()) {
            log.error("/find Request with invalied name={}!", name);
        } else {
            name = name.trim();
        }
        if (field == null || field.trim().isEmpty()) {
            field = DEFAULT_FIND_FIELD;
        } else {
            field = field.trim();
        }
        FieldQuery query = new FieldQueryImpl();
        if (language == null) {
            query.setConstraint(field, new TextConstraint(name, PatternType.wildcard, false));
        } else {
            query.setConstraint(field, new TextConstraint(name, PatternType.wildcard, false, language));
        }
        Collection<String> selectedFields = new ArrayList<String>();
        selectedFields.add(field); //select also the field used to find entities
        if (select == null || select.isEmpty()) {
            selectedFields.addAll(DEFAULT_FIND_SELECTED_FIELDS);
        } else {
            for (String selected : select.trim().split(" ")) {
                if (selected != null && !selected.isEmpty()) {
                    selectedFields.add(selected);
                }
            }
        }
        query.addSelectedFields(selectedFields);
        final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers, APPLICATION_JSON_TYPE);
        try {
            return Response.ok(entityhub.find(query), acceptedMediaType).build();
        } catch (Exception e) {
            log.error("Error while accessing Yard of the Entityhub" + entityhub.getYard().getName() + " (id=" + entityhub.getYard().getId() + ")", e);
            throw new WebApplicationException(e, INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces({APPLICATION_JSON, RDF_XML, N3, TURTLE, X_TURTLE, RDF_JSON, N_TRIPLE})
    public Response removeSymbol() {
        return null;
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
        try {
            return Response.ok(entityhub.find(fieldQuery), acceptedMediaType).build();
        } catch (EntityhubException e) {
            log.error("Exception while performing the parsed query on the EntityHub", e);
            log.error("Query:\n"+query);
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        
    }
}
