package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import java.util.List;

import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * Generates new instances of {@link OWLOntologyManager} with optional offline support already enabled for
 * each of them.
 * 
 * @author alessandro
 * 
 */
public interface OWLOntologyManagerFactory {

    /**
     * Returns the list of local IRI mappers that are automatically bound to a newly created
     * {@link OWLOntologyManager} if set to support offline mode. The IRI mappers are typically applied in
     * order, therefore mappers at the end of the list may supersede those at its beginning.
     * 
     * @return the list of local IRI mappers, in the order they are applied.
     */
    List<OWLOntologyIRIMapper> getLocalIRIMapperList();

    /**
     * Creates a new instance of {@link OWLOntologyManager}, with optional offline support.
     * 
     * @param withOfflineSupport
     *            if true, the local IRI mappers obtained by calling {@link #getLocalIRIMapperList()} will be
     *            applied to the new ontology manager.
     * @return a new OWL ontology manager.
     */
    OWLOntologyManager createOntologyManager(boolean withOfflineSupport);
}
