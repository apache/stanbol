package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropertyDefinition;

public interface DObjectAdapter {

	DObject wrapAsDObject(CMSObject node);

	DObjectType wrapAsDObjectType(ObjectTypeDefinition definition);

	DPropertyDefinition wrapAsDPropertyDefinition(
			PropertyDefinition propertyDefinition);

	DChildObjectType wrapAsDChildObjectType(
			ChildObjectDefinition childObjectDefinition);

	DProperty wrapAsDProperty(Property property);

	Object getSession();

	/**
	 * Mode must be set before fetching an object from CMS.
	 * Otherwise inconsistent/erroneous behavior can be faced. 
	 * @param mode
	 * @return
	 */
	void setMode(AdapterMode mode);

	AdapterMode getMode();

}
