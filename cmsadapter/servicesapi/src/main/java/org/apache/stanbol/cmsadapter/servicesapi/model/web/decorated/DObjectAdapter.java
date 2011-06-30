package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

/**
 * Adapter Class for converting CMS model objects to Decorated CMS objects.
 * The former group has no access to the original CMS repository they are extracted from.
 * The latter can be configured to access to the repository if needed.
 * @author cihan
 *
 */
public interface DObjectAdapter {

    
    DObject wrapAsDObject(CMSObject node);

    DObjectType wrapAsDObjectType(ObjectTypeDefinition definition);

    DPropertyDefinition wrapAsDPropertyDefinition(PropertyDefinition propertyDefinition);

    DProperty wrapAsDProperty(Property property);

    /**
     * Session object to connect Remote CMS repository.
     * @return
     */
    Object getSession();

    /**
     * Mode must be set before fetching an object from CMS. Otherwise inconsistent/erroneous behavior can be
     * faced.
     * 
     * @param mode
     */
    void setMode(AdapterMode mode);

    AdapterMode getMode();

}
