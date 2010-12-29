package eu.iksproject.kres.manager.ontology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.iksproject.kres.api.manager.KReSONManager;
import eu.iksproject.kres.api.manager.ontology.NoSuchScopeException;
import eu.iksproject.kres.api.manager.ontology.OntologyIndex;
import eu.iksproject.kres.api.manager.ontology.OntologyScope;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.manager.ONManager;

public class OntologyIndexImpl implements OntologyIndex {

	private final Logger log = LoggerFactory.getLogger(getClass());
	/*
	 * We only use IRIs, so the actual scopes can get garbage-collected once
	 * they are deregistered.
	 */
	private Map<IRI, Set<IRI>> ontScopeMap;

	private ScopeRegistry scopeRegistry;

	private KReSONManager onm;

	public OntologyIndexImpl(KReSONManager onm) {

		ontScopeMap = new HashMap<IRI, Set<IRI>>();
		if (onm == null)
			this.scopeRegistry = new ScopeRegistryImpl();
		else {
			this.onm=onm;
			this.scopeRegistry = onm.getScopeRegistry();
		}
		this.scopeRegistry.addScopeRegistrationListener(this);
	}

	/**
	 * Given a scope, puts its ontologies in its scopeMap
	 * 
	 * @param scope
	 */
	private void addScopeOntologies(OntologyScope scope) {
		for (OWLOntology o : getOntologiesForScope(scope)) {
			IRI ontid = o.getOntologyID().getOntologyIRI();
			Set<IRI> scopez = ontScopeMap.get(ontid);
			if (scopez == null)
				scopez = new HashSet<IRI>();
			scopez.add(scope.getID());
			ontScopeMap.put(ontid, scopez);
		}
	}

	private Set<OWLOntology> getOntologiesForScope(OntologyScope scope) {
		Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
		try {
			// ontologies.add(scope.getCoreSpace().getTopOntology());
			ontologies.addAll(scope.getCoreSpace().getOntologies());
		} catch (Exception ex) {
		}
		try {
			ontologies.addAll(scope.getCustomSpace().getOntologies());
		} catch (Exception ex) {
		}
		// for (OWLOntology o : ontologies) {
		// System.out.println(o.getOntologyID());
		// for (OWLImportsDeclaration im: o.getImportsDeclarations())
		// System.out.println("\t"+im);
		// }
		return ontologies;
	}

	@Override
	public OWLOntology getOntology(IRI ontologyIri) {
		Set<IRI> scopez = ontScopeMap.get(ontologyIri);
		if (scopez == null || scopez.isEmpty())
			return null;
		OWLOntology ont = null;
		OntologyScope scope = scopeRegistry.getScope(scopez.iterator().next());
		try {
			ont = scope.getCustomSpace().getOntology(ontologyIri);
			if (ont != null)
				return ont;
		} catch (Exception ex) {
		}
		try {
			ont = scope.getCoreSpace().getOntology(ontologyIri);
			if (ont != null)
				return ont;
		} catch (Exception ex) {
		}
		return ont;
	}

	@Override
	public OWLOntology getOntology(IRI ontologyIri, IRI scopeId) {
		OWLOntology ont = null;
		OntologyScope scope = scopeRegistry.getScope(scopeId);
		try {
			ont = scope.getCustomSpace().getOntology(ontologyIri);
			if (ont != null)
				return ont;
		} catch (Exception ex) {
		}
		try {
			ont = scope.getCoreSpace().getOntology(ontologyIri);
			if (ont != null)
				return ont;
		} catch (Exception ex) {
		}
		return ont;
	}

	@Override
	public Set<IRI> getReferencingScopes(IRI ontologyIRI,
			boolean includingSessionSpaces) {
		return ontScopeMap.get(ontologyIRI);
	}

	@Override
	public boolean isOntologyLoaded(IRI ontologyIRI) {
		Set<IRI> scopez = ontScopeMap.get(ontologyIRI);
		return scopez != null && !scopez.isEmpty();
	}

