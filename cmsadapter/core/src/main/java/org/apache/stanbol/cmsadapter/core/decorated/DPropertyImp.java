package org.apache.stanbol.cmsadapter.core.decorated;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DPropertyImp implements DProperty {
    private static final Logger log = LoggerFactory.getLogger(DPropertyImp.class);

    private Property instance;
    private DObjectAdapter factory;
    private RepositoryAccess access;
    private DObject sourceObject;
    private DPropertyDefinition propertyDefinition;

    public DPropertyImp(Property instance, DObjectAdapter adapter, RepositoryAccess access) {
        this.instance = instance;
        this.factory = adapter;
        this.access = access;
    }

    @Override
    public PropType getType() {
        return instance.getType();
    }

    @Override
    public DPropertyDefinition getDefinition() throws RepositoryAccessException {
        if (propertyDefinition == null) {
            switch (factory.getMode()) {
                case ONLINE:
                    propertyDefinition = getPropertyDefinitionOnline();
                    break;
                case TOLERATED_OFFLINE:
                    try {
                        propertyDefinition = getPropertyDefinitionOnline();
                    } catch (RepositoryAccessException e) {
                        log.debug("Can not access repository at fetching source object of property {}",
                            instance.getLocalname());
                    }
                    break;
                case STRICT_OFFLINE:
                    break;
            }

        }

        return propertyDefinition;
    }

    private DPropertyDefinition getPropertyDefinitionOnline() throws RepositoryAccessException {
        PropertyDefinition propDef = access.getPropertyDefinition(instance, factory.getSession());
        return factory.wrapAsDPropertyDefinition(propDef);
    }

    @Override
    public DObject getSourceObject() throws RepositoryAccessException {
        if (sourceObject == null) {
            switch (factory.getMode()) {
                case ONLINE:
                    sourceObject = getSourceObjectOnline();
                    break;
                case TOLERATED_OFFLINE:
                    try {
                        sourceObject = getSourceObjectOnline();
                    } catch (RepositoryAccessException e) {
                        log.debug("Can not access repository at fetching source object of property {}",
                            instance.getLocalname());
                    }
                    break;
                case STRICT_OFFLINE:
                    break;
            }
        }
        return sourceObject;
    }

    private DObject getSourceObjectOnline() throws RepositoryAccessException {
        CMSObject source = access.getContainerObject(instance, factory.getSession());
        return factory.wrapAsDObject(source);
    }

    @Override
    public List<String> getValue() {
        return instance.getValue();
    }

    @Override
    public Property getInstance() {
        return instance;
    }

    @Override
    public String getName() {
        return this.getName();
    }

}
