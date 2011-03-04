package eu.iksproject.kres.rules;

import eu.iksproject.kres.api.rules.SPARQLObject;

public class SPARQLFunction implements SPARQLObject {

	
	private String function;
	
	public SPARQLFunction(String function) {
		this.function = function;
	}

	@Override
	public String getObject() {
		return function;
	}
	
}
