/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
