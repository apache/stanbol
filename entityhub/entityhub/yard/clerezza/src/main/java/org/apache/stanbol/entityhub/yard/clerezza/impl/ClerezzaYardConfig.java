package org.apache.stanbol.entityhub.yard.clerezza.impl;

import java.util.Dictionary;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.osgi.service.cm.ConfigurationException;

public class ClerezzaYardConfig extends YardConfig {

    
    
    
    public ClerezzaYardConfig(String id) throws IllegalArgumentException {
        super(id);
    }
    public ClerezzaYardConfig(Dictionary<String,Object> config) throws ConfigurationException, IllegalArgumentException {
        super(config);
    }

    /**
     * Getter for the {@link ClerezzaYard#GRAPH_URI} property
     * @return the graph URI or <code>null</code> if non is configured
     */
    public UriRef getGraphUri(){
        Object value = config.get(ClerezzaYard.GRAPH_URI);
        if(value instanceof UriRef){
            return (UriRef)value;
        } else if (value != null){
            return new UriRef(value.toString());
        } else {
            return null;
        }
    }
    /**
     * Setter for the {@link ClerezzaYard#GRAPH_URI} property
     * @param uri the uri or <code>null</code> to remove this configuration
     */
    public void setGraphUri(UriRef uri){
        if(uri == null){
            config.remove(ClerezzaYard.GRAPH_URI);
        } else {
            config.put(ClerezzaYard.GRAPH_URI, uri.getUnicodeString());
        }
    }
    
    @Override
    protected void validateConfig() throws ConfigurationException {
        //nothing to validate
    }

}
