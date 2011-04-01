package org.apache.stanbol.reasoners.base.api;

/**
 * 
 * @author andrea.nuzzolese
 *
 */
public class InconcistencyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InconcistencyException(String message) {
		super(message);
	}
	
	public InconcistencyException(Throwable cause) {
		initCause(cause);
	}

}
