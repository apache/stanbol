package org.apache.stanbol.contenthub.search.featured;

import org.apache.stanbol.contenthub.servicesapi.search.featured.ResultantDocument;
import org.apache.stanbol.contenthub.store.solr.util.ContentItemIDOrganizer;

public class ResultantDocumentImpl implements ResultantDocument {

    private String id;
    private String dereferencableURI;
    private String mimetype;
    private long enhancementCount;
    private String title;

    public ResultantDocumentImpl(String uri,
                                 String mimeType,
                                 long enhancementCount,
                                 String title) {
        this.id = ContentItemIDOrganizer.detachBaseURI(uri);
        this.mimetype = mimeType;
        this.title = (title == null || title.trim().equals("") ? id : title);
        this.enhancementCount = enhancementCount;
    }

    public ResultantDocumentImpl(String uri,
                                 String dereferencableURI,
                                 String mimeType,
                                 long enhancementCount,
                                 String title) {
        this.id = ContentItemIDOrganizer.detachBaseURI(uri);
        this.dereferencableURI = dereferencableURI;
        this.mimetype = mimeType;
        this.title = (title == null || title.trim().equals("") ? id : title);
        this.enhancementCount = enhancementCount;
    }
    
    @Override
    public String getLocalId() {
        return this.id;
    }
    
    @Override
    public String getDereferencableURI() {
        return this.dereferencableURI;
    }

    @Override
    public String getMimetype() {
        return this.mimetype;
    }

    @Override
    public long getEnhancementCount() {
        return this.enhancementCount;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDereferencableURI(String dereferencableURI) {
        this.dereferencableURI = dereferencableURI;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    public void setEnhancements(long enhancementCount) {
        this.enhancementCount = enhancementCount;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    
}
