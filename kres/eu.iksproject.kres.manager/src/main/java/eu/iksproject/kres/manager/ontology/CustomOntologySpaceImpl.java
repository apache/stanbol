package eu.iksproject.kres.manager.ontology;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.CustomOntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.SpaceType;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.api.storage.OntologyStorage;
import eu.iksproject.kres.manager.io.RootOntologySource;
import eu.iksproject.kres.manager.util.StringUtils;

public class CustomOntologySpaceImpl extends AbstractOntologySpaceImpl
		implements CustomOntologySpace {

	public static final String SUFFIX = SpaceType.CUSTOM.getIRISuffix();
//	static {
//		SUFFIX = SpaceType.CUSTOM.getIRISuffix();
//	}
	
	public CustomOntologySpaceImpl(IRI scopeID, OntologyStorage storage) {
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.CUSTOM.getIRISuffix()), SpaceType.CUSTOM/*, scopeID*/,storage
				);
	}

	public CustomOntologySpaceImpl(IRI scopeID, OntologyStorage storage,
			 OWLOntologyManager ontologyManager) {
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.CUSTOM.getIRISuffix()), SpaceType.CUSTOM, storage, /*scopeID,*/
				ontologyManager);
	}
	
//	public CustomOntologySpaceImpl(IRI scopeID, OntologyInputSource topOntology) {
//	super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
//			+ SpaceType.CUSTOM.getIRISuffix()), SpaceType.CUSTOM, scopeID,
//			topOntology);
//}
//
//public CustomOntologySpaceImpl(IRI scopeID,
//		OntologyInputSource topOntology, OWLOntologyManager ontologyManager) {
//	super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
//			+ SpaceType.CUSTOM.getIRISuffix()), SpaceType.CUSTOM, scopeID,
//			ontologyManager, topOntology);
//}

	@Override
	public void attachCoreSpace(CoreOntologySpace coreSpace, boolean skipRoot)
			throws UnmodifiableOntologySpaceException {

		OWLOntology o = coreSpace.getTopOntology();
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

	/**
	 * Once it is set up, a custom space is write-locked.
	 */
	@Override
	public synchronized void setUp() {
		locked = true;
	}

	@Override
	public synchronized void tearDown() {
		locked = false;
	}

}
