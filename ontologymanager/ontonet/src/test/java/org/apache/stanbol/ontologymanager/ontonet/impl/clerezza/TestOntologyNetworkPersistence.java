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
package org.apache.stanbol.ontologymanager.ontonet.impl.clerezza;

import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.onManager;
import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.parser;
import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.resetManagers;
import static org.apache.stanbol.ontologymanager.ontonet.MockOsgiContext.sessionManager;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.IS_MANAGED_BY;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary.MANAGES;
import static org.apache.stanbol.ontologymanager.ontonet.api.Vocabulary._NS_STANBOL_INTERNAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.session.Session;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This suite is for testing that all the meta-level information stored by OntoNet can be retrieved and
 * rebuilt if OntoNet or Stanbol goes down but the Clerezza store is not cleared.
 * 
 * TODO add tests for preserving added ontologies
 * 
 * @author alexdma
 * 
 */
public class TestOntologyNetworkPersistence {

    private Logger log = LoggerFactory.getLogger(getClass());

    private OntologyProvider<TcProvider> ontologyProvider;

    private ONManager onm;

    @Test
    public void updatesGraphOnSpaceModification() throws Exception {

        // Ensure the metadata graph is there.
        TripleCollection meta = ontologyProvider.getMetaGraph(TripleCollection.class);
        assertNotNull(meta);

        String scopeId = "updateTest";
        OntologyScope scope = onm.createOntologyScope(scopeId, new GraphContentInputSource(getClass()
                .getResourceAsStream("/ontologies/test1.owl")));

        UriRef collector = new UriRef(_NS_STANBOL_INTERNAL + OntologySpace.shortName + "/"
                                      + scope.getCoreSpace().getID());
        UriRef test1id = new UriRef(ontologyProvider.getKey(IRI
                .create("http://stanbol.apache.org/ontologies/test1.owl"))); // Has no versionIRI
        // Be strict: the whole property pair must be there.
        UriRef predicate = MANAGES;
        assertTrue(meta.contains(new TripleImpl(collector, predicate, test1id)));
        predicate = IS_MANAGED_BY;
        assertTrue(meta.contains(new TripleImpl(test1id, predicate, collector)));

        scope.tearDown(); // To modify the core space.

        scope.getCoreSpace().addOntology(
            new GraphContentInputSource(getClass().getResourceAsStream("/ontologies/minorcharacters.owl")));
        UriRef minorId = new UriRef(ontologyProvider.getKey(IRI
                .create("http://stanbol.apache.org/ontologies/pcomics/minorcharacters.owl")));
        predicate = MANAGES;
        assertTrue(meta.contains(new TripleImpl(collector, predicate, minorId)));
        predicate = IS_MANAGED_BY;
        assertTrue(meta.contains(new TripleImpl(minorId, predicate, collector)));

        scope.getCustomSpace().addOntology(
            new GraphContentInputSource(getClass().getResourceAsStream("/ontologies/test1.owl")));

        scope.getCustomSpace().addOntology(
            new GraphContentInputSource(getClass().getResourceAsStream("/ontologies/minorcharacters.owl")));
    }

    @Test
    public void preservesManagedOntologies() throws Exception {
        String id = "preserve";
        OntologyScope scope = onManager.createOntologyScope(id, new GraphContentInputSource(getClass()
                .getResourceAsStream("/ontologies/mockfoaf.rdf")));
        scope.getCustomSpace().addOntology(
            new GraphContentInputSource(getClass().getResourceAsStream(
                "/ontologies/nonexistentcharacters.owl")));

        // Simulate Stanbol going down.
        log.info("Stanbol going down...");
        resetOntologyProvider(); // but keep the TcProvider
        resetManagers();

        OntologyScope sc = onManager.getScope(id);
        assertNotNull(sc);
        // assertEquals(scope, sc); XXX should scopes be equal on ID + content?
    }

    @Test
    public void scopesAndSessionsOutliveOntoNet() throws Exception {
        /*
         * Both scopes will be created, but scope1 will be unregistered and we expect not to be able to
         * rebuild it.
         */
        String id1 = "scope1", id2 = "scope2", sid2 = "auto-" + System.currentTimeMillis();

        // Setup a network
        OntologyScope scope1 = onManager.createOntologyScope(id1);
        assertNotNull(scope1);
        OntologyScope scope2 = onManager.createOntologyScope(id2);
        assertNotNull(scope2);
        onManager.deregisterScope(scope1);

        // A session with a system ID
        Session ses1 = sessionManager.createSession();
        String sid1 = ses1.getID();
        assertNotNull(ses1);
        assertNotNull(sid1);
        assertFalse(sid1.isEmpty());
        // A session with an ID chosen manually
        Session ses2 = sessionManager.createSession(sid2);
        assertNotNull(ses2);
        assertNotNull(ses2.getID());
        assertEquals(sid2, ses2.getID());

        log.info("Stanbol going down...");
        resetOntologyProvider(); // but keep the TcProvider
        resetManagers();

        // The unregistered scope should be missing.
        assertNull(onManager.getScope(id1));
        // The other collectors should have been rebuilt.
        assertNotNull(onManager.getScope(id2));
        assertNotNull(sessionManager.getSession(sid1));
        assertNotNull(sessionManager.getSession(sid2));
    }

