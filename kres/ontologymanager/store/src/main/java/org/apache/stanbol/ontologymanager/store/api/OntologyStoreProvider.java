package org.apache.stanbol.ontologymanager.store.api;

import java.util.Collection;

public interface OntologyStoreProvider {

	
	public void registerOntologyStorage(OntologyStorage ontologyStorage);
	
	public void unregisterOntologyStorage(Class<? extends OntologyStorage>  ontologyStorage);
	
	public void activateOntologyStorage(OntologyStorage ontologyStorage);
	
	public void deactivateOntologyStorage();
	
	public boolean isActiveOntologyStorage(OntologyStorage ontologyStorage);
	
	public OntologyStorage getActiveOntologyStorage();
	
	public Collection<OntologyStorage> listOntologyStorages();
	
	
}
