package org.apache.stanbol.cmsadapter.core.decorated;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectAdapter;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DObjectType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DProperty;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public class DObjectImp implements DObject {

    private CMSObject instance;
    private DObjectAdapter factory;
    private RepositoryAccess access;
    private List<DObject> children = null;
    private List<DProperty> properties = null;
    private DObject parent = null;
    private DObjectType objectType = null;

    public DObjectImp(CMSObject instance, DObjectAdapter factory, RepositoryAccess access) {
        this.instance = instance;
        this.factory = factory;
        this.access = access;
    }

    @Override
    public String getID() {
        return instance.getUniqueRef();
    }

    @Override
    public String getPath() {
        return instance.getPath();
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
    public List<DObject> getChildren() throws RepositoryAccessException {
        if (children != null) {
            return children;
        }

        List<CMSObject> nodes = access.getChildren(instance, factory.getSession());

        children = new ArrayList<DObject>(nodes.size());
        for (CMSObject node : nodes) {
            children.add(this.factory.wrapAsDObject(node));
        }
        return children;
    }

    @Override
    public DObject getParent() throws RepositoryAccessException {
        if (parent != null) {
            return parent;
        }

        parent = factory.wrapAsDObject(access.getParentByNode(instance, factory.getSession()));

        return parent;
    }

    @Override
    public DObjectType getObjectType() throws RepositoryAccessException {
        if (objectType != null) {
            return this.objectType;
        }

        String typeRef = instance.getObjectTypeRef();
        objectType = factory.wrapAsDObjectType(access.getObjectTypeDefinition(typeRef, factory.getSession()));
        return objectType;
    }

    @Override
    public List<DProperty> getProperties() throws RepositoryAccessException {
        if (properties != null) {
            return properties;
        }

        List<Property> props = access.getProperties(instance, factory.getSession());

        properties = new ArrayList<DProperty>(props.size());

        for (Property prop : props) {
            properties.add(factory.wrapAsDProperty(prop));
        }
        return properties;
    }

    @Override
    public CMSObject getInstance() {
        return this.instance;
    }

}
