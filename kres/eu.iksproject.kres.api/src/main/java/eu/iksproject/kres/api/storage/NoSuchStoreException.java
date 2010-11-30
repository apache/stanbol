package eu.iksproject.kres.api.storage;

public class NoSuchStoreException extends Exception {

	private String message;
	
	public NoSuchStoreException(String message) {
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
}
