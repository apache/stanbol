package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;

import com.hp.hpl.jena.ontology.OntModel;

public interface MappingEngine {
    void mapCR(MappingConfiguration conf) throws RepositoryAccessException;

    void createModel(MappingConfiguration conf);

    void updateModel(MappingConfiguration conf);

    void deleteModel(MappingConfiguration conf);

    String getOntologyURI();
    
    OntModel getOntModel();

    DObjectAdapter getDObjectAdapter();

    OntologyResourceHelper getOntologyResourceHelper();

    Object getSession();

    BridgeDefinitions getBridgeDefinitions();

    RepositoryAccessManager getRepositoryAccessManager();
    
    RepositoryAccess getRepositoryAccess();
    
    NamingStrategy getNamingStrategy();
}
