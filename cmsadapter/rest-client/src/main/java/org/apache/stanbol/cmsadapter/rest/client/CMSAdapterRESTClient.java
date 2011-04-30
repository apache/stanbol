package org.apache.stanbol.cmsadapter.rest.client;

import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObjects;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;

public interface CMSAdapterRESTClient {
    public void addBridgeDefinitions(BridgeDefinitions bridgeDefinitions, ConnectionInfo connectionInfo);

    public void updateBridgeDefinitions(String ontologyURI, BridgeDefinitions bridgeDefinitions);

    public void notifyContentChange(String ontologyURI, CMSObjects cmsObjects);
}
