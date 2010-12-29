package eu.iksproject.kres.manager.ontology;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.DuplicateIDException;
import eu.iksproject.kres.api.manager.ontology.OntologyInputSource;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeEventListener;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.manager.ONManager;

/**
 * Utility class that instantiates default implementations of ontology scope.
 * 
 * @author alessandro
 * 
 */
public class OntologyScopeFactoryImpl implements OntologyScopeFactory {

	private Set<ScopeEventListener> listeners = new HashSet<ScopeEventListener>();

	protected ScopeRegistry registry;
	protected OntologySpaceFactory spaceFactory;
	
	public OntologyScopeFactoryImpl(ScopeRegistry registry, OntologySpaceFactory spaceFactory) {
		this.registry = registry;
		this.spaceFactory = spaceFactory;
	}
	
	@Override
	public void addScopeEventListener(ScopeEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void clearScopeEventListeners() {
		listeners.clear();
	}

	@Override
	public OntologyScope createOntologyScope(IRI scopeID,
			OntologyInputSource coreSource) throws DuplicateIDException {

		return createOntologyScope(scopeID, coreSource, null);
	}

	@Override
	public OntologyScope createOntologyScope(IRI scopeID,
			OntologyInputSource coreSource, OntologyInputSource customSource)
			throws DuplicateIDException {

		if (registry.containsScope(scopeID))
			throw new DuplicateIDException(scopeID,
					"Scope registry already contains ontology scope with ID "
							+ scopeID);

		OntologyScope scope = new OntologyScopeImpl(scopeID,spaceFactory, coreSource,
				customSource);
		// scope.addOntologyScopeListener(ONManager.get().getOntologyIndex());
		// TODO : manage scopes with null core ontologies
		fireScopeCreated(scope);
		return scope;
	}

	protected void fireScopeCreated(OntologyScope scope) {
		for (ScopeEventListener l : listeners)
			l.scopeCreated(scope);
	}

	@Override
	public Collection<ScopeEventListener> getScopeEventListeners() {
		return listeners;
	}

	@Override
	public void removeScopeEventListener(ScopeEventListener listener) {
		listeners.remove(listener);
	}

}
