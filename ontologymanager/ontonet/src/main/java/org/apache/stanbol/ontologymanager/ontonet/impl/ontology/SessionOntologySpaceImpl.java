package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import java.util.Random;

import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.OntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SessionOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.StringUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SessionOntologySpaceImpl extends AbstractOntologySpaceImpl
		implements SessionOntologySpace {

	
	public static final String SUFFIX = SpaceType.SESSION.getIRISuffix();
//	static {
//		SUFFIX = SpaceType.SESSION.getIRISuffix();
//	}
	
	public SessionOntologySpaceImpl(IRI scopeID, ClerezzaOntologyStorage store) {
		// FIXME : sync session id with session space ID
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.SESSION.getIRISuffix() + "-"
				+ new Random().nextLong()), SpaceType.SESSION, store/*, scopeID*/);

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

	public SessionOntologySpaceImpl(IRI scopeID, ClerezzaOntologyStorage store,
			OWLOntologyManager ontologyManager) {
		
		// FIXME : sync session id with session space ID
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.SESSION.getIRISuffix() + "-"
				+ new Random().nextLong()), SpaceType.SESSION, store, /*scopeID,*/ ontologyManager);
		
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

    @Override
    public void attachSpace(OntologySpace space, boolean skipRoot) throws UnmodifiableOntologySpaceException {
        if (!(space instanceof SessionOntologySpace)) {
        OWLOntology o = space.getTopOntology();
        // This does the append thingy
        log.debug("Attaching " + o + " TO " + getTopOntology() + " ...");
        try {
            // It is in fact the addition of the core space top ontology to the
            // custom space, with import statements and all.
            addOntology(new RootOntologySource(o, null));
            // log.debug("ok");
        } catch (Exception ex) {
            log.error("FAILED", ex);
        }
        }
    }

}
