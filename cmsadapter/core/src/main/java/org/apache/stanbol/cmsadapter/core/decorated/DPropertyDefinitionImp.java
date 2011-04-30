package org.apache.stanbol.cmsadapter.core.decorated;

import java.math.BigInteger;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.AnnotationType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated.DPropertyDefinition;

public class DPropertyDefinitionImp implements DPropertyDefinition {

    private PropertyDefinition instance;

    public DPropertyDefinitionImp(PropertyDefinition instance) {
        this.instance = instance;
    }

    @Override
    public String getUniqueRef() {
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
    public AnnotationType getAnnotations() {
        return instance.getAnnotation();
    }

    @Override
    public BigInteger getCardinality() {
        return instance.getCardinality();
    }

    @Override
    public PropType getPropertyType() {
        return instance.getPropertyType();
    }

    @Override
    public List<String> getValueConstraints() {
        return instance.getValueConstraint();
    }

    @Override
    public PropertyDefinition getInstance() {
        return instance;
    }

}
