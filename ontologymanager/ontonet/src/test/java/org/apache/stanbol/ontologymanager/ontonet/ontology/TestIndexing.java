package org.apache.stanbol.ontologymanager.ontonet.ontology;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.ParentPathInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyIndex;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.io.OntologyRegistryIRISource;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class TestIndexing {

    private static ONManager onm;

    private static OWLOntologyManager mgr;

    private static IRI semionXmlIri = IRI.create("http://www.ontologydesignpatterns.org/ont/iks/oxml.owl"),
            communitiesCpIri = IRI.create("http://www.ontologydesignpatterns.org/cp/owl/communities.owl"),
            topicCpIri = IRI.create("http://www.ontologydesignpatterns.org/cp/owl/topic.owl"), objrole = IRI
                    .create("http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl"), scopeIri = IRI
                    .create("http://fise.iks-project.eu/TestIndexing"),
            // submissionsIri = IRI
            // .create("http://www.ontologydesignpatterns.org/registry/submissions.owl"),
            testRegistryIri = IRI.create("http://www.ontologydesignpatterns.org/registry/krestest.owl");

    private static OntologyScope scope = null;

    @BeforeClass
    public static void setup() {
        // An ONManagerImpl with no store and default settings
        onm = new ONManagerImpl(null, null, new Hashtable<String,Object>());
        mgr = onm.getOntologyManagerFactory().createOntologyManager(true);

        // Since it is registered, this scope must be unique, or subsequent
        // tests will fail on duplicate ID exceptions!
        scopeIri = IRI.create("http://fise.iks-project.eu/TestIndexing");
        IRI coreroot = IRI.create(scopeIri + "/core/root.owl");
        OWLOntology oParent = null;
        try {
            oParent = mgr.createOntology(coreroot);
        } catch (OWLOntologyCreationException e1) {
            // Uncomment if annotated with @BeforeClass instead of @Before
            fail("Could not create core root ontology.");
        }
        // The factory call also invokes loadRegistriesEager() and
        // gatherOntologies() so no need to test them individually.
        try {
            scope = onm.getOntologyScopeFactory().createOntologyScope(
                scopeIri,
                new OntologyRegistryIRISource(testRegistryIri, onm.getOwlCacheManager(), onm
                        .getRegistryLoader(), null
//                 new RootOntologySource(oParent
                ));
            
            for (OWLOntology o : scope.getCustomSpace().getOntologies())
                System.out.println("SCOPONE "+o.getOntologyID());
            
            onm.getScopeRegistry().registerScope(scope);
        } catch (DuplicateIDException e) {
            // Uncomment if annotated with @BeforeClass instead of @Before ,
            // comment otherwise.
            fail("DuplicateID exception caught when creating test scope.");
        }
    }

    @Test
    public void testAddOntology() throws Exception {
        OntologyIndex index = onm.getOntologyIndex();

        // Load communities ODP (and its import closure) from local resource.
        URL url = getClass().getResource("/ontologies/odp/communities.owl");
        assertNotNull(url);
        File f = new File(url.toURI());
        assertNotNull(f);
        OntologyInputSource commSrc = new ParentPathInputSource(f);
        
        OntologySpace cust = scope.getCustomSpace();     
        cust.addOntology(commSrc);

        assertTrue(index.isOntologyLoaded(communitiesCpIri));
        url = getClass().getResource("/ontologies/odp/topic.owl");
        assertNotNull(url);
        f = new File(url.toURI());
        assertNotNull(f);
        cust.addOntology(new ParentPathInputSource(f));
        cust.removeOntology(commSrc);

        assertFalse(index.isOntologyLoaded(communitiesCpIri));
    }

    @Test
    public void testGetOntology() throws Exception {
        // Load the original objectRole ODP
        OWLOntology oObjRole = mgr.loadOntology(objrole);
        assertNotNull(oObjRole);
        // Compare it against the one indexed.
        // FIXME reinstate these checks
//        OntologyIndex index = onm.getOntologyIndex();
//        assertNotNull(index.getOntology(objrole));
//        // assertSame() would fail.
//        assertEquals(index.getOntology(objrole), oObjRole);
    }

    @Test
    public void testIsOntologyLoaded() {
        OntologyIndex index = onm.getOntologyIndex();
        IRI coreroot = IRI.create(scopeIri + "/core/root.owl");
        IRI dne = IRI.create("http://www.ontologydesignpatterns.org/cp/owl/doesnotexist.owl");
        IRI objrole = IRI.create("http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl");

        // FIXME reinstate these checks
//        assertTrue(index.isOntologyLoaded(coreroot));
//        assertTrue(index.isOntologyLoaded(objrole));
        // TODO : find a way to index anonymous ontologies
        assertTrue(!index.isOntologyLoaded(dne));
    }

}
