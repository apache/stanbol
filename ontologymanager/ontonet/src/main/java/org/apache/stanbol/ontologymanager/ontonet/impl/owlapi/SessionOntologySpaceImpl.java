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
package org.apache.stanbol.ontologymanager.ontonet.impl.owlapi;

import java.util.Random;

import org.apache.stanbol.ontologymanager.ontonet.api.collector.UnmodifiableOntologyCollectorException;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.SessionOntologySpace;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Default implementation of the session ontology space.
 * 
 * @deprecated obsolete, as is its implemented type {@link SessionOntologySpace}.
 */
public class SessionOntologySpaceImpl extends AbstractOntologySpaceImpl implements SessionOntologySpace {

    public static final String SUFFIX = SpaceType.SESSION.getIRISuffix();

    protected static String buildId(String scopeID) {
        return (scopeID != null ? scopeID : "") + "/" + SpaceType.SESSION.getIRISuffix() + "-"
               + new Random().nextLong();
    }

    public SessionOntologySpaceImpl(String scopeID, IRI namespace
    // , ClerezzaOntologyStorage store
    ) {
        // FIXME : sync session id with session space ID
        super(buildId(scopeID), namespace, SpaceType.SESSION/* , store */);
    }

    public SessionOntologySpaceImpl(String scopeID, IRI namespace,
    // ClerezzaOntologyStorage store,
                                    OWLOntologyManager ontologyManager) {
        // FIXME : sync session id with session space ID
        super(buildId(scopeID), namespace, SpaceType.SESSION, /* store, */ontologyManager);
    }

    @Override
    public void attachSpace(OntologySpace space, boolean skipRoot) throws UnmodifiableOntologyCollectorException {
        // FIXME re-implement!
        // if (!(space instanceof SessionOntologySpace)) {
        // OWLOntology o = space.getTopOntology();
        // // This does the append thingy
        // log.debug("Attaching " + o + " TO " + getTopOntology() + " ...");
        // try {
        // // It is in fact the addition of the core space top ontology to the
        // // custom space, with import statements and all.
        // addOntology(new RootOntologySource(o, null));
        // // log.debug("ok");
        // } catch (Exception ex) {
        // log.error("FAILED", ex);
        // }
        // }
    }

    @Override
    public OWLOntologyManager getOntologyManager() {
        // Session spaces do expose their ontology managers.
        return ontologyManager;
    }

    @Override
    public synchronized void setUp() {
        // Once it is set up, a session space is write-enabled.
        locked = false;
    }

    @Override
    public synchronized void tearDown() {
        // TODO Do we really unlock?
        locked = false;
    }

}
