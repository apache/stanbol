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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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
 * </code></pre><br>
 * and the bundle contains the resources: <br>
 * <pre><code>
 *     resources/bundles/10/myBundle.jar
 *     resources/config/myComponent.cfg
 *     resoruces/data/myIndex.solrondex.zip
 * </code></pre><br>
 * then the following resources will be installed:
 * <pre><code>
 *     {@value BundleInstallerConstants#PROVIDER_SCHEME}:bundles/10/myBundle.jar
 *     {@value BundleInstallerConstants#PROVIDER_SCHEME}:config/myComponent.cfg
 *     {@value BundleInstallerConstants#PROVIDER_SCHEME}:data/myIndex.solrondex.zip
 * </code></pre>
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
     * The directory used to keep the IDs of registered resources
     */
    File configDir;
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
        this.configDir = context.getDataFile(".config");
        if(configDir.exists()) {
            if(!configDir.isDirectory()){
                throw new IllegalStateException("The config directory '" 
                    + configDir.getAbsolutePath()+"' exists but is NOT a directory!");
            }
        } else {
            if(!configDir.mkdirs()){
                throw new IllegalStateException("Unable to create the config directory '" 
                        + configDir.getAbsolutePath()+"'!");
            }
        }
        //start with the assumption that the framework is active
        this.context.addBundleListener(this);
        //register the already active bundles
        registerActive(this.context);
        
    }
    /**
     * Checks if the state is not {@link BundleEvent#STOPPED}, 
     * {@link BundleEvent#STOPPING} or {@link BundleEvent#UNINSTALLED}
     * @return the state
     */
    private boolean isFrameworkActive(){
        return (context.getBundle(0).getState() & 
            (BundleEvent.STOPPED | BundleEvent.STOPPING | BundleEvent.UNINSTALLED))
            == 0;
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
        log.debug("bundleChanged(bundle:{}|state:{})",event.getBundle().getSymbolicName(),event.getType());
        Bundle source = event.getBundle();
        //if(event instanceof Framework)
        switch (event.getType()) {
            case BundleEvent.STARTED:
                register(source);
                break;
            //use uninstalled instead of stopped so that unregister is not called
            //when the OSGI environment closes
            case BundleEvent.STOPPED:
                unregister(source);
                break;

            case BundleEvent.UPDATED:
                unregister(source);
                register(source);
        }
    }
    /**
     * Registers the bundle to the {@link #activated} map.
     *
     * @param bundle the bundle to register
     */
    @SuppressWarnings("unchecked")
    private void register(Bundle bundle) {
        log.debug("register request for Bundle {}",bundle.getSymbolicName());
        synchronized (activated) {
            if(!isFrameworkActive()){
                log.debug("ignore because Framework is shutting down!");
                return;
            }
            if (activated.containsKey(bundle)) {
                log.debug("  .. already registered ");
                return;
            }
        }
        log.debug("  ... registering");
        Dictionary<String, String> headers = (Dictionary<String, String>) bundle.getHeaders();
        //        log.info("With Headers:");
        //        for(Enumeration<String> keys = headers.keys();keys.hasMoreElements();){
        //            String key = keys.nextElement();
        //            log.info(" > "+key+"="+headers.get(key));
        //        }
        String path = (String) headers.get(BUNDLE_INSTALLER_HEADER);
        activated.put(bundle, path);
        if (path != null) {
            log.info(" ... process configuration within path {} for bundle {}",path,bundle.getSymbolicName());
            Enumeration<URL> resources = (Enumeration<URL>) bundle.findEntries(path, null, true);
            if(resources != null){
                ArrayList<InstallableResource> updated = new ArrayList<InstallableResource>();
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    if(url != null){
                        log.debug("  > installable RDFTerm {}",url);
                        InstallableResource resource = createInstallableResource(bundle, path, url);
                        if (resource != null) {
                            updated.add(resource);
                        }
                    }
                }
                try {
                    storeConfig(bundle, updated);
                } catch (IOException e) {
                    throw new IllegalStateException("Unablt to save the IDs of" 
                    		+ "the resources installed for bundle '"
                            + bundle.getSymbolicName() + "'!",e);
                }
                installer.updateResources(PROVIDER_SCHEME, updated.toArray(new InstallableResource[updated.size()]), new String[]{});
            } else {
                log.warn(" ... no Entries found in path '{}' configured for Bundle '{}' with Manifest header field '{}'!",
                    new Object[]{path,bundle.getSymbolicName(),BUNDLE_INSTALLER_HEADER});
            }
        } else {
            log.debug("  ... no Configuration to process");
        }
    }
    /**
     * Used to stores/overrides the ids of installed resource for a bundle.
     * @param bundle
     * @param resources
     * @throws IOException
     */
    private void storeConfig(Bundle bundle,Collection<InstallableResource> resources) throws IOException{
        synchronized (configDir) {
            File config = new File(configDir,bundle.getBundleId()+".resources");
            if(config.exists()){
                config.delete();
            }
            FileOutputStream out = new FileOutputStream(config);
            List<String> ids = new ArrayList<String>(resources.size());
            for(InstallableResource resource : resources){
                ids.add(resource.getId());
            }
            try {
                IOUtils.writeLines(ids, null, out, "UTF-8");
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }
    /**
     * Reads the installed resources for the parsed bundle and deletes the
     * configuration afterwards.
     * @param bundle the bundle
     * @return the list of resources
     * @throws IOException if the config file was not found or on any other
     * exception while reading the file
     */
    private Collection<String> consumeConfig(Bundle bundle) throws IOException{
        synchronized (configDir) {
            File config = new File(configDir,bundle.getBundleId()+".resources");
            if(!config.exists()){
                throw new IOException("Configuration File '"+ config.getAbsolutePath()
                    + "' not found!");
            }
            FileInputStream in = new FileInputStream(config);
            try {
                return IOUtils.readLines(in, "UTF-8");
            } finally {
                IOUtils.closeQuietly(in);
                config.delete();
            }
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
        String namespace; //do not search the path within the file name!!
        int nsIndex = Math.max(id.lastIndexOf(':'), id.lastIndexOf(File.separatorChar));
        if(nsIndex > 0){
            namespace = id.substring(0,Math.min(nsIndex+1,id.length()));
        } else {
            namespace = id;
        }
        String relPath = id.substring(namespace.lastIndexOf(path) + path.length(), id.length());
        return relPath;
    }

    private void unregister(Bundle bundle) {
        log.debug("unregister request for Bundle {}",bundle.getSymbolicName());
        String path;
        synchronized (activated) {
            if(!isFrameworkActive()){
                log.debug("ignore because Framework is shutting down!");
                return;
            }
            if (!activated.containsKey(bundle)) {
                log.debug("  .. not registered ");
                return;
            }
            path = activated.remove(bundle);
        }
        if (path != null) {
            log.info(" ... remove configurations for Bundle {}", bundle.getSymbolicName());
            Collection<String> removedResources;
            try {
                removedResources = consumeConfig(bundle);
                installer.updateResources(PROVIDER_SCHEME, null, removedResources.toArray(new String[removedResources.size()]));
            } catch (IOException e) {
                log.warn("Unable to remove installed Resources for Bundle '" +
                		bundle.getSymbolicName()+ "' because an Exeption while" +
                				"reading the installed ID from the config file",e);
            }
        } else {
            log.debug("  ... no Configuration to process");
        }
    }

    /**
     * removes the bundle listener
     */
    public void close() {
        context.removeBundleListener(this);
    }
}
