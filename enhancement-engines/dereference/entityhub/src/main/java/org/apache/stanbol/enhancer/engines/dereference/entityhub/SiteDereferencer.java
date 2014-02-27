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

import java.util.Collections;
import java.util.concurrent.ExecutorService;

import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.stanbol.entityhub.ldpath.backend.SiteBackend;
import org.apache.stanbol.entityhub.servicesapi.EntityhubException;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class SiteDereferencer extends TrackingDereferencerBase<Site> {
    
//    private final Logger log = LoggerFactory.getLogger(SiteDereferencer.class);
    
    private final String siteId;

    public SiteDereferencer(BundleContext context, String siteId){
        this(context,siteId, null, null);
    }
    public SiteDereferencer(BundleContext context, String siteId, ExecutorServiceProvider executorServiceProvider){
        this(context,siteId, null, executorServiceProvider);
    }
    public SiteDereferencer(BundleContext context, String siteId, ServiceTrackerCustomizer customizer, ExecutorServiceProvider executorServiceProvider) {
        super(context, Site.class, 
            Collections.singletonMap(SiteConfiguration.ID,siteId),
            customizer, executorServiceProvider);
        this.siteId = siteId;
    }
    
    @Override
    public boolean supportsOfflineMode() {
        Site site = getService();
        //Do not throw an exception here if the site is not available. Just return false
        return site == null ? false : site.supportsLocalMode();
    }
    
    @Override
    protected Representation getRepresentation(Site site, String id, boolean offlineMode) throws EntityhubException {
        Entity entity = site.getEntity(id);
        return entity == null ? null : entity.getRepresentation();
    }
    @Override
    protected RDFBackend<Object> createRdfBackend(Site service) {
        return new SiteBackend(service);
    }

}
