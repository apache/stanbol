package org.apache.stanbol.ontologymanager.store.adapter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

public class SimpleContentItem implements ContentItem {

    private String id;

    private MGraph metadata;

    private String mimeType;

    private byte[] content;

    public SimpleContentItem(String id, MGraph metadata, String mimeType, byte[] content) {
        this.id = id;
        this.content = content;
        this.metadata = metadata;
        this.mimeType = mimeType;

    }

    public String getId() {
        return id;
    }

    public MGraph getMetadata() {
        return metadata;
    }

    public String getMimeType() {
        return mimeType;
    }

    public InputStream getStream() {
        return new ByteArrayInputStream(content);
    }

    public static ContentItem create(String id, byte[] content, String contentType) {
        return new SimpleContentItem(id, new SimpleMGraph(), contentType, content);

    }

}
