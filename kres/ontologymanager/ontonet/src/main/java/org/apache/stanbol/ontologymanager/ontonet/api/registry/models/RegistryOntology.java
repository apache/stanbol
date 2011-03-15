package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;



public class RegistryOntology extends AbstractRegistryItem {

	public RegistryOntology(String name) {
		super(name);
	}

	@Override
	public boolean isLibrary() {
		return false;
	}

	@Override
	public boolean isOntology() {
		return true;
	}
}
