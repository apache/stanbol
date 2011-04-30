package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.ChildObjectDefinition;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public interface DChildObjectType {

    boolean isRequired();

    DObjectType getAllowedObjectDefinitions() throws RepositoryAccessException;

    ChildObjectDefinition getInstance();
    
}
