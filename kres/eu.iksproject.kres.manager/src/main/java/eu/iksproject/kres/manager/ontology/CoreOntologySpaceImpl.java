package eu.iksproject.kres.manager.ontology;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.manager.util.StringUtils;

public class CoreOntologySpaceImpl extends AbstractOntologySpaceImpl implements
		CoreOntologySpace {

	public static final String SUFFIX = "core";

	public CoreOntologySpaceImpl(IRI scopeID,
			OntologyInputSource topOntology) {
		super(
				IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
						+ SUFFIX), scopeID, topOntology);
	}

	public CoreOntologySpaceImpl(IRI scopeID,
			OntologyInputSource topOntology, OWLOntologyManager ontologyManager) {
		super(
				IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
						+ SUFFIX), scopeID, ontologyManager, topOntology);
	}

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
