package org.apache.stanbol.contenthub.servicesapi.search.featured;

public interface ResultantDocument {
    
    String getLocalId();

    String getDereferencableURI();

    String getMimetype();

    long getEnhancementCount();

    String getTitle();
}
