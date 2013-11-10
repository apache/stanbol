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
package org.apache.stanbol.reasoners.manager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.reasoners.servicesapi.ReasoningService;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServicesManager;
import org.apache.stanbol.reasoners.servicesapi.UnboundReasoningServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the standard {@see ReasoningServicesManager}
 * This is only intended to be used in OSGi environments.
 * 
 * @author enridaga
 * 
 */
@Component(immediate=true)
@Service
public class ReasoningServicesManagerImpl implements ReasoningServicesManager {

    private static final Logger log = LoggerFactory.getLogger(ReasoningServicesManagerImpl.class);

    @Reference(name = "ReasoningService", referenceInterface = org.apache.stanbol.reasoners.servicesapi.ReasoningService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
    private Set<ReasoningService<?,?,?>> services = new HashSet<ReasoningService<?,?,?>>();

    public void bindReasoningService(ReasoningService<?,?,?> service) {
        services.add(service);
        log.debug("Reasoning service {} added to path {}", service, service.getPath());
        log.debug("{} services bound.", services.size());
    }

    public void unbindReasoningService(ReasoningService<?,?,?> service) {
        services.remove(service);
        log.debug("Reasoning service {} removed from path {}", service, service.getPath());
        log.debug("{} services bound.", services.size());
    }

    @Override
    public int size() {
        return services.size();
    }

    @Override
    public ReasoningService<?,?,?> get(String path) throws UnboundReasoningServiceException {
        for (ReasoningService<?,?,?> service : services) {
            log.debug("Does service {} match path {}?", service, path);
            if (service.getPath().equals(path)) {
                return service;
            }
        }
        throw new UnboundReasoningServiceException();
    }

    @Override
    public Set<ReasoningService<?,?,?>> asUnmodifiableSet() {
        return Collections.unmodifiableSet(services);
    }
}
