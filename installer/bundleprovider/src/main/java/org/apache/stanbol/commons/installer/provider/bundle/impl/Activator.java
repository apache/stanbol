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
package org.apache.stanbol.commons.installer.provider.bundle.impl;

import org.apache.sling.installer.api.OsgiInstaller;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple {@link BundleActivator} that also listens to the {@link OsgiInstaller}
 * service.
 * <p>
 * If the Bundle is active and the {@link OsgiInstaller} is available the
 * {@link BundleInstaller} is created. If the bundle is stopped or the
 * {@link OsgiInstaller} goes away the {@link BundleInstaller} is closed. 
 * @author Rupert Westenthaler
 *
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer {
    
    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    private ServiceTracker installerTracker;
    
    private BundleContext bundleContext;
    
    private BundleInstaller bundleInstaller;
    
    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        //Note that this class implements ServiceTrackerCustomizer to init/stop
        // the BundleInstaller
        installerTracker = new ServiceTracker(context, OsgiInstaller.class.getName(),this);
        installerTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        closeBundleInstaller();
        installerTracker.close();
        bundleContext = null;
    }
    
    @Override
    public Object addingService(ServiceReference reference) {
        Object service = bundleContext.getService(reference);
        if(service instanceof OsgiInstaller){
            initBundleInstaller((OsgiInstaller)service);
            return service;
        } else {
            return null;
        }
    }

    /* not needed for the OsgiInstaller */
    @Override
    public void modifiedService(ServiceReference arg0, Object arg1) { /* unused */ }

    @Override
    public void removedService(ServiceReference sr, Object s) {
        //stop the BundleInstaller
        closeBundleInstaller();
        //unget the service
        bundleContext.ungetService(sr);
    }
    
    private synchronized void initBundleInstaller(OsgiInstaller installer){
        if(bundleInstaller == null){
            log.info("start BundleInstaller");
            bundleInstaller = new BundleInstaller(installer, bundleContext);
        }
    }
    
    private synchronized void closeBundleInstaller(){
        if(bundleInstaller != null){
            log.info("close BundleInstaller");
            bundleInstaller.close();
            bundleInstaller = null;
        }
    }

}
