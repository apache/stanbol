package org.apache.stanbol.ontologymanager.ontonet;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class StructureTest {

	private static IRI baseIri = IRI.create(Constants.PEANUTS_MAIN_BASE);

	private static OWLOntologyManager ontMgr = null;

	@BeforeClass
	public static void setUp() {
		try {
			//new Activator().start(null);
			ontMgr = OWLManager.createOWLOntologyManager();
		} catch (Exception e) {
			fail("Bundle activator could not be started");
		}
	}

	@Test
	public void testOWLManagerCreation() {
		assertNotNull(ontMgr);
	}

	@Test
	public void testOntologyCreation() {
		try {
			assertNotNull(ontMgr.createOntology(baseIri));
		} catch (OWLOntologyCreationException e) {
			fail("An empty ontology manager failed to create ontology with base IRI "
					+ baseIri + " !");
		}
	}

	// @Test
	// public void testReasoner() {
	// OWLOntology ont = null;
	// ;
	// try {
	// ont = ontMgr.createOntology(baseIri);
	// } catch (OWLOntologyCreationException e) {
	// fail("Could not create ontology with base IRI " + Constants.base);
	// }
	// OWLReasoner reasoner = ManagerContext.get().getReasonerFactory()
	// .createReasoner(ont);
	// assertNotNull(reasoner.getRootOntology());
	// assertTrue(true);
	// }

}
