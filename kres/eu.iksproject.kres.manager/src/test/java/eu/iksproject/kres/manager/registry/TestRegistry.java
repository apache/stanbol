package eu.iksproject.kres.manager.registry;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.io.OntologyRegistryIRISource;

public class TestRegistry {

	private static ONManager context;

	private static IRI testRegistryIri = IRI
			.create("http://www.ontologydesignpatterns.org/registry/krestest.owl");

	@SuppressWarnings("unused")
	private static IRI submissionsIri = IRI
			.create("http://www.ontologydesignpatterns.org/registry/submissions.owl");

	@BeforeClass
	public static void setup() {
		context = ONManager.get();
		// Uncomment next line for verbose output.
		// context.getRegistryLoader().setPrintLoadedOntologies(true);
	}

	@Test
	public void testAddRegistryToSessionSpace() {
		IRI scopeIri = IRI.create("http://fise.iks-project.eu/scopone");
		SessionOntologySpace space = null;
		space = context.getOntologySpaceFactory().createSessionOntologySpace(
				scopeIri);

		space.setUp();
		try {
			space.addOntology(new OntologyRegistryIRISource(testRegistryIri));
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
			scope = context.getOntologyScopeFactory().createOntologyScope(
					scopeIri, new OntologyRegistryIRISource(testRegistryIri));
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
		space = context.getOntologySpaceFactory().createCoreOntologySpace(
				scopeIri, new OntologyRegistryIRISource(testRegistryIri));

		assertTrue(space != null && space.getTopOntology() != null);
	}

}
