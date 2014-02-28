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

import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.stanbol.entityhub.ldpath.backend.SiteManagerBackend;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.SiteManager;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class SitesDereferencer extends TrackingDereferencerBase<SiteManager> {
    
//    private final Logger log = LoggerFactory.getLogger(SiteDereferencer.class);

    public SitesDereferencer(BundleContext context){
        this(context, null, null);
    }
    public SitesDereferencer(BundleContext context, ExecutorServiceProvider executorServiceProvider){
        this(context, null, executorServiceProvider);
    }
    public SitesDereferencer(BundleContext context, ServiceTrackerCustomizer customizer, 
    		ExecutorServiceProvider executorServiceprovider) {
        super(context, SiteManager.class, null, customizer, executorServiceprovider);
    }
    
    @Override
    public boolean supportsOfflineMode() {
        return true; //can not be determined here .. return true
    }
    
    @Override
    protected Representation getRepresentation(SiteManager sm, String id, boolean offlineMode) throws EntityhubException {
        Entity entity = sm.getEntity(id);
        return entity == null ? null : entity.getRepresentation();
    }
    
    @Override
    protected RDFBackend<Object> createRdfBackend(SiteManager service) {
        return new SiteManagerBackend(service);
    }

}
