package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Provides an interface to the ontologies provided by registered scopes in the
 * ontology manager.
 * 
 * @author alessandro
 * 
 */
public interface OntologyIndex extends ScopeOntologyListener,
		ScopeEventListener {

	/**
	 * Returns an ontology having the specified IRI as its identifier, or null
	 * if no such ontology is indexed.<br>
	 * <br>
	 * Which ontology is returned in case more ontologies with this IRI are
	 * registered in different scopes is at the discretion of implementors.
	 * 
	 * @param ontologyIri
	 * @return
	 */
	public OWLOntology getOntology(IRI ontologyIri);

	/**
	 * Returns the ontology loaded within an ontology scope having the specified
	 * IRI as its identifier, or null if no such ontology is loaded in that
	 * scope.
	 * 
	 * @param ontologyIri
	 * @return
	 */
	public OWLOntology getOntology(IRI ontologyIri, IRI scopeId);

	/**
	 * Returns the set of ontology scopes where an ontology with the specified
	 * IRI is registered in either their core spaces or their custom spaces.
	 * Optionally, session spaces can be queried as well.
	 * 
	 * @param ontologyIri
	 * @param includingSessionSpaces
	 * @return
	 */
	public Set<IRI> getReferencingScopes(IRI ontologyIri,
			boolean includingSessionSpaces);

	/**
	 * Determines if an ontology with the specified identifier is loaded within
	 * some registered ontology scope.
	 * 
	 * @param ontologyIri
	 * @return
	 */
	public boolean isOntologyLoaded(IRI ontologyIri);

}
