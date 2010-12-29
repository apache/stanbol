package eu.iksproject.kres.manager.ontology;

import org.semanticweb.owlapi.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.ontology.CoreOntologySpace;
import eu.iksproject.kres.api.manager.ontology.CustomOntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologySpace;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceListener;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.ontology.SessionOntologySpace;
import eu.iksproject.kres.api.manager.ontology.UnmodifiableOntologySpaceException;
import eu.iksproject.kres.api.storage.OntologyStorage;

/**
 * Utility class that generates default implementations of the three types of
 * ontology scope.
 * 
 * @author alessandro
 * 
 */
public class OntologySpaceFactoryImpl implements OntologySpaceFactory {

	protected Logger log = LoggerFactory.getLogger(getClass());

	protected ScopeRegistry registry;
	protected OntologyStorage storage;

	public OntologySpaceFactoryImpl(ScopeRegistry registry, OntologyStorage storage) {
		this.registry = registry;
		this.storage = storage;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory#createCoreOntologySpace(org.semanticweb.owlapi.model.IRI, eu.iksproject.kres.api.manager.ontology.OntologyInputSource)
	 */
	@Override
	public CoreOntologySpace createCoreOntologySpace(IRI scopeID,
			OntologyInputSource coreSource) {
		CoreOntologySpace s = new CoreOntologySpaceImpl(scopeID,storage);
		setupSpace(s, scopeID, coreSource);
		return s;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory#createCustomOntologySpace(org.semanticweb.owlapi.model.IRI, eu.iksproject.kres.api.manager.ontology.OntologyInputSource)
	 */
	@Override
	public CustomOntologySpace createCustomOntologySpace(IRI scopeID,
			OntologyInputSource customSource) {
		CustomOntologySpace s = new CustomOntologySpaceImpl(scopeID,storage);
		setupSpace(s, scopeID, customSource);
		return s;
	}

	/*
	 * (non-Javadoc)
	 * @see eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory#createSessionOntologySpace(org.semanticweb.owlapi.model.IRI)
	 */
	@Override
	public SessionOntologySpace createSessionOntologySpace(IRI scopeID) {
		SessionOntologySpace s = new SessionOntologySpaceImpl(scopeID,storage);
		// s.setUp();
		return s;
	}

	private void setupSpace(OntologySpace s, IRI scopeID,
			OntologyInputSource rootSource) {
		// FIXME: ensure that this is not null
		OntologyScope parentScope = registry.getScope(scopeID);

		if (parentScope != null && parentScope instanceof OntologySpaceListener)
			s.addOntologySpaceListener((OntologySpaceListener) parentScope);
		// Set the supplied ontology's parent as the root for this space.
		try {
			s.setTopOntology(rootSource, true);
		} catch (UnmodifiableOntologySpaceException e) {
			log.error("KReS :: Ontology space " + s.getID()
					+ " found locked at creation time!", e);
		}
		// s.setUp();
	}

}
