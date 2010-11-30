package eu.iksproject.rick.jersey.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
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
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.ontologies.RDFS;
import org.codehaus.jettison.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.rick.core.query.FieldQueryImpl;
import eu.iksproject.rick.jersey.utils.JerseyUtils;
import eu.iksproject.rick.servicesapi.model.Sign;
import eu.iksproject.rick.servicesapi.query.FieldQuery;
import eu.iksproject.rick.servicesapi.query.TextConstraint;
import eu.iksproject.rick.servicesapi.query.TextConstraint.PatternType;
import eu.iksproject.rick.servicesapi.site.ReferencedSiteManager;

/**
 * Resource to provide a REST API for the {@link ReferencedSiteManager}
 *
 * TODO: add description
 *
 */
@Path("/sites")
public class SiteManagerRootResource extends NavigationMixin {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final Set<String> RDF_MEDIA_TYPES = new TreeSet<String>(
            Arrays.asList(SupportedFormat.N3, SupportedFormat.N_TRIPLE,
                    SupportedFormat.RDF_XML, SupportedFormat.TURTLE,
                    SupportedFormat.X_TURTLE, SupportedFormat.RDF_JSON));
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

    public SiteManagerRootResource(
    		@Context ServletContext context) {
    	super();
    	log.info("... init SiteManagerRootResource");
        referencedSiteManager = (ReferencedSiteManager) context.getAttribute(ReferencedSiteManager.class.getName());
        serializer = (Serializer) context.getAttribute(Serializer.class.getName());
        if (referencedSiteManager == null) {
            log.error("Missing referencedSiteManager={}", referencedSiteManager);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }
    /**
     * Getter for the id's of all referenced sites
     * @return the id's of all referenced sites.
     */
    @GET
    @Path(value="/referenced")
    @Produces(MediaType.APPLICATION_JSON)
    public JSONArray getReferencedSites(@Context UriInfo uriInfo){
    	log.info("sites/referenced Request");
    	JSONArray referencedSites = new JSONArray();
    	for(String site : referencedSiteManager.getReferencedSiteIds()){
    		referencedSites.put(String.format("%ssite/%s/",uriInfo.getBaseUri(),site));
    	}
    	log.info("  ... return "+referencedSites.toString());
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
    public Response getSignById(
    		@QueryParam(value= "id") String id,
            @Context HttpHeaders headers) {
    	log.info("sites/entity Request");
    	log.info("  > id       : "+id);
    	log.info("  > accept   : "+headers.getAcceptableMediaTypes());
    	log.info("  > mediaType: "+headers.getMediaType());
    	if(id == null || id.isEmpty()){
    		log.error("No or emptpy ID was parsd as query parameter (id={})",id);
    		throw new WebApplicationException(Response.Status.BAD_REQUEST);
    	}
    	Sign sign;
//		try {
			sign = referencedSiteManager.getSign(id);
//		} catch (IOException e) {
//			log.error("IOException while accessing ReferencedSiteManager",e);
//			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
//		}
    	final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,MediaType.APPLICATION_JSON_TYPE);
		if(sign != null){
			return Response.ok(sign, acceptedMediaType).build();
		} else {
			//TODO: How to parse an ErrorMessage?
			// create an Response with the the Error?
			log.info(" ... Entity {} not found on any referenced site");
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		}
    }
    @GET
    @Path("/find")
    public Response findEntityfromGet(
    		@QueryParam(value="name") String name,
    		//@FormParam(value="field") String field,
    		@QueryParam(value="lang") String language,
    		//@FormParam(value="select") String select,
    		@QueryParam(value="limit") @DefaultValue(value="-1")int limit,
    		@QueryParam(value="offset") @DefaultValue(value="0") int offset,
    		@Context HttpHeaders headers) {
    	return findEntity(name, language, limit, offset, headers);
    }
    @POST
    @Path("/find")
    public Response findEntity(
    		@FormParam(value="name") String name,
    		//@FormParam(value="field") String field,
    		@FormParam(value="lang") String language,
    		//@FormParam(value="select") String select,
    		@FormParam(value="limit") @DefaultValue(value="-1")int limit,
    		@FormParam(value="offset") @DefaultValue(value="0") int offset,
            @Context HttpHeaders headers) {
    	log.info("sites/find Request");
    	log.info("  > name  : "+name);
    	log.info("  > lang  : "+language);
    	log.info("  > limit : "+limit);
    	log.info("  > offset: "+offset);
    	log.info("  > accept: "+headers.getAcceptableMediaTypes());
    	if(name == null || name.isEmpty()){
    		log.error("/find Request with invalied name={}!",name);
    	}
    	String field = DEFAULT_FIND_FIELD;
    	FieldQuery query = new FieldQueryImpl();
    	if(language == null){
    		query.setConstraint(field, new TextConstraint(name,PatternType.wildcard,false));
    	} else {
    		query.setConstraint(field, new TextConstraint(name,PatternType.wildcard,false,language));
    	}
    	Collection<String> selectedFields = new ArrayList<String>();
    	selectedFields.add(field); //select also the field used to find entities
//    	if(select == null ||select.isEmpty()){
//    		selectedFields.addAll(DEFAULT_FIND_SELECTED_FIELDS);
//    	} else {
//    		for(String selected : select.trim().split(" ")){
//    			if(selected != null && !selected.isEmpty()){
//    				selectedFields.add(selected);
//    			}
//    		}
//    	}
    	query.addSelectedFields(selectedFields);
    	if(limit < 1){
    		limit = DEFAULT_FIND_RESULT_LIMIT;
    	}
    	query.setLimit(limit);
    	query.setOffset(offset);
    	final MediaType acceptedMediaType = JerseyUtils.getAcceptableMediaType(headers,MediaType.APPLICATION_JSON_TYPE);
//    	try {
			return Response.ok(referencedSiteManager.find(query),acceptedMediaType).build();
//		} catch (IOException e) {
//			log.error("IOException while accessing Referenced Site Manager",e);
//			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
//		}
    }
}
