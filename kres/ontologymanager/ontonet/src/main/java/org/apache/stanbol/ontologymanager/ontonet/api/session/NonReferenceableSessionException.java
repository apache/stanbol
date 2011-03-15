package org.apache.stanbol.ontologymanager.ontonet.api.session;

/**
 * Thrown whenever an attempt to access a KReS session that is bound for removal
 * is detected.
 * 
 * @author alessandro
 * 
 */
public class NonReferenceableSessionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1642512088759774124L;

	public NonReferenceableSessionException() {

	}

	public NonReferenceableSessionException(String message) {
		super(message);
	}

	public NonReferenceableSessionException(Throwable cause) {
		initCause(cause);
	}

}
