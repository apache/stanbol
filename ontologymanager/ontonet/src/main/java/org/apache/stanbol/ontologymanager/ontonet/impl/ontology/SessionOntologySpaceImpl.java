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
package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import java.util.Random;

import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.StringUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the session ontology space.
 */
public class SessionOntologySpaceImpl extends AbstractOntologySpaceImpl implements SessionOntologySpace {

    public static final String SUFFIX = SpaceType.SESSION.getIRISuffix();

    public SessionOntologySpaceImpl(IRI scopeID, ClerezzaOntologyStorage store) {
        // FIXME : sync session id with session space ID
        super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/" + SpaceType.SESSION.getIRISuffix()
                         + "-" + new Random().nextLong()), SpaceType.SESSION, store/* , scopeID */);

        IRI iri = IRI.create(StringUtils.stripIRITerminator(getID()) + "/root.owl");
        try {
            setTopOntology(new RootOntologySource(ontologyManager.createOntology(iri), null), false);
        } catch (OWLOntologyCreationException e) {
            log.error("Could not create session space root ontology " + iri, e);
        } catch (UnmodifiableOntologySpaceException e) {
            // Should not happen...
            log.error("Session space ontology " + iri
                      + " was denied modification by the space itself. This should not happen.", e);
        }
    }

    public SessionOntologySpaceImpl(IRI scopeID,
                                    ClerezzaOntologyStorage store,
                                    OWLOntologyManager ontologyManager) {

        // FIXME : sync session id with session space ID
        super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/" + SpaceType.SESSION.getIRISuffix()
                         + "-" + new Random().nextLong()), SpaceType.SESSION, store, /* scopeID, */
                ontologyManager);

        Logger log = LoggerFactory.getLogger(getClass());
        IRI iri = IRI.create(StringUtils.stripIRITerminator(getID()) + "/root.owl");
        try {
            setTopOntology(new RootOntologySource(ontologyManager.createOntology(iri), null), false);
        } catch (OWLOntologyCreationException e) {
            log.error("Could not create session space root ontology " + iri, e);
        } catch (UnmodifiableOntologySpaceException e) {
            // Should not happen...
            log.error("Session space ontology " + iri
                      + " was denied modification by the space itself. This should not happen.", e);
        }
    }

    @Override
    public void attachSpace(OntologySpace space, boolean skipRoot) throws UnmodifiableOntologySpaceException {
        if (!(space instanceof SessionOntologySpace)) {
            OWLOntology o = space.getTopOntology();
            // This does the append thingy
            log.debug("Attaching " + o + " TO " + getTopOntology() + " ...");
            try {
                // It is in fact the addition of the core space top ontology to the
                // custom space, with import statements and all.
                addOntology(new RootOntologySource(o, null));
                // log.debug("ok");
            } catch (Exception ex) {
                log.error("FAILED", ex);
            }
        }
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
        // TODO Auto-generated method stub
    }

}
