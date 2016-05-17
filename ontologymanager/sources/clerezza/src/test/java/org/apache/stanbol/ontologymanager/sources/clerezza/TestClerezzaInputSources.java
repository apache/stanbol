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
package org.apache.stanbol.ontologymanager.sources.clerezza;

import static org.apache.stanbol.ontologymanager.sources.clerezza.MockOsgiContext.parser;
import static org.apache.stanbol.ontologymanager.sources.clerezza.MockOsgiContext.tcManager;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.InputStream;

import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.ontologymanager.servicesapi.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyLoadingException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test suite for the correctness of {@link OntologyInputSource} implementations based on Clerezza.
 * 
 * @author alexdma
 * 
 */
public class TestClerezzaInputSources {

    @BeforeClass
    public static void loadGraphs() throws Exception {
    	//nothing to do
    }

    private String dummy_RdfXml = "/ontologies/dummy-01.rdfxml.rdf";

    private String dummy_Turtle = "/ontologies/dummy-01.turtle.rdf";

    private Logger log = LoggerFactory.getLogger(getClass());

    private OntologyInputSource<Graph> src;

    @Before
    public void bind() throws Exception {
        src = null;
    }

    private void checkOntology(boolean usesTcProvider) throws Exception {
        assertNotNull(src);
        if (usesTcProvider) assertNotNull(src.getOrigin());
        else assertNull(src.getOrigin());
        Graph o = src.getRootOntology();
        assertNotNull(o);
        log.info("Ontology loaded, is a {}", o.getClass().getCanonicalName());
        assertSame(5, o.size()); // The owl:Ontology declaration and versionInfo also count as triples.
    }

    @After
    public void cleanup() {
    	//nothing to do
    }

    /*
     * If the format is specificed and correct, the ontology source should be created as expected.
     */
    @Test
    public void fromInputStreamWithFormat() throws Exception {
        InputStream in = getClass().getResourceAsStream(dummy_Turtle);
        src = new GraphContentInputSource(in, SupportedFormat.TURTLE);
        checkOntology(false);
    }

    /*
     * An ontology input source created using a custom TC Provider should create a non-null Origin (i.e. the
     * name of the generated graph) and increase the triple collection count by 1.
     */
    @Test
    public void fromInputStreamInSimpleTcProvider() throws Exception {
        InputStream in = getClass().getResourceAsStream(dummy_RdfXml);
        TcProvider tcp = new SimpleTcProvider();
        assertSame(0, tcp.listGraphs().size());
        int before = tcp.listGraphs().size();
        src = new GraphContentInputSource(in, tcp);
        checkOntology(true);
        assertSame(before + 1, tcp.listGraphs().size());
    }

    /*
     * An ontology input source created using the Clerezza TC Manager should create a non-null Origin (i.e.
     * the name of the generated graph) and increase the triple collection count by 1.
     */
    @Test
    public void fromInputStreamInTcManager() throws Exception {
        InputStream in = getClass().getResourceAsStream(dummy_RdfXml);
        int before = tcManager.listGraphs().size();
        src = new GraphContentInputSource(in, tcManager);
        checkOntology(true);
        assertSame(before + 1, tcManager.listGraphs().size());
    }

    /*
     * If the format is unspecificed, input source creation should still succeed if the resource is in the
     * preferred format (RDF/XML). In all other cases it is OK whether it fails or succeeds.
     */
    @Test
    public void fromInputStreamNoFormat() throws Exception {
        // This should be successful as the RDF/XML parser is tried first.
        InputStream in = getClass().getResourceAsStream(dummy_RdfXml);
        src = new GraphContentInputSource(in);
        checkOntology(false);

        // This should fail unless the input stream can be reset.
        in = getClass().getResourceAsStream(dummy_Turtle);
        try {
            src = new GraphContentInputSource(in);
            log.warn("Unexpected behaviour: no {} caught.", OntologyLoadingException.class.getSimpleName());
            log.warn("Will check if loading was successful.");
            checkOntology(false);
        } catch (OntologyLoadingException ex) {
            log.info("Caught expected {}", ex.getClass().getSimpleName());
        }
    }

    // TODO move this test where we have access to the Clerezza implementation.
    //
    // @Test
    // public void testGraphContentSource() throws Exception {
    // // Make sure the tc manager has been reset
    // assertEquals(1, tcManager.listGraphs().size());
    //
    // OntologyProvider<TcProvider> provider = new ClerezzaOntologyProvider(tcManager,
    // new OfflineConfigurationImpl(new Hashtable<String,Object>()), parser);
    // int tcs = tcManager.listGraphs().size();
    // InputStream content = TestClerezzaInputSources.class
    // .getResourceAsStream("/ontologies/droppedcharacters.owl");
    // OntologyInputSource<?> src = new GraphContentInputSource(content, SupportedFormat.RDF_XML,
    // ontologyProvider.getStore(), parser);
    //
    // log.info("After input source creation, TcManager has {} graphs. ", tcManager.listGraphs()
    // .size());
    // for (IRI name : tcManager.listGraphs())
    // log.info("-- {} (a {})", name, tcManager.getTriples(name).getClass().getSimpleName());
    // assertEquals(tcs + 1, tcManager.listGraphs().size());
    // Space spc = new CoreSpaceImpl(TestClerezzaInputSources.class.getSimpleName(),
    // IRI.create("http://stanbol.apache.org/ontologies/"), provider);
    // spc.addOntology(src);
    // log.info("After addition to space, TcManager has {} graphs. ", tcManager.listGraphs()
    // .size());
    //
    // for (IRI name : tcManager.listGraphs())
    // log.info("-- {} (a {})", name, tcManager.getTriples(name).getClass().getSimpleName());
    // // Adding the ontology from the same storage should not create new graphs
    // assertEquals(tcs + 1, tcManager.listGraphs().size());
    //
    // }

    @Test
    public void testGraphSource() throws Exception {
        IRI uri = new IRI(Locations.CHAR_ACTIVE.toString());
        InputStream inputStream = TestClerezzaInputSources.class
                .getResourceAsStream("/ontologies/characters_all.owl");
        parser.parse(tcManager.createGraph(uri), inputStream, SupportedFormat.RDF_XML, uri);
        uri = new IRI(Locations.CHAR_MAIN.toString());
        inputStream = TestClerezzaInputSources.class.getResourceAsStream("/ontologies/maincharacters.owl");
        parser.parse(tcManager.createGraph(uri), inputStream, SupportedFormat.RDF_XML, uri);
        uri = new IRI(Locations.CHAR_MINOR.toString());
        inputStream = TestClerezzaInputSources.class.getResourceAsStream("/ontologies/minorcharacters.owl");
        parser.parse(tcManager.createGraph(uri), inputStream, SupportedFormat.RDF_XML, uri);

        src = new GraphSource(new IRI(Locations.CHAR_ACTIVE.toString()));
        assertNotNull(src);
        assertNotNull(src.getRootOntology());
        // Set<Graph> imported = gis.getImports(false);
        // // Number of stored graphs minus the importing one minus the reserved graph = imported graphs
        // assertEquals(tcManager.listGraphs().size() - 2, imported.size());
        // for (Graph g : imported)
        // assertNotNull(g);
    }

}
