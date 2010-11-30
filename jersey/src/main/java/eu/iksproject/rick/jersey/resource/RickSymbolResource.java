package eu.iksproject.rick.jersey.resource;

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
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.rick.core.query.FieldQueryImpl;
import eu.iksproject.rick.jersey.utils.JerseyUtils;
import eu.iksproject.rick.servicesapi.Rick;
import eu.iksproject.rick.servicesapi.RickException;
import eu.iksproject.rick.servicesapi.model.Symbol;
import eu.iksproject.rick.servicesapi.model.rdf.RdfResourceEnum;
import eu.iksproject.rick.servicesapi.query.FieldQuery;
import eu.iksproject.rick.servicesapi.query.TextConstraint;
import eu.iksproject.rick.servicesapi.query.TextConstraint.PatternType;

/**
 * RESTful interface for The {@link Rick}. To access referenced sites directly
 * see {@link ReferencedSiteRootResource}.
 *
 */
@Path("/symbol")
//@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class RickSymbolResource extends NavigationMixin {
	/**
	 * The default search field for /find queries is the rick-maodel:label
	 */
    private static final String DEFAULT_FIND_FIELD = RdfResourceEnum.label.getUri();

    /**
     * The default result fields for /find queries is the rick-maodel:label and the
     * rick-maodel:description.
     */
	private static final Collection<? extends String> DEFAULT_FIND_SELECTED_FIELDS = 
		Arrays.asList(
				RdfResourceEnum.label.getUri(),
				RdfResourceEnum.description.getUri());
  

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Rick rick;

    protected TcManager tcManager;

    protected Serializer serializer;

    protected TripleCollection entityCache;

    // bind the job manager by looking it up from the servlet request context
    public RickSymbolResource(@Context ServletContext context) {
    	super();
        rick = (Rick) context.getAttribute(Rick.class.getName());
        tcManager = (TcManager) context.getAttribute(TcManager.class.getName());
        serializer = (Serializer) context.getAttribute(Serializer.class.getName());
    }
    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON,SupportedFormat.RDF_XML,SupportedFormat.N3,
    	SupportedFormat.TURTLE,SupportedFormat.X_TURTLE,SupportedFormat.RDF_JSON,
    	SupportedFormat.N_TRIPLE})
    public Response getSymbol(
    		@QueryParam("id") String symbolId,
    		@Context HttpHeaders headers)
    	throws WebApplicationException{
    	log.info("/symbol/lookup Request");
    	log.info("  > id: "+symbolId);
    	log.info("  > accept: "+headers.getAcceptableMediaTypes());
       	if(symbolId == null || symbolId.isEmpty()){
    		//TODO: how to parse an error message
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	}
       	Symbol symbol;
		try {
			symbol = rick.getSymbol(symbolId);
		} catch (RickException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
       	if(symbol == null){
       		throw new WebApplicationException(Response.Status.NOT_FOUND);
       	} else {
        	MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,MediaType.APPLICATION_JSON_TYPE);
    		return Response.ok(symbol,acceptedMediaType).build();
       	}
    }
    @GET
    @Path("/lookup")
    @Produces({MediaType.APPLICATION_JSON,SupportedFormat.RDF_XML,SupportedFormat.N3,
    	SupportedFormat.TURTLE,SupportedFormat.X_TURTLE,SupportedFormat.RDF_JSON,
    	SupportedFormat.N_TRIPLE})
    public Response lookupSymbol(
    		@QueryParam("id") String reference,
    		@QueryParam("create") boolean create,
    		@Context HttpHeaders headers)
    	throws WebApplicationException{
    	log.info("/symbol/lookup Request");
    	log.info("  > id: "+reference);
    	log.info("  > create   : "+create);
    	log.info("  > accept: "+headers.getAcceptableMediaTypes());
    	if(reference == null || reference.isEmpty()){
    		//TODO: how to parse an error message
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	}
    	Symbol symbol;
		try {
			symbol = rick.lookupSymbol(reference,create);
		} catch (RickException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
    	if(symbol == null){
    		throw new WebApplicationException(404);
    	} else {
        	MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,MediaType.APPLICATION_JSON_TYPE);
    		return Response.ok(symbol,acceptedMediaType).build();
    	}
    }
    @POST
    @Path("/find")
    @Produces({MediaType.APPLICATION_JSON,SupportedFormat.RDF_XML,SupportedFormat.N3,
    	SupportedFormat.TURTLE,SupportedFormat.X_TURTLE,SupportedFormat.RDF_JSON,
    	SupportedFormat.N_TRIPLE})
    public Response findEntity(
    		@FormParam(value="name") String name,
    		@FormParam(value="field") String field,
    		@FormParam(value="lang") String language,
    		@FormParam(value="select") String select,
            @Context HttpHeaders headers) {
    	log.info("symbol/find Request");
    	log.info("  > name  : "+name);
    	log.info("  > field : "+field);
    	log.info("  > lang  : "+language);
    	log.info("  > select: "+select);
    	log.info("  > accept: "+headers.getAcceptableMediaTypes());
    	//TODO: Implement by using EntityQuery as soon as implemented
    	if(name == null || name.isEmpty()){
    		log.error("/find Request with invalied name={}!",name);
    	}
    	if(field == null || field.isEmpty()){
    		field = DEFAULT_FIND_FIELD;
    	}
    	FieldQuery query = new FieldQueryImpl();
    	if(language == null){
    		query.setConstraint(field, new TextConstraint(name,PatternType.wildcard,false));
    	} else {
    		query.setConstraint(field, new TextConstraint(name,PatternType.wildcard,false,language));
    	}
    	Collection<String> selectedFields = new ArrayList<String>();
    	selectedFields.add(field); //select also the field used to find entities
    	if(select == null ||select.isEmpty()){
    		selectedFields.addAll(DEFAULT_FIND_SELECTED_FIELDS);
    	} else {
    		for(String selected : select.trim().split(" ")){
    			if(selected != null && !selected.isEmpty()){
    				selectedFields.add(selected);
    			}
    		}
    	}
    	query.addSelectedFields(selectedFields);
    	final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,MediaType.APPLICATION_JSON_TYPE);
    	try {
			return Response.ok(rick.find(query),acceptedMediaType).build();
		} catch (Exception e) {
			log.error("Error while accessing RickYard "+rick.getRickYard().getName()+" (id="+rick.getRickYard().getId()+")",e);
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
    }
    @DELETE
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON,SupportedFormat.RDF_XML,SupportedFormat.N3,
    	SupportedFormat.TURTLE,SupportedFormat.X_TURTLE,SupportedFormat.RDF_JSON,
    	SupportedFormat.N_TRIPLE})
    public Response removeSymbol(){
    	return null;
    }
}
