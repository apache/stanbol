/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.ontologymanager.ontonet.impl.session;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.session.DuplicateSessionIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session.State;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent.OperationType;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionIDGenerator;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionListener;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Calls to <code>getSessionListeners()</code> return a {@link Set} of listeners.
 * 
 * TODO: implement storage (using persistence layer).
 * 
 * @author alexdma
 * 
 */
@Component(immediate = true, metatype = true)
@Service(SessionManager.class)
public class SessionManagerImpl implements SessionManager {

    public static final String _ID_DEFAULT = "session";
    public static final int _MAX_ACTIVE_SESSIONS_DEFAULT = -1;
    public static final String _ONTOLOGY_NETWORK_NS_DEFAULT = "http://localhost:8080/ontonet/";

    /**
     * Concatenated with the sessionManager ID, it identifies the Web endpoint and default base URI for all
     * sessions.
     */
    @Property(name = SessionManager.SESSIONS_NS, value = _ONTOLOGY_NETWORK_NS_DEFAULT)
    private String baseNS;

    @Property(name = SessionManager.ID, value = _ID_DEFAULT)
    protected String id;

    protected SessionIDGenerator idgen;

    protected Set<SessionListener> listeners;

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Property(name = SessionManager.MAX_ACTIVE_SESSIONS, intValue = _MAX_ACTIVE_SESSIONS_DEFAULT)
    private int maxSessions;

    @Reference
    private OntologyProvider<?> ontologyProvider;

