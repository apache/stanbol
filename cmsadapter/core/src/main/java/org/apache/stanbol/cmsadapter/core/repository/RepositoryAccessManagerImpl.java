package org.apache.stanbol.cmsadapter.core.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.cmsadapter.servicesapi.model.web.ConnectionInfo;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccess;
import org.apache.stanbol.cmsadapter.servicesapi.repository.RepositoryAccessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service
public class RepositoryAccessManagerImpl implements RepositoryAccessManager {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, referenceInterface = RepositoryAccess.class, policy = ReferencePolicy.DYNAMIC, bind = "bindRepositoryAccess", unbind = "unbindRepositoryAccess")
    private List<RepositoryAccess> accessors = new ArrayList<RepositoryAccess>();

    private static final Logger logger = LoggerFactory.getLogger(RepositoryAccessManagerImpl.class);

    @Override
    public RepositoryAccess getRepositoryAccessor(ConnectionInfo connectionInfo) {
        Iterator<RepositoryAccess> rai;
        synchronized (accessors) {
            rai = accessors.iterator();
        }

        while (rai.hasNext()) {
            RepositoryAccess ra = rai.next();
            if (ra.canRetrieve(connectionInfo)) {
                return ra;
            }
        }

        logger.warn("No suitable repository access implementation for connection type {} ",
            connectionInfo.getConnectionType());
        return null;
    }

    @Override
    public RepositoryAccess getRepositoryAccess(Object session) {
        Iterator<RepositoryAccess> rai;
        synchronized (accessors) {
            rai = accessors.iterator();
        }

        while (rai.hasNext()) {
            RepositoryAccess ra = rai.next();
            if (ra.canRetrieve(session)) {
                return ra;
            }
        }

        if (session instanceof List<?>) {
            try {
                return new OfflineAccess((List<Object>) session);
            } catch (IllegalArgumentException e) {
                logger.debug(e.getMessage());
            }
            logger.debug("Using offline accessor");

        }

        logger.warn("No suitable repository access implementation for session {} ", session);
        return null;
    }

    protected void bindRepositoryAccess(RepositoryAccess repositoryAccess) {
        synchronized (accessors) {
            accessors.add(repositoryAccess);
        }
    }

    protected void unbindRepositoryAccess(RepositoryAccess repositoryAccess) {
        synchronized (repositoryAccess) {
            accessors.remove(repositoryAccess);
        }
    }
}
