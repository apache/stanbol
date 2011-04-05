package org.apache.stanbol.ontologymanager.ontonet.api.registry.models;

import java.net.URL;

public interface RegistryItem {

	String getName();

	RegistryLibrary getParent();

	URL getURL();

	boolean isLibrary();

	boolean isOntology();

	void setName(String string);

	void setParent(RegistryLibrary parent);

	void setURL(URL url);

	String toString();

}