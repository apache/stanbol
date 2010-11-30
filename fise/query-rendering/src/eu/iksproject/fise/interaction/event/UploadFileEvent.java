package eu.iksproject.fise.interaction.event;

public class UploadFileEvent extends Event {
	
	private String filename;
	private String uri;
	
	public UploadFileEvent (String filename, String uri) {
		this.filename = filename;
		this.uri = uri;
	}
	
	public String getFilename() {
		return filename;
	}

	public String getUri() {
		return uri;
	}

}
