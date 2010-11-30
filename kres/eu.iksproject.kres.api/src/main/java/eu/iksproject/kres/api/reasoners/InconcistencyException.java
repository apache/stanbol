package eu.iksproject.kres.api.reasoners;

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
