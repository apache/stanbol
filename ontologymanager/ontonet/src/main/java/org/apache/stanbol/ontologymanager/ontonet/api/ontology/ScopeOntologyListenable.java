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

	void addOntologyScopeListener(ScopeOntologyListener listener);

	void clearOntologyScopeListeners();

	Collection<ScopeOntologyListener> getOntologyScopeListeners();

	void removeOntologyScopeListener(ScopeOntologyListener listener);

}
