package eu.iksproject.kres.rules;

import org.apache.stanbol.rules.base.api.SPARQLObject;

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
