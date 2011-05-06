package org.apache.stanbol.cmsadapter.core.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.AdapterMode;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DChildObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public class DObjectFactoryImp implements DObjectAdapter {

    private RepositoryAccess access;
    private Object session;
    private AdapterMode mode;

    public DObjectFactoryImp(RepositoryAccess access, Object session) {
        this.access = access;
        this.session = session;
        this.mode = AdapterMode.ONLINE;
    }

    public DObjectFactoryImp(RepositoryAccess access, ConnectionInfo connectionInfo) throws RepositoryAccessException {
        this.access = access;
        this.session = access.getSession(connectionInfo);
        this.mode = AdapterMode.ONLINE;
    }

    public DObjectFactoryImp(RepositoryAccess access, Object session, AdapterMode mode) {
        this.access = access;
        this.session = session;
        this.mode = mode;
    }

    public DObjectFactoryImp(RepositoryAccess access, ConnectionInfo connectionInfo, AdapterMode mode) throws RepositoryAccessException {
        this.access = access;
        this.session = access.getSession(connectionInfo);
        this.mode = mode;
    }

    @Override
    public DObject wrapAsDObject(CMSObject node) {
        return new DObjectImp(node, this, access);
    }

    @Override
    public DObjectType wrapAsDObjectType(ObjectTypeDefinition definition) {
        return new DObjectTypeImp(definition, this, access);
    }

    @Override
    public DPropertyDefinition wrapAsDPropertyDefinition(PropertyDefinition propertyDefinition) {
        return new DPropertyDefinitionImp(propertyDefinition);
    }

    @Override
    public DChildObjectType wrapAsDChildObjectType(ChildObjectDefinition childObjectDefinition) {
        return new DChildObjectTypeImp(childObjectDefinition, this, access);
    }

    @Override
    public DProperty wrapAsDProperty(Property property) {
        return new DPropertyImp(property, this, access);
    }

    @Override
    public Object getSession() {
        return session;
    }

    @Override
    public void setMode(AdapterMode mode) {
        this.mode = mode;
    }

    @Override
    public AdapterMode getMode() {
        return mode;
    }

}
