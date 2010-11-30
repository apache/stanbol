package eu.iksproject.kres.manager.ontology;

import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.CustomOntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;

/**
 * Utility class that generates default implementations of the three types of
 * ontology scope.
 * 
 * @author alessandro
 * 
 */
public class OntologySpaceFactoryImpl implements OntologySpaceFactory {

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory#createCoreOntologySpace(org.semanticweb.owlapi.model.IRI, eu.iksproject.kres.api.manager.ontology.OntologyInputSource)
	 */
	@Override
	public CoreOntologySpace createCoreOntologySpace(IRI scopeID,
			OntologyInputSource coreSource) {
		CoreOntologySpace s = new CoreOntologySpaceImpl(scopeID, coreSource);
		// s.setUp();
		return s;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory#createCustomOntologySpace(org.semanticweb.owlapi.model.IRI, eu.iksproject.kres.api.manager.ontology.OntologyInputSource)
	 */
	@Override
	public CustomOntologySpace createCustomOntologySpace(IRI scopeID,
			OntologyInputSource customSource) {
		CustomOntologySpace s = new CustomOntologySpaceImpl(scopeID , customSource);
		// s.setUp();
		return s;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory#createSessionOntologySpace(org.semanticweb.owlapi.model.IRI)
	 */
	@Override
	public SessionOntologySpace createSessionOntologySpace(IRI scopeID) {
		SessionOntologySpace s = new SessionOntologySpaceImpl(scopeID);
		// s.setUp();
		return s;
	}

}
