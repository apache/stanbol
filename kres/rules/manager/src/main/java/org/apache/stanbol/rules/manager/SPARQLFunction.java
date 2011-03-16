package org.apache.stanbol.rules.manager;

import org.apache.stanbol.rules.base.api.SPARQLObject;

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
