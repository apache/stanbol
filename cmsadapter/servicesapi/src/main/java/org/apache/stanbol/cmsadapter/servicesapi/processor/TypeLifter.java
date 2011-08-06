package org.apache.stanbol.cmsadapter.servicesapi.processor;

import org.apache.stanbol.cmsadapter.servicesapi.helper.OntologyResourceHelper;
import org.apache.stanbol.cmsadapter.servicesapi.mapping.MappingEngine;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

/**
 * This interface provides a uniform way to semantically lift node/object type definitions in content
 * management systems.
 * 
 * @author suat
 * 
 */
public interface TypeLifter {
    /**
     * This method takes all node/object type definitions in the repository and creates related ontological
     * resources namely classes, object and data properties. It creates a class for each object/node type and
     * object properties for property definitions having types PATH, REFERENCE, etc.; and data properties for
     * property definitions takes literal values.
     * 
     * @param mappingEngine
     *            is the {@link MappingEngine} instance of which acts as context for the implementations of
     *            this interface. It provides context variables such as Session to access repository or
     *            {@link OntologyResourceHelper} to create ontology resources.
     * @throws RepositoryAccessException
     */

    void liftNodeTypes(MappingEngine mappingEngine) throws RepositoryAccessException;

    /**
     * Takes a protocol type e.g JCR/CMIS and returns whether implementation of this interface is capable of
     * lifting type definitions through the specified protocol
     * 
     * @param type
     *            protocol type e.g JCR/CMIS
     * @return
     */
    boolean canLift(String type);
}
