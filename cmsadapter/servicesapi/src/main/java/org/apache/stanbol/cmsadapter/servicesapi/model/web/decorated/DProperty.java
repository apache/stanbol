package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.PropType;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

public interface DProperty {

    String getName();

    PropType getType();

    DPropertyDefinition getDefinition() throws RepositoryAccessException;

    DObject getSourceObject() throws RepositoryAccessException;

    List<String> getValue();

    Property getInstance();

}
