package eu.iksproject.kres.api.semion.util;

public class UnsupportedReengineerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String reenginner;
	
	public UnsupportedReengineerException(String reenginner) {
		this.reenginner = reenginner;
	}

	public String getReenginner() {
		return reenginner;
	}
	
}
