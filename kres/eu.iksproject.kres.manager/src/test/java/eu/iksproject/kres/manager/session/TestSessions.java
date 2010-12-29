package eu.iksproject.kres.manager.session;

import static junit.framework.Assert.*;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.session.KReSSession;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.manager.session.NonReferenceableSessionException;
import eu.iksproject.kres.api.manager.session.KReSSession.State;
import eu.iksproject.kres.manager.Constants;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.io.RootOntologySource;

public class TestSessions {

	public static IRI baseIri = IRI.create(Constants.base),
			baseIri2 = IRI.create(Constants.base2),
			scopeIri1 = IRI.create("http://kres.iks-project.eu/scope/Ranma12"),
			scopeIri2 = IRI
					.create("http://kres.iks-project.eu/scope/HokutoNoKen"),
			scopeIri3 = IRI.create("http://kres.iks-project.eu/scope/Doraemon");

	private static OntologyScopeFactory scopeFactory = null;

	private static ScopeRegistry scopeRegistry = null;
	
	private static KReSSessionManager sesmgr = null;

	private static OntologySpaceFactory spaceFactory = null;

	private static OntologyInputSource src1 = null, src2 = null;

	@BeforeClass
	public static void setup() {
		// An ONManager with no store and default settings
		KReSONManager onm = new ONManager(null, new Hashtable<String, Object>());
		sesmgr = onm.getSessionManager();
		scopeFactory = onm.getOntologyScopeFactory();
		spaceFactory = onm.getOntologySpaceFactory();
		scopeRegistry = onm.getScopeRegistry();
		if (spaceFactory == null)
			fail("Could not instantiate ontology space factory");
		if (scopeFactory == null)
			fail("Could not instantiate ontology scope factory");
		OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
		try {
			src1 = new RootOntologySource(mgr.createOntology(baseIri), null);
			src2 = new RootOntologySource(mgr.createOntology(baseIri2), null);
		} catch (OWLOntologyCreationException e) {
			fail("Could not setup ontology with base IRI " + Constants.base);
		}
	}

	@Test
	public void testCreateSessionSpaceManual() {
		OntologyScope scope = null;
		try {
			// we don't register it
			scope = scopeFactory.createOntologyScope(scopeIri1, src1, src2);
			scope.setUp();
		} catch (DuplicateIDException e) {
			fail("Unexpected DuplicateIDException was caught while testing scope "
					+ e.getDulicateID());
		}
		assertNotNull(scope);
		KReSSession ses = sesmgr.createSession();
		assertTrue(scope.getSessionSpaces().isEmpty());
		scope.addSessionSpace(spaceFactory
				.createSessionOntologySpace(scopeIri1), ses.getID());
		assertFalse(scope.getSessionSpaces().isEmpty());
	}

	@Test
	public void testCreateSessionSpaceAutomatic() {
		OntologyScope scope1 = null, scope2 = null, scope3 = null;
		try {
			scope1 = scopeFactory.createOntologyScope(scopeIri1, src1, src2);
			scopeRegistry.registerScope(scope1);
			scope2 = scopeFactory.createOntologyScope(scopeIri2, src2, src1);
			scopeRegistry.registerScope(scope2);
			scope3 = scopeFactory.createOntologyScope(scopeIri3, src2, src2);
			scopeRegistry.registerScope(scope3);
			// We do all activations after registering, otherwise the component
			// property value will override these activations.
			scopeRegistry.setScopeActive(scopeIri1, true);
			scopeRegistry.setScopeActive(scopeIri2, false);
			scopeRegistry.setScopeActive(scopeIri3, true);
		} catch (DuplicateIDException e) {
			fail("Unexpected DuplicateIDException was caught while testing scope "
					+ e.getDulicateID());
		}
		KReSSession ses = sesmgr.createSession();
		IRI sesid = ses.getID();
		assertFalse(scope1.getSessionSpaces().isEmpty());
		assertNotNull(scope1.getSessionSpace(sesid));
		assertFalse(scope3.getSessionSpaces().isEmpty());
		assertNull(scope2.getSessionSpace(sesid));
		assertNotNull(scope3.getSessionSpace(sesid));
	}

	@Test
	public void testRegisterSession() {
		int before = sesmgr.getRegisteredSessionIDs().size();
		KReSSession ses = sesmgr.createSession();
		assertNotNull(ses);
		assertEquals(before + 1, sesmgr.getRegisteredSessionIDs().size());
	}

	@Test
	public void testSessionCreationDestruction() {
		int size = 500;
		int initialSize = sesmgr.getRegisteredSessionIDs().size();
		Set<KReSSession> sessions = new HashSet<KReSSession>();
		// Create and open 500 sessions.
		synchronized (sesmgr) {
			for (int i = 0; i < size; i++) {
				KReSSession ses = sesmgr.createSession();
				try {
					ses.open();
				} catch (NonReferenceableSessionException e) {
					fail("Test method tried to open nonreferenceable session.");
				}
				sessions.add(ses);
			}
			// Check that 500 sessions have been created
			assertEquals(initialSize + size, sesmgr.getRegisteredSessionIDs()
					.size());
		}
		boolean open = true;
		for (KReSSession ses : sessions)
			open &= ses.getSessionState() == State.ACTIVE;
		// Check that all created sessions have been opened
		assertTrue(open);
		// Kill 'em all, to quote Metallica
		synchronized (sesmgr) {
			for (KReSSession ses : sessions)
				sesmgr.destroySession(ses.getID());
			assertEquals(initialSize, sesmgr.getRegisteredSessionIDs().size());
		}
		// Check that they are all zombies
		boolean zombi = true;
		for (KReSSession ses : sessions)
			zombi &= ses.getSessionState() == State.ZOMBIE;
		assertTrue(zombi);
		// Try to resurrect them (hopefully failing)
		boolean resurrect = false;
		for (KReSSession ses : sessions)
			try {
				ses.open();
				resurrect |= true;
			} catch (NonReferenceableSessionException e) {
				resurrect |= false;
				continue;
			}
		assertFalse(resurrect);
	}

}
