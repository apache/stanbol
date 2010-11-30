package org.stlab.xd.registry.models;



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
