package org.apache.stanbol.cmsadapter.servicesapi.repository;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;

/**
 * This class is responsible for retrieving a suitable accessor when a 
 * session or connection description is given.
 * 
 * @author cihan
 * 
 */
public interface RepositoryAccessManager {

    /**
     * 
     * @param connectionInfo
     * @return Any suitable {@link RepositoryAccess} instance that can connect to the
     * CMS repository described in <b>connectionInfo</b> parameter or null if no suitable 
     * accessor can be found. 
     */
    RepositoryAccess getRepositoryAccessor(ConnectionInfo connectionInfo);
    
    /**
     * 
     * @param session
     * @return Any suitable {@link RepositoryAccess} instance that can connect to the
     * CMS repository through session given in  <b>session</b> parameter or null if no suitable 
     * accessor can be found. 
     */
    RepositoryAccess getRepositoryAccess(Object session);

}
