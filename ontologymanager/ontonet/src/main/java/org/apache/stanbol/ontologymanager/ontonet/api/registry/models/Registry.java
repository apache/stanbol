package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;


public class Registry extends RegistryLibrary {

	private String message = "";

	public Registry(String name) {
		super(name);
	}

	public void setError(String message) {
		this.message = message;
	}

	public String getName() {
		return super.getName() + getError();
	}

	public String getError() {
		return this.message;
	}

	public boolean isOK() {
		return this.getError().equals("");
	}

	public boolean isError() {
		return !isOK();
	}
}
