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
package org.apache.stanbol.enhancer.engines.dereference.entityhub;

import java.util.concurrent.ExecutorService;

import org.apache.stanbol.enhancer.engines.dereference.EntityDereferencer;
import org.apache.stanbol.entityhub.ldpath.backend.EntityhubBackend;
import org.apache.stanbol.entityhub.servicesapi.Entityhub;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import at.newmedialab.ldpath.api.backend.RDFBackend;

/**
 * {@link EntityDereferencer} implementation for the {@link Entityhub}
 * @author Rupert Westenthaler
 *
 */
public final class EntityhubDereferencer extends TrackingDereferencerBase<Entityhub> implements EntityDereferencer {
    
    private final ExecutorService executorService;
    
    public EntityhubDereferencer(BundleContext context, ExecutorService executorService) {
        this(context,null,executorService);
    }
    public EntityhubDereferencer(BundleContext context, ServiceTrackerCustomizer customizer, ExecutorService executorService) {
        super(context,Entityhub.class,null,customizer);
        this.executorService = executorService;
    }
    
    @Override
    protected Representation getRepresentation(Entityhub eh, String id, boolean offlineMode) throws EntityhubException {
        Entity e = eh.getEntity(id);
        return e == null ? null : e.getRepresentation();
    }
    
    @Override
    public boolean supportsOfflineMode() {
        return true; //the entityhub is always offline
    }
    @Override
    public ExecutorService getExecutor() {
        return executorService;
    }
    
    @Override
    protected RDFBackend<Object> createRdfBackend(Entityhub service) {
        return new EntityhubBackend(service);
    }
    

    
}
