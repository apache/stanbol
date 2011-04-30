package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ObjectTypeDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public interface DObjectType {

	String getID();
	
	String getName();
	
	String getNamespace();
	
	List<DPropertyDefinition> getPropertyDefinitions() throws RepositoryAccessException;
	
	List<DObjectType> getParentDefinitions() throws RepositoryAccessException;
	
	List<DChildObjectType> getChildDefinitions() throws RepositoryAccessException;
	
	ObjectTypeDefinition getInstance();
	
}
