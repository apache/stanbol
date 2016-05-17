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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.io;

import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.onManager;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.ontologyProvider;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.commons.rdf.ImmutableGraph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.Constants;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.scope.Scope;
import org.apache.stanbol.ontologymanager.sources.owlapi.ParentPathInputSource;
import org.apache.stanbol.ontologymanager.sources.owlapi.RootOntologySource;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestStorage {

    @BeforeClass
    public static void setup() {
        reset();
    }

    private Logger log = LoggerFactory.getLogger(getClass());

    private String scopeId = "StorageTest";

    @After
    public void cleanup() {
        reset();
    }

    @Test
    public void storageOnScopeCreation() throws Exception {

        assertEquals(1, ontologyProvider.getStore().listGraphs().size());
        // This one has an import that we want to hijack locally, so we use the ParentPathInputSource.
        OntologyInputSource<?> ois = new ParentPathInputSource(new File(getClass().getResource(
            "/ontologies/minorcharacters.owl").toURI()));

        Scope sc = onManager.createOntologyScope(scopeId, ois);

        Set<Triple> triples = new HashSet<Triple>();

        for (IRI iri : ontologyProvider.getStore().listGraphs()) {
            log.info("{}", iri.toString());
            IRI entity = new IRI(Constants.PEANUTS_MINOR_BASE + "#" + Constants.truffles);
            ImmutableGraph ctx = new GraphNode(entity, ontologyProvider.getStore().getGraph(iri)).getNodeContext();
            Iterator<Triple> it = ctx.iterator();
            while (it.hasNext())
                triples.add(it.next());
        }

        assertFalse(ontologyProvider.getStore().listGraphs().isEmpty());
        assertEquals(3, triples.size());

    }

    /**
     * If an ontology is removed from a scope, or the scope itself is torn down, this should not result in the
     * deletion of that ontology in general.
     */
    @Test
    public void storedOntologyOutlivesScope() throws Exception {
        String ephemeralScopeId = "CaducousScope";
        OntologyInputSource<OWLOntology> ois = new RootOntologySource(org.semanticweb.owlapi.model.IRI.create(getClass().getResource(
            "/ontologies/nonexistentcharacters.owl")));
        OWLOntologyID ontologyId = ois.getRootOntology().getOntologyID();
        Scope scope = onManager.createOntologyScope(ephemeralScopeId);
        // Initially, the ontology is not there
        assertFalse(ontologyProvider.hasOntology(ontologyId));
        // Once added, the ontology is there
        scope.getCustomSpace().addOntology(ois);
        assertTrue(ontologyProvider.hasOntology(ontologyId));
        // Once removed from the scope, the ontology is still there
        scope.getCustomSpace().removeOntology(ontologyId);
        assertTrue(ontologyProvider.hasOntology(ontologyId));
        // Once the scope is killed, the ontology is still there
        // TODO find a more appropriate method to kill scopes?
        scope.tearDown();
        assertTrue(ontologyProvider.hasOntology(ontologyId));
    }

}
