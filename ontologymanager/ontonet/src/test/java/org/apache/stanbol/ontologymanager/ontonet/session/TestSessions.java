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
package org.apache.stanbol.ontologymanager.ontonet.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.ontonet.Constants;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.collector.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.api.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session.State;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionManager;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.session.SessionManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class TestSessions {

    public static IRI baseIri = IRI.create(Constants.PEANUTS_MAIN_BASE), baseIri2 = IRI
            .create(Constants.PEANUTS_MINOR_BASE);

    public static String scopeId1 = "Ranma12", scopeId2 = "HokutoNoKen", scopeId3 = "Doraemon";

    private static OntologyScopeFactory scopeFactory = null;

    private static ScopeRegistry scopeRegistry = null;

    private static SessionManager sesmgr = null;

    private static OntologySpaceFactory spaceFactory = null;

    private static OntologyInputSource src1 = null, src2 = null;

    @BeforeClass
    public static void setup() {
        Dictionary<String,Object> onmconf = new Hashtable<String,Object>();
        // An ONManagerImpl with no store and default settings
        ONManager onm = new ONManagerImpl(null, null, new OfflineConfigurationImpl(onmconf), onmconf);
        sesmgr = new SessionManagerImpl(null, onmconf);
        scopeFactory = onm.getOntologyScopeFactory();
        spaceFactory = onm.getOntologySpaceFactory();
        scopeRegistry = onm.getScopeRegistry();
        assertNotNull(spaceFactory);
        assertNotNull(scopeFactory);
        OWLOntologyManager mgr = OWLOntologyManagerFactory.createOWLOntologyManager(null);
        try {
            src1 = new RootOntologySource(mgr.createOntology(baseIri), null);
            src2 = new RootOntologySource(mgr.createOntology(baseIri2), null);
        } catch (OWLOntologyCreationException e) {
            fail("Could not setup ontology with base IRI " + Constants.PEANUTS_MAIN_BASE);
        }
    }

    @Test
    public void testCreateSessionSpaceManual() throws Exception {
        OntologyScope scope = null;
        try {
            // we don't register it
            scope = scopeFactory.createOntologyScope(scopeId1, src1, src2);
            scope.setUp();
        } catch (DuplicateIDException e) {
            fail("Unexpected DuplicateIDException was caught while testing scope " + e.getDuplicateID());
        }
        assertNotNull(scope);
        Session ses = sesmgr.createSession();
        assertTrue(scope.getSessionSpaces().isEmpty());
        // scope.addSessionSpace(spaceFactory.createSessionOntologySpace(scopeId1), ses.getID());
        // assertFalse(scope.getSessionSpaces().isEmpty());
    }

    @Test
    public void testCreateSessionSpaceAutomatic() throws Exception {
        OntologyScope scope1 = null, scope2 = null, scope3 = null;
        try {
            scope1 = scopeFactory.createOntologyScope(scopeId1, src1, src2);
            scopeRegistry.registerScope(scope1);
            scope2 = scopeFactory.createOntologyScope(scopeId2, src2, src1);
            scopeRegistry.registerScope(scope2);
            scope3 = scopeFactory.createOntologyScope(scopeId3, src2, src2);
            scopeRegistry.registerScope(scope3);
            // We do all activations after registering, otherwise the component
            // property value will override these activations.
            scopeRegistry.setScopeActive(scopeId1, true);
            scopeRegistry.setScopeActive(scopeId2, false);
            scopeRegistry.setScopeActive(scopeId3, true);
        } catch (DuplicateIDException e) {
            fail("Unexpected DuplicateIDException was caught while testing scope " + e.getDuplicateID());
        }
        Session ses = sesmgr.createSession();
        String sesid = ses.getID();
        // FIXME replace with proper tests
        // assertFalse(scope1.getSessionSpaces().isEmpty());
        // assertNotNull(scope1.getSessionSpace(sesid));
        // assertFalse(scope3.getSessionSpaces().isEmpty());
        // assertNull(scope2.getSessionSpace(sesid));
        // assertNotNull(scope3.getSessionSpace(sesid));
    }

    @Test
    public void testRegisterSession() throws Exception {
        int before = sesmgr.getRegisteredSessionIDs().size();
        Session ses = sesmgr.createSession();
        assertNotNull(ses);
        assertEquals(before + 1, sesmgr.getRegisteredSessionIDs().size());
    }

    @Test
    public void testSessionCreationDestruction() throws Exception {
        int size = 100;
        int initialSize = sesmgr.getRegisteredSessionIDs().size();
        Set<Session> sessions = new HashSet<Session>();
        // Create and open many sessions.
        synchronized (sesmgr) {
            for (int i = 0; i < size; i++) {
                Session ses = sesmgr.createSession();
                try {
                    ses.open();
                } catch (NonReferenceableSessionException e) {
                    fail("Test method tried to open nonreferenceable session.");
                }
                sessions.add(ses);
            }
            // Check that 500 sessions have been created
            assertEquals(initialSize + size, sesmgr.getRegisteredSessionIDs().size());
        }
        boolean open = true;
        for (Session ses : sessions)
            open &= ses.getSessionState() == State.ACTIVE;
        // Check that all created sessions have been opened
        assertTrue(open);
        // Kill 'em all, to quote Metallica
        synchronized (sesmgr) {
            for (Session ses : sessions)
                sesmgr.destroySession(ses.getID());
            assertEquals(initialSize, sesmgr.getRegisteredSessionIDs().size());
        }
        // Check that they are all zombies
        boolean zombi = true;
        for (Session ses : sessions)
            zombi &= ses.getSessionState() == State.ZOMBIE;
        assertTrue(zombi);
        // Try to resurrect them (hopefully failing)
        boolean resurrect = false;
        for (Session ses : sessions)
            try {
                ses.open();
                resurrect |= true;
            } catch (NonReferenceableSessionException e) {
                resurrect |= false;
                continue;
            }
        assertFalse(resurrect);
    }

}
