package org.apache.stanbol.explanation.impl;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.stanbol.explanation.Data;
import org.apache.stanbol.explanation.MockOsgiContext;
import org.apache.stanbol.ontologymanager.ontonet.api.OfflineConfiguration;
import org.apache.stanbol.ontologymanager.ontonet.impl.OfflineConfigurationImpl;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.apache.stanbol.ontologymanager.registry.impl.RegistryManagerImpl;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSchemaMatchers {

    private String kbId = "http://stanbol.apache.org/ontologies/explanation_testdata";

    private Logger log = LoggerFactory.getLogger(getClass());

    private static Library kps, kpmappings;

    private static RegistryManager regman;

    @BeforeClass
    public static void setupLibrary() {

        Dictionary<String,Object> configuration = new Hashtable<String,Object>();
        // We need this to make sure the local meta.owl (which does not import codolight) is loaded.
        configuration.put(OfflineConfiguration.ONTOLOGY_PATHS, new String[] {"/ontologies", "/schemas",
                                                                             "/schemas/registry"});
        configuration.put(
            RegistryManager.REGISTRY_LOCATIONS,
            new String[] {
                          TestSchemaMatchers.class.getResource("/schemas/registry/explanation.owl")
                                  .toString(),
                          TestSchemaMatchers.class.getResource("/schemas/registry/explanation-mappings.owl")
                                  .toString()});
        OfflineConfiguration offline = new OfflineConfigurationImpl(configuration);
        regman = new RegistryManagerImpl(offline, configuration);

        // The model should be created by now.

        for (Library lib : regman.getLibraries()) {
            System.out.println(lib.getIRI() + " : " + lib.getName());
        }

        // IRI id =
        // IRI.create("http://www.ontologydesignpatterns.org/registry/explanation.owl#ExplanationSchemaCatalog");
        // IRI[] locations = new IRI[] {};
        // kps = new LibraryImpl(id, "Knowledge Pattern additions",
        // OWLOntologyManagerFactory.createOWLOntologyManager(locations));
        // kps.addChild(child)
    }

    @After
    public void reset() {
        MockOsgiContext.reset();
    }

    @Test
    public void testClerezzaMatcher() throws Exception {
        TcManager tcm = MockOsgiContext.tcManager;
        MGraph kb = tcm.createMGraph(new UriRef(kbId));

        InputStream inputStream = getClass().getResourceAsStream("/ontologies/explanation_testdata.owl");

        // get the singleton instance of Parser
        final Parser parser = MockOsgiContext.parser;

        Graph deserializedGraph = parser.parse(inputStream, "application/rdf+xml");

        kb.addAll(deserializedGraph);

        /* DEBUG code */
        // UriRef fp = new UriRef("http://xmlns.com/foaf/0.1/Person");
        // Iterator<Triple> triples = kb.filter(null, null, fp);
        // while (triples.hasNext())
        // log.debug("{}", triples.next());
        // triples = kb.filter(fp, null, null);
        // while (triples.hasNext())
        // log.debug("{}", triples.next());

        String uf = Data.URI_FANTOZZI;

        ClerezzaSchemaMatcher matcher = new ClerezzaSchemaMatcherImpl(MockOsgiContext.onManager,
                new Hashtable<String,Object>());
        matcher.setKnowledgeBase(kb.getGraph());
        matcher.getSatisfiableSchemas(regman.getLibraries(), new UriRef(uf));

        uf = Data.URI_RICCARDELLI;
        matcher.getSatisfiableSchemas(regman.getLibraries(), new UriRef(uf));

    }

}
