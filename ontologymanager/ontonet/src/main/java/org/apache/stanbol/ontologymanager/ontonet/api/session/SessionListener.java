package org.apache.stanbol.ontologymanager.ontonet.api.session;

/**
 * Objects that want to listen to events affecting KReS sessions should
 * implement this interface and add themselves as listener to a manager.
 * 
 * @author alessandro
 * 
 */
public interface SessionListener {

	/**
	 * Called whenever an event affecting a KReS session is fired.
	 * 
	 * @param event
	 *            the session event.
	 */
	public void sessionChanged(SessionEvent event);

}
