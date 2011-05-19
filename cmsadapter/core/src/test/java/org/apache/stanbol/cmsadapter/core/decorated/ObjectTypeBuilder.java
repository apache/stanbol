package org.apache.stanbol.cmsadapter.core.decorated;

import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.LOCAL_NAME;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.NAMESPACE;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.UNIQUE_REF;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

public class ObjectTypeBuilder {
    private static ObjectFactory of = new ObjectFactory();
    private ObjectTypeDefinition instance = of.createObjectTypeDefinition();

    public ObjectTypeBuilder(String prefix) {
        instance.setUniqueRef(prefix + UNIQUE_REF);
        instance.setLocalname(prefix + LOCAL_NAME);
        instance.setNamespace(prefix + NAMESPACE);
    }

    public ObjectTypeBuilder parentRef(String parentRef) {
        instance.getParentRef().add(parentRef);
        return this;
    }

    public ObjectTypeBuilder propertyDefinition(PropertyDefinition propDef) {
        instance.getPropertyDefinition().add(propDef);
        return this;
    }

    public ObjectTypeBuilder childObjectDefinition(ObjectTypeDefinition childDef) {
        instance.getObjectTypeDefinition().add(childDef);
        return this;
    }

    public ObjectTypeDefinition build() {
        return instance;
    }

}
