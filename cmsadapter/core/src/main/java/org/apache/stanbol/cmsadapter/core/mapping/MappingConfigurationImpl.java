package org.apache.stanbol.cmsadapter.core.mapping;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingConfiguration;
import org.apache.stanbol.cmsadapter.servicesapi.model.mapping.BridgeDefinitions;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.AdapterMode;

import com.hp.hpl.jena.ontology.OntModel;

public class MappingConfigurationImpl implements MappingConfiguration {
    private OntModel ontModel;
    private BridgeDefinitions bridgeDefinitions;
    private AdapterMode adapterMode;
    private String ontologyURI;
    private List<Object> objects;
    private ConnectionInfo connectionInfo;

    @Override
    public void setOntModel(OntModel ontModel) {
        this.ontModel = ontModel;
    }

    @Override
    public OntModel getOntModel() {
        return this.ontModel;
    }

    @Override
    public void setBridgeDefinitions(BridgeDefinitions bridgeDefinitions) {
        this.bridgeDefinitions = bridgeDefinitions;
    }

    @Override
    public BridgeDefinitions getBridgeDefinitions() {
        return this.bridgeDefinitions;
    }

    @Override
    public void setAdapterMode(AdapterMode adapterMode) {
        this.adapterMode = adapterMode;
    }

    @Override
    public AdapterMode getAdapterMode() {
        return this.adapterMode;
    }

    @Override
    public void setOntologyURI(String ontologyURI) {
        this.ontologyURI = ontologyURI;
    }

    @Override
    public String getOntologyURI() {
        return this.ontologyURI;
    }

    @Override
    public void setObjects(List<Object> objects) {
        this.objects = objects;
    }

    @Override
    public List<Object> getObjects() {
        return this.objects;
    }

    @Override
    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    @Override
    public ConnectionInfo getConnectionInfo() {
        return this.connectionInfo;
    }

}
