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
package org.apache.stanbol.commons.stanboltools.datafileprovider.bundle.impl;

import static org.apache.stanbol.commons.stanboltools.datafileprovider.bundle.BundleResourceProviderConstants.*;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.commons.stanboltools.datafileprovider.bundle.BundleResourceProviderConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link BundleDataFileProvider} instances for bundles that define
 * the {@link BundleResourceProviderConstants#BUNDLE_DATAFILE_HEADER} property
 * in there headers as described by the 
 * <a href="http://www.aqute.biz/Snippets/Extender"> OSGi extender  pattern </a>
 * <p>
 *
 * @author Rupert Westenthaler
 */
public class DataBundleInstaller implements BundleListener {

    private static final Logger log = LoggerFactory.getLogger(DataBundleInstaller.class);

    /**
     * The scheme we use to register our resources.
     */
    private final BundleContext context;

    /**
     * contains all active bundles as key and the ServiceRegistration for the
     * {@link BundleDataFileProvider} as value. A <code>null</code> value 
     * indicates that this bundle needs not to be processed.
     */
    private final Map<Bundle, ServiceRegistration> activated = new HashMap<Bundle, ServiceRegistration>();

    public DataBundleInstaller(BundleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("The BundleContext MUST NOT be NULL");
        }
        this.context = context;
        this.context.addBundleListener(this);
        //register the already active bundles
        registerActive(this.context);
    }

    /**
     * Uses the parsed bundle context to register the already active (and currently
     * starting) bundles.
     */
    private void registerActive(BundleContext context) {
        for (Bundle bundle : context.getBundles()) {
            if ((bundle.getState() & (Bundle.STARTING | Bundle.ACTIVE)) != 0) {
                register(bundle);
            }
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTED:
                register(event.getBundle());
                break;

            case BundleEvent.STOPPED:
                unregister(event.getBundle());
                break;

            case BundleEvent.UPDATED:
                unregister(event.getBundle());
                register(event.getBundle());
        }
    }

    /**
     * Registers the bundle to the {@link #activated} map.
     *
     * @param bundle the bundle to register
     */
    @SuppressWarnings("unchecked")
    private void register(Bundle bundle) {
        synchronized (activated) {
            if (activated.containsKey(bundle)) {
                return;
            }
            //for now put the bundle with a null key to avoid duplicate adding
            activated.put(bundle, null);
        }
        log.debug("Register Bundle {} with DataBundleInstaller",bundle.getSymbolicName());
        Dictionary<String, String> headers = (Dictionary<String, String>) bundle.getHeaders();
        //        log.info("With Headers:");
        //        for(Enumeration<String> keys = headers.keys();keys.hasMoreElements();){
        //            String key = keys.nextElement();
        //            log.info(" > "+key+"="+headers.get(key));
        //        }
        String pathsString = (String) headers.get(BUNDLE_DATAFILE_HEADER);
        if(pathsString != null){
            Dictionary<String,Object> properties = new Hashtable<String,Object>();
            String dataFilesRankingString = (String) headers.get(BUNDLE_DATAFILES_PRIORITY_HEADER);
            if(dataFilesRankingString != null){
                try {
                    properties.put(Constants.SERVICE_RANKING, Integer.valueOf(dataFilesRankingString));
                } catch (NumberFormatException e) {
                    log.warn("Unable to parse integer value for '{}' from the configured value '{}'. " +
                    		"Will use default ranking",
                        BUNDLE_DATAFILES_PRIORITY_HEADER,dataFilesRankingString);
                }
            } //else no service ranking
            List<String> paths = Arrays.asList(pathsString.replaceAll("\\s", "").split(","));
            
            BundleDataFileProvider provider = new BundleDataFileProvider(bundle, paths);
            properties.put(Constants.SERVICE_DESCRIPTION, String.format(
                "%s for Bundle %s and Paths %s", 
                BundleDataFileProvider.class.getSimpleName(),bundle.getSymbolicName(),
                provider.getSearchPaths()));
            ServiceRegistration registration = context.registerService(
                DataFileProvider.class.getName(),provider, properties);
            log.info("Registerd BundleResourceProvider for {} and relative paths {}",
                context.getBundle().getSymbolicName(),provider.getSearchPaths());
            synchronized (activated) { //update with the registration
                if(activated.containsKey(bundle)){
                    activated.put(bundle, registration);
                } else { //the bundle was deactivated in the meantime ... unregister :(
                    registration.unregister();
                }
            }
        } //else key not preset ... ignore bundle!
    }

    private void unregister(Bundle bundle) {
        synchronized (activated) {
            if (!activated.containsKey(bundle)) {
                return;
            }
            ServiceRegistration registration = activated.remove(bundle);
            if(registration != null){
                log.info("Unregister BundleDataFileProvider for Bundel {}",bundle.getSymbolicName());
                registration.unregister();
            }
        }
    }
    /**
     * removes the bundle listener
     */
    public void close() {
        context.removeBundleListener(this);
        synchronized (activated) {
            for(Entry<Bundle,ServiceRegistration> entry : activated.entrySet()) {
                if(entry.getValue() != null){
                    log.info("Unregister BundleDataFileProvider for Bundel {}",entry.getKey().getSymbolicName());
                    entry.getValue().unregister();
                }
            }
        }
    }
}
