package org.apache.stanbol.enhancer.interaction.event;

public class UploadFileEvent extends Event {

    private final String filename;
    private final String uri;

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
