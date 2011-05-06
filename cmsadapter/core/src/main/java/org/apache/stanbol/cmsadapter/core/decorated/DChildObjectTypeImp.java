package org.apache.stanbol.cmsadapter.core.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DChildObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DChildObjectTypeImp implements DChildObjectType {
    private static final Logger log = LoggerFactory.getLogger(DChildObjectTypeImp.class);

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
        if (allowedObjectDefinition == null) {
            switch (factory.getMode()) {
                case ONLINE:
                    allowedObjectDefinition = getAllowedOnline();
                    break;
                case TOLERATED_OFFLINE:
                    try {
                        allowedObjectDefinition = getAllowedOnline();
                    } catch (RepositoryAccessException e) {
                        log.debug("Can not access repository at fetching allowed object type d");
                    }
                    break;
                case STRICT_OFFLINE:
                    break;
            }
        }

        return allowedObjectDefinition;
    }

    private DObjectType getAllowedOnline() throws RepositoryAccessException {
        ObjectTypeDefinition allowedDefinition = access.getAllowableTypeDef(instance, factory.getSession());
        return factory.wrapAsDObjectType(allowedDefinition);
    }

    @Override
    public ChildObjectDefinition getInstance() {
        return instance;
    }

}
