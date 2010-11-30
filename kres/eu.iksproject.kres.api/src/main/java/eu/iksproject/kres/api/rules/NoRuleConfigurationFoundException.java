package eu.iksproject.kres.api.rules;

public class NoRuleConfigurationFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String file;
	
	public NoRuleConfigurationFoundException(String file) {
		this.file = file;
	}
	
	public String getFile() {
		return file;
	}

}
