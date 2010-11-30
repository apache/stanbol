package eu.iksproject.kres.api.manager.ontology;

import org.semanticweb.owlapi.model.IRI;

import eu.iksproject.kres.api.manager.DuplicateIDException;

/**
 * An ontology scope factory is responsible for the creation of new ontology
 * scopes from supplied ontology input sources for their core and custom spaces.<br>
 * <br>
 * Factory implementations should not call the setup method of the ontology
 * scope once it is created, so that its spaces are not locked from editing
 * since creation time.
 * 
 * @author alessandro
 * 
 */
public interface OntologyScopeFactory extends ScopeEventListenable {

	/**
	 * Creates and returns a new ontology scope with the core space ontologies
	 * obtained from <code>coreSource</code> and the custom space not set.
	 * 
	 * @param scopeID
	 *            the desired unique identifier for the ontology scope.
	 * @param coreSource
	 *            the input source that provides the top ontology for the core
	 *            space.
	 * @return the newly created ontology scope.
	 * @throws DuplicateIDException
	 *             if an ontology scope with the given identifier is already
	 *             <i>registered</i>. The exception is not thrown if another
	 *             scope with the same ID has been created but not registered.
	 */
	public OntologyScope createOntologyScope(IRI scopeID,
			OntologyInputSource coreSource) throws DuplicateIDException;

	/**
	 * Creates and returns a new ontology scope with the core space ontologies
	 * obtained from <code>coreSource</code> and the custom ontologies obtained
	 * from <code>customSource</code>.
	 * 
	 * @param scopeID
	 *            the desired unique identifier for the ontology scope.
	 * @param coreSource
	 *            the input source that provides the top ontology for the core
	 *            space.
	 * @param customSource
	 *            the input source that provides the top ontology for the custom
	 *            space. If null, no custom space should be created at all.
	 * @return the newly created ontology scope.
	 * @throws DuplicateIDException
	 *             if an ontology scope with the given identifier is already
	 *             <i>registered</i>. The exception is not thrown if another
	 *             scope with the same ID has been created but not registered.
	 */
	public OntologyScope createOntologyScope(IRI scopeID,
			OntologyInputSource coreSource, OntologyInputSource customSource)
			throws DuplicateIDException;
}
