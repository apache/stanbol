package org.apache.stanbol.contenthub.servicesapi.index;

import java.util.List;
import java.util.Map;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.osgi.framework.ServiceReference;

public interface SemanticIndex {
    String getName();
    
    String getDescription();
    
    boolean index(ContentItem ci);
    
    void remove(UriRef ciURI);
    
    void persist(long revision);
    
    long getRevision();
    
    List<String> getFieldsNames();
    
    Map<String,Object> getFieldProperties(String name);
    
    Map<String, String> getRESTSearchEndpoints();

    Map<Class<?>, ServiceReference> getSearchEndPoints();
}
