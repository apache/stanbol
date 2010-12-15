package eu.iksproject.kres.api.format;

import javax.ws.rs.core.MediaType;

public class KReSFormat extends MediaType {

	
	
	public static final String RDF_XML = "application/rdf+xml";
	
	public static final MediaType RDF_XML_TYPE = new MediaType("application", "rdf+xml");
	
	public static final String OWL_XML = "application/owl+xml";
	
	public static final MediaType OWL_XML_TYPE = new MediaType("application", "owl+xml");
	
	public static final String MANCHESTER_OWL = "application/manchester+owl";
	
	public static final MediaType MANCHESTER_OWL_TYPE = new MediaType("application", "manchester+xml");
	
	public static final String FUNCTIONAL_OWL = "application/functional+owl";
	
	public static final MediaType FUNCTIONAL_OWL_TYPE = new MediaType("application", "functional+xml");
	
	public static final String TURTLE = "application/turtle";
	
	public static final MediaType TURTLE_TYPE = new MediaType("application", "turtle");
	
	public static final String RDF_JSON = "application/rdf+json";
	
	public static final MediaType RDF_JSON_TYPE = new MediaType("application", "rdf+json");
	
	
}
