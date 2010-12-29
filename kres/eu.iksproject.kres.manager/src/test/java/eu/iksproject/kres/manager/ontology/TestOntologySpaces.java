package eu.iksproject.kres.manager.ontology;

import static org.junit.Assert.*;

import java.util.Hashtable;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.CustomOntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceModificationException;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.manager.Constants;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.io.RootOntologyIRISource;
import eu.iksproject.kres.manager.io.RootOntologySource;
import eu.iksproject.kres.manager.util.OntologyUtils;

public class TestOntologySpaces {

	public static IRI baseIri = IRI.create(Constants.base), baseIri2 = IRI
			.create(Constants.base2), scopeIri = IRI
			.create("http://kres.iks-project.eu/scope/Peanuts");

	private static IRI pizzaIRI = IRI
			.create("http://www.co-ode.org/ontologies/pizza/2007/02/12/pizza.owl"),
			wineIRI = IRI
					.create("http://www.schemaweb.info/webservices/rest/GetRDFByID.aspx?id=62");

	private static OWLOntology ont = null, ont2 = null;

	private static KReSONManager onm;
	
	private static OntologyInputSource ontSrc, ont2Src, pizzaSrc, wineSrc;

	private static OntologySpaceFactory spaceFactory;

	private static OWLAxiom linusIsHuman = null;

	@BeforeClass
	public static void setup() {
		// An ONManager with no store and default settings
		onm = new ONManager(null, new Hashtable<String, Object>());
		spaceFactory = onm.getOntologySpaceFactory();
		if (spaceFactory == null)
			fail("Could not instantiate ontology space factory");

		OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
		OWLDataFactory df = mgr.getOWLDataFactory();
		try {
			ont = mgr.createOntology(baseIri);
			ontSrc = new RootOntologySource(ont, null);
			// Let's state that Linus is a human being
			OWLClass cHuman = df.getOWLClass(IRI.create(baseIri + "/"
					+ Constants.humanBeing));
			OWLIndividual iLinus = df.getOWLNamedIndividual(IRI.create(baseIri
					+ "/" + Constants.linus));
			linusIsHuman = df.getOWLClassAssertionAxiom(cHuman, iLinus);
			mgr.applyChange(new AddAxiom(ont, linusIsHuman));
		} catch (OWLOntologyCreationException e) {
			fail("Could not setup ontology with base IRI " + baseIri);
		}
		try {
			ont2 = mgr.createOntology(baseIri2);
			ont2Src = new RootOntologySource(ont2);
		} catch (OWLOntologyCreationException e) {
			fail("Could not setup ontology with base IRI " + baseIri2);
		}
		try {
			pizzaSrc = new RootOntologyIRISource(pizzaIRI, mgr);
			wineSrc = new RootOntologyIRISource(wineIRI, mgr);
			ont2Src = new RootOntologySource(ont2, null);
		} catch (OWLOntologyCreationException e) {
			fail("Could not setup ontology with base IRI ");
		}
	}

	@Test
	public void testAddOntology() {
		CustomOntologySpace space = null;
		IRI logicalId = wineSrc.getRootOntology().getOntologyID()
				.getOntologyIRI();
		try {
			space = spaceFactory.createCustomOntologySpace(scopeIri, pizzaSrc);
			space.addOntology(wineSrc);
		} catch (UnmodifiableOntologySpaceException e) {
			fail("Add operation on " + scopeIri
					+ " custom space was denied due to unexpected lock.");
		}
		assertTrue(space.containsOntology(logicalId));
	}

	@Test
	public void testCoreLock() {
		CoreOntologySpace space = spaceFactory.createCoreOntologySpace(
				scopeIri, ontSrc);
		space.setUp();
		try {
			space.addOntology(ont2Src);
			fail("Modification was permitted on locked ontology space.");
		} catch (UnmodifiableOntologySpaceException e) {
			assertSame(space, e.getSpace());
		}
	}

	@Test
	public void testRemoveCustomOntology() {
		CustomOntologySpace space = null;
		space = spaceFactory.createCustomOntologySpace(scopeIri, pizzaSrc);
		IRI pizzaId = pizzaSrc.getRootOntology().getOntologyID()
				.getOntologyIRI();
		IRI wineId = wineSrc.getRootOntology().getOntologyID().getOntologyIRI();
		try {
			space.addOntology(ontSrc);
			space.addOntology(wineSrc);
			// The other remote ontologies may change base IRI...
			assertTrue(space.containsOntology(ont.getOntologyID()
					.getOntologyIRI())
					&& space.containsOntology(pizzaId)
					&& space.containsOntology(wineId));
			space.removeOntology(pizzaSrc);
			assertFalse(space.containsOntology(pizzaId));
			space.removeOntology(wineSrc);
			assertFalse(space.containsOntology(wineId));
			// OntologyUtils.printOntology(space.getTopOntology(), System.err);
		} catch (UnmodifiableOntologySpaceException e) {
			fail("Modification was disallowed on non-locked ontology space.");
		} catch (OntologySpaceModificationException e) {
			fail("Modification failed on ontology space "
					+ e.getSpace().getID());
		}
	}

	@Test
	public void testCustomLock() {
		CustomOntologySpace space = spaceFactory.createCustomOntologySpace(
				scopeIri, ontSrc);
		space.setUp();
		try {
			space.addOntology(ont2Src);
			fail("Modification was permitted on locked ontology space.");
		} catch (UnmodifiableOntologySpaceException e) {
			assertSame(space, e.getSpace());
		}
	}

	@Test
	public void testSessionModification() {
		SessionOntologySpace space = spaceFactory
				.createSessionOntologySpace(scopeIri);
		space.setUp();
		try {
			// First add an in-memory ontology with a few axioms.
			space.addOntology(ontSrc);
			// Now add a real online ontology
			space.addOntology(pizzaSrc);
			// The in-memory ontology must be in the space.
			assertTrue(space.getOntologies().contains(ont));
			// The in-memory ontology must still have its axioms.
			assertTrue(space.getOntology(ont.getOntologyID().getOntologyIRI())
					.containsAxiom(linusIsHuman));
			// The top ontology must still have axioms from in-memory
			// ontologies.
			assertTrue(space.getTopOntology().containsAxiom(linusIsHuman));
		} catch (UnmodifiableOntologySpaceException e) {
			fail("Modification was denied on unlocked ontology space.");
		}
	}

}
