package org.apache.stanbol.reengineer.db;

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
import org.apache.stanbol.reengineer.base.impl.ReengineerManagerImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

public class DBExtractorTest {

    static DBExtractor dbExtractor;
    static String graphNS;
    static ScopeManager onManager;
    static IRI outputIRI;

    @BeforeClass
    public static void setupClass() {
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

        OfflineConfiguration offline = new OfflineConfigurationImpl(emptyConf);
        OntologyProvider<TcProvider> ontologyProvider = new ClerezzaOntologyProvider(tcm, offline,
                new Parser());

        // Two different ontology storages, the same sparql engine and tcprovider
        ClerezzaCollectorFactory sf = new ClerezzaCollectorFactory(ontologyProvider, emptyConf);

        onManager = new ScopeManagerImpl(ontologyProvider, offline, sf, sf, emptyConf);
        dbExtractor = new DBExtractor(new ReengineerManagerImpl(emptyConf), onManager, tcm, wtcp, emptyConf);
        graphNS = "http://kres.iks-project.eu/reengineering/test";
        outputIRI = IRI.create(graphNS);
    }

    @Before
    public void setup() {

    }

    @Before
    public void tearDown() {

    }

    @Test
    public void testDataReengineering() {
        // graphNS = "http://kres.iks-project.eu/reengineering/test";
        // outputIRI = IRI.create(graphNS);
        // try {
        // OWLOntology ontology = dbExtractor.dataReengineering(graphNS,
        // outputIRI, null, dbExtractor.schemaReengineering(graphNS,
        // outputIRI, null));
        // } catch (ReengineeringException e) {
        // fail("Some errors occur with dataReengineering of DBExtractor.");
        // }
    }

    @Test
    public void testReengineering() {
        // graphNS = "http://kres.iks-project.eu/reengineering/test";
        // outputIRI = IRI.create(graphNS);
        // try {
        // OWLOntology ontology = dbExtractor.reengineering(graphNS,
        // outputIRI, null);
        // } catch (ReengineeringException e) {
        // fail("Some errors occur with reengineering of DBExtractor.");
        // }
    }

    @Test
    public void testSchemaReengineering() {
        // OWLOntology ontology = dbExtractor.schemaReengineering(graphNS,
        // outputIRI, null);
    }
}
