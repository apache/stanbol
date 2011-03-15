package eu.iksproject.kres.manager.ontology;

import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.store.api.OntologyStorage;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.manager.util.StringUtils;

public class CoreOntologySpaceImpl extends AbstractOntologySpaceImpl implements
		CoreOntologySpace {

	public static final String SUFFIX = SpaceType.CORE.getIRISuffix();
//	static {
//		SUFFIX = SpaceType.CORE.getIRISuffix();
//	}
	
	public CoreOntologySpaceImpl(IRI scopeID, OntologyStorage storage) {
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.CORE.getIRISuffix()), SpaceType.CORE/*, scopeID*/,storage);
	}

	public CoreOntologySpaceImpl(IRI scopeID, OntologyStorage storage,
			OWLOntologyManager ontologyManager) {
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.CORE.getIRISuffix()), SpaceType.CORE, /*scopeID,*/storage,
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
