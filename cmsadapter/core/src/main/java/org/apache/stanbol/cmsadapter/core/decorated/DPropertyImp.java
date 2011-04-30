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

public class DPropertyImp implements DProperty {

    private Property instance;
    private DObjectAdapter adapter;
    private RepositoryAccess access;
    private DObject sourceObject;
    private DPropertyDefinition propertyDefinition;

    public DPropertyImp(Property instance, DObjectAdapter adapter, RepositoryAccess access) {
        this.instance = instance;
        this.adapter = adapter;
        this.access = access;
    }

    @Override
    public PropType getType() {
        return instance.getType();
    }

    @Override
    public DPropertyDefinition getDefinition() throws RepositoryAccessException {
        if (propertyDefinition != null) {
            return propertyDefinition;
        }

        PropertyDefinition propDef = access.getPropertyDefinition(instance, adapter.getSession());

        propertyDefinition = adapter.wrapAsDPropertyDefinition(propDef);
        return propertyDefinition;

    }

    @Override
    public DObject getSourceObject() throws RepositoryAccessException {
        CMSObject source = access.getContainerObject(instance, adapter.getSession());
        sourceObject = adapter.wrapAsDObject(source);
        return sourceObject;
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
