package org.apache.stanbol.ontologymanager.ontonet.api.session;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.semanticweb.owlapi.model.IRI;


/**
 * Manages KReS session objects via CRUD-like operations. A
 * <code>SessionManager</code> maintains in-memory storage of KReS sessions,
 * creates new ones and either destroys or stores existing ones persistently.
 * All KReS sessions are managed via unique identifiers of the
 * <code>org.semanticweb.owlapi.model.IRI</code> type.<br>
 * <br>
 * NOTE: implementations should be synchronized, or document whenever they are
 * not.
 * 
 * @author alessandro
 * 
 */
public interface SessionManager {

	/**
	 * Adds the given SessionListener to the pool of registered listeners.
	 * 
	 * @param listener
	 *            the session listener to be added
	 */
	public void addSessionListener(SessionListener listener);

	/**
	 * Clears the pool of registered session listeners.
	 */
	public void clearSessionListeners();

	/**
	 * Generates a new KReS session and assigns a unique session ID generated
	 * internally.
	 * 
	 * @return the generated KReS session
	 */
	public KReSSession createSession();

	/**
	 * Generates a new KReS session and tries to assign it the supplied session
	 * ID. If a session with that ID is already registered, the new session is
	 * <i>not</i> created and a <code>DuplicateSessionIDException</code> is
	 * thrown.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 * @return the generated KReS session
	 * @throws DuplicateSessionIDException
	 *             if a KReS session with that sessionID is already registered
	 */
	public KReSSession createSession(IRI sessionID)
			throws DuplicateSessionIDException;

	/**
	 * Deletes the KReS session identified by the supplied sessionID and
	 * releases its resources.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 */
	public void destroySession(IRI sessionID);

	/**
	 * Retrieves the unique KReS session identified by <code>sessionID</code>.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 * @return the unique KReS session identified by <code>sessionID</code>
	 */
	public KReSSession getSession(IRI sessionID);

	/**
	 * Returns all the registered session listeners. It is up to developers to
	 * decide whether implementations should return sets (unordered but without
	 * redundancy), lists (e.g. in the order they wer registered but potentially
	 * redundant) or other data structures that implement {@link Collection}.
	 * 
	 * @return a collection of registered session listeners.
	 */
	public Collection<SessionListener> getSessionListeners();

	/**
	 * Returns the ontology space associated with this session.
	 * 
	 * @return the session space
	 */
	public Set<SessionOntologySpace> getSessionSpaces(IRI sessionID)
			throws NonReferenceableSessionException;

	/**
	 * Removes the given SessionListener from the pool of active listeners.
	 * 
	 * @param listener
	 *            the session listener to be removed
	 */
	public void removeSessionListener(SessionListener listener);

	/**
	 * Stores the KReS session identified by <code>sessionID</code> using the
	 * output stream <code>out</code>.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 * @param out
	 *            the output stream to store the session
	 */
	public void storeSession(IRI sessionID, OutputStream out);

}
