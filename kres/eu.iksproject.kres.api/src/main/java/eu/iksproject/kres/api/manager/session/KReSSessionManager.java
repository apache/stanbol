package eu.iksproject.kres.api.manager.session;

import java.io.OutputStream;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;

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
public interface KReSSessionManager extends SessionListenable {

	public Set<IRI> getRegisteredSessionIDs();

	/**
	 * Generates AND REGISTERS a new KReS session and assigns a unique session
	 * ID generated internally.
	 * 
	 * @return the generated KReS session
	 */
	public KReSSession createSession();

	/**
	 * Generates AND REGISTERS a new KReS session and tries to assign it the
	 * supplied session ID. If a session with that ID is already registered, the
	 * new session is <i>not</i> created and a
	 * <code>DuplicateSessionIDException</code> is thrown.
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
	 * Returns the ontology space associated with this session.
	 * 
	 * @return the session space
	 */
	public Set<SessionOntologySpace> getSessionSpaces(IRI sessionID)
			throws NonReferenceableSessionException;

	/**
	 * Stores the KReS session identified by <code>sessionID</code> using the
	 * output stream <code>out</code>.
	 * 
	 * @param sessionID
	 *            the IRI that uniquely identifies the session
	 * @param out
	 *            the output stream to store the session
	 */
	public void storeSession(IRI sessionID, OutputStream out)
			throws NonReferenceableSessionException;

}
