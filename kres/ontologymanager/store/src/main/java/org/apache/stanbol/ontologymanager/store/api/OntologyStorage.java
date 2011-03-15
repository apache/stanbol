package org.apache.stanbol.ontologymanager.store.api;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * An interface providing the basic, implementation-independent storage
 * functions for OWL ontologies.
 * 
 * @author alessandro
 * 
 */
public interface OntologyStorage {

	/**
	 * Clears the ontology storage system of all it content.
	 */
	public void clear();

	/**
	 * Removes every occurrence of the ontology identified by {@code ontologyId}
	 * from the storage system.
	 * 
	 * @param ontologyId
	 *            the IRI that identifies the ontology to be deleted.
	 */
	public void delete(IRI ontologyId);

	/**
	 * Removes every occurrence of the ontologies identified by {@code
	 * ontologyIds} from the storage system.
	 * 
	 * @param ontologyId
	 *            the IRIs that identify the ontologies to be deleted.
	 */
	public void deleteAll(Set<IRI> ontologyIds);

	/**
	 * Obtains an {@code OWLOntology} representation of the ontology logically
	 * identified by {@code ontologyId}. How the ontology is fetched is at the
	 * discretion of implementations, which may arbitrarily try to physically
	 * dereference the IRI or use it as an identifier to retrieve the ontlology
	 * from a triplestore.
	 * 
	 * @param ontologyId
	 *            the <i<logical</i> identifier of the ontology.
	 * @return an {@code OWLOntology} representation of the ontology.
	 */
	public OWLOntology load(IRI ontologyId);

	/**
	 * Saves the {@code OWLOntology} object to a persistence space.
	 * 
	 * @param o
	 *            the ontology to be stored.
	 */
	public void store(OWLOntology o);
	
	public void store(OWLOntology o, IRI ontologyID);
	
	public OWLOntology sparqlConstruct(String sparql, String datasetURI);
	
	public Set<IRI> listGraphs();
	
	public OWLOntology getGraph(IRI ontologyID) throws NoSuchStoreException;
	
	
}
