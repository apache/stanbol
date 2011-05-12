package org.apache.stanbol.ontologymanager.ontonet.registry;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.api.registry.io.OntologyRegistryIRISource;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;


public class TestRegistry {

	private static ONManager onm;

	private static IRI testRegistryIri = IRI
			.create("http://www.ontologydesignpatterns.org/registry/krestest.owl");

	@SuppressWarnings("unused")
	private static IRI submissionsIri = IRI
			.create("http://www.ontologydesignpatterns.org/registry/submissions.owl");

	@BeforeClass
	public static void setup() {
		// An ONManagerImpl with no store and default settings
		onm = new ONManagerImpl(null,null, new Hashtable<String, Object>());
	}

	@Test
	public void testAddRegistryToSessionSpace() {
		IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
		SessionOntologySpace space = null;
		space = onm.getOntologySpaceFactory().createSessionOntologySpace(
				scopeIri);

		space.setUp();
		try {
			space.addOntology(new OntologyRegistryIRISource(testRegistryIri,onm.getOwlCacheManager(),onm.getRegistryLoader()));
		} catch (UnmodifiableOntologySpaceException e) {
			fail("Adding libraries to session space failed. "
					+ "This should not happen for active session spaces.");
		}

		assertTrue(space.getTopOntology() != null);
		assertTrue(space.getOntologies().contains(space.getTopOntology()));
	}

	@Test
	public void testScopeCreationWithRegistry() {
		IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
		OntologyScope scope = null;
		// The factory call also invokes loadRegistriesEager() and
		// gatherOntologies() so no need to test them individually.
		try {
			scope = onm.getOntologyScopeFactory().createOntologyScope(
					scopeIri, new OntologyRegistryIRISource(testRegistryIri,onm.getOwlCacheManager(),onm.getRegistryLoader()));
		} catch (DuplicateIDException e) {
			fail("DuplicateID exception caught when creating test scope.");
		}

		assertTrue(scope != null
				&& scope.getCoreSpace().getTopOntology() != null);
		// OntologyUtils.printOntology(scope.getCoreSpace().getTopOntology(),
		// System.err);
	}

	@Test
	public void testSpaceCreationWithRegistry() {
		IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
		CoreOntologySpace space = null;
		// The factory call also invokes loadRegistriesEager() and
		// gatherOntologies() so no need to test them individually.
		space = onm.getOntologySpaceFactory().createCoreOntologySpace(
				scopeIri, new OntologyRegistryIRISource(testRegistryIri,onm.getOwlCacheManager(),onm.getRegistryLoader()));

		assertTrue(space != null && space.getTopOntology() != null);
	}

}
