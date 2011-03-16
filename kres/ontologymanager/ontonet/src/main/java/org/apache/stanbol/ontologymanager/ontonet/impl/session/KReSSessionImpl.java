package org.apache.stanbol.ontologymanager.ontonet.impl.session;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.api.session.KReSSession;
import org.apache.stanbol.ontologymanager.ontonet.api.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionListener;
import org.apache.stanbol.ontologymanager.ontonet.api.session.KReSSession.State;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent.OperationType;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation of the {@link KReSSession} interface. A
 * KReSSessionImpl is initially inactive and creates its own identifier.
 * 
 * @author alessandro
 * 
 */
public class KReSSessionImpl implements KReSSession {

	/**
	 * A KReS session knows about its own ID.
	 */
	protected IRI id = null;
	protected Set<SessionListener> listeners;

	/**
	 * A KReS session knows about its own state.
	 */
	State state = State.HALTED;

	/**
	 * Utility constructor for enforcing a given IRI as a session ID. It will
	 * not throw duplication exceptions, since a KReS session does not know
	 * about other sessions.
	 * 
	 * @param sessionID
	 *            the IRI to be set as unique identifier for this session
	 */
	public KReSSessionImpl(IRI sessionID) {
		this.id = sessionID;
		listeners = new HashSet<SessionListener>();
	}

	public KReSSessionImpl(IRI sessionID, State initialState)
			throws NonReferenceableSessionException {
		this(sessionID);
		if (initialState == State.ZOMBIE)
			throw new NonReferenceableSessionException();
		else
			setActive(initialState == State.ACTIVE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionListenable#addSessionListener
	 * (eu.iksproject.kres.api.manager.session.SessionListener)
	 */
	@Override
	public void addSessionListener(SessionListener listener) {
		listeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeeu.iksproject.kres.api.manager.session.SessionListenable#
	 * clearSessionListeners()
	 */
	@Override
	public void clearSessionListeners() {
		listeners.clear();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.api.manager.session.KReSSession#close()
	 */
	@Override
	public void close() throws NonReferenceableSessionException {
		// if (getSessionState() == State.ZOMBIE)
		// throw new NonReferenceableSessionException();
		// state = State.ZOMBIE;
		this.setActive(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.api.manager.session.KReSSession#getID()
	 */
	@Override
	public IRI getID() {
		return id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionListenable#getSessionListeners
	 * ()
	 */
	@Override
	public Collection<SessionListener> getSessionListeners() {
		return listeners;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.api.manager.session.KReSSession#getSessionState()
	 */
	@Override
	public State getSessionState() {
		return state;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.iksproject.kres.api.manager.session.KReSSession#isActive()
	 */
	@Override
	public boolean isActive() {
		return state == State.ACTIVE;
	}

	@Override
	public void removeSessionListener(SessionListener listener) {
		listeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.KReSSession#setActive(boolean)
	 */
	@Override
	public State setActive(boolean active)
			throws NonReferenceableSessionException {
		if (getSessionState() == State.ZOMBIE)
			throw new NonReferenceableSessionException();
		else
			state = active ? State.ACTIVE : State.HALTED;
		return getSessionState();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getID().toString();
	}

	protected void fireClosed() {
		SessionEvent e = null;
		try {
			e = new SessionEvent(this, OperationType.CLOSE);
		} catch (Exception e1) {
			LoggerFactory.getLogger(getClass()).error(
					"KReS :: Could not close session " + getID(), e1);
			return;
		}
		for (SessionListener l : listeners)
			l.sessionChanged(e);
	}

	@Override
	public void open() throws NonReferenceableSessionException {
		setActive(true);
	}

}
