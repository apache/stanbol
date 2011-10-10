package org.apache.stanbol.explanation.api;

import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;

/**
 * This object contains every shared information that other components need to know in order to work. It
 * should be the primary object referenced by service components and passed as an argument to non-default
 * constructors.
 * 
 * @author alessandro
 * 
 */
public interface Configuration {

    /**
     * The key used to configure the path of the ontology network configuration.
     */
    String SCOPE_SHORT_ID = "org.apache.stanbol.explanation.scopeid";

    ONManager getOntologyNetworkManager();

    String getScopeID();
    
    String getScopeShortId();
    
    

}
