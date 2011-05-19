package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public interface DProperty {

    String getName();

    /**
     * 
     * @return Property definition of property, null if <b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException if can not access repository in <b>ONLINE</> mode.
     */
    DPropertyDefinition getDefinition() throws RepositoryAccessException;

    /**
     * 
     * @return source object of property, null if <b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException if can not access repository in <b>ONLINE</> mode.
     */
    DObject getSourceObject() throws RepositoryAccessException;

    List<String> getValue();

    Property getInstance();

}
