package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

/**
 * Objects that want to listen to the registration of ontology scopes should
 * implement this interface and add themselves as listener to a scope registry.
 * 
 * @author alessandro
 * 
 */
public interface ScopeEventListener {

	/**
	 * Called <i>after</i> an ontology scope, assuming it is already registered
	 * somewhere, is activated.
	 * 
	 * @param scope
	 *            the activated ontology scope
	 */
	public void scopeActivated(OntologyScope scope);

	/**
	 * Called <i>after</i> a new ontology scope has been created.
	 * 
	 * @param scope
	 *            the created ontology scope
	 */
	public void scopeCreated(OntologyScope scope);

	/**
	 * Called <i>after</i> an ontology scope, assuming it is already registered
	 * somewhere, is deactivated. If the deactivation of a scope implies
	 * deregistering of it, a separate event should be fired for deregistration.
	 * 
	 * @param scope
	 *            the deactivated ontology scope
	 */
	public void scopeDeactivated(OntologyScope scope);

	/**
	 * Called <i>after</i> an ontology scope is removed from the scope registry.
	 * 
	 * @param scope
	 *            the deregistered ontology scope
	 */
	public void scopeDeregistered(OntologyScope scope);

	/**
	 * Called <i>after</i> an ontology scope is added to the scope registry.
	 * 
	 * @param scope
	 *            the registered ontology scope
	 */
	public void scopeRegistered(OntologyScope scope);

}
