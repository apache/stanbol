package org.apache.stanbol.ontologymanager.ontonet.api.io;

import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * An ontology input source provides a point for loading an ontology. Currently it provides two ways of
 * obtaining an ontology document:
 * 
 * <ol>
 * <li>From an OWLOntology.
 * <li>By dereferencing an physical IRI.
 * </ol>
 * 
 * Consumers that use an ontology input source will attempt to obtain a concrete representation of an ontology
 * in the above order. Implementations of this interface may try to dereference the IRI internally and just
 * provide the OWLOntology, or directly provide the physical IRI for other classes to dereference.
 * Implementations should allow multiple attempts at loading an ontology.
 * 
 * @author alessandro
 * 
 */
public interface OntologyInputSource {

    /**
     * Gets the ontology network resulting from the transitive closure of import statements on the root
     * ontology. Useful for implementations with a custom management of ontology loading.
     * 
     * @return the import closure of the root ontology.
     */
    Set<OWLOntology> getClosure();

    /**
     * Returns the IRI by dereferencing which it should be possible to obtain the ontology. This method is
     * supposed to return null if the ontology lives in-memory and was not or is not going to be stored
     * publicly.
     * 
     * @return the physical location for this ontology source, or null if unknown.
     */
    IRI getPhysicalIRI();

    /**
     * Returns the OWL Ontology that imports the whole ontology network addressed by this input source.
     * 
     * @return the ontology network root.
     */
    OWLOntology getRootOntology();

    /**
     * Determines if a physical IRI is known for this ontology source. Note that an anonymous ontology may
     * have been fetched from a physical location, just as a named ontology may have been stored in memory and
     * have no physical location.
     * 
     * @return true if a physical location is known for this ontology source.
     */
    boolean hasPhysicalIRI();

    /**
     * Determines if a root ontology that imports the entire network is available.
     * 
     * @return true if a root ontology is available, false otherwise.
     */
    boolean hasRootOntology();

}
