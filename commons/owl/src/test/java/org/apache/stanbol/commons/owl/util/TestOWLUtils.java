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
package org.apache.stanbol.commons.owl.util;

import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.TURTLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.jena.parser.JenaParserProvider;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test suite for the OWL language utility methods.
 * 
 * @author alexdma
 * 
 */
public class TestOWLUtils {

    /*
     * I need a new Parser because if I use getInsance() I keep getting an annoying exception on a
     * RdfJsonParsing Provider that I don't need.
     */
    private static Parser parser = new Parser();

    @BeforeClass
    public static void setupTests() throws Exception {
        TcManager.getInstance().addWeightedTcProvider(new SimpleTcProvider());
        parser.bindParsingProvider(new JenaParserProvider());
    }

    private Logger log = LoggerFactory.getLogger(getClass());

    private IRI ontologyIri = IRI.create("http://stanbol.apache.org/ontologies/test");

    private UriRef uri = new UriRef("ontonet:http://stanbol.apache.org/prova");

    /*
     * Guessing the ID of a named ontology whose IRI is near the end of the graph.
     */
    @Test
    public void lookaheadNamed() throws Exception {
        String location = "/owl/named.owl";
        log.info("Testing lookahead for location {}", location);

        // Try a low triple limit (the ontology IRI triple is much further).
        InputStream content = getClass().getResourceAsStream(location);
        OWLOntologyID id = OWLUtils.guessOntologyID(content, parser, RDF_XML, 10);
        assertTrue(id.isAnonymous());

        // Try again with no limit
        content = getClass().getResourceAsStream(location);
        id = OWLUtils.guessOntologyID(content, parser, RDF_XML);
        assertNotNull(id);
        assertFalse(id.isAnonymous());
        assertEquals(new OWLOntologyID(ontologyIri), id);
    }

    /*
     * Guessing the ID of a named ontology whose IRI is at the beginning of the graph.
     */
    @Test
    public void lookaheadNamedImmediate() throws Exception {
        String location = "/owl/named_immediate.owl";
        log.info("Testing lookahead for location {}", location);

        // Try a low triple limit (the ontology IRI triple is much further).
        InputStream content = getClass().getResourceAsStream(location);
        OWLOntologyID id = OWLUtils.guessOntologyID(content, parser, RDF_XML, 10);
        assertNotNull(id);
        assertEquals(new OWLOntologyID(ontologyIri), id);
    }

    /*
     * Guessing the ID of a versioned ontology whose IRIs are grouped at the end of the graph.
     */
    @Test
    public void lookaheadVersioned() throws Exception {
        // Identifiers are at position > 10 . Triples are grouped.
        // Minimum offset required is 2 because of an owl:versionInfo triple in-between.
        String location = "/owl/versioned.owl";
        log.info("Testing lookahead for location {}", location);
        IRI incubatedVersion = IRI
                .create("http://svn.apache.org/repos/asf/incubator/stanbol/trunk/commons/owl/src/test/resources/owl/versioned.owl");
        OWLOntologyID expectedOntId = new OWLOntologyID(ontologyIri, incubatedVersion);

        // Low triple limit: guessing should fail.
        InputStream content = getClass().getResourceAsStream(location);
        OWLOntologyID id = OWLUtils.guessOntologyID(content, parser, RDF_XML, 10);
        assertTrue(id.isAnonymous());

        // Reasonable triple limit with low offset: guessing should return the unversioned ID.
        content = getClass().getResourceAsStream(location);
        id = OWLUtils.guessOntologyID(content, parser, RDF_XML, 256, 1);
        assertNotNull(id);
        assertFalse(id.isAnonymous());
        assertEquals(new OWLOntologyID(ontologyIri), id);

        // Reasonable triple limit with auto offset: guessing should succeed.
        content = getClass().getResourceAsStream(location);
        id = OWLUtils.guessOntologyID(content, parser, RDF_XML, 256);
        assertNotNull(id);
        assertFalse(id.isAnonymous());
        assertEquals(expectedOntId, id);

        // No triple limit: guessing should succeed.
        content = getClass().getResourceAsStream(location);
        id = OWLUtils.guessOntologyID(content, parser, RDF_XML);
        assertNotNull(id);
        assertFalse(id.isAnonymous());
        assertEquals(expectedOntId, id);
    }

