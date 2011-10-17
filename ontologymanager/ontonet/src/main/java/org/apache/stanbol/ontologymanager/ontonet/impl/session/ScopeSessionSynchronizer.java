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

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpaceFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionEvent;
import org.apache.stanbol.ontologymanager.ontonet.api.session.SessionListener;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.LoggerFactory;

public class ScopeSessionSynchronizer implements SessionListener {

    private ONManager manager;

    public ScopeSessionSynchronizer(ONManager manager) {
        // WARN do not use ONManager here, as it will most probably be
        // instantiated by it.
        this.manager = manager;
    }

    @Deprecated
    private void addSessionSpaces(IRI sessionId) {
        OntologySpaceFactory factory = manager.getOntologySpaceFactory();
        for (OntologyScope scope : manager.getScopeRegistry().getActiveScopes()) {
            try {
                String scopeId = scope.getID().toString()
                        .substring(scope.getID().toString().lastIndexOf("/") + 1);
                scope.addSessionSpace(factory.createSessionOntologySpace(scopeId), sessionId.toString());
            } catch (UnmodifiableOntologyCollectorException e) {
                LoggerFactory.getLogger(getClass()).warn("Tried to add session to unmodifiable space ");
                continue;
            }
        }
    }

    @Override
    public void sessionChanged(SessionEvent event) {
        // System.err.println("Session " + event.getSession() + " has been "
        // + event.getOperationType());
        Session ses = event.getSession();
        switch (event.getOperationType()) {
            case CREATE:
                ses.addSessionListener(this);
                // addSessionSpaces(ses.getID());
                break;
            case CLOSE:
                break;
            case KILL:
                ses.removeSessionListener(this);
                break;
            default:
                break;
        }
    }

}
