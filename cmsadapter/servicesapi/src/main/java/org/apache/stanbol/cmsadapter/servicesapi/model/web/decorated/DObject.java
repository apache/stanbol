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
	
	/**
	 * Fetches parent of the item from CMS repository.
	 * @return parent of the object, null in <b>OFFLINE</b> mode. 
	 * @throws RepositoryAccessException If repository can not be accessed in <b>ONLINE</b> mode.
	 */
	DObject getParent() throws RepositoryAccessException;
	
	/**
	 * Fetches object type of the item from CMS repository.
	 * @return Object type of the object, null in <b>OFFLINE</b> mode. 
	 * @throws RepositoryAccessException If repository can not be accessed in <b>ONLINE</b> mode.
	 */
	DObjectType getObjectType() throws RepositoryAccessException;
	
	List<DProperty> getProperties() throws RepositoryAccessException;
	
}