    /*
     * Guessing the ID of a versioned ontology whose ontology IRI is declared >100 triples before its version
     * IRI.
     */
    @Test
    public void lookaheadVersionedDistance100() throws Exception {
        // Actual distance is 102
        String location = "/owl/versioned_distance-100.owl";
        log.info("Testing lookahead for location {}", location);
        IRI incubatedVersion = IRI
                .create("http://svn.apache.org/repos/asf/incubator/stanbol/trunk/commons/owl/src/test/resources/owl/versioned_distance-100.owl");
        OWLOntologyID expectedOntId = new OWLOntologyID(ontologyIri, incubatedVersion);

        // No triple limit but offset < 102 : guessing should return the unversioned ID.
        InputStream content = getClass().getResourceAsStream(location);
        OWLOntologyID id = OWLUtils.guessOntologyID(content, parser, TURTLE, -1, 99);
        assertNotNull(id);
        assertEquals(new OWLOntologyID(ontologyIri), id);

        // Try again, setting limit = 1024 (offset = 102) should work.
        content = getClass().getResourceAsStream(location);
        id = OWLUtils.guessOntologyID(content, parser, TURTLE, 1024);
        assertNotNull(id);
        assertEquals(expectedOntId, id);
    }

    /*
     * Guessing the ID of an ontology whose ontology IRI is declared >100 triples after its version IRI.
     */
    @Test
    public void lookaheadVersionedDistance100Reversed() throws Exception {
        // Actual distance is 102
        String location = "/owl/versioned_distance-100-reversed.owl";
        log.info("Testing lookahead for location {}", location);
        IRI incubatedVersion = IRI
                .create("http://svn.apache.org/repos/asf/incubator/stanbol/trunk/commons/owl/src/test/resources/owl/versioned_distance-100-reversed.owl");
        OWLOntologyID expectedOntId = new OWLOntologyID(ontologyIri, incubatedVersion);

        // No triple limit but offset < 102 : guessing should fail.
        InputStream content = getClass().getResourceAsStream(location);
        OWLOntologyID id = OWLUtils.guessOntologyID(content, parser, TURTLE, -1, 99);
        assertTrue(id.isAnonymous());

        // Try again, setting limit = 1024 (offset = 102) should work.
        content = getClass().getResourceAsStream(location);
        id = OWLUtils.guessOntologyID(content, parser, TURTLE, 1024);
        assertNotNull(id);
        assertFalse(id.isAnonymous());
        assertEquals(expectedOntId, id);
    }

    /*
     * Guessing the ID of a versioned ontology whose ID is at the beginning of the graph.
     */
    @Test
    public void lookaheadVersionedImmediate() throws Exception {
        String location = "/owl/versioned_immediate.owl";
        log.info("Testing lookahead for location {}", location);
        IRI incubatedVersion = IRI
                .create("http://svn.apache.org/repos/asf/incubator/stanbol/trunk/commons/owl/src/test/resources/owl/versioned_immediate.owl");
        OWLOntologyID expectedOntId = new OWLOntologyID(ontologyIri, incubatedVersion);

        // Try a low triple limit (the ontology IRI triple is much further).
        InputStream content = getClass().getResourceAsStream(location);
        OWLOntologyID id = OWLUtils.guessOntologyID(content, parser, RDF_XML, 10);
        assertNotNull(id);
        assertEquals(expectedOntId, id);
    }

    /*
     * Extracting the OWL ontology identifier on a *whole* ontology.
     */
    @Test
    public void namedUriRef() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/owl/maincharacters.owl");
        MGraph mg = TcManager.getInstance().createMGraph(uri);
        parser.parse(mg, inputStream, "application/rdf+xml", uri);
        assertNotNull(OWLUtils.extractOntologyID(mg.getGraph()));
    }

    /*
     * Extracting the OWL ontology identifier on a *whole* nameless ontology must return a null value.
     */
    @Test
    public void namelessUriRef() throws Exception {
        InputStream inputStream = getClass().getResourceAsStream("/owl/nameless.owl");
        MGraph mg = TcManager.getInstance().createMGraph(uri);
        parser.parse(mg, inputStream, "application/rdf+xml", uri);
        assertNull(OWLUtils.extractOntologyID(mg.getGraph()));
    }

    @After
    public void reset() throws Exception {
        if (TcManager.getInstance().listTripleCollections().contains(uri)) TcManager.getInstance()
                .deleteTripleCollection(uri);
    }
}
