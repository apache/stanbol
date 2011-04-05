package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import org.apache.stanbol.ontologymanager.ontonet.api.io.OntologyInputSource;
import org.semanticweb.owlapi.model.IRI;


/**
 * An ontology space factory is responsible for the creation of new, readily
 * specialized ontology spaces from supplied ontology input sources.
 * 
 * Implementations should not call the setup method of the ontology space once
 * it is created, so that it is not locked from editing since creation time.
 * 
 * @author alessandro
 * 
 */
public interface OntologySpaceFactory {

	/**
	 * Creates and sets up a default core ontology space.
	 * 
	 * @param scopeID
	 *            the unique identifier of the ontology scope that will
	 *            reference this space. It can be used for generating the
	 *            identifier for this ontology space.
	 * @param coreSource
	 *            the input source for the ontologies in this space.
	 * @return the generated ontology space.
	 */
    CoreOntologySpace createCoreOntologySpace(IRI scopeID,
            OntologyInputSource coreSource);

	/**
	 * Creates and sets up a default custom ontology space.
	 * 
	 * @param scopeID
	 *            the unique identifier of the ontology scope that will
	 *            reference this space. It can be used for generating the
	 *            identifier for this ontology space.
	 * @param customSource
	 *            the input source for the ontologies in this space.
	 * @return the generated ontology space.
	 */
    CustomOntologySpace createCustomOntologySpace(IRI scopeID,
            OntologyInputSource customSource);

	/**
	 * Creates and sets up a default session ontology space.
	 * 
	 * @param scopeID
	 *            the unique identifier of the ontology scope that will
	 *            reference this space. It can be used for generating the
	 *            identifier for this ontology space.
	 * @return the generated ontology space.
	 */
    SessionOntologySpace createSessionOntologySpace(IRI scopeID);

}
