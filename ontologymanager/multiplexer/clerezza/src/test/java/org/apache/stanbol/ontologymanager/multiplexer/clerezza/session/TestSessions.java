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

import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.collectorfactory;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.onManager;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.ontologyProvider;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.reset;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.sessionManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.commons.owl.OWLOntologyManagerFactory;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.Constants;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.servicesapi.session.NonReferenceableSessionException;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session;
import org.apache.stanbol.ontologymanager.servicesapi.session.Session.State;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class TestSessions {

    public static IRI baseIri = IRI.create(Constants.PEANUTS_MAIN_BASE), baseIri2 = IRI
            .create(Constants.PEANUTS_MINOR_BASE);

    public static String scopeId1 = "Ranma12", scopeId2 = "HokutoNoKen", scopeId3 = "Doraemon";

    private static OntologyInputSource<?> src1 = null, src2 = null;

    @BeforeClass
    public static void setup() throws Exception {
        OWLOntologyManager mgr = OWLOntologyManagerFactory.createOWLOntologyManager(null);
        src1 = new RootOntologySource(mgr.createOntology(baseIri));
        src2 = new RootOntologySource(mgr.createOntology(baseIri2));
        reset();
    }

    @After
    public void cleanup() throws Exception {
        reset();
    }

    @Test
    public void testCreateSessionSpaceAutomatic() throws Exception {
        Scope scope1 = null, scope2 = null, scope3 = null;

        scope1 = collectorfactory.createOntologyScope(scopeId1, src1, src2);
        onManager.registerScope(scope1);
        scope2 = collectorfactory.createOntologyScope(scopeId2, src2, src1);
        onManager.registerScope(scope2);
        scope3 = collectorfactory.createOntologyScope(scopeId3, src2, src2);
        onManager.registerScope(scope3);
        // We do all activations after registering, otherwise the component
        // property value will override these activations.
        onManager.setScopeActive(scopeId1, true);
        onManager.setScopeActive(scopeId2, false);
        onManager.setScopeActive(scopeId3, true);

        // Session ses = sesmgr.createSession();
        // String sesid = ses.getID();
        // TODO replace with proper tests
        // assertFalse(scope1.getSessionSpaces().isEmpty());
        // assertNotNull(scope1.getSessionSpace(sesid));
        // assertFalse(scope3.getSessionSpaces().isEmpty());
        // assertNull(scope2.getSessionSpace(sesid));
        // assertNotNull(scope3.getSessionSpace(sesid));
    }

    @Test
    public void testRegisterSession() throws Exception {
        int before = sessionManager.getRegisteredSessionIDs().size();
        Session ses = sessionManager.createSession();
        assertNotNull(ses);
        assertEquals(before + 1, sessionManager.getRegisteredSessionIDs().size());
    }

    @Test
    public void testSessionCreationDestruction() throws Exception {
        int size = 100;
        int initialSize = sessionManager.getRegisteredSessionIDs().size();
        Set<Session> sessions = new HashSet<Session>();
        // Create and open many sessions.
        synchronized (sessionManager) {
            for (int i = 0; i < size; i++) {
                Session ses = sessionManager.createSession();
                ses.open();
                sessions.add(ses);
            }
            // Check that 500 sessions have been created
            assertEquals(initialSize + size, sessionManager.getRegisteredSessionIDs().size());
        }
        boolean open = true;
        for (Session ses : sessions)
            open &= ses.getSessionState() == State.ACTIVE;
        // Check that all created sessions have been opened
        assertTrue(open);
        // Kill 'em all, to quote Metallica
        synchronized (sessionManager) {
            for (Session ses : sessions)
                sessionManager.destroySession(ses.getID());
            assertEquals(initialSize, sessionManager.getRegisteredSessionIDs().size());
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

    // @Test
    public void zombieSessionClearsContents() throws Exception {
        Session ses = sessionManager.createSession();
        ses.addOntology(new RootOntologySource((IRI
                .create(getClass().getResource("/ontologies/mockfoaf.rdf")))));
        OWLOntologyID expectedKey = new OWLOntologyID(IRI.create("http://xmlns.com/foaf/0.1/"));
        assertTrue(ontologyProvider.hasOntology(expectedKey));
        sessionManager.destroySession(ses.getID());
    }

}
