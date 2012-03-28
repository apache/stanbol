package org.apache.stanbol.contenthub.servicesapi.store;

import java.util.Set;

import org.apache.clerezza.rdf.core.UriRef;

public interface Changes {
    long from();
    
    long to();
    
    Set<UriRef>changed();
    
}
