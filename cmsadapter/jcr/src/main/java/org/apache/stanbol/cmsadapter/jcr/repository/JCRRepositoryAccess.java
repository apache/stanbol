package org.apache.stanbol.cmsadapter.jcr.repository;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessException;

/**
 * This interface provides JCR specific repository access functionalities on top of {@link RepositoryAccess}
 * 
 * @author suat
 * 
 */
public interface JCRRepositoryAccess extends RepositoryAccess {
    Node getNodeByPath(String path, Session session) throws RepositoryAccessException;
}
