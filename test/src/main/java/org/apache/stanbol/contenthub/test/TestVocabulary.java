package org.apache.stanbol.contenthub.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.stanbol.contenthub.servicesapi.store.vocabulary.SolrVocabulary;
import org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer;

public class TestVocabulary {
	
	public static final String PREFIX = "154a3b342bce37d432a322e443z";
	
	public static final String queryTerm = "Paris";
	public static SolrQuery solrQuery = new SolrQuery();
	public static final String resultTerm = "France";
	public static final UriRef ontologyURI = new UriRef(PREFIX+"testOnt");
	
	public static final String content = "a text containing Paris";
	public static final byte[] contentByte = content.getBytes();
	public static final String title = "Test Title";
	public static final String contentType = "text/plain";
	public static final String id = PREFIX + "5532b32a4ab";
	public static final String attachedId = ContentItemIDOrganizer.attachBaseURI(id);
	public static final String creationDate = "2010-01-27T10:00:15Z";
	
	public static Map<String, List<Object>> constraints = new HashMap<String, List<Object>>();
	public static List<Object> consValuesArray = new ArrayList<Object>();
	public static final String consFieldName = "author";
	public static final String consFieldType = SolrVocabulary.SOLR_DYNAMIC_FIELD_TEXT;
	public static final String consValues = "meric";
	
	public static final String programName = PREFIX+"testProgram";
	public static final String ldPathProgram = "@prefix dct : <http://purl.org/dc/terms/>; @prefix geo : <http://www.w3.org/2003/01/geo/wgs84_pos#>; name = rdfs:label[@en] :: xsd:string; labels = rdfs:label :: xsd:string; comment = rdfs:commen [@en] :: xsd:string; categories = dc:subject :: xsd:anyURI; homepage = foaf:homepage :: xsd:anyURI; location = fn:concat(\"[\",geo:lat,\",\",geo:long,\"]\") :: xsd:string;";
	
	static {
		List<Object> value = new ArrayList<Object>();
		value.add("meric");
		consValuesArray = value;
		constraints.put(consFieldName, value);
		
		solrQuery.setQuery("Paris");
	}

}
