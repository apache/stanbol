package eu.iksproject.kres.rules;

import eu.iksproject.kres.api.rules.SPARQLObject;

public class SPARQLTriple implements SPARQLObject {

	
	private String triple;
	
	public SPARQLTriple(String triple) {
		this.triple = triple;
	}
	
	@Override
	public String getObject() {
		return triple;
	}

	
}
