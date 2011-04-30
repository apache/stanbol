package org.apache.stanbol.cmsadapter.servicesapi.mapping;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;

import com.hp.hpl.jena.ontology.OntModel;

public interface MappingEngine {
	void mapCR(OntModel model, String ontologyURI, List<CMSObject> cmsObjects) throws RepositoryAccessException;
	
    void mapCR(BridgeDefinitions bridges, ConnectionInfo connectionInfo, String ontologyURI) throws RepositoryAccessException;

    void liftNodeTypes(ConnectionInfo connectionInfo, String ontologyURI);

    String getOntologyURI();

    OntModel getOntModel();

    DObjectAdapter getDObjectAdapter();

    OntologyResourceHelper getOntologyResourceHelper();

    Object getSession();

    BridgeDefinitions getBridgeDefinitions();

    RepositoryAccessManager getRepositoryAccessManager();

    NamingStrategy getNamingStrategy();
}
