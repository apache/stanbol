package eu.iksproject.kres.manager.ontology;

import java.util.Random;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.ontology.SpaceType;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.manager.io.RootOntologySource;
import eu.iksproject.kres.manager.util.StringUtils;

public class SessionOntologySpaceImpl extends AbstractOntologySpaceImpl
		implements SessionOntologySpace {

	
	public static final String SUFFIX = SpaceType.SESSION.getIRISuffix();
//	static {
//		SUFFIX = SpaceType.SESSION.getIRISuffix();
//	}
	
	public SessionOntologySpaceImpl(IRI scopeID, OntologyStorage storage) {
		// FIXME : sync session id with session space ID
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.SESSION.getIRISuffix() + "-"
				+ new Random().nextLong()), SpaceType.SESSION,storage/*, scopeID*/);

		IRI iri = IRI.create(StringUtils.stripIRITerminator(getID())
				+ "/root.owl");
		try {
			setTopOntology(new RootOntologySource(ontologyManager
					.createOntology(iri), null), false);
		} catch (OWLOntologyCreationException e) {
			log.error("KReS :: Could not create session space root ontology "
					+ iri, e);
		} catch (UnmodifiableOntologySpaceException e) {
			// Should not happen...
			log
					.error(
							"KReS :: Session space ontology "
									+ iri
									+ " was denied modification by the space itself. This should not happen.",
							e);
		}
	}

	public SessionOntologySpaceImpl(IRI scopeID, OntologyStorage storage,
			OWLOntologyManager ontologyManager) {
		// FIXME : sync session id with session space ID
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.SESSION.getIRISuffix() + "-"
				+ new Random().nextLong()), SpaceType.SESSION,storage, /*scopeID,*/ ontologyManager);
		Logger log = LoggerFactory.getLogger(getClass());
		IRI iri = IRI.create(StringUtils.stripIRITerminator(getID())
				+ "/root.owl");
		try {
			setTopOntology(new RootOntologySource(ontologyManager
					.createOntology(iri), null), false);
		} catch (OWLOntologyCreationException e) {
			log.error("KReS :: Could not create session space root ontology "
					+ iri, e);
		} catch (UnmodifiableOntologySpaceException e) {
			// Should not happen...
			log
					.error(
							"KReS :: Session space ontology "
									+ iri
									+ " was denied modification by the space itself. This should not happen.",
							e);
		}
	}

	/**
	 * Session spaces expose their ontology managers.
	 */
	@Override
	public OWLOntologyManager getOntologyManager() {
		return ontologyManager;
	}

	/**
	 * Once it is set up, a session space is write-enabled.
	 */
	@Override
	public synchronized void setUp() {
		locked = false;
	}

	@Override
	public synchronized void tearDown() {
		// TODO Auto-generated method stub
	}

}
