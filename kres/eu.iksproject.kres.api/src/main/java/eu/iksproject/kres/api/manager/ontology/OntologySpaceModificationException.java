package eu.iksproject.kres.api.manager.ontology;

/**
 * Thrown whenever an illegal operation that modifies an ontology space is
 * detected and denied.
 * 
 * @author alessandro
 * 
 */
public class OntologySpaceModificationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5147080356192253724L;

	protected OntologySpace space;

	/**
	 * Creates a new instance of OntologySpaceModificationException.
	 * 
	 * @param space
	 *            the ontology space whose modification was attempted.
	 */
	public OntologySpaceModificationException(OntologySpace space) {
		this.space = space;
	}

	/**
	 * Returns the ontology space that threw the exception (presumably after a
	 * failed modification attempt).
	 * 
	 * @return the ontology space on which the exception was thrown.
	 */
	public OntologySpace getSpace() {
		return space;
	}

}
