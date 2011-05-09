package org.apache.stanbol.cmsadapter.core.decorated;

import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.LOCAL_NAME;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.NAMESPACE;
import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.UNIQUE_REF;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

public class PropertyDefinitionBuilder {
    private ObjectFactory of = new ObjectFactory();
    private PropertyDefinition instance = of.createPropertyDefinition();

    public PropertyDefinitionBuilder(String prefix) {
        instance.setUniqueRef(prefix + UNIQUE_REF);
        instance.setLocalname(prefix + LOCAL_NAME);
        instance.setNamespace(prefix + NAMESPACE);
    }

    public PropertyDefinition build() {
        return instance;
    }

}
