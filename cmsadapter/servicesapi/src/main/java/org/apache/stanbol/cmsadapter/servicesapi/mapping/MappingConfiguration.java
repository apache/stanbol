package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.AdapterMode;

import com.hp.hpl.jena.ontology.OntModel;

public interface MappingConfiguration {
    void setOntModel(OntModel ontModel);
    
    OntModel getOntModel();
    
    void setBridgeDefinitions(BridgeDefinitions bridgeDefinitions);
    
    BridgeDefinitions getBridgeDefinitions();
    
    void setAdapterMode(AdapterMode adapterMode);
    
    AdapterMode getAdapterMode();
    
    void setOntologyURI(String ontologyURI);
    
    String getOntologyURI();
    
    void setConnectionInfo(ConnectionInfo connectionInfo);
    
    ConnectionInfo getConnectionInfo();
    
    void setObjects(List<Object> objects);
    
    List<Object> getObjects();
}
