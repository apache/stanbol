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
package org.apache.stanbol.reengineer.xml;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.TcProvider;
import org.apache.clerezza.rdf.core.access.WeightedTcProvider;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.sparql.QueryEngine;
import org.apache.clerezza.rdf.jena.sparql.JenaSparqlEngine;
import org.apache.clerezza.rdf.simple.storage.SimpleTcProvider;
import org.apache.stanbol.ontologymanager.core.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.core.scope.ScopeManagerImpl;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.collector.ClerezzaCollectorFactory;
import org.apache.stanbol.ontologymanager.multiplexer.clerezza.ontology.ClerezzaOntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.servicesapi.ontology.OntologyProvider;
import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.reengineer.base.api.DataSource;
import org.apache.stanbol.reengineer.base.api.Reengineer;
import org.apache.stanbol.reengineer.base.api.util.ReengineerType;
import org.apache.stanbol.reengineer.base.impl.ReengineerManagerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

public class XMLReengineerTest {

    private static DataSource dataSource;
    private static String graphNS;
    private static OfflineConfiguration offline;
    private static IRI outputIRI;
    private static Reengineer xmlExtractor;

    @BeforeClass
    public static void setupClass() {

        Dictionary<String,Object> conf = new Hashtable<String,Object>();
        conf.put(OfflineConfiguration.ONTOLOGY_PATHS, new String[] {"/meta"});
        offline = new OfflineConfigurationImpl(conf);

        graphNS = "http://kres.iks-project.eu/reengineering/test";
        outputIRI = IRI.create(graphNS);
        dataSource = new DataSource() {

            @Override
            public Object getDataSource() {
                InputStream xmlStream = this.getClass().getResourceAsStream("/xml/weather.xml");
                return xmlStream;
            }

            @Override
            public int getDataSourceType() {
                return ReengineerType.XML;
            }

            @Override
            public String getID() {
                // Not going to check ID
                return null;
            }
        };

    }

    @Test
    public void dataReengineeringTest() throws Exception {
        OWLOntology schemaOntology = xmlExtractor.schemaReengineering(graphNS, outputIRI, dataSource);
        assertNotNull(schemaOntology);
        OWLOntology reengineered = xmlExtractor.dataReengineering(graphNS,
            IRI.create(outputIRI.toString() + "_new"), dataSource, schemaOntology);
        assertNotNull(reengineered);
    }

    @Test
    public void reengineeringTest() throws Exception {
        OWLOntology reengineered = xmlExtractor.reengineering(graphNS, outputIRI, dataSource);
        assertNotNull(reengineered);
    }

    @Test
    public void schemaReengineeringTest() throws Exception {
        OWLOntology schemaOntology = xmlExtractor.schemaReengineering(graphNS, outputIRI, dataSource);
        assertNotNull(schemaOntology);
    }

    @Before
    public void setup() {

        Dictionary<String,Object> emptyConf = new Hashtable<String,Object>();

        class SpecialTcManager extends TcManager {
            public SpecialTcManager(QueryEngine qe, WeightedTcProvider wtcp) {
                super();
                bindQueryEngine(qe);
                bindWeightedTcProvider(wtcp);
            }
        }

        QueryEngine qe = new JenaSparqlEngine();
        WeightedTcProvider wtcp = new SimpleTcProvider();
        TcManager tcm = new SpecialTcManager(qe, wtcp);

        // Two different ontology storages, the same sparql engine and tcprovider

        OntologyProvider<TcProvider> provider = new ClerezzaOntologyProvider(tcm, offline, new Parser());
        ClerezzaCollectorFactory factory = new ClerezzaCollectorFactory(provider, emptyConf);
        ScopeManager onManager = new ScopeManagerImpl(provider, offline, factory, factory, emptyConf);
        xmlExtractor = new XMLExtractor(new ReengineerManagerImpl(emptyConf), onManager, emptyConf);
    }

    @After
    public void tearDown() {
        xmlExtractor = null;
    }

}
