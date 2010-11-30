package eu.iksproject.kres.api.manager.ontology;

/**
 * Thrown whenever an attempt to modify the ontology network within a read-only
 * ontology space (e.g. a core or custom space in a bootstrapped system) is
 * detected and denied.
 * 
 * @author alessandro
 * 
 */
public class UnmodifiableOntologySpaceException extends
		OntologySpaceModificationException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6747720213098173405L;

	/**
	 * Creates a new instance of UnmodifiableOntologySpaceException.
	 * 
	 * @param space
	 *            the ontology space whose modification was attempted.
	 */
	public UnmodifiableOntologySpaceException(OntologySpace space) {
		super(space);
	}

}
