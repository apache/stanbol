package org.apache.stanbol.reengineer.base.api;

import org.semanticweb.owlapi.model.IRI;



public class Semion_OWL {

	public static final String URI = "http://ontologydesignpatterns.org/ont/iks/semion.owl";
	
	public static final String NS = "http://ontologydesignpatterns.org/ont/iks/semion.owl#";
	
	public static final IRI DataSource = IRI.create( NS + "DataSource" );
	
	public static final IRI hasDataSourceType = IRI.create( NS + "hasDataSourceType" );
	
	public static final IRI hasDataSourceURI = IRI.create( NS + "hasDataSourceURI" );
}
