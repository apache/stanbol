package org.apache.stanbol.contenthub.servicesapi.index;

public enum EndpointType {
    SOLR
    ;
    
    private static final String prefix = "http://stanbol.apache.org/ontology/contenthub#endpointType_";
    
    public String getUri() {
    	return prefix+name().toLowerCase();
	}

    @Override
    public String toString() {
    	return getUri();
    }

}
