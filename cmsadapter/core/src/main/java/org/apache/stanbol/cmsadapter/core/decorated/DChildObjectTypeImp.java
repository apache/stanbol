package org.apache.stanbol.cmsadapter.core.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DChildObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public class DChildObjectTypeImp implements DChildObjectType {

    private ChildObjectDefinition instance;
    private DObjectAdapter factory;
    private RepositoryAccess access;
    private DObjectType allowedObjectDefinition = null;

    public DChildObjectTypeImp(ChildObjectDefinition instance, DObjectAdapter factory, RepositoryAccess access) {
        this.instance = instance;
        this.factory = factory;
        this.access = access;
    }

    @Override
    public boolean isRequired() {
        return instance.isRequired();
    }

    @Override
    public DObjectType getAllowedObjectDefinitions() throws RepositoryAccessException {
        ObjectTypeDefinition allowedDefinition = access.getAllowableTypeDef(instance, factory.getSession());
        allowedObjectDefinition = factory.wrapAsDObjectType(allowedDefinition);
        return allowedObjectDefinition;
    }

    @Override
    public ChildObjectDefinition getInstance() {
        return instance;
    }

}
