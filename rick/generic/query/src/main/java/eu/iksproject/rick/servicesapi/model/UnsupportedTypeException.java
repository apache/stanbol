package eu.iksproject.rick.servicesapi.model;

public class UnsupportedTypeException extends RuntimeException {

	/**
	 * uses the default serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	protected UnsupportedTypeException(Class<?> type) {
		super("Type "+type+" is not supported");
	}

	protected UnsupportedTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	protected UnsupportedTypeException(String message) {
		super(message);
	}

	protected UnsupportedTypeException(Class<?> type,Throwable cause) {
		super("Type "+type+" is not supported",cause);
	}

}
