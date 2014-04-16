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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.session;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.ontologymanager.core.session.TimestampedSessionIDGenerator;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.impl.SessionImpl;
import org.apache.stanbol.ontologymanager.ontonet.api.OntologyNetworkConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.collector.MissingOntologyException;
import org.apache.stanbol.ontologymanager.servicesapi.collector.OntologyCollectorListener;
import org.apache.stanbol.ontologymanager.servicesapi.io.StoredOntologySource;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.Multiplexer;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OWLExportable.ConnectivityPolicy;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeEventListener;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.servicesapi.session.DuplicateSessionIDException;
import org.apache.stanbol.ontologymanager.servicesapi.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session.State;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionEvent;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionEvent.OperationType;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionIDGenerator;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionLimitException;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionListener;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.osgi.service.component.ComponentContext;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
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
@Service({SessionManager.class, org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager.class})
public class SessionManagerImpl implements
        org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager, ScopeEventListener {

    public static final String _CONNECTIVITY_POLICY_DEFAULT = "TIGHT";
    public static final String _ID_DEFAULT = "session";
    public static final int _MAX_ACTIVE_SESSIONS_DEFAULT = -1;
    public static final String _ONTOLOGY_NETWORK_NS_DEFAULT = "http://localhost:8080/ontonet/";

    private static SessionManagerImpl me = null;

    public static SessionManagerImpl get() {
        return me;
    }

    /**
     * Concatenated with the sessionManager ID, it identifies the Web endpoint and default base URI for all
     * sessions.
     */
    private IRI baseNS;

    @Property(name = SessionManager.CONNECTIVITY_POLICY, options = {
                                                                    @PropertyOption(value = '%'
                                                                                            + SessionManager.CONNECTIVITY_POLICY
                                                                                            + ".option.tight", name = "TIGHT"),
                                                                    @PropertyOption(value = '%'
                                                                                            + SessionManager.CONNECTIVITY_POLICY
                                                                                            + ".option.loose", name = "LOOSE")}, value = _CONNECTIVITY_POLICY_DEFAULT)
    private String connectivityPolicyString;

    @Property(name = SessionManager.ID, value = _ID_DEFAULT)
    protected String id;

    protected SessionIDGenerator idgen;

    protected Set<SessionListener> listeners;

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Property(name = SessionManager.MAX_ACTIVE_SESSIONS, intValue = _MAX_ACTIVE_SESSIONS_DEFAULT)
    private int maxSessions;

    @Reference
    private OfflineConfiguration offline;

    @Reference
    // (cardinality = ReferenceCardinality.MANDATORY_UNARY, policy = ReferencePolicy.DYNAMIC, bind =
    // "bindScopeManager", unbind = "unbindScopeManager", strategy = ReferenceStrategy.EVENT)
    private ScopeRegistry scopeRegistry;

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
     * To be invoked by non-OSGi environments.
     * 
     * @param the
     *            ontology provider that will store and provide ontologies for this session manager.
     * @param configuration
     */
    public SessionManagerImpl(OntologyProvider<?> ontologyProvider,
                              OfflineConfiguration offline,
                              Dictionary<String,Object> configuration) {
        this(ontologyProvider, null, offline, configuration);
    }

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param the
     *            ontology provider that will store and provide ontologies for this session manager.
     * @param configuration
     */
    public SessionManagerImpl(OntologyProvider<?> ontologyProvider,
                              ScopeRegistry scopeRegistry,
                              OfflineConfiguration offline,
                              Dictionary<String,Object> configuration) {
        this();
        this.ontologyProvider = ontologyProvider;
        this.scopeRegistry = scopeRegistry;
        this.offline = offline;
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

        long before = System.currentTimeMillis();
        me = this;
        // Parse configuration
        id = (String) configuration.get(SessionManager.ID);
        if (id == null) id = _ID_DEFAULT;
        String s = null;
        try {
            setDefaultNamespace(offline.getDefaultOntologyNetworkNamespace());
        } catch (Exception e) {
            log.warn("Invalid namespace {}. Setting to default value {}",
                offline.getDefaultOntologyNetworkNamespace(), _ONTOLOGY_NETWORK_NS_DEFAULT);
            setDefaultNamespace(IRI.create(_ONTOLOGY_NETWORK_NS_DEFAULT));
        }
        try {
            s = (String) configuration.get(SessionManager.MAX_ACTIVE_SESSIONS);
            maxSessions = Integer.parseInt(s);
        } catch (Exception e) {
            log.warn("Invalid session limit {}. Setting to default value {}",
                configuration.get(SessionManager.MAX_ACTIVE_SESSIONS), _MAX_ACTIVE_SESSIONS_DEFAULT);
            maxSessions = _MAX_ACTIVE_SESSIONS_DEFAULT;
        }

        if (id == null || id.isEmpty()) {
            log.warn("The Ontology Network Manager configuration does not define a ID for the Ontology Network Manager");
        }

        idgen = new TimestampedSessionIDGenerator();

        Object connectivityPolicy = configuration.get(SessionManager.CONNECTIVITY_POLICY);
        if (connectivityPolicy == null) {
            this.connectivityPolicyString = _CONNECTIVITY_POLICY_DEFAULT;
        } else {
            this.connectivityPolicyString = connectivityPolicy.toString();
        }

        // Add listeners
        if (ontologyProvider instanceof SessionListener) this
                .addSessionListener((SessionListener) ontologyProvider);
        this.addSessionListener(ontologyProvider.getOntologyNetworkDescriptor());

        if (scopeRegistry != null) scopeRegistry.addScopeRegistrationListener(this);

        // Rebuild sessions
        rebuildSessions();

        log.debug(SessionManager.class + " activated. Time : {} ms.", System.currentTimeMillis() - before);

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
        if (sessionsByID.containsKey(sessionID)) throw new DuplicateSessionIDException(sessionID);
        checkSessionLimit();
        IRI ns = IRI.create(getDefaultNamespace() + getID() + "/");
        Session session = new SessionImpl(sessionID, ns, ontologyProvider);

        // Have the ontology provider listen to ontology events
        if (ontologyProvider instanceof OntologyCollectorListener) session
                .addOntologyCollectorListener((OntologyCollectorListener) ontologyProvider);
        if (ontologyProvider instanceof SessionListener) session
                .addSessionListener((SessionListener) ontologyProvider);

        Multiplexer multiplexer = ontologyProvider.getOntologyNetworkDescriptor();
        session.addOntologyCollectorListener(multiplexer);
        session.addSessionListener(multiplexer);

        ConnectivityPolicy policy;
        try {
            policy = ConnectivityPolicy.valueOf(connectivityPolicyString);
        } catch (IllegalArgumentException e) {
            log.warn("The value {}", connectivityPolicyString);
            log.warn(" -- configured as default ConnectivityPolicy does not match any value of the Enumeration!");
            log.warn(" -- Setting the default policy as defined by the {}.", ConnectivityPolicy.class);
            policy = ConnectivityPolicy.valueOf(_CONNECTIVITY_POLICY_DEFAULT);
        }
        session.setConnectivityPolicy(policy);

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
            log.warn("Tried to kick a dead horse on session \"{}\" which was already in a zombie state.",
                sessionID);
        }
    }

    protected void fireSessionCreated(Session session) {
        SessionEvent e = new SessionEvent(session, OperationType.CREATE);
        for (SessionListener l : listeners)
            l.sessionChanged(e);

    }

    protected void fireSessionDestroyed(Session session) {
        SessionEvent e = new SessionEvent(session, OperationType.KILL);
        for (SessionListener l : listeners)
            l.sessionChanged(e);
    }

    @Override
    public int getActiveSessionLimit() {
        return maxSessions;
    }

    @Override
    public IRI getDefaultNamespace() {
        return baseNS;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public IRI getNamespace() {
        return getDefaultNamespace();
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

    private void rebuildSessions() {
        if (ontologyProvider == null) {
            log.warn("No ontology provider supplied. Cannot rebuild sessions");
            return;
        }
        OntologyNetworkConfiguration struct = ontologyProvider.getOntologyNetworkConfiguration();
        for (String sessionId : struct.getSessionIDs()) {
            long before = System.currentTimeMillis();
            log.debug("Rebuilding session with ID \"{}\"", sessionId);
            Session session;
            try {
                session = createSession(sessionId);
            } catch (DuplicateSessionIDException e) {
                log.warn("Session \"{}\" already exists and will be reused.", sessionId);
                session = getSession(sessionId);
            } catch (SessionLimitException e) {
                log.error("Cannot create session {}. Session limit of {} reached.", sessionId,
                    getActiveSessionLimit());
                break;
            }
            // Register even if some ontologies were to fail to be restored afterwards.
            sessionsByID.put(sessionId, session);
            session.setActive(false); // Restored sessions are inactive at first.
            for (OWLOntologyID key : struct.getOntologyKeysForSession(sessionId))
                try {
                    session.addOntology(new StoredOntologySource(key));
                } catch (MissingOntologyException ex) {
                    log.error(
                        "Could not find an ontology with public key {} to be managed by session \"{}\". Proceeding to next ontology.",
                        key, sessionId);
                    continue;
                } catch (Exception ex) {
                    log.error("Exception caught while trying to add ontology with public key " + key
                              + " to rebuilt session \"" + sessionId + "\". Proceeding to next ontology.", ex);
                    continue;
                }
            for (String scopeId : struct.getAttachedScopes(sessionId)) {
                /*
                 * The scope is attached by reference, so we won't have to bother checking if the scope has
                 * been rebuilt by then (which could not happen if the SessionManager is being activated
                 * first).
                 */
                session.attachScope(scopeId);
            }
            log.info("Session \"{}\" rebuilt in {} ms.", sessionId, System.currentTimeMillis() - before);
        }
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
    public void scopeActivated(Scope scope) {}

    @Override
    public void scopeCreated(Scope scope) {}

    @Override
    public void scopeDeactivated(Scope scope) {
        for (String sid : getRegisteredSessionIDs())
            getSession(sid).detachScope(scope.getID());
    }

    @Override
    public void scopeRegistered(Scope scope) {}

    @Override
    public void scopeUnregistered(Scope scope) {
        for (String sid : getRegisteredSessionIDs())
            getSession(sid).detachScope(scope.getID());
    }

    @Override
    public void setActiveSessionLimit(int limit) {
        this.maxSessions = limit;
    }

    @Override
    public void setDefaultNamespace(IRI namespace) {
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
            this.baseNS = IRI.create(namespace + "/");
            return;
        }
        this.baseNS = namespace;
    }

    @Override
    public void setNamespace(IRI namespace) {
        setDefaultNamespace(namespace);
    }

    @Override
    public void storeSession(String sessionID, OutputStream out) throws NonReferenceableSessionException,
                                                                OWLOntologyStorageException {
        throw new UnsupportedOperationException(
                "Not necessary. Session content is always stored by default in the current implementation.");
    }

}
