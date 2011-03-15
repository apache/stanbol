package org.apache.stanbol.ontologymanager.ontonet.api;

import org.semanticweb.owlapi.model.IRI;

/**
 * Indicates an attempt to illegally create a resource by assigning it an IRI
 * that already identifies another known resource. This exception typically
 * results in the new resource not being created at all.<br>
 * <br>
 * This exception can be subclassed for managing specific resource type-based
 * behaviour (e.g. scopes, spaces or sessions).
 * 
 * @author alessandro
 * 
 */
public class DuplicateIDException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 802959772682427494L;

	protected IRI dupe;

	/**
	 * Returns the IRI that identifies the existing resource. This can be use to
	 * obtain the resource itself by passing it onto appropriate managers.
	 * 
	 * @return the duplicate identifier
	 */
	public IRI getDulicateID() {
		return dupe;
	}

	/**
	 * Creates a new instance of DuplicateIDException.
	 * 
	 * @param dupe
	 *            the duplicate ID.
	 */
	public DuplicateIDException(IRI dupe) {
		this.dupe = dupe;
	}

	/**
	 * Creates a new instance of DuplicateIDException.
	 * 
	 * @param dupe
	 *            the duplicate ID.
	 * @param message
	 *            the detail message.
	 */
	public DuplicateIDException(IRI dupe, String message) {
		super(message);
		this.dupe = dupe;
	}

	/**
	 * Creates a new instance of DuplicateIDException.
	 * 
	 * @param dupe
	 *            the duplicate ID.
	 * @param cause
	 *            the throwable that caused this exception to be thrown.
	 */
	public DuplicateIDException(IRI dupe, Throwable cause) {
		this(dupe);
		initCause(cause);
	}

}
