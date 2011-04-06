package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.StringUtils;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class CoreOntologySpaceImpl extends AbstractOntologySpaceImpl implements
		CoreOntologySpace {

	public static final String SUFFIX = SpaceType.CORE.getIRISuffix();
//	static {
//		SUFFIX = SpaceType.CORE.getIRISuffix();
//	}
	
	public CoreOntologySpaceImpl(IRI scopeID, PersistenceStore persistenceStore) {
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.CORE.getIRISuffix()), SpaceType.CORE/*, scopeID*/, persistenceStore);
	}

	public CoreOntologySpaceImpl(IRI scopeID, PersistenceStore persistenceStore,
			OWLOntologyManager ontologyManager) {
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.CORE.getIRISuffix()), SpaceType.CORE, /*scopeID,*/ persistenceStore,
				ontologyManager);
	}

//	public CoreOntologySpaceImpl(IRI scopeID, OntologyInputSource topOntology) {
//		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
//				+ SpaceType.CORE.getIRISuffix()), SpaceType.CORE, scopeID,
//				topOntology);
//	}
//
//	public CoreOntologySpaceImpl(IRI scopeID, OntologyInputSource topOntology,
//			OWLOntologyManager ontologyManager) {
//		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
//				+ SpaceType.CORE.getIRISuffix()), SpaceType.CORE, scopeID,
//				ontologyManager, topOntology);
//	}

	/**
	 * When set up, a core space is write-locked.
	 */
	@Override
	public synchronized void setUp() {
		locked = true;
	}

	/**
	 * When torn down, a core space releases its write-lock.
	 */
	@Override
	public synchronized void tearDown() {
		locked = false;
	}

}
