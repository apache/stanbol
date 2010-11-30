package eu.iksproject.kres.ontologies;

import org.apache.clerezza.rdf.core.UriRef;

public class Semion {

	public static final String URI = "http://andriry.altervista.org/tesiSpecialistica/semion.owl";
	
	public static final String NS = "http://andriry.altervista.org/tesiSpecialistica/semion.owl#";
	
	public static final UriRef DataSource = new UriRef( NS + "DataSource" );
	
	public static final UriRef hasDataSourceType = new UriRef( NS + "hasDataSourceType" );
	
	public static final UriRef hasDataSourceURI = new UriRef( NS + "hasDataSourceURI" );
	
	
}
