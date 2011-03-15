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

	public void addScopeEventListener(ScopeEventListener listener);

	public void clearScopeEventListeners();

	public Collection<ScopeEventListener> getScopeEventListeners();

	public void removeScopeEventListener(ScopeEventListener listener);

}
