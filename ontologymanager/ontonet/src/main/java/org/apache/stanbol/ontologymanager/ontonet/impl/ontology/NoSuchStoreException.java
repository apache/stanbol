package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

public class NoSuchStoreException extends Exception {

	private String message;
	
	public NoSuchStoreException(String message) {
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
}
