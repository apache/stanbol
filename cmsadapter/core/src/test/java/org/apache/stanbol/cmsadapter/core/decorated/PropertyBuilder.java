package org.apache.stanbol.cmsadapter.core.decorated;

import static org.apache.stanbol.cmsadapter.core.decorated.NamingHelper.LOCAL_NAME;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectFactory;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

public class PropertyBuilder {
    private static ObjectFactory of = new ObjectFactory();
    private Property instance = of.createProperty();

    public PropertyBuilder(String prefix) {
        instance.setLocalname(prefix + LOCAL_NAME);
    }

    public PropertyBuilder propertyDefinition(PropertyDefinition propDef) {
        instance.setPropertyDefinition(propDef);
        return this;
    }
    
    
    public PropertyBuilder containerObjectRef(CMSObject obj){
        instance.setContainerObjectRef(obj.getUniqueRef());
        return this;
    }
    
    public Property build(){
        return instance;
    }
    
}
