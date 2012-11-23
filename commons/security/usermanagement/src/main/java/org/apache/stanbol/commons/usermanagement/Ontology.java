package org.apache.stanbol.commons.usermanagement;

import org.apache.clerezza.rdf.core.UriRef;

public class Ontology {
	
	public final static UriRef EditableUser = 
			new UriRef("http://stanbol.apache.org/ontologies/usermanagement#EditableUser");
	
	public final static UriRef Change = 
			new UriRef("http://stanbol.apache.org/ontologies/usermanagement#Change");
	
	public final static UriRef predicate = 
			new UriRef("http://stanbol.apache.org/ontologies/usermanagement#predicate");
	
	public final static UriRef oldValue = 
			new UriRef("http://stanbol.apache.org/ontologies/usermanagement#oldValue");
	
	public final static UriRef newValue = 
			new UriRef("http://stanbol.apache.org/ontologies/usermanagement#newValue");

}
