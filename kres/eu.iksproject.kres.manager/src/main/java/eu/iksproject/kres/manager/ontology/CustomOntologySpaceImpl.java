package eu.iksproject.kres.manager.ontology;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;

import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.CustomOntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.manager.ONManager;
import eu.iksproject.kres.manager.io.RootOntologySource;
import eu.iksproject.kres.manager.util.StringUtils;

public class CustomOntologySpaceImpl extends AbstractOntologySpaceImpl
		implements CustomOntologySpace {

	Logger log = ONManager.get().log;

	public static final String SUFFIX = "custom";

	public CustomOntologySpaceImpl(IRI scopeID, OntologyInputSource topOntology) {
		super(
				IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
						+ SUFFIX), scopeID, topOntology);
	}

	public CustomOntologySpaceImpl(IRI scopeID,
			OntologyInputSource topOntology, OWLOntologyManager ontologyManager) {
		super(
				IRI.create(StringUtils.stripIRITerminator(scopeID) + "/"
						+ SUFFIX), scopeID, ontologyManager, topOntology);
	}

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
