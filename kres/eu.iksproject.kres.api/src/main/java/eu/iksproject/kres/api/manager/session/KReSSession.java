package eu.iksproject.kres.api.manager.session;

import org.semanticweb.owlapi.model.IRI;

/**
 * Note that KReS sessions are possibly disjoint with HTTP sessions or the like.
 * 
 * @author alessandro
 * 
 */
public interface KReSSession extends SessionListenable {

	/**
	 * The states a KReS session can be in: ACTIVE (for running sessions),
	 * HALTED (for inactive sessions that may later be activated, e.g. when a
	 * user logs in), ZOMBIE (inactive and bound for destruction, no longer
	 * referenceable).
	 * 
	 * @author alessandro
	 * 
	 */
	public enum State {
		/**
		 * Running session
		 */
		ACTIVE,
		/**
		 * inactive sessions that may later be activated
		 */
		HALTED,
		/**
		 * Inactive and bound for destruction, no longer referenceable
		 */
		ZOMBIE
	}

	/**
	 * Closes this KReS Session irreversibly. Most likely includes setting the
	 * state to ZOMBIE.
	 */
	public void close() throws NonReferenceableSessionException;

	/**
	 * Returns the unique Internationalized Resource Identifier (IRI) that
	 * identifies this KReS session.<br>
	 * <br>
	 * NOTE: There is no set method for the session ID as it is assumed to be
	 * set in its constructor once and for all.
	 * 
	 * @return the IRI that identifies this session
	 */
	public IRI getID();

	/**
	 * Returns the current state of this KReS session.
	 * 
	 * @return the state of this session
	 */
	public State getSessionState();

	/**
	 * Equivalent to <code>getState() == State.ACTIVE</code>.
	 * 
	 * @return true iff this session is in the ACTIVE state
	 */
	public boolean isActive();
	
	public void open() throws NonReferenceableSessionException;

	/**
	 * Sets the KReS session as ACTIVE if <code>active</code> is true, INACTIVE
	 * otherwise. The state set is returned, which should match the input state
	 * unless an error occurs.<br>
	 * <br>
	 * Should throw an exception if this session is in a ZOMBIE state.
	 * 
	 * @param active
	 *            the desired activity state for this session
	 * @return the resulting state of this KReS session
	 */
	public State setActive(boolean active)
			throws NonReferenceableSessionException;

}