	@Override
	public void onOntologyAdded(IRI scopeId, IRI addedOntology) {
		log.debug("Ontology " + addedOntology + " added to scope "
				+ scopeId);
		Set<IRI> scopez = ontScopeMap.get(addedOntology);
		if (scopez == null)
			scopez = new HashSet<IRI>();
		scopez.add(scopeId);
		ontScopeMap.put(addedOntology, scopez);
		Set<IRI> scopez2 = ontScopeMap.get(addedOntology);
		if (!scopez2.contains(scopeId))
			log.warn("Addition was not reindexed!");
	}

	@Override
	public void onOntologyRemoved(IRI scopeId, IRI removedOntology) {
		log.debug("Removing ontology " + removedOntology
				+ " from scope " + scopeId);
		Set<IRI> scopez = ontScopeMap.get(removedOntology);
		if(scopez != null){
			if (scopez.contains(scopeId))
				scopez.remove(scopeId);
			else{
//				System.out.println("...but it was not indexed!");
				log.warn("The scope "+scopeId+" is not indexed");
			}
			Set<IRI> scopez2 = ontScopeMap.get(removedOntology);
			if (scopez2.contains(scopeId))
				/**
				 * FIXME
				 * This message is obscure
				 */
				log.warn("Removal was not reindexed!");
		}
	}

	private void removeScopeOntologies(OntologyScope scope) {
		log.debug("Removing all ontologies from Scope " + scope);
		for (OWLOntology o : getOntologiesForScope(scope)) {
			IRI ontid = o.getOntologyID().getOntologyIRI();
			Set<IRI> scopez = ontScopeMap.get(ontid);
			if (scopez != null) {
				scopez.remove(scope.getID());
				if (scopez.isEmpty())
					ontScopeMap.remove(ontid);
			}
		}
	}

	@Override
	public void scopeActivated(OntologyScope scope) {
		log.info("Scope " + scope.getID()
				+ " activated.");
		scope.removeOntologyScopeListener(this);
	}

	@Override
	public void scopeCreated(OntologyScope scope) {
		// scope.addOntologyScopeListener(this);
		this.scopeDeactivated(scope);
	}

	@Override
	public void scopeDeactivated(OntologyScope scope) {
		// Scope has been deactivated but not due to deregistration
		// if (scopeRegistry.containsScope(scope.getID())) {
		scope.addOntologyScopeListener(this);
		log.debug("Adding index as listener for scope "
				+ scope.getID());
		// }
		log.info("Scope " + scope.getID() + " deactivated.");
	}

	@Override
	public void scopeDeregistered(OntologyScope scope) {
		log.info("Scope " + scope.getID()
				+ " deregistered.");
		this.scopeDeactivated(scope);
		removeScopeOntologies(scope);
	}

	@Override
	public void scopeRegistered(OntologyScope scope) {
		log.info("Scope " + scope.getID()
				+ " registered. Now you can check for its activation status.");

		Set<OntologyScope> scopez = scopeRegistry.getRegisteredScopes();
		for (String token : onm.getUrisToActivate()) {
			try {
				IRI scopeId = IRI.create(token.trim());
				scopeRegistry.setScopeActive(scopeId, true);
				scopez.remove(scopeRegistry.getScope(scopeId));
				log.info("KReS :: Ontology scope " + scopeId + " "
						+ " activated.");
			} catch (NoSuchScopeException ex) {
				log.warn("KReS :: Tried to activate unavailable scope "
								+ token + ".");
			} catch (Exception ex) {
				log.error("Exception caught while activating scope "
						+ token + " " + ex.getClass());
				continue;
			}
		}
		// Stop deactivating other scopes
//		for (OntologyScope scopp : scopez) {
//			IRI scopeId = scopp.getID();
//			try {
//				if (scopeRegistry.isScopeActive(scopeId)) {
//					scopeRegistry.setScopeActive(scopeId, false);
//					System.out.println("KReS :: Ontology scope " + scopeId
//							+ " " + " deactivated.");
//				}
//			} catch (NoSuchScopeException ex) {
//				// Shouldn't happen because we already have the scope handle,
//				// however exceptions could be thrown erroneously...
//				System.err
//						.println("KReS :: Tried to deactivate unavailable scope "
//								+ scopeId + ".");
//			} catch (Exception ex) {
//				System.err.println("Exception caught while deactivating scope "
//						+ scope.getID() + " " + ex.getClass());
//				continue;
//			}
//		}

		addScopeOntologies(scope);

	}

}
