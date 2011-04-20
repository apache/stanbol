package org.apache.stanbol.ontologymanager.ontonet.io;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;
import java.util.Set;

import org.apache.stanbol.ontologymanager.ontonet.Constants;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.ParentPathInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologyIRISource;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;

public class TestOntologyInputSources {

    private static ONManager onm;

    private static OWLDataFactory df;

    @BeforeClass
    public static void setUp() {
        onm = new ONManagerImpl(null, null, new Hashtable<String,Object>());
        df = onm.getOwlFactory();
    }

    /**
     * Loads a modified FOAF by resolving a URI from a resource directory.
     * 
     * @throws Exception
     */
    @Test
    public void testOfflineSingleton() throws Exception {
        URL url = getClass().getResource("/ontologies/index.rdf");
        assertNotNull(url);
        OntologyInputSource coreSource = new RootOntologyIRISource(IRI.create(url));
        assertNotNull(df);
        /*
         * To check it fetched the correct ontology, we look for a declaration of the bogus class foaf:Perzon
         * (added in the local FOAF)
         */
        OWLClass cPerzon = df.getOWLClass(IRI.create("http://xmlns.com/foaf/0.1/Perzon"));
        assertTrue(coreSource.getRootOntology().getClassesInSignature().contains(cPerzon));
    }

    /**
     * Uses a {@link ParentPathInputSource} to load an ontology importing a modified FOAF, both located in the
     * same resource directory.
     * 
     * @throws Exception
     */
    @Test
    public void testOfflineImport() throws Exception {
        URL url = getClass().getResource("/ontologies/maincharacters.owl");
        assertNotNull(url);
        File f = new File(url.toURI());
        assertNotNull(f);
        OntologyInputSource coreSource = new ParentPathInputSource(f);

        // Check that all the imports closure is made of local files
        Set<OWLOntology> closure = coreSource.getClosure();
        for (OWLOntology o : closure)
            assertEquals("file", o.getOWLOntologyManager().getOntologyDocumentIRI(o).getScheme());

        assertEquals(coreSource.getRootOntology().getOntologyID().getOntologyIRI(),
            IRI.create(Constants.base));
        // Linus is stated to be a foaf:Person
        OWLNamedIndividual iLinus = df.getOWLNamedIndividual(IRI.create(Constants.base + "#Linus"));
        // Lucy is stated to be a foaf:Perzon
        OWLNamedIndividual iLucy = df.getOWLNamedIndividual(IRI.create(Constants.base + "#Lucy"));
        OWLClass cPerzon = df.getOWLClass(IRI.create("http://xmlns.com/foaf/0.1/Perzon"));

        Set<OWLIndividual> instances = cPerzon.getIndividuals(coreSource.getRootOntology());
        assertTrue(!instances.contains(iLinus) && instances.contains(iLucy));
    }

}
