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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import org.apache.stanbol.ontologymanager.servicesapi.io.Origin;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider.Status;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verify that it is always possible to obtain the correct ontologies given their IDs whenever available, or
 * their expected public keys.
 * 
 * @author alexdma
 * 
 */
public class TestOntologyReconciliation {

    @AfterClass
    public static void cleanup() {
        reset();
    }

    @BeforeClass
    public static void setup() {
        reset();
    }

    private String location_nameless = "/ontologies/nameless_ontology.owl";

    private Logger log = LoggerFactory.getLogger(getClass());

    /*
     * Anonymous ontologies loaded from a data stream must be stored with at least one non-null and
     * non-anonymous public key.
     */
    @Test
    public void anonymousFromStream() throws Exception {
        InputStream in = getClass().getResourceAsStream(location_nameless);
        in.mark(Integer.MAX_VALUE);
        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(in);
        assertTrue(o1.isAnonymous());
        in.reset();
        // in = getClass().getResourceAsStream(location); // use if stream cannot be reset
        OWLOntologyID key = ontologyProvider.loadInStore(in, RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        log.info("Anonymous ontology loaded with non-anonymous public key {}", key);

        OWLOntology o2 = ontologyProvider.getStoredOntology(key, OWLOntology.class, false);
        assertTrue(o2.isAnonymous());
        assertEquals(o1.getAxioms(), o2.getAxioms()); // Cannot equal OWLOntology objects
    }

    /*
     * If an anonymous ontology is loaded from a stream and at least one override is provided, the first
     * override should be the primary key, while every other override should be an alias for that key.
     */
    @Test
    public void anonymousFromStreamWithCustomKeys() throws Exception {
        OWLOntologyID myKey = new OWLOntologyID(IRI.create("nameless"), IRI.create(getClass()
                .getCanonicalName() + "#anonymousFromStreamWithCustomKeys()"));
        OWLOntologyID alias = new OWLOntologyID(IRI.create("nameless"), IRI.create(getClass()
                .getCanonicalName() + "#anonymousFromStreamWithCustomKeys().alias"));
        InputStream in = getClass().getResourceAsStream(location_nameless);
        in.mark(Integer.MAX_VALUE);
        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(in);
        assertTrue(o1.isAnonymous());
        in.reset();
        // in = getClass().getResourceAsStream(location); // use if stream cannot be reset
        OWLOntologyID key = ontologyProvider.loadInStore(in, RDF_XML, false, Origin.create(myKey),
            Origin.create(alias));
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        assertEquals(myKey, key);
        log.info("Anonymous ontology loaded with non-anonymous public key {} (submitted)", key);

        assertEquals(1, ontologyProvider.listAliases(key).size());
        for (OWLOntologyID al : ontologyProvider.listAliases(key)) {
            assertFalse(al.isAnonymous());
            log.info("Named alias detected {}", al);
        }

        // Now retrieve using the alias
        OWLOntology o2 = ontologyProvider.getStoredOntology(alias, OWLOntology.class, false);
        assertTrue(o2.isAnonymous());
        assertEquals(o1.getAxioms(), o2.getAxioms()); // Cannot equal OWLOntology objects
    }

    /*
     * Anonymous ontologies loaded from a URL must reconcile with a public key that matches the resource URL
     * in its ontology IRI.
     */
    @Test
    public void anonymousFromURL() throws Exception {
        URL in = getClass().getResource(location_nameless);
        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(IRI.create(in));
        assertTrue(o1.isAnonymous());
        // in = getClass().getResourceAsStream(location); // use if stream cannot be reset
        OWLOntologyID key = ontologyProvider.loadInStore(IRI.create(in), RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        log.info("Anonymous ontology loaded with non-anonymous public key {}", key);
        OWLOntology o2 = ontologyProvider.getStoredOntology(key, OWLOntology.class, false);
        assertTrue(o2.isAnonymous());
        assertEquals(o1.getAxioms(), o2.getAxioms()); // Cannot equal OWLOntology objects
    }

    /*
     * If an anonymous ontology is loaded from a URL and at least one override is provided, the first override
     * should be the primary key, while everything else, including the URL, should be an alias for that key.
     */
    @Test
    public void anonymousFromURLWithCustomKeys() throws Exception {
        OWLOntologyID myKey = new OWLOntologyID(IRI.create("nameless"), IRI.create(getClass()
                .getCanonicalName() + "#anonymousFromURLWithCustomKeys()"));
        OWLOntologyID alias = new OWLOntologyID(IRI.create("nameless"), IRI.create(getClass()
                .getCanonicalName() + "#anonymousFromURLWithCustomKeys().alias"));
        URL url = getClass().getResource(location_nameless);
        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(IRI.create(url));
        assertTrue(o1.isAnonymous());

        OWLOntologyID key = ontologyProvider.loadInStore(IRI.create(url), RDF_XML, false,
            Origin.create(myKey), Origin.create(alias));
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        assertEquals(myKey, key);
        log.info("Anonymous ontology loaded with non-anonymous public key {} (submitted)", key);

        // should have 2 aliases: the physical location and the submitted alias.
        assertEquals(2, ontologyProvider.listAliases(key).size());
        for (OWLOntologyID al : ontologyProvider.listAliases(key)) {
            assertFalse(al.isAnonymous());
            log.info("Named alias detected {}", al);
        }

        // Now retrieve using the alias...
        OWLOntology o2 = ontologyProvider.getStoredOntology(alias, OWLOntology.class, false);
        assertTrue(o2.isAnonymous());
        assertEquals(o1.getAxioms(), o2.getAxioms()); // Cannot equal OWLOntology objects

        // ... and using the physical IRI
        o2 = ontologyProvider.getStoredOntology(new OWLOntologyID(IRI.create(url)), OWLOntology.class, false);
        assertTrue(o2.isAnonymous());
        assertEquals(o1.getAxioms(), o2.getAxioms()); // Cannot equal OWLOntology objects
    }

    /*
     * Named ontologies loaded from a data stream should have no aliases and directly reconcile with the
     * ontology IRI.
     */
    @Test
    public void namedFromStream() throws Exception {
        String location = "/ontologies/naming/named-2.owl";
        OWLOntologyID expectedId = new OWLOntologyID(
                IRI.create("http://stanbol.apache.org/ontologies/test/naming/named-2"));
        InputStream in = getClass().getResourceAsStream(location);
        in.mark(Integer.MAX_VALUE);
        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(in);
        assertFalse(o1.isAnonymous());
        in.reset();
        assertEquals(expectedId, o1.getOntologyID());
        // in = getClass().getResourceAsStream(location); // use if stream cannot be reset
        OWLOntologyID key = ontologyProvider.loadInStore(in, RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        log.info("Named ontology loaded with public key {}", key);
        assertEquals(expectedId, key);
        log.info(" -- (matches actual ontology ID).");

        OWLOntology o1_1 = ontologyProvider.getStoredOntology(key, OWLOntology.class, false);
        assertFalse(o1_1.isAnonymous());
        assertEquals(expectedId, o1_1.getOntologyID()); // Cannot equal OWLOntology objects
        // Check that axioms match
        log.warn("Plain OWL API seems to be failing to preserve owl:versionInfo. Will test non-annotation axioms only.");
        assertEquals(o1.getTBoxAxioms(false), o1_1.getTBoxAxioms(false));
        log.info(" -- TBox axiom check successful.");
        assertEquals(o1.getABoxAxioms(false), o1_1.getABoxAxioms(false));
        log.info(" -- ABox axiom check successful.");

        // Now check there are no aliases.
        assertSame(0, ontologyProvider.listAliases(expectedId).size());
    }

    /*
     * Named ontologies loaded from an URL must reconcile with both their logical ID and their resource URL
     * (through aliasing).
     */
    @Test
    public void namedFromURL() throws Exception {
        String location = "/ontologies/naming/named-1.owl";
        OWLOntologyID expectedId = new OWLOntologyID(
                IRI.create("http://stanbol.apache.org/ontologies/test/naming/named-1"));
        URL url = getClass().getResource(location);
        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(IRI.create(url));
        assertFalse(o1.isAnonymous());
        assertEquals(expectedId, o1.getOntologyID());
        // in = getClass().getResourceAsStream(location); // use if stream cannot be reset
        OWLOntologyID key = ontologyProvider.loadInStore(IRI.create(url), RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        log.info("Named ontology loaded with public key {}", key);
        assertEquals(expectedId, key);
        log.info(" -- (matches actual ontology ID).");

        OWLOntology o1_1 = ontologyProvider.getStoredOntology(key, OWLOntology.class, false);
        assertFalse(o1_1.isAnonymous());
        assertEquals(expectedId, o1_1.getOntologyID()); // Cannot equal OWLOntology objects
        // Check that axioms match
        log.warn("Plain OWL API seems to be failing to preserve owl:versionInfo. Will test non-annotation axioms only.");
        assertEquals(o1.getTBoxAxioms(false), o1_1.getTBoxAxioms(false));
        log.info(" -- TBox axiom check successful.");
        assertEquals(o1.getABoxAxioms(false), o1_1.getABoxAxioms(false));
        log.info(" -- ABox axiom check successful.");

        // Now check the alias from the physical URL
        OWLOntologyID aliasId = new OWLOntologyID(IRI.create(url));
        Set<OWLOntologyID> aliases = ontologyProvider.listAliases(expectedId);
        assertSame(1, aliases.size());
        assertTrue(aliases.contains(aliasId));

        // Check that it actually *is* an alias
        OWLOntology alias = ontologyProvider.getStoredOntology(aliasId, OWLOntology.class);
        assertNotNull(alias);
        assertEquals(expectedId, alias.getOntologyID());
        // Both ontologies come from the ontology provider and should have preserved ontology annotations.
        // Therefore ass axioms should match safely.
        assertEquals(o1_1.getAxioms(), alias.getAxioms());
    }

    /*
     * Two versioned ontologies that share their ontology IRI are stored with separate public keys that manage
     * their full ontology IDs.
     */
    @Test
    public void versioned() throws Exception {
        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();

        // Load the first ontology
        String location = "/ontologies/versiontest_v1.owl";
        InputStream in = getClass().getResourceAsStream(location);
        in.mark(Integer.MAX_VALUE);
        // Keep tack of the original in a separate ontology.
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(in);
        assertFalse(o1.isAnonymous());
        OWLOntologyID id1 = o1.getOntologyID();
        in.reset();
        // in = getClass().getResourceAsStream(location); // use if stream cannot be reset
        OWLOntologyID key = ontologyProvider.loadInStore(in, RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        log.info("Named ontology loaded with public key {}", key);
        assertEquals(id1, key);
        log.info(" -- (matches actual ontology ID).");

        // The unversioned ID should return no match...
        OWLOntologyID unversioned = new OWLOntologyID(key.getOntologyIRI());
        assertSame(Status.NO_MATCH, ontologyProvider.getStatus(unversioned));

        // ...but a query on the available versions should return only the public key.
        Set<OWLOntologyID> versions = ontologyProvider.listVersions(key.getOntologyIRI());
        assertFalse(versions.isEmpty());
        assertSame(1, versions.size());
        assertTrue(versions.contains(id1));

        // Now load the second version.
        location = "/ontologies/versiontest_v2.owl";
        in = getClass().getResourceAsStream(location);
        in.mark(Integer.MAX_VALUE);
        // Keep tack of the original in a separate ontology.
        OWLOntology o2 = onMgr.loadOntologyFromOntologyDocument(in);
        assertFalse(o2.isAnonymous());
        OWLOntologyID id2 = o2.getOntologyID();
        in.reset();
        // in = getClass().getResourceAsStream(location); // use if stream cannot be reset
        key = ontologyProvider.loadInStore(in, RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        log.info("Named ontology loaded with public key {}", key);
        assertEquals(id2, key);
        log.info(" -- (matches actual ontology ID).");

        // The unversioned ID should still return no match...
        assertSame(Status.NO_MATCH, ontologyProvider.getStatus(unversioned));

        // ...but a query on the available versions should return both public keys now.
        versions = ontologyProvider.listVersions(key.getOntologyIRI());
        assertFalse(versions.isEmpty());
        assertSame(2, versions.size());
        assertTrue(versions.contains(id1));
        assertTrue(versions.contains(id2));

        // Check that axioms match for version 1
        log.info("Version 1:");
        OWLOntology o1_1 = ontologyProvider.getStoredOntology(id1, OWLOntology.class, false);
        assertFalse(o1_1.isAnonymous());
        assertEquals(o1.getOntologyID(), o1_1.getOntologyID()); // Cannot equal OWLOntology objects
        log.warn("Plain OWL API seems to be failing to preserve owl:versionInfo. Will test non-annotation axioms only.");
        assertEquals(o1.getTBoxAxioms(false), o1_1.getTBoxAxioms(false));
        log.info(" -- TBox axiom check successful.");
        assertEquals(o1.getABoxAxioms(false), o1_1.getABoxAxioms(false));
        log.info(" -- ABox axiom check successful.");

        // Check that axioms match for version 2 (therefore differ from each other)
        log.info("Version 2:");
        OWLOntology o2_1 = ontologyProvider.getStoredOntology(id2, OWLOntology.class, false);
        assertFalse(o2_1.isAnonymous());
        assertEquals(o2.getOntologyID(), o2_1.getOntologyID()); // Cannot equal OWLOntology objects
        log.warn("Plain OWL API seems to be failing to preserve owl:versionInfo. Will test non-annotation axioms only.");
        assertEquals(o2.getTBoxAxioms(false), o2_1.getTBoxAxioms(false));
        log.info(" -- TBox axiom check successful.");
        assertEquals(o2.getABoxAxioms(false), o2_1.getABoxAxioms(false));
        log.info(" -- ABox axiom check successful.");

        // There should be no aliases.
        assertSame(0, ontologyProvider.listAliases(unversioned).size());
        assertSame(0, ontologyProvider.listAliases(id1).size());
        assertSame(0, ontologyProvider.listAliases(id2).size());
    }

    /*
     * If an ontology has no ontology IRI but does have a version IRI, it should still be possible to load it,
     * but the version IRI must be erased.
     */
    @Test
    public void versionedOnlyFromStream() throws Exception {
        String location = "/ontologies/naming/versionedonly.owl";

        InputStream in = getClass().getResourceAsStream(location);
        in.mark(Integer.MAX_VALUE);
        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(in);
        // Ensure that the OWL API erases the version IRI.
        assertTrue(o1.isAnonymous());
        assertNull(o1.getOntologyID().getVersionIRI());
        in.reset();
        // in = getClass().getResourceAsStream(location); // use if stream cannot be reset

        // The public key must be non-anonymous nonetheless.
        OWLOntologyID key = ontologyProvider.loadInStore(in, RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isAnonymous());
        assertNull(key.getVersionIRI());
        log.info("Wrongly versioned ontology loaded with public key {}", key);
        assertFalse(o1.equals(key));

        OWLOntology o1_1 = ontologyProvider.getStoredOntology(key, OWLOntology.class, false);
        assertNotNull(o1_1);
        assertTrue(o1_1.isAnonymous());
        assertNull(o1_1.getOntologyID().getVersionIRI());

        // Cannot equal two OWLOntology objects, especially if anonymous.
        // Check that they match axiom-wise.
        log.warn("Plain OWL API seems to be failing to preserve owl:versionInfo. Will test non-annotation axioms only.");
        assertEquals(o1.getTBoxAxioms(false), o1_1.getTBoxAxioms(false));
        log.info(" -- TBox axiom check successful.");
        assertEquals(o1.getABoxAxioms(false), o1_1.getABoxAxioms(false));
        log.info(" -- ABox axiom check successful.");

        // No aliases should have been created.
        assertSame(0, ontologyProvider.listAliases(key).size());
    }

    /*
     * If an ontology has no ontology IRI but does have a version IRI, it should still be possible to load it,
     * but the version IRI must be erased. Plus, the public key should be created after the resource URL.
     */
    @Test
    public void versionedOnlyFromURL() throws Exception {
        String location = "/ontologies/naming/versionedonly.owl";
        IRI url = IRI.create(getClass().getResource(location));

        OWLOntologyID expected = new OWLOntologyID(url);

        OWLOntologyManager onMgr = OWLManager.createOWLOntologyManager();
        OWLOntology o1 = onMgr.loadOntologyFromOntologyDocument(url);
        // Ensure that the OWL API erases the version IRI.
        assertTrue(o1.isAnonymous());
        assertNull(o1.getOntologyID().getVersionIRI());

        // The public key must be non-anonymous nonetheless.
        OWLOntologyID key = ontologyProvider.loadInStore(url, RDF_XML, false);
        assertNotNull(key);
        assertFalse(key.isAnonymous());

        log.info("Wrongly versioned ontology loaded with public key {}", key);
        assertFalse(o1.equals(key));
        assertEquals(expected, key);
        log.info(" -- (matches resource URL).");

        OWLOntology o1_1 = ontologyProvider.getStoredOntology(key, OWLOntology.class, false);
        assertNotNull(o1_1);
        assertTrue(o1_1.isAnonymous());
        assertNull(o1_1.getOntologyID().getVersionIRI());

        // Cannot equal two OWLOntology objects, especially if anonymous.
        // Check that they match axiom-wise.
        log.warn("Plain OWL API seems to be failing to preserve owl:versionInfo. Will test non-annotation axioms only.");
        assertEquals(o1.getTBoxAxioms(false), o1_1.getTBoxAxioms(false));
        log.info(" -- TBox axiom check successful.");
        assertEquals(o1.getABoxAxioms(false), o1_1.getABoxAxioms(false));
        log.info(" -- ABox axiom check successful.");

        // No aliases should have been created.
        assertSame(0, ontologyProvider.listAliases(key).size());
    }

    /*
     * Ensures ontology IDs with only the version IRI are illegal.
     */
    @Test
    public void versionIriOnlyIsIllegal() {
        try {
            new OWLOntologyID(null, IRI.create("http://stanbol.apache.org/ontologies/version/bad/1"));
            fail("An anonymous ontology ID with a version IRI was unexpectedly accepted!");
        } catch (Exception ex) {}
    }

}
