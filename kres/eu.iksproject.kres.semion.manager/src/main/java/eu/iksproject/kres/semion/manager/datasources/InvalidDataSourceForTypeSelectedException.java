package eu.iksproject.kres.semion.manager.datasources;

public class InvalidDataSourceForTypeSelectedException extends Exception {

	private Object source;
	
	public InvalidDataSourceForTypeSelectedException(Object source) {
		this.source = source;
	}
	
	public Object getSource() {
		return source;
	}
}
