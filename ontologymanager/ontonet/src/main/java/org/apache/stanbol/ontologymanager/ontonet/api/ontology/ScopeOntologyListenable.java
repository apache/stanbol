package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import java.util.Collection;

/**
 * Implementations of this interface are able to fire events related to the
 * modification of ontologies within an ontology scope.
 * 
 * @author alessandro
 * 
 */
public interface ScopeOntologyListenable {

	public void addOntologyScopeListener(ScopeOntologyListener listener);

	public void clearOntologyScopeListeners();

	public Collection<ScopeOntologyListener> getOntologyScopeListeners();

	public void removeOntologyScopeListener(ScopeOntologyListener listener);

}
