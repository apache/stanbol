package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public interface DChildObjectType {

    boolean isRequired();

    /**
     * 
     * @return Allowed object type declerations, null if in<b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException if can not access CMS repository in <b>ONLINE</b> mode.
     */
    DObjectType getAllowedObjectDefinitions() throws RepositoryAccessException;

    ChildObjectDefinition getInstance();
    
}
