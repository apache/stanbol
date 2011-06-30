package org.apache.stanbol.cmsadapter.servicesapi.model.web.decorated;

import java.util.List;

import org.apache.stanbol.cmsadapter.servicesapi.model.web.Property;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

/**
 * Decorated form of {@link Property}. While {@link Property} is completely separated from the repository it
 * is generated, {@link DProperty} is able to reconnect to the repository and fetch any data that is not
 * present in {@link Property}. </br> Details of when the repository is determined by {@link AdapterMode}s.
 * See {@link DObjectAdapter} and {@link AdapterMode} for more details.
 * 
 * @author cihan
 * 
 */
public interface DProperty {

    /**
     * 
     * @return Name of the underlying {@link Property}.
     * 
     */
    String getName();

    /**
     * 
     * @return In which property definition this property is defined, wrapped as {@link DObject} .May return
     *         null in <b>TOLERATED_OFFLINE</b> or <b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException
     *             if can not access repository in <b>ONLINE</> mode.
     */
    DPropertyDefinition getDefinition() throws RepositoryAccessException;

    /**
     * 
     * @return CMS object to which this property belongs, wrapped as {@link DObject}, .May return null in
     *         <b>TOLERATED_OFFLINE</b> or <b>STRICT_OFFLINE</b> mode.
     * @throws RepositoryAccessException
     *             if can not access repository in <b>ONLINE</> mode.
     */
    DObject getSourceObject() throws RepositoryAccessException;

    /**
     * 
     * @return Property values in string representation.
     */
    List<String> getValue();

    /**
     * 
     * @return Underlying {@link Property}
     */
    Property getInstance();

}
