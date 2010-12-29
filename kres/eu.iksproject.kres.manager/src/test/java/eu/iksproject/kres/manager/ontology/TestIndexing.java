package eu.iksproject.kres.manager.ontology;

import static org.junit.Assert.*;

import java.util.Hashtable;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyIndex;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceModificationException;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.io.OntologyRegistryIRISource;
import eu.iksproject.kres.manager.io.RootOntologyIRISource;
import eu.iksproject.kres.manager.io.RootOntologySource;
import eu.iksproject.kres.manager.util.OntologyUtils;

public class TestIndexing {

	private static KReSONManager onm;

	private static OWLOntologyManager mgr = OWLManager
			.createOWLOntologyManager();

	private static IRI semionXmlIri = IRI
			.create("http://ontologydesignpatterns.org/ont/iks/oxml.owl"), communitiesCpIri = IRI
			.create("http://www.ontologydesignpatterns.org/cp/owl/communities.owl"), topicCpIri = IRI
			.create("http://www.ontologydesignpatterns.org/cp/owl/topic.owl"),
			objrole = IRI
					.create("http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl"),
			scopeIri = IRI.create("http://fise.iks-project.eu/TestIndexing"),
			// submissionsIri = IRI
			// .create("http://www.ontologydesignpatterns.org/registry/submissions.owl"),
			testRegistryIri = IRI
					.create("http://www.ontologydesignpatterns.org/registry/krestest.owl");

	private static OntologyScope scope = null;

	@BeforeClass
	public static void setup() {
		// An ONManager with no store and default settings
		onm = new ONManager(null, new Hashtable<String, Object>());

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
					new OntologyRegistryIRISource(testRegistryIri,onm.getOwlCacheManager(),onm.getRegistryLoader(), null
					// new RootOntologySource(oParent)
					));
			onm.getScopeRegistry().registerScope(scope);
		} catch (DuplicateIDException e) {
			// Uncomment if annotated with @BeforeClass instead of @Before ,
			// comment otherwise.
			fail("DuplicateID exception caught when creating test scope.");
		}
	}

	@Test
	public void testAddOntology() {
		OntologyIndex index = onm.getOntologyIndex();
		try {
			scope.getCustomSpace().addOntology(
					new RootOntologyIRISource(communitiesCpIri));
			assertTrue(index.isOntologyLoaded(communitiesCpIri));
			scope.getCustomSpace().addOntology(
					new RootOntologyIRISource(topicCpIri));
			scope.getCustomSpace().removeOntology(
					new RootOntologyIRISource(communitiesCpIri));
		} catch (UnmodifiableOntologySpaceException e1) {
			fail("Unit test Failed to modify seemingly unlocked ontology scope "
					+ scope.getID());
		} catch (OWLOntologyCreationException e1) {
			fail("Unit test Failed to load ontology " + communitiesCpIri);
		} catch (OntologySpaceModificationException e) {
			fail("Unit test Failed to remove ontology " + communitiesCpIri);
		}
		assertFalse(index.isOntologyLoaded(communitiesCpIri));
	}

	@Test
	public void testGetOntology() {
		OWLOntology oObjRole = null;
		try {
			oObjRole = mgr.loadOntologyFromOntologyDocument(objrole);
		} catch (OWLOntologyCreationException e) {
			fail("Could not instantiate other ObjectRole ontology for comparison");
		}
		OntologyIndex index = onm.getOntologyIndex();
		assertNotNull(index.getOntology(objrole));
		// assertSame() would fail.
		assertEquals(index.getOntology(objrole), oObjRole);
	}

	@Test
	public void testIsOntologyLoaded() {
		OntologyIndex index = onm.getOntologyIndex();
		IRI coreroot = IRI.create(scopeIri + "/core/root.owl");
		IRI dne = IRI
				.create("http://www.ontologydesignpatterns.org/cp/owl/doesnotexist.owl");
		IRI objrole = IRI
				.create("http://www.ontologydesignpatterns.org/cp/owl/objectrole.owl");

		assertTrue(index.isOntologyLoaded(coreroot));
		assertTrue(index.isOntologyLoaded(objrole));
		// TODO : find a way to index anonymous ontologies
		assertTrue(!index.isOntologyLoaded(dne));
	}

}