    private Map<String,Session> sessionsByID;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ReengineerManagerImpl instances do need to be configured!
     * YOU NEED TO USE {@link #SessionManagerImpl(OntologyProvider, Dictionary)} to parse the configuration
     * and then initialise the rule store if running outside an OSGI environment.
     */
    public SessionManagerImpl() {
        super();
        listeners = new HashSet<SessionListener>();
        sessionsByID = new HashMap<String,Session>();
    }

    /**
     * @deprecated In non-OSGi+DS environments, please invoke
     *             {@link #SessionManagerImpl(IRI, OntologyProvider, Dictionary)}. With this constructor,
     *             baseIri and scopeRegistry are ignored.
     * 
     * @param baseIri
     * @param scopeRegistry
     * @param ontologyProvider
     */
    public SessionManagerImpl(IRI baseIri, ScopeRegistry scopeRegistry, OntologyProvider<?> ontologyProvider) {
        this(ontologyProvider, new Hashtable<String,Object>());
    }

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param the
     *            ontology provider that will store and provide ontologies for this session manager.
     * @param configuration
     */
    public SessionManagerImpl(OntologyProvider<?> ontologyProvider, Dictionary<String,Object> configuration) {
        this();
        this.ontologyProvider = ontologyProvider;
        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + SessionManagerImpl.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        // Parse configuration
        id = (String) configuration.get(SessionManager.ID);
        if (id == null) id = _ID_DEFAULT;
        String s = null;
        try {
            s = (String) configuration.get(SessionManager.SESSIONS_NS);
            setNamespace(IRI.create(s));
        } catch (Exception e) {
            log.warn("Invalid namespace {}. Setting to default value {}", s, _ONTOLOGY_NETWORK_NS_DEFAULT);
            setNamespace(IRI.create(_ONTOLOGY_NETWORK_NS_DEFAULT));
        }
        try {
            s = (String) configuration.get(SessionManager.MAX_ACTIVE_SESSIONS);
            maxSessions = Integer.parseInt(s);
        } catch (Exception e) {
            log.warn("Invalid session limit {}. Setting to default value {}", s, _MAX_ACTIVE_SESSIONS_DEFAULT);
            maxSessions = _MAX_ACTIVE_SESSIONS_DEFAULT;
        }

        if (id == null || id.isEmpty()) {
            log.warn("The Ontology Network Manager configuration does not define a ID for the Ontology Network Manager");
        }

        idgen = new TimestampedSessionIDGenerator(IRI.create(getNamespace() + getID() + "/"));
    }

    protected synchronized void addSession(Session session) {
        sessionsByID.put(session.getID(), session);
    }

    @Override
    public void addSessionListener(SessionListener listener) {
        listeners.add(listener);
    }

    private void checkSessionLimit() throws SessionLimitException {
        if (maxSessions >= 0 && sessionsByID.size() >= maxSessions) throw new SessionLimitException(
                maxSessions, "Cannot create new session. Limit of " + maxSessions + " already raeached.");
    }

    @Override
    public void clearSessionListeners() {
        listeners.clear();
    }

    @Override
    public Session createSession() throws SessionLimitException {
        checkSessionLimit();
        Set<String> exclude = getRegisteredSessionIDs();
        Session session = null;
        while (session == null)
            try {
                session = createSession(idgen.createSessionID(exclude));
            } catch (DuplicateSessionIDException e) {
                exclude.add(e.getDuplicateID());
                continue;
            }
        return session;
    }

    @Override
    public synchronized Session createSession(String sessionID) throws DuplicateSessionIDException,
                                                               SessionLimitException {
        /*
         * Throw the duplicate ID exception first, in case developers decide to reuse the existing session
         * before creating a new one.
         */
        if (sessionsByID.containsKey(sessionID)) throw new DuplicateSessionIDException(sessionID.toString());
        checkSessionLimit();
        IRI ns = IRI.create(getNamespace() + getID() + "/");
        Session session = new SessionImpl(sessionID, ns, ontologyProvider);
        addSession(session);
        fireSessionCreated(session);
        return session;
    }

    /**
     * Deactivation of the ONManagerImpl resets all its resources.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        id = null;
        baseNS = null;
        maxSessions = 0; // No sessions allowed for an inactive component.
        log.info("in " + SessionManagerImpl.class + " deactivate with context " + context);
    }

    @Override
    public synchronized void destroySession(String sessionID) {
        try {
            Session ses = sessionsByID.get(sessionID);
            if (ses == null) log.warn(
                "Tried to destroy nonexisting session {} . Could it have been previously destroyed?",
                sessionID);
            else {
                ses.close();
                if (ses instanceof SessionImpl) ((SessionImpl) ses).state = State.ZOMBIE;
                // Make session no longer referenceable
                removeSession(ses);
                fireSessionDestroyed(ses);
            }
        } catch (NonReferenceableSessionException e) {
            log.warn("Tried to kick a dead horse on session " + sessionID
                     + " which was already in a zombie state.", e);
        }
    }

    protected void fireSessionCreated(Session session) {
        SessionEvent e;
        try {
            e = new SessionEvent(session, OperationType.CREATE);
            for (SessionListener l : listeners)
                l.sessionChanged(e);
        } catch (Exception e1) {
            log.error("An error occurred while attempting to fire session creation event for session "
                      + session.getID(), e1);
            return;
        }

    }

    protected void fireSessionDestroyed(Session session) {
        SessionEvent e;
        try {
            e = new SessionEvent(session, OperationType.KILL);
            for (SessionListener l : listeners)
                l.sessionChanged(e);
        } catch (Exception e1) {
            log.error("An error occurred while attempting to fire session destruction event for session "
                      + session.getID(), e1);
            return;
        }

    }

    @Override
    public int getActiveSessionLimit() {
        return maxSessions;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public IRI getNamespace() {
        return IRI.create(baseNS);
    }

    @Override
    public Set<String> getRegisteredSessionIDs() {
        return sessionsByID.keySet();
    }

    @Override
    public Session getSession(String sessionID) {
        return sessionsByID.get(sessionID);
    }

    @Override
    public Collection<SessionListener> getSessionListeners() {
        return listeners;
    }

    @Override
    public Set<SessionOntologySpace> getSessionSpaces(String sessionID) throws NonReferenceableSessionException {
        throw new UnsupportedOperationException(
                "Session Manager is now agnostic to scopes, and session spaces are deprecated. Please perform CRUD operations on Session objects directly.");
        // Set<SessionOntologySpace> result = new HashSet<SessionOntologySpace>();
        // // Brute force search
        // for (OntologyScope scope : scopeRegistry.getRegisteredScopes()) {
        // SessionOntologySpace space = scope.getSessionSpace(sessionID);
        // if (space != null) result.add(space);
        // }
        // return result;
    }

    protected synchronized void removeSession(Session session) {
        String id = session.getID();
        Session s2 = sessionsByID.get(id);
        if (session == s2) sessionsByID.remove(id);
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setActiveSessionLimit(int limit) {
        this.maxSessions = limit;
    }

    @Override
    public void setNamespace(IRI namespace) {
        if (namespace == null) throw new IllegalArgumentException("Namespace cannot be null.");
        if (namespace.toURI().getQuery() != null) throw new IllegalArgumentException(
                "URI Query is not allowed in OntoNet namespaces.");
        if (namespace.toURI().getFragment() != null) throw new IllegalArgumentException(
                "URI Fragment is not allowed in OntoNet namespaces.");
        if (namespace.toString().endsWith("#")) throw new IllegalArgumentException(
                "OntoNet namespaces must not end with a hash ('#') character.");
        if (!namespace.toString().endsWith("/")) {
            log.warn("Namespace {} does not end with slash character ('/'). It will be added automatically.",
                namespace);
            this.baseNS = namespace + "/";
            return;
        }
        this.baseNS = namespace.toString();
    }

    @Override
    public void storeSession(String sessionID, OutputStream out) throws NonReferenceableSessionException,
                                                                OWLOntologyStorageException {
        throw new UnsupportedOperationException(
                "Session content is always stored by default in the current implementation.");
        /*
         * For each gession space in the session save all the ontologies contained in the space.
         */
        // for (SessionOntologySpace so : getSessionSpaces(sessionID)) {
        // for (OWLOntology owlOntology : so.getOntologies(true)) {
        //
        // // store.store(owlOntology);
        //
        // }
        // }

    }

}
