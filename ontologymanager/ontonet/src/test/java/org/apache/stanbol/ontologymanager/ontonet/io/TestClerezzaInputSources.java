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
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Set;

import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.ontologymanager.ontonet.Locations;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphContentInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.GraphSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.api.scope.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.ClerezzaOntologyProvider;
import org.apache.stanbol.ontologymanager.ontonet.impl.clerezza.CoreOntologySpaceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClerezzaInputSources {

    private Logger log = LoggerFactory.getLogger(getClass());

    @BeforeClass
    public static void loadGraphs() throws Exception {
        reset();
    }

    private OntologyInputSource<TripleCollection,?> gis;

    @Before
    public void bind() throws Exception {

    }

    @After
    public void cleanup() {
        reset();
    }

    @Test
    public void testGraphContentSource() throws Exception {
        // Make sure the tc manager has been reset
        assertEquals(1, tcManager.listTripleCollections().size());

        OntologyProvider<TcProvider> provider = new ClerezzaOntologyProvider(tcManager,
                new OfflineConfigurationImpl(new Hashtable<String,Object>()), parser);

        InputStream content = TestClerezzaInputSources.class
                .getResourceAsStream("/ontologies/droppedcharacters.owl");
        OntologyInputSource<?,TcProvider> src = new GraphContentInputSource(content, SupportedFormat.RDF_XML,
            new SimpleTcProvider(), parser);
        
        log.info("After input source creation, TcManager has {} graphs. ", tcManager.listTripleCollections()
                .size());
        for (UriRef name : tcManager.listTripleCollections())
            log.info("-- {} (a {})", name, tcManager.getTriples(name).getClass().getSimpleName());
        assertEquals(1, tcManager.listTripleCollections().size());
        OntologySpace spc = new CoreOntologySpaceImpl(TestClerezzaInputSources.class.getSimpleName(),
                IRI.create("http://stanbol.apache.org/ontologies/"), provider);
        spc.addOntology(src);
        log.info("After addition to space, TcManager has {} graphs. ", tcManager.listTripleCollections()
                .size());

        for (UriRef name : tcManager.listTripleCollections())
            log.info("-- {} (a {})", name, tcManager.getTriples(name).getClass().getSimpleName());
        assertEquals(2, tcManager.listTripleCollections().size());
    
    }

    @Test
    public void testGraphSource() throws Exception {
        UriRef uri = new UriRef(Locations.CHAR_ACTIVE.toString());
        InputStream inputStream = TestClerezzaInputSources.class
                .getResourceAsStream("/ontologies/characters_all.owl");
        parser.parse(tcManager.createMGraph(uri), inputStream, SupportedFormat.RDF_XML, uri);
        uri = new UriRef(Locations.CHAR_MAIN.toString());
        inputStream = TestClerezzaInputSources.class.getResourceAsStream("/ontologies/maincharacters.owl");
        parser.parse(tcManager.createMGraph(uri), inputStream, SupportedFormat.RDF_XML, uri);
        uri = new UriRef(Locations.CHAR_MINOR.toString());
        inputStream = TestClerezzaInputSources.class.getResourceAsStream("/ontologies/minorcharacters.owl");
        parser.parse(tcManager.createMGraph(uri), inputStream, SupportedFormat.RDF_XML, uri);

        gis = new GraphSource(new UriRef(Locations.CHAR_ACTIVE.toString()));
        assertNotNull(gis);
        assertNotNull(gis.getRootOntology());
        Set<TripleCollection> imported = gis.getImports(false);
        // Number of stored graphs minus the importing one minus the reserved graph = imported graphs
        assertEquals(tcManager.listTripleCollections().size() - 2, imported.size());
        for (TripleCollection g : imported)
            assertNotNull(g);
    }

}
