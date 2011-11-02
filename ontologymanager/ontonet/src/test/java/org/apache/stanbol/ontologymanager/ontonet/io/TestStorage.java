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
package org.apache.stanbol.ontologymanager.ontonet.io;

import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.parser;
import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.reset;
import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.tcManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.stanbol.ontologymanager.ontonet.Constants;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.ClerezzaOntologyProvider;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestStorage {

    private static ONManager onm;

    private Logger log = LoggerFactory.getLogger(getClass());

    private String scopeId = "StorageTest";

    private static OntologyProvider<TcProvider> provider;

    @BeforeClass
    public static void setup() {
        reset();
        OfflineConfiguration offline = new OfflineConfigurationImpl(new Hashtable<String,Object>());
        provider = new ClerezzaOntologyProvider(tcManager, offline, parser);
        // Empty configurations are fine, but this time we provide a Clerezza context.
        onm = new ONManagerImpl(provider, offline, new Hashtable<String,Object>());
    }

    @Test
    public void storageOnScopeCreation() throws Exception {
        assertTrue(provider.getStore().listTripleCollections().isEmpty());
        OntologyInputSource ois = new RootOntologyIRISource(IRI.create(getClass().getResource(
            "/ontologies/minorcharacters.owl")));

        OntologyScope sc = onm.getOntologyScopeFactory().createOntologyScope(scopeId, ois);

        Set<Triple> triples = new HashSet<Triple>();

        for (UriRef iri : provider.getStore().listTripleCollections()) {
            log.info("{}", iri.toString());
            UriRef entity = new UriRef(Constants.PEANUTS_MINOR_BASE + "#" + Constants.truffles);
            Graph ctx = new GraphNode(entity, provider.getStore().getTriples(iri)).getNodeContext();
            Iterator<Triple> it = ctx.iterator();
            while (it.hasNext())
                triples.add(it.next());
        }

        assertFalse(provider.getStore().listTripleCollections().isEmpty());
        assertEquals(3, triples.size());
    }

    @After
    public void cleanup() {
        reset();
    }

}
