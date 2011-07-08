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

import static org.apache.stanbol.commons.installer.provider.bundle.BundleInstallerConstants.BUNDLE_INSTALLER_HEADER;
import static org.apache.stanbol.commons.installer.provider.bundle.BundleInstallerConstants.PROVIDER_SCHEME;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.stanbol.commons.installer.provider.bundle.BundleInstallerConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs resources within bundles by using the Apache Sling Installer
 * framework.
 * <p>
 * NOTE that currently installed resources are not removed if the bundle is
 * deactivated because it is not clear if this is a good thing to do. maybe one
 * should use {@link Bundle#UNINSTALLED} for that. However this needs some
 * additional testing.
 * <p>
 * The OSGi extender pattern (as described at [1]) is used. The value of the
 * {@link BundleInstallerConstants#BUNDLE_INSTALLER_HEADER}
 * ({@value BundleInstallerConstants#BUNDLE_INSTALLER_HEADER}) is used as relative path within
 * the bundle to search for installable  resources. Also files in sub folders
 * are considered as installable resources.<p>
 * The files are installed in the order as returned by
 * {@link Bundle#findEntries(String, String, boolean)}. Directories are
 * ignored.<p>
 * All resources installed by this provider do use
 * {@link BundleInstallerConstants#PROVIDER_SCHEME} ({@value BundleInstallerConstants#PROVIDER_SCHEME}) as
 * scheme and the path additional to the value of
 * {@link BundleInstallerConstants#BUNDLE_INSTALLER_HEADER}.<p>
 * To give an example:<p>
 * If the Bundle header notes<br>
 * <pre><code>
 *     {@value BundleInstallerConstants#BUNDLE_INSTALLER_HEADER}=resources
 * </pre></code><br>
 * and the bundle contains the resources: <br>
 * <pre><code>
 *     resources/bundles/10/myBundle.jar
 *     resources/config/myComponent.cfg
 *     resoruces/data/myIndex.solrondex.zip
 * </pre></code><br>
 * then the following resources will be installed:
 * <pre><code>
 *     {@value BundleInstallerConstants#PROVIDER_SCHEME}:bundles/10/myBundle.jar
 *     {@value BundleInstallerConstants#PROVIDER_SCHEME}:config/myComponent.cfg
 *     {@value BundleInstallerConstants#PROVIDER_SCHEME}:data/myIndex.solrondex.zip
 * </pre></code>
 * <p>
 * This means that {@link ResourceTransformer}s can both use the original name
 * of the resource and the path relative to the install folder.
 * <p>
 * [1]  <a href="http://www.aqute.biz/Snippets/Extender"> The OSGi extender  pattern </a>
 * <p>
 *
 * @author Rupert Westenthaler
 */
public class BundleInstaller implements BundleListener {

    private static final Logger log = LoggerFactory.getLogger(BundleInstaller.class);

    /**
     * The scheme we use to register our resources.
     */
    private final OsgiInstaller installer;
    private final BundleContext context;

    /**
     * contains all active bundles as key and the path to the config directory
     * as value. A <code>null</code> value indicates that this bundle needs not
     * to be processed.
     */
    private final Map<Bundle, String> activated = new HashMap<Bundle, String>();

    public BundleInstaller(OsgiInstaller installer, BundleContext context) {
        if (installer == null) {
            throw new IllegalArgumentException("The OsgiInstaller service MUST NOT be NULL");
        }
        if (context == null) {
            throw new IllegalArgumentException("The BundleContext MUST NOT be NULL");
        }
        this.installer = installer;
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
        }
        log.info("Register Bundle " + bundle.getSymbolicName() + " with BundleInstaller");
        Dictionary<String, Object> headers = (Dictionary<String, Object>) bundle.getHeaders();
        //        log.info("With Headers:");
        //        for(Enumeration<String> keys = headers.keys();keys.hasMoreElements();){
        //            String key = keys.nextElement();
        //            log.info(" > "+key+"="+headers.get(key));
        //        }
        String path = (String) headers.get(BUNDLE_INSTALLER_HEADER);
        activated.put(bundle, path);
        if (path != null) {
            log.info(" ... process configuration within path " + path);
            ArrayList<InstallableResource> updated = new ArrayList<InstallableResource>();
            for (Enumeration<URL> resources = (Enumeration<URL>) bundle.findEntries(path, null, true); resources.hasMoreElements();) {
                InstallableResource resource = createInstallableResource(bundle, path, resources.nextElement());
                if (resource != null) {
                    updated.add(resource);
                }
            }
            installer.updateResources(PROVIDER_SCHEME, updated.toArray(new InstallableResource[updated.size()]), new String[]{});
        } else {
            log.debug("  ... no Configuration to process");
        }
    }

    /**
     * Creates an {@link InstallableResource} for {@link URL}s of files within
     * the parsed bundle.
     *
     * @param bundle the bundle containing the parsed resource
     * @param bundleResource a resource within the bundle that need to be installed
     *
     * @return the installable resource or <code>null</code> in case of an error
     */
    private InstallableResource createInstallableResource(Bundle bundle, String path, URL bundleResource) {
        //define the id
        String relPath = getInstallableResourceId(path, bundleResource);
        String name = FilenameUtils.getName(relPath);
        if (name == null || name.isEmpty()) {
            return null; //ignore directories!
        }
        InstallableResource resource;
        try {
            /*
             * Notes:
             *  - use <relativepath> as id
             *  - parse null as type to enable autodetection for configs as
             *    implemented by InternalReseouce.create(..)
             *  - we use the symbolic name and the modification date of the bundle as digest
             *  - the Dictionary will be ignored if an input stream is present
             *    so it is best to parse null
             *  - No idea how the priority is used by the Sling Installer. For
             *    now parse null than the default priority is used.
             */
            resource = new InstallableResource(relPath,
                    bundleResource.openStream(), null,
                    String.valueOf(bundle.getSymbolicName()+bundle.getLastModified()), null, null);
            log.info(" ... found installable resource " + bundleResource);
        } catch (IOException e) {
            log.error(String.format("Unable to process configuration File %s from Bundle %s",
                bundleResource, bundle.getSymbolicName()), e);
            return null;
        }
        return resource;
    }

    /**
     * @param path
     * @param bundleResource
     * @return
     */
    private String getInstallableResourceId(String path, URL bundleResource) {
        String id = bundleResource.toString();
        String relPath = id.substring(id.lastIndexOf(path) + path.length(), id.length());
        return relPath;
    }

    @SuppressWarnings("unchecked")
    private void unregister(Bundle bundle) {
        String path;
        synchronized (activated) {
            if (!activated.containsKey(bundle)) {
                return;
            }
            path = activated.remove(bundle);
        }
        //TODO: This code does not yet work correctly if the bundle is restarted
        //      and the resources need to be readded. Therefore uninstalling is
        //      currently deactivated
/*        if (path != null) {
            log.info(" ... remove configurations within path " + path);
            ArrayList<String> removedResources = new ArrayList<String>();
            for (Enumeration<URL> resources = (Enumeration<URL>) bundle.findEntries(path, null, true); resources.hasMoreElements();) {
                String installableResourceId = getInstallableResourceId(path, resources.nextElement());
                if (installableResourceId != null) {
                    log.info("  ... remove Installable Resource {}",installableResourceId);
                    removedResources.add(installableResourceId);
                }
            }
            installer.updateResources(PROVIDER_SCHEME, null, removedResources.toArray(new String[removedResources.size()]));
        } else {
            log.info("  ... no Configuration to process");
        }    */
    }

    /**
     * removes the bundle listener
     */
    public void close() {
        context.removeBundleListener(this);
    }
}
