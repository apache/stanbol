package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;

/**
 * A registry that keeps track of the active ontology scopes in a running KReS
 * instance.
 * 
 * @author alessandro
 * 
 */
public interface ScopeRegistry {

	/**
	 * Adds a scope registration listener to this registry. If the listener was
	 * already added, this should result in no effect.
	 * 
	 * @param listener
	 *            the listener to be added
	 */
    void addScopeRegistrationListener(ScopeEventListener listener);

	/**
	 * Removes all registered scope registration listeners.
	 */
    void clearScopeRegistrationListeners();

	/**
	 * 
	 * @param scopeID
	 * @return true iff an ontology scope with ID <code>scopeID</code> is
	 *         registered.
	 */
    boolean containsScope(IRI scopeID);

	/**
	 * Removes an ontology scope from this registry, thus deactivating the scope
	 * and all of its associated spaces. All attached listeners should hear this
	 * deregistration on their <code>scopeDeregistered()</code> method.
	 * 
	 * @param scope
	 *            the ontology scope to be removed
	 */
    void deregisterScope(OntologyScope scope);

	/**
	 * Returns the set of registered ontology scopes.
	 * 
	 * @return the set of ontology scopes
	 */
    Set<OntologyScope> getRegisteredScopes();

	/**
	 * Returns the unique ontology scope identified by the given ID.
	 * 
	 * @param scopeID
	 *            the scope identifier
	 * @return the ontology scope with that ID, or null if no scope with such ID
	 *         is registered
	 */
    OntologyScope getScope(IRI scopeID);

	void setScopeActive(IRI scopeID, boolean active);

	boolean isScopeActive(IRI scopeID);

	Set<OntologyScope> getActiveScopes();

	/**
	 * Returns the set of registered scope registration listeners, in no
	 * particular order.
	 * 
	 * @return the set of scope registration listeners
	 */
    Set<ScopeEventListener> getScopeRegistrationListeners();

	/**
	 * Equivalent to <code>registerScope(scope, false)</code>.
	 * 
	 * @param scope
	 *            the ontology scope to be added
	 */
    void registerScope(OntologyScope scope);

	/**
	 * Adds an ontology scope to this registry, thus activating the scope if
	 * <code>activate</code> is set and (at a bare minumum) its core space. All
	 * attached listeners should hear this registration on their
	 * <code>scopeRegistered()</code> method.
	 * 
	 * @param scope
	 *            the ontology scope to be added
	 */
    void registerScope(OntologyScope scope, boolean activate);

	/**
	 * Removes a scope registration listener from this registry. If the listener
	 * was not previously added, this should result in no effect.
	 * 
	 * @param listener
	 *            the listener to be removed
	 */
    void removeScopeRegistrationListener(
            ScopeEventListener listener);

}
