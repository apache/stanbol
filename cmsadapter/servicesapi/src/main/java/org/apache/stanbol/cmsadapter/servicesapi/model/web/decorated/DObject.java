package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.CMSObject;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public interface DObject {

	String getID();
	
	String getPath();
	
	String getName();
	
	String getNamespace();
	
	CMSObject getInstance();
	
	List<DObject> getChildren() throws RepositoryAccessException;
	
	//Make list?
	DObject getParent() throws RepositoryAccessException;
	
	//Make list?
	DObjectType getObjectType() throws RepositoryAccessException;
	
	List<DProperty> getProperties() throws RepositoryAccessException;
	
}
