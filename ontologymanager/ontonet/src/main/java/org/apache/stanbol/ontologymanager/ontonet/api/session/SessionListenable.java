package org.apache.stanbol.ontologymanager.ontonet.api.session;

import java.util.Collection;

public interface SessionListenable {

	/**
	 * Adds the given SessionListener to the pool of registered listeners.
	 * 
	 * @param listener
	 *            the session listener to be added
	 */
    void addSessionListener(SessionListener listener);

	/**
	 * Clears the pool of registered session listeners.
	 */
    void clearSessionListeners();

	/**
	 * Returns all the registered session listeners. It is up to developers to
	 * decide whether implementations should return sets (unordered but without
	 * redundancy), lists (e.g. in the order they wer registered but potentially
	 * redundant) or other data structures that implement {@link Collection}.
	 * 
	 * @return a collection of registered session listeners.
	 */
    Collection<SessionListener> getSessionListeners();

	/**
	 * Removes the given SessionListener from the pool of active listeners.
	 * 
	 * @param listener
	 *            the session listener to be removed
	 */
    void removeSessionListener(SessionListener listener);

}
