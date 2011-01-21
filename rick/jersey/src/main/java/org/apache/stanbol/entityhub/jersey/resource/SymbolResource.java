package org.apache.stanbol.entityhub.jersey.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N3;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.N_TRIPLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_JSON;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.X_TURTLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletContext;
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
import org.apache.stanbol.entityhub.core.query.FieldQueryImpl;
import org.apache.stanbol.entityhub.jersey.utils.JerseyUtils;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Symbol;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint.PatternType;
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
    public Response findEntity(@FormParam(value = "name") String name, @FormParam(value = "field") String field,
            @FormParam(value = "lang") String language, @FormParam(value = "select") String select, @Context HttpHeaders headers) {
        log.info("symbol/find Request");
        log.info("  > name  : " + name);
        log.info("  > field : " + field);
        log.info("  > lang  : " + language);
        log.info("  > select: " + select);
        log.info("  > accept: " + headers.getAcceptableMediaTypes());
        //TODO: Implement by using EntityQuery as soon as implemented
        if (name == null || name.isEmpty()) {
            log.error("/find Request with invalied name={}!", name);
        }
        if (field == null || field.isEmpty()) {
            field = DEFAULT_FIND_FIELD;
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
}
