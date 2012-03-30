package org.apache.stanbol.contenthub.servicesapi.index;

public enum IndexState {
    UNINIT,
    INDEXING,
    ACTIVE,
    REINDEXING;
    
    private static final String prefix = "http://stanbol.apache.org/ontology/contenthub#indexState_";
    
    public String getUri() {
    	return prefix+name().toLowerCase();
	}
    @Override
    public String toString() {
    	return getUri();
    }
}
