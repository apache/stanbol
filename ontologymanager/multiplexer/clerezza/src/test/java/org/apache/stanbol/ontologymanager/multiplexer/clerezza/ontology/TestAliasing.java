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
package org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology;

import static org.apache.stanbol.commons.web.base.format.KRFormat.RDF_XML;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.ontologyProvider;
import static org.apache.stanbol.ontologymanager.multiplexer.clerezza.MockOsgiContext.reset;
import static org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider.Status.MATCH;
import static org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider.Status.UNCHARTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test suite checks the coherence of registered ontology entries and their relationship with stored
 * graphs, if any. It also verifies that entries can be created at one time but will only become accessible
 * when filled.
 * 
 * @author alexdma
 * 
 */
public class TestAliasing {

    @AfterClass
    public static void cleanup() {
        reset();
    }

    private Logger log = LoggerFactory.getLogger(getClass());

    private String prefix = "http://stanbol.apache.org/ontologies/test/aliasing";

    @Test
    public void canCreateUnchartedOnce() throws Exception {
        OWLOntologyID uncharted = new OWLOntologyID(IRI.create(prefix), IRI.create(getClass()
                .getCanonicalName() + "#canCreateUnchartedOnce()"));
        OWLOntologyID uncharted_copy = ontologyProvider.createBlankOntologyEntry(uncharted);
        log.info("Created uncharted ontology entry.");
        log.info(" ... key: {}", uncharted);
        assertEquals(uncharted, uncharted_copy);
        assertEquals(UNCHARTED, ontologyProvider.getStatus(uncharted));
        int entries = ontologyProvider.listAllRegisteredEntries().size();
        log.info("{} entries registered.", entries);

        try {
            log.info("Re-creating uncharted ontology entry...");
            OWLOntologyID recreated = ontologyProvider.createBlankOntologyEntry(uncharted);
            log.error(" ... Created same entry twice. This should not happen.");
            log.error(" ... key: {}", recreated);
            fail("Created same entry twice. This should not happen.");
        } catch (Exception ex) {
            log.info(" ... Creation failed. This is expected.");
            assertEquals(UNCHARTED, ontologyProvider.getStatus(uncharted));
            int newsize = ontologyProvider.listAllRegisteredEntries().size();
            assertEquals(entries, newsize);
            log.info("{} entries registered.", newsize);
            assertTrue(ontologyProvider.listOrphans().contains(uncharted));
            assertFalse(ontologyProvider.listPrimaryKeys().contains(uncharted));
        }
    }

    @Test
    public void canFillDeferred() throws Exception {
        log.info("Creating uncharted ontology entry.");
        OWLOntologyID key = new OWLOntologyID(IRI.create(prefix), IRI.create(getClass().getCanonicalName()
                                                                             + "#canFillDeferred()"));
        log.info(" ... key: {}", key);
        OWLOntologyID key2 = ontologyProvider.createBlankOntologyEntry(key);
        assertEquals(key, key2);
        assertEquals(UNCHARTED, ontologyProvider.getStatus(key));

        String location = "/ontologies/test1.owl";
        log.info("Filling from data stream.");
        log.info(" ... source: {}", getClass().getResource(location));
        log.info(" ... target: {}", key);
        InputStream in = getClass().getResourceAsStream(location);
        key2 = ontologyProvider.loadInStore(in, RDF_XML, false, Origin.create(key));
        assertEquals(key, key2);
        log.info(" ... DONE. Primary keys match.");
        assertEquals(MATCH, ontologyProvider.getStatus(key));
    }

    @Test
    public void cannotReplaceExistingDataStream() throws Exception {

        // Create a new full entry.
        String location = "/ontologies/test1.owl";
        InputStream in = getClass().getResourceAsStream(location);
        OWLOntologyID key = ontologyProvider.loadInStore(in, RDF_XML, false);
        log.info("Created and filled entry {}", key);
        assertNotNull(key);
        assertEquals(MATCH, ontologyProvider.getStatus(key));
        assertEquals(1, ontologyProvider.listAllRegisteredEntries().size());

        // Try to replace the data stream of a full entry (should fail).
        location = "/ontologies/mockfoaf.rdf";
        in = getClass().getResourceAsStream(location);
        try {
            OWLOntologyID wrongKey = ontologyProvider.loadInStore(in, RDF_XML, false, Origin.create(key));
            log.error("Replaced existing data stream. This should not happen.");
            log.error(" ... key: ", wrongKey);
            fail("Replaced existing data stream without warning.");
        } catch (Exception ex) {
            log.info("Caught exception of type {}", ex.getClass().getCanonicalName());
            assertEquals(MATCH, ontologyProvider.getStatus(key));
            assertEquals(1, ontologyProvider.listAllRegisteredEntries().size());
            assertEquals(1, ontologyProvider.listPrimaryKeys().size());
        }

        // Create a new uncharted entry and fill its data stream with the failed one.
        OWLOntologyID uncharted = new OWLOntologyID(IRI.create(prefix), IRI.create(getClass()
                .getCanonicalName() + "#cannotReplaceExistingDataStream()"));
        ontologyProvider.createBlankOntologyEntry(uncharted);
        assertEquals(UNCHARTED, ontologyProvider.getStatus(uncharted));
        in = getClass().getResourceAsStream(location);
        ontologyProvider.loadInStore(in, RDF_XML, false, Origin.create(uncharted));
        assertEquals(MATCH, ontologyProvider.getStatus(uncharted));
        assertEquals(2, ontologyProvider.listPrimaryKeys().size());

    }

    /*
     * Nothing needs to be preserved across tests.
     */
    @Before
    public void setup() {
        reset();
    }
}
