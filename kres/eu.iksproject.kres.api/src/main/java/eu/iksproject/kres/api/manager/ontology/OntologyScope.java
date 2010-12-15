package eu.iksproject.kres.api.manager.ontology;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;

/**
 * Represents an ontology network that is used by KReS for modelling a given
 * knowledge component or domain, e.g. workflows, organisations, devices,
 * content or business domain.<br>
 * <br>
 * Each ontology scope comprises in turn a number of ontology spaces of three
 * kinds.
 * <ul>
 * <li>Exactly one core space, which defines the immutable components of the
 * scope.
 * <li>At most one custom space, which contains user-defined components.
 * <li>Zero or more session spaces, which contains (potentially volatile)
 * components specific for user sessions.
 * </ul>
 * An ontology scope can thus be seen as a fa&ccedil;ade for ontology spaces.
 * 
 * 
 * @author alessandro
 * 
 */
public interface OntologyScope extends ScopeOntologyListenable {

	/**
	 * Adds a new ontology space to the list of user session spaces for this
	 * scope.
	 * 
	 * @param sessionSpace
	 *            the ontology space to be added.
	 */
	public void addSessionSpace(OntologySpace sessionSpace, IRI sessionID);

	/**
	 * Returns the core ontology space for this ontology scope. The core space
	 * should never be null for any scope.
	 * 
	 * @return the core ontology space
	 */
	public OntologySpace getCoreSpace();

	/**
	 * Returns the custom ontology space for this ontology scope.
	 * 
	 * @return the custom ontology space, or null if no custom space is
	 *         registered for this scope.
	 */
	public OntologySpace getCustomSpace();

	/**
	 * Returns an object that uniquely identifies this ontology scope.
	 * 
	 * TODO : check if we'd rather use another class for identifiers.
	 * 
	 * @return the unique identifier for this ontology scope
	 */
	public IRI getID();

	/**
	 * Return the ontology space for this scope that is identified by the
	 * supplied IRI.
	 * 
	 * @param sessionID
	 *            the unique identifier of the KReS session.
	 * @return the ontology space identified by <code>sessionID</code>, or null
	 *         if no such space is registered for this scope and session.
	 */
	public SessionOntologySpace getSessionSpace(IRI sessionID);

	/**
	 * Returns all the active ontology spaces for this scope.
	 * 
	 * @return a set of active ontology spaces for this scope.
	 */
	public Set<OntologySpace> getSessionSpaces();

	/**
	 * Sets an ontology space as the custom space for this scope.
	 * 
	 * @param customSpace
	 *            the custom ontology space.
	 * @throws UnmodifiableOntologySpaceException
	 *             if either the scope or the supplied space are locked.
	 */
	public void setCustomSpace(OntologySpace customSpace)
			throws UnmodifiableOntologySpaceException;

	/**
	 * Performs the operations required for activating the ontology scope. It
	 * should be possible to perform them <i>after</i> the constructor has been
	 * invoked.<br>
	 * <br>
	 * When the core ontology space is created for this scope, this should be
	 * set in the scope constructor. It can be changed in the
	 * <code>setUp()</code> method though.
	 */
	public void setUp();

	/**
	 * Performs whatever operations are required for making sure the custom
	 * space of this scope is aware of changes occurring in its core space, that
	 * all session spaces are aware of changes in the custom space, and so on.
	 * Typically, this includes updating all import statements in the top
	 * ontologies for each space.<br>
	 * <br>
	 * This method is not intended for usage by ontology managers. Since its
	 * invocation is supposed to be automatic, it should be invoked by whatever
	 * classes are responsible for listening to changes in an ontology
	 * scope/space. In the default implementation, it is the scope itself, yet
	 * the method is left public in order to allow for external controllers.
	 */
	public void synchronizeSpaces();

	/**
	 * Performs the operations required for deactivating the ontology scope. In
	 * general, this is not equivalent to finalizing the object for garbage
	 * collection. It should be possible to activate the same ontology scope
	 * again if need be.
	 */
	public void tearDown();

}
