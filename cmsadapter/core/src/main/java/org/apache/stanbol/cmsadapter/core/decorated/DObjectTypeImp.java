package org.apache.stanbol.cmsadapter.core.decorated;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DChildObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DObjectTypeImp implements DObjectType {
    private static final Logger log = LoggerFactory.getLogger(DObjectTypeImp.class);

    private ObjectTypeDefinition instance;
    private DObjectAdapter factory;
    private RepositoryAccess access;
    private List<DPropertyDefinition> propertyDefinitions;
    private List<DObjectType> parentDefinitions;
    private List<DChildObjectType> childDefinitions;

    public DObjectTypeImp(ObjectTypeDefinition instance, DObjectAdapter factory, RepositoryAccess access) {
        this.instance = instance;
        this.factory = factory;
        this.access = access;
    }

    @Override
    public String getID() {
        return instance.getUniqueRef();
    }

    @Override
    public String getName() {
        return instance.getLocalname();
    }

    @Override
    public String getNamespace() {
        return instance.getNamespace();
    }

    @Override
    public List<DPropertyDefinition> getPropertyDefinitions() throws RepositoryAccessException {
        if (propertyDefinitions != null) {
            return propertyDefinitions;
        } else {
            List<PropertyDefinition> propDefinitions = access.getPropertyDefinitions(instance,
                factory.getSession());

            propertyDefinitions = new ArrayList<DPropertyDefinition>(propDefinitions.size());
            for (PropertyDefinition propDefinition : propDefinitions) {
                propertyDefinitions.add(factory.wrapAsDPropertyDefinition(propDefinition));
            }

            return propertyDefinitions;
        }
    }

    @Override
    public List<DObjectType> getParentDefinitions() throws RepositoryAccessException {
        if (parentDefinitions != null) {
            return parentDefinitions;
        }

        List<ObjectTypeDefinition> parDefinitions = access.getParentTypeDefinitions(instance,
            factory.getSession());
        propertyDefinitions = new ArrayList<DPropertyDefinition>(parDefinitions.size());
        for (ObjectTypeDefinition parentTypeRef : parDefinitions) {
            parentDefinitions.add(factory.wrapAsDObjectType(parentTypeRef));
        }

        return parentDefinitions;
    }

    @Override
    public List<DChildObjectType> getChildDefinitions() throws RepositoryAccessException {
        if (childDefinitions != null) {
            switch (factory.getMode()) {
                case ONLINE:
                    childDefinitions = getChildDefinitionsOnline();
                    break;
                case TOLERATED_OFFLINE:
                    try {
                        childDefinitions = getChildDefinitionsOnline();
                    } catch (RepositoryAccessException e) {
                        log.debug("Can not access repository while fetching childs definitions of type {}",
                            instance.getUniqueRef());
                        childDefinitions = getChildDefinitionsOffline();
                    }
                    break;
                case STRICT_OFFLINE:
                    childDefinitions = getChildDefinitionsOffline();
                    break;
            }
        }

        return childDefinitions;
    }

    private List<DChildObjectType> getChildDefinitionsOnline() throws RepositoryAccessException {
        List<ChildObjectDefinition> childDefs = access.getChildObjectTypeDefinitions(instance,
            factory.getSession());
        return wrapChildObjectDefinitions(childDefs);
    }

    private List<DChildObjectType> getChildDefinitionsOffline() {
        return wrapChildObjectDefinitions(instance.getChildObjectDefinition());
    }

    private List<DChildObjectType> wrapChildObjectDefinitions(List<ChildObjectDefinition> childDefs) {
        List<DChildObjectType> childDefinitions = new ArrayList<DChildObjectType>(childDefs.size());
        for (ChildObjectDefinition childDef : childDefs) {
            childDefinitions.add(factory.wrapAsDChildObjectType(childDef));
        }
        return childDefinitions;
    }

    @Override
    public ObjectTypeDefinition getInstance() {
        return instance;
    }

}