    /*
     * Use a dedicated TC Provider that is setup once before the tests begin and never cleared.
     */
    private TcProvider tcp;

    @Test
    public void canRetrieveOntologyImported() throws Exception {
        String pcomics = "http://stanbol.apache.org/ontologies/pcomics/";
        OWLOntologyID foaf = new OWLOntologyID(IRI.create("http://xmlns.com/foaf/0.1/")), all = new OWLOntologyID(
                IRI.create(pcomics + "characters_all.owl")), main = new OWLOntologyID(
                IRI.create(pcomics + "maincharacters.owl")), minor = new OWLOntologyID(
                IRI.create(pcomics + "minorcharacters.owl"));
        OWLOntology oAll, oMain, oMinor, oFoaf;
        final int total = 4;

        // Load the Peanuts characters_all ontology (has 2 import levels)
        InputStream data = getClass().getResourceAsStream("/ontologies/characters_all.owl");
        String key = ontologyProvider.loadInStore(data, SupportedFormat.RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isEmpty());

        /*
         * characters_all, main, minor + mockfoaf (note: imports are available only because the xml:base is
         * set to be the same as the import target)
         */
        assertEquals(total, ontologyProvider.getPublicKeys().size());

        // Check that each imported ontology is stored
        oAll = ontologyProvider.getStoredOntology(ontologyProvider.getKey(all), OWLOntology.class, false);
        OWLOntologyID id = oAll.getOntologyID();
        assertNotNull(id);
        assertEquals(all, id);
        oMain = ontologyProvider.getStoredOntology(ontologyProvider.getKey(main), OWLOntology.class, false);
        id = oMain.getOntologyID();
        assertNotNull(id);
        assertEquals(main, id);
        oMinor = ontologyProvider.getStoredOntology(ontologyProvider.getKey(minor), OWLOntology.class, false);
        id = oMinor.getOntologyID();
        assertNotNull(id);
        assertEquals(minor, id);
        oFoaf = ontologyProvider.getStoredOntology(ontologyProvider.getKey(foaf), OWLOntology.class, false);
        id = oFoaf.getOntologyID();
        assertNotNull(id);
        assertEquals(foaf, id);

        log.info("Stanbol going down...");
        resetOntologyProvider(); // but keep the TcProvider
        assertEquals(total, ontologyProvider.getPublicKeys().size());

        // The OWL API implements OWLOntology#equals()
        assertEquals(oAll,
            ontologyProvider.getStoredOntology(ontologyProvider.getKey(all), OWLOntology.class, false));
        assertEquals(oMain,
            ontologyProvider.getStoredOntology(ontologyProvider.getKey(main), OWLOntology.class, false));
        assertEquals(oMinor,
            ontologyProvider.getStoredOntology(ontologyProvider.getKey(minor), OWLOntology.class, false));
        assertEquals(oFoaf,
            ontologyProvider.getStoredOntology(ontologyProvider.getKey(foaf), OWLOntology.class, false));
    }

    @Test
    public void canRetrieveOntologySingleton() throws Exception {

        OWLOntologyID foaf = new OWLOntologyID(IRI.create("http://xmlns.com/foaf/0.1/"));
        OWLOntology o1;

        // Get the fake FOAF and load it into the ontology provider
        InputStream data = getClass().getResourceAsStream("/ontologies/mockfoaf.rdf");
        // Keep track of its storage key
        String key = ontologyProvider.loadInStore(data, SupportedFormat.RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isEmpty());

        // Retrieve the stored fake FOAF
        assertEquals(1, ontologyProvider.getPublicKeys().size());
        o1 = ontologyProvider.getStoredOntology(key, OWLOntology.class, false);
        OWLOntologyID id = o1.getOntologyID();
        assertNotNull(id);
        assertEquals(foaf, id);

        // Check there is a storage key for the (real) ID of the FOAF ontology
        key = ontologyProvider.getKey(foaf);
        assertNotNull(key);
        assertFalse(key.isEmpty());

        log.info("Stanbol going down...");
        resetOntologyProvider(); // but keep the TcProvider
        assertEquals(1, ontologyProvider.getPublicKeys().size());

        // Check again for the FOAF key
        key = ontologyProvider.getKey(foaf);
        assertNotNull(key);
        assertFalse(key.isEmpty());

        // The OWL API implements OWLOntology#equals()
        assertEquals(o1, ontologyProvider.getStoredOntology(key, OWLOntology.class, false));
    }

    /*
     * Before each test, everything is cleaned up, including the TcProvider.
     */
    @Before
    public void cleanup() throws Exception {
        tcp = new SimpleTcProvider();
        resetOntologyProvider();
        Dictionary<String,Object> empty = new Hashtable<String,Object>();
        onm = new ONManagerImpl(ontologyProvider, new OfflineConfigurationImpl(empty),
                new OntologySpaceFactoryImpl(ontologyProvider, empty), empty);
    }

    /*
     * With this method, the ontology provider and all its internal indices are cleared. However, the Clerezza
     * persistence objects are not cleared, so we can check if we can still retrieve metadata from them.
     */
    private void resetOntologyProvider() {
        ontologyProvider = new ClerezzaOntologyProvider(tcp, new OfflineConfigurationImpl(
                new Hashtable<String,Object>()), parser);
    }

}
