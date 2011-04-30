package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

public interface DObjectAdapter {

	DObject wrapAsDObject(CMSObject node);
	
	DObjectType wrapAsDObjectType(ObjectTypeDefinition definition);
	
	DPropertyDefinition wrapAsDPropertyDefinition(PropertyDefinition propertyDefinition);
	
	DChildObjectType wrapAsDChildObjectType(ChildObjectDefinition childObjectDefinition);
	
	DProperty wrapAsDProperty(Property property);
	
	Object getSession();
	
}
