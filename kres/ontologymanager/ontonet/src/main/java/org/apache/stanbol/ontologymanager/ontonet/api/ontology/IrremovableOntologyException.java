package org.apache.stanbol.ontologymanager.ontonet.api.ontology;

import org.semanticweb.owlapi.model.IRI;

/**
 * Thrown whenever an illegal attempt at removing an ontology from an ontology
 * space is detected. This can happen e.g. if the ontology is the space root or
 * not a direct child thereof.
 * 
 * @author alessandro
 * 
 */
public class IrremovableOntologyException extends
		OntologySpaceModificationException {

	protected IRI ontologyId;

	/**
	 * 
	 */
	private static final long serialVersionUID = -3301398666369788964L;

	/**
	 * Constructs a new instance of <code>IrremovableOntologyException</code>.
	 * 
	 * @param space
	 *            the space that holds the ontology.
	 * @param ontologyId
	 *            the logical IRI of the ontology whose removal was denied.
	 */
	public IrremovableOntologyException(OntologySpace space, IRI ontologyId) {
		super(space);
		this.ontologyId = ontologyId;
	}

	/**
	 * Returns the unique identifier of the ontology whose removal was denied.
	 * 
	 * @return the ID of the ontology that was not removed.
	 */
	public IRI getOntologyId() {
		return ontologyId;
	}

}
