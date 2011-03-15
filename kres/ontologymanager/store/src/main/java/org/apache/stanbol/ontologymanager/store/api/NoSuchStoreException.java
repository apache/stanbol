package org.apache.stanbol.ontologymanager.store.api;

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
