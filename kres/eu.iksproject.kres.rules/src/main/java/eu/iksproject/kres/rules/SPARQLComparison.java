package eu.iksproject.kres.rules;

import eu.iksproject.kres.api.rules.SPARQLObject;

public class SPARQLComparison implements SPARQLObject {

	private String filter;
	
	public SPARQLComparison(String filter) {
		this.filter = filter;
	}
	
	@Override
	public String getObject() {
		return filter;
	}
	

}
