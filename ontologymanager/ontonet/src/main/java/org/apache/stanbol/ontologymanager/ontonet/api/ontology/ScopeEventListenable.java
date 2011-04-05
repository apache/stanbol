package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import java.util.Collection;

/**
 * Implementations of this interface are able to fire events related to the
 * modification of an ontology scope, not necessarily including its ontologies.
 * 
 * @author alessandro
 * 
 */
public interface ScopeEventListenable {

	void addScopeEventListener(ScopeEventListener listener);

	void clearScopeEventListeners();

	Collection<ScopeEventListener> getScopeEventListeners();

	void removeScopeEventListener(ScopeEventListener listener);

}
