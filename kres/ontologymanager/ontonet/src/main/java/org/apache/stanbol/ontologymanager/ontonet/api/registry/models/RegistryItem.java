package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;

import java.net.URL;

public interface RegistryItem {

	public abstract String getName();

	public abstract RegistryLibrary getParent();

	public abstract URL getURL();

	public abstract boolean isLibrary();

	public abstract boolean isOntology();

	public abstract void setName(String string);

	public abstract void setParent(RegistryLibrary parent);

	public abstract void setURL(URL url);

	public abstract String toString();

}