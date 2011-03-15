package eu.iksproject.kres.rules;

import org.apache.stanbol.rules.base.api.SPARQLObject;

public class SPARQLNot implements SPARQLObject {

	
	private String optional;
	private String[] filters;
	
	public SPARQLNot(String optional, String[] filters) {
		this.optional = optional;
		this.filters = filters;
	}
	
	@Override
	public String getObject() {
		return optional;
	}
	
	public String[] getFilters() {
		return filters;
	}

}
