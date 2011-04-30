package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import java.math.BigInteger;
import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.AnnotationType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

public interface DPropertyDefinition {

    String getUniqueRef();

    String getName();

    String getNamespace();

    AnnotationType getAnnotations();

    BigInteger getCardinality();

    PropType getPropertyType();

    // TODO getSourceObjectTypeRef ne

    List<String> getValueConstraints();
    
    PropertyDefinition getInstance();

}
