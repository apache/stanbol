package eu.iksproject.kres.manager.session;

import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.session.DuplicateSessionIDException;
import eu.iksproject.kres.api.manager.session.KReSSession;
import eu.iksproject.kres.api.manager.session.KReSSessionIDGenerator;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.manager.session.NonReferenceableSessionException;
import eu.iksproject.kres.api.manager.session.SessionEvent;
import eu.iksproject.kres.api.manager.session.SessionListener;
import eu.iksproject.kres.api.manager.session.KReSSession.State;
import eu.iksproject.kres.api.manager.session.SessionEvent.OperationType;
import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.manager.ONManager;

/**
 * 
 * Calls to <code>getSessionListeners()</code> return a {@link Set} of
 * listeners.
 * 
 * TODO: implement storage (using persistence layer).
 * 
 * @author alessandro
 * 
 */
public class KReSSessionManagerImpl implements KReSSessionManager {

	private Map<IRI, KReSSession> sessionsByID;

	protected Set<SessionListener> listeners;

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected KReSSessionIDGenerator idgen;

	protected ScopeRegistry scopeRegistry;
	protected OntologyStorage store;

	public KReSSessionManagerImpl(IRI baseIri, ScopeRegistry scopeRegistry, OntologyStorage store) {
		idgen = new TimestampedSessionIDGenerator(baseIri);
		listeners = new HashSet<SessionListener>();
		sessionsByID = new HashMap<IRI, KReSSession>();
this.scopeRegistry = scopeRegistry;
		this.store = store;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#addSessionListener
	 * (eu.iksproject.kres.api.manager.session.SessionListener)
	 */
	@Override
	public void addSessionListener(SessionListener listener) {
		listeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#clearSessionListeners
	 * ()
	 */
	@Override
	public void clearSessionListeners() {
		listeners.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#createSession()
	 */
	@Override
	public KReSSession createSession() {
		Set<IRI> exclude = getRegisteredSessionIDs();
		KReSSession session = null;
		while (session == null)
			try {
				session = createSession(idgen.createSessionID(exclude));
			} catch (DuplicateSessionIDException e) {
				exclude.add(e.getDulicateID());
				continue;
			}
		return session;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#createSession(org
	 * .semanticweb.owlapi.model.IRI)
	 */
	@Override
	public KReSSession createSession(IRI sessionID)
			throws DuplicateSessionIDException {
		if (sessionsByID.containsKey(sessionID))
			throw new DuplicateSessionIDException(sessionID);
		KReSSession session = new KReSSessionImpl(sessionID);
		addSession(session);
		fireSessionCreated(session);
		return session;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#destroySession(
	 * org.semanticweb.owlapi.model.IRI)
	 */
	@Override
	public void destroySession(IRI sessionID) {
		try {
			KReSSession ses = sessionsByID.get(sessionID);
			ses.close();
			if (ses instanceof KReSSessionImpl)
				((KReSSessionImpl) ses).state = State.ZOMBIE;
			// Make session no longer referenceable
			removeSession(ses);
			fireSessionDestroyed(ses);
		} catch (NonReferenceableSessionException e) {
			log.warn(
					"KReS :: tried to kick a dead horse on session "
							+ sessionID
							+ " which was already in a zombie state.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#getSession(org.
	 * semanticweb.owlapi.model.IRI)
	 */
	@Override
	public KReSSession getSession(IRI sessionID) {
		return sessionsByID.get(sessionID);
	}

	@Override
	public Set<IRI> getRegisteredSessionIDs() {
		return sessionsByID.keySet();
	}

	protected void fireSessionCreated(KReSSession session) {
		SessionEvent e;
		try {
			e = new SessionEvent(session, OperationType.CREATE);
			for (SessionListener l : listeners)
				l.sessionChanged(e);
		} catch (Exception e1) {
			LoggerFactory
					.getLogger(getClass())
					.error(
							"KReS :: Exception occurred while attempting to fire session creation event for session "
									+ session.getID(), e1);
			return;
		}

	}

	protected void fireSessionDestroyed(KReSSession session) {
		SessionEvent e;
		try {
			e = new SessionEvent(session, OperationType.KILL);
			for (SessionListener l : listeners)
				l.sessionChanged(e);
		} catch (Exception e1) {
			LoggerFactory
					.getLogger(getClass())
					.error(
							"KReS :: Exception occurred while attempting to fire session destruction event for session "
									+ session.getID(), e1);
			return;
		}

	}

	protected void addSession(KReSSession session) {
		sessionsByID.put(session.getID(), session);
	}

	protected void removeSession(KReSSession session) {
		IRI id = session.getID();
		KReSSession s2 = sessionsByID.get(id);
		if (session == s2)
			sessionsByID.remove(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#getSessionListeners
	 * ()
	 */
	@Override
	public Collection<SessionListener> getSessionListeners() {
		return listeners;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * TODO : optimize with indexing.
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#getSessionSpaces
	 * (org.semanticweb.owlapi.model.IRI)
	 */
	@Override
	public Set<SessionOntologySpace> getSessionSpaces(IRI sessionID)
			throws NonReferenceableSessionException {
		Set<SessionOntologySpace> result = new HashSet<SessionOntologySpace>();
		// Brute force search
		for (OntologyScope scope : scopeRegistry
				.getRegisteredScopes()) {
			SessionOntologySpace space = scope.getSessionSpace(sessionID);
			if (space != null)
				result.add(space);
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#removeSessionListener
	 * (eu.iksproject.kres.api.manager.session.SessionListener)
	 */
	@Override
	public void removeSessionListener(SessionListener listener) {
		listeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * TODO : storage not implemented yet
	 * 
	 * @see
	 * eu.iksproject.kres.api.manager.session.SessionManager#storeSession(org
	 * .semanticweb.owlapi.model.IRI, java.io.OutputStream)
	 */
	@Override
	public void storeSession(IRI sessionID, OutputStream out)
			throws NonReferenceableSessionException {
		for (SessionOntologySpace so : getSessionSpaces(sessionID))
			for (OWLOntology o : so.getOntologies())
				store.store(o);
	}

}
