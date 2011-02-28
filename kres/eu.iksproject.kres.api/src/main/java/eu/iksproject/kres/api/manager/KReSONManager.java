package eu.iksproject.kres.api.manager;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import eu.iksproject.kres.api.manager.ontology.OntologyIndex;
import eu.iksproject.kres.api.manager.ontology.OntologyScopeFactory;
import eu.iksproject.kres.api.manager.ontology.OntologySpaceFactory;
import eu.iksproject.kres.api.manager.ontology.ScopeRegistry;
import eu.iksproject.kres.api.manager.registry.KReSRegistryLoader;
import eu.iksproject.kres.api.manager.session.KReSSessionManager;
import eu.iksproject.kres.api.storage.OntologyStorage;

/**
 * A KReS Ontology Network Manager.
 * 
 * @author andrea.nuzzolese
 * 
 */
public interface KReSONManager {

	/**
	 * Returns the default object that automatically indexes ontologies as they
	 * are loaded within scopes.
	 * 
	 * @return the default ontology index.
	 */
	public OntologyIndex getOntologyIndex();

	/**
	 * Returns the ontology scope factory that was created along with the
	 * manager context.
	 * 
	 * @return the default ontology scope factory
	 */
	public OntologyScopeFactory getOntologyScopeFactory();

	/**
	 * Returns the ontology space factory that was created along with the
	 * manager context.
	 * 
	 * @return the default ontology space factory.
	 */
	public OntologySpaceFactory getOntologySpaceFactory();

	/**
	 * Returns the default ontology storage system for this KReS instance.
	 * 
	 * @return the default ontology store.
	 */
	public OntologyStorage getOntologyStore();

	/**
	 * Returns an OWL Ontology Manager that is never cleared of its ontologies,
	 * so it can be used for caching ontologies without having to reload them
	 * using other managers. It is sufficient to catch
	 * {@link OWLOntologyAlreadyExistsException}s and obtain the ontology with
	 * that same ID from this manager.
	 * 
	 * @return the OWL Ontology Manager used for caching ontologies.
	 */
	public OWLOntologyManager getOwlCacheManager();

	/**
	 * Returns a factory object that can be used for obtaining OWL API objects.
	 * 
	 * @return the default OWL data factory
	 */
	public OWLDataFactory getOwlFactory();

	/**
	 * Returns the default ontology registry loader.
	 * 
	 * @return the default ontology registry loader.
	 */
	public KReSRegistryLoader getRegistryLoader();

	/**
	 * Returns the unique ontology scope registry for this context.
	 * 
	 * @return the ontology scope registry.
	 */
	public ScopeRegistry getScopeRegistry();

	/**
	 * Returns the unique KReS session manager for this context.
	 * 
	 * @return the KreS session manager.
	 */
	public KReSSessionManager getSessionManager();

	/**
	 * Returns the list of IRIs that identify scopes that should be activated on
	 * startup, <i>if they exist</i>.
	 * 
	 * @return the list of scope IDs to activate.
	 */
	public String[] getUrisToActivate();
	
	/**
	 * Returns the String that represent the namespace used by KReS for its ontologies
	 * 
	 * @return the namespace of KReS.
	 */
	public String getKReSNamespace();
}
