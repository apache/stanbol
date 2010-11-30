package eu.iksproject.kres.api.manager.ontology;

import org.semanticweb.owlapi.model.IRI;

/**
 * Thrown whenever an attempt to modify an ontology within an ontology space
 * that does not contain it is detected.
 * 
 * @author alessandro
 * 
 */
public class MissingOntologyException extends
		OntologySpaceModificationException {

	public MissingOntologyException(OntologySpace space, IRI ontologyId) {
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

	protected IRI ontologyId;

	/**
	 * 
	 */
	private static final long serialVersionUID = -3449667155191079302L;

}
