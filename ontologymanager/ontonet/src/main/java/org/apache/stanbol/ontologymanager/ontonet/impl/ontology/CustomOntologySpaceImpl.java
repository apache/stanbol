package org.apache.stanbol.ontologymanager.ontonet.impl.ontology;

import org.apache.stanbol.ontologymanager.ontonet.api.io.RootOntologySource;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CoreOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.CustomOntologySpace;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.SpaceType;
import org.apache.stanbol.ontologymanager.ontonet.api.ontology.UnmodifiableOntologySpaceException;
import org.apache.stanbol.ontologymanager.ontonet.impl.io.ClerezzaOntologyStorage;
import org.apache.stanbol.ontologymanager.ontonet.impl.util.StringUtils;
import org.apache.stanbol.ontologymanager.store.api.PersistenceStore;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class CustomOntologySpaceImpl extends AbstractOntologySpaceImpl
		implements CustomOntologySpace {

	public static final String SUFFIX = SpaceType.CUSTOM.getIRISuffix();
//	static {
//		SUFFIX = SpaceType.CUSTOM.getIRISuffix();
//	}
	
	public CustomOntologySpaceImpl(IRI scopeID, ClerezzaOntologyStorage storage) {
		super(IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
				+ SpaceType.CUSTOM.getIRISuffix()), SpaceType.CUSTOM/*, scopeID*/, storage
				);
	}

	public CustomOntologySpaceImpl(IRI scopeID, ClerezzaOntologyStorage storage,
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
