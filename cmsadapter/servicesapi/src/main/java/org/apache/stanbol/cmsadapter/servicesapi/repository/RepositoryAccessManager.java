package org.apache.stanbol.cmsadapter.servicesapi.repository;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;

public interface RepositoryAccessManager {

	RepositoryAccess getRepositoryAccessor(ConnectionInfo connectionInfo);
	
	RepositoryAccess getRepositoryAccess(Object session);
	
	
}
