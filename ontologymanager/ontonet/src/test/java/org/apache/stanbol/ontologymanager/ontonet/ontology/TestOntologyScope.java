package org.apache.stanbol.ontologymanager.ontonet.ontology;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import org.apache.stanbol.ontologymanager.ontonet.Constants;
import org.apache.stanbol.ontologymanager.ontonet.api.DuplicateIDException;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScope;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologyScopeFactory;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.ScopeRegistry;
import org.apache.stanbol.ontologymanager.ontonet.impl.ONManagerImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.CoreOntologySpaceImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.CustomOntologySpaceImpl;
import org.apache.stanbol.ontologymanager.ontonet.impl.ontology.OntologyScopeFactoryImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class TestOntologyScope {

	public static IRI baseIri = IRI.create(Constants.base),
			baseIri2 = IRI.create(Constants.base2),
			scopeIriBlank = IRI
					.create("http://kres.iks-project.eu/scope/WackyRaces"),
			scopeIri1 = IRI.create("http://kres.iks-project.eu/scope/Peanuts"),
			scopeIri2 = IRI
					.create("http://kres.iks-project.eu/scope/CalvinAndHobbes");

	/**
	 * An ontology scope that initially contains no ontologies, and is rebuilt
	 * from scratch before each test method.
	 */
	private static OntologyScope blankScope;

	private static ONManager onm;
	
	private static OntologyScopeFactory factory = null;

	private static OntologyInputSource src1 = null, src2 = null;

	@Before
	public void cleaup() throws DuplicateIDException {
		if (factory != null)
			blankScope = factory.createOntologyScope(scopeIriBlank, null);
	}

	@BeforeClass
	public static void setup() {
		// An ONManagerImpl with no store and default settings
		onm = new ONManagerImpl(null,null, new Hashtable<String, Object>());
		factory = onm.getOntologyScopeFactory();
		if (factory == null)
			fail("Could not instantiate ontology space factory");
		OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
		try {
			src1 = new RootOntologySource(mgr.createOntology(baseIri), null);
			src2 = new RootOntologySource(mgr.createOntology(baseIri2), null);
		} catch (OWLOntologyCreationException e) {
			fail("Could not setup ontology with base IRI " + Constants.base);
		}
	}

	/**
	 * Tests that a scope is generated with the expected identifiers for both
	 * itself and its core and custom spaces.
	 */
	@Test
	public void testIdentifiers() {
		OntologyScope scope = null;
		try {
			scope = factory.createOntologyScope(scopeIri1, src1, src2);
			scope.setUp();
		} catch (DuplicateIDException e) {
			fail("Unexpected DuplicateIDException caught when creating scope "
					+ "with non-null parameters in a non-registered environment.");
		}
		boolean condition = scope.getID().equals(scopeIri1);
		condition &= scope.getCoreSpace().getID().equals(
				IRI.create(scopeIri1 + "/" + CoreOntologySpaceImpl.SUFFIX));
		condition &= scope.getCustomSpace().getID().equals(
				IRI.create(scopeIri1 + "/" + CustomOntologySpaceImpl.SUFFIX));
		assertTrue(condition);
	}

	/**
	 * Tests that creating an ontology scope with null identifier fails to
	 * generate the scope at all.
	 */
	@Test
	public void testNullScopeCreation() {
		OntologyScope scope = null;
		try {
			scope = factory.createOntologyScope(null, null);
		} catch (DuplicateIDException e) {
			fail("Unexpected DuplicateIDException caught while testing scope creation"
					+ " with null parameters.");
		} catch (NullPointerException ex) {
			// Expected behaviour.
		}
		assertNull(scope);
	}

	/**
	 * Tests that an ontology scope is correctly generated with both a core
	 * space and a custom space. The scope is set up but not registered.
	 */
	@Test
	public void testScopeSetup() {
		OntologyScope scope = null;
		try {
			scope = factory.createOntologyScope(scopeIri1, src1, src2);
			scope.setUp();
		} catch (DuplicateIDException e) {
			fail("Unexpected DuplicateIDException was caught while testing scope "
					+ e.getDulicateID());
		}
		assertNotNull(scope);
	}

	/**
	 * Tests that an ontology scope is correctly generated even when missing a
	 * custom space. The scope is set up but not registered.
	 */
	@Test
	public void testScopeSetupNoCustom() {
		OntologyScope scope = null;
		try {
			scope = factory.createOntologyScope(scopeIri2, src1);
			scope.setUp();
		} catch (DuplicateIDException e) {
			fail("Duplicate ID exception caught for scope iri " + src1);
		}

		assertTrue(scope != null && scope.getCoreSpace() != null
				&& scope.getCustomSpace() != null);
	}

	@Test
	public void testScopesRendering() {
		ScopeRegistry reg = onm.getScopeRegistry();
		OntologyScopeFactoryImpl scf = new OntologyScopeFactoryImpl(reg,onm.getOntologySpaceFactory());
		OntologyScope scope = null, scope2 = null;
		try {
			scope = scf.createOntologyScope(scopeIri1, src1, src2);
			scope2 = scf.createOntologyScope(scopeIri2, src2);
			scope.setUp();
			reg.registerScope(scope);
			scope2.setUp();
			reg.registerScope(scope2);
		} catch (DuplicateIDException e) {
			fail("Duplicate ID exception caught on " + e.getDulicateID());
		}
		// System.err.println(new ScopeSetRenderer().getScopesAsRDF(reg
		// .getRegisteredScopes()));
		assertTrue(true);
	}

}
