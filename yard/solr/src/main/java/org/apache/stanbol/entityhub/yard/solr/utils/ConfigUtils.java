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
package org.apache.stanbol.entityhub.yard.solr.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.stanbol.entityhub.yard.solr.embedded.EmbeddedSolrPorovider;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Utility provides Methods that copy a configuration from a {@link Bundle}
 * and copy it to a directory.<p>
 * This is currently used by the {@link EmbeddedSolrPorovider} to initialise the
 * internally managed {@link EmbeddedSolrServer} and/or to add additional cores.
 * However such methods are also useful for the initialisation of an prepared
 * index. Assuming that such prepared indexes are provided as a OSGI bundle.
 * @author Rupert Westenthaler
 *
 */
public class ConfigUtils {
    /**
     * The logger
     */
    private static final Logger log = LoggerFactory.getLogger(ConfigUtils.class);
    /**
     * The name of the directoy used in the bundle to search for the default config
     */
    public static final String CONFIG_DIR = "solr/conf";
    /**
     * The name of the root directoy used within an bundle to search for all cores
     * that need to be added to an existing solr multi core configuration
     */
    public static final String CORE_CONFIG_DIR = "solr/core";
    /**
     * Initialises the default configuration for the SolrYard based on data in
     * the parsed bundle. The configuration will be copied to the parsed root
     * directory.
     * @param bundle the bundle used to load the defaultConfiguration from the
     * {@link #CONFIG_DIR} (value=" {@value #CONFIG_DIR}") directory.
     * @param rootDir the target directory for the configuration. 
     * @param override if true existing configurations are overridden.
     * @return the root directory of the solr configuration (same as parsed as rootDir)
     * @throws IOException On any IO error while coping the configuration
     * @throws NullPointerException if <code>null</code> is parsed as bundle or rootDir
     * @throws IllegalStateException If the parsed bundle is in the {@link Bundle#UNINSTALLED}
     * state, the parsed rootDir does exist but is not a directory.
     * @throws IllegalArgumentException If the parsed bundle does not contain the
     * required information to set up an configuration 
     */
    @SuppressWarnings("unchecked") //Enumeration<URL> required by OSGI specification
    public static File copyDefaultConfig(Bundle bundle, File rootDir,boolean override) throws IOException, NullPointerException, IllegalStateException, IllegalArgumentException {
        if(bundle == null){
            throw new NullPointerException("The parsed Bundle MUST NOT be NULL!");
        }
        if(rootDir == null){
            throw new NullPointerException("The parsed root directory MUST NOT be NULL!");
        }
        if(rootDir.exists() && !rootDir.isDirectory()){
            throw new IllegalStateException("The parsed root directory "+rootDir.getAbsolutePath()+" extists but is not a directory!");
        }
        log.info(String.format("Copy Default Config from Bundle %s to %s (override=%s)",
            (bundle.getSymbolicName()+bundle.getVersion()),rootDir.getAbsolutePath(),override));
        Enumeration<URL> resources = (Enumeration<URL>) bundle.findEntries(CONFIG_DIR, "*.*", true);
        //TODO: check validity of config and thorw IllegalArgumentException if not valid
        while(resources.hasMoreElements()){
            URL resource = resources.nextElement();
            copyResource(rootDir, resource, CONFIG_DIR,override);
        }
        log.debug(" ... default Configuration copied to "+rootDir.getAbsolutePath());
        return rootDir;
    }

    /**
     * Copies a resource (URL of an resource within a Bundle) to a file
     * @param rootDir the directory used as target
     * @param resource the resource URL
     * @param context the context used to search for the relative path within the URL
     * @param override if resources in the target should be overridden if they already exist
     * @throws IOException on any IO error
     */
    private static void copyResource(File rootDir, URL resource, String context, boolean override) throws IOException {
        if(!(context.charAt(context.length()-1) == '/')){
            context = context+'/';
        }
        String resourcePath = resource.toString();
        int contextPos = resourcePath.lastIndexOf(context);
        if(contextPos >=0){
            contextPos = contextPos+context.length();
        } else {
            log.warn(String.format("Context %s not found in resource %s -> ignored!",
                context,resource));
            return;
        }
        String relativePath = resourcePath.substring(contextPos);
        String[] relativePathElements = relativePath.split("/");
        File parentElement = rootDir;
        for(int i=0;i<relativePathElements.length-1;i++){
            File pathElement = new File(parentElement,relativePathElements[i]);
            if(!pathElement.exists()){
                pathElement.mkdir();
            }
            parentElement = pathElement;
        }
        File file = new File(parentElement,relativePathElements[relativePathElements.length-1]);
        boolean overrideState = false;
        if(file.exists() && override){
            FileUtils.deleteQuietly(file);
            overrideState = true;
        }
        if(!file.exists()) {
            FileUtils.copyURLToFile(resource, file);
            log.debug(String.format(" > %s %s",overrideState?"override":"copy",relativePath));
        }
    }
    /**
     * Copy the configuration of an core.
     * @param bundle The bundle used to load the core
     * @param coreDir the target directory for the core
     * @param coreName the core name or <code>null</code> to directly load the
     * configuration as present under {@value #CONFIG_DIR} in the bundle. This
     * property can be used if a bundle needs to provide multiple core
     * configurations
     * @param override if files in the target directory should be overridden
     * @throws IOException On any IO error while coping the configuration
     * @throws NullPointerException if <code>null</code> is parsed as bundle or coreDir
     * @throws IllegalStateException If the parsed bundle is in the {@link Bundle#UNINSTALLED}
     * state, the parsed coreDir does exist but is not a directory.
     * @throws IllegalArgumentException If the parsed bundle does not contain the
     * required information to set up an configuration or the parsed coreName is empty.
     */
    @SuppressWarnings("unchecked") //Enumeration<URL> required by OSGI specification
    public static void copyCore(Bundle bundle, File coreDir, String coreName, boolean override) throws IOException,NullPointerException,IllegalStateException,IllegalArgumentException{
        if(bundle == null){
            throw new NullPointerException("The parsed Bundle MUST NOT be NULL!");
        }
        if(coreDir == null){
            throw new NullPointerException("The parsed core directory MUST NOT be NULL!");
        }
        if(coreDir.exists() && !coreDir.isDirectory()){
            throw new IllegalStateException("The parsed core directory "+coreDir.getAbsolutePath()+" extists but is not a directory!");
        }
        if(coreName!= null && coreName.isEmpty()){
            throw new IllegalArgumentException("The parsed core name MUST NOT be empty (However NULL is supported)!");
        }
        String context = CORE_CONFIG_DIR+(coreName!=null?'/'+coreName:"");
        Enumeration<URL> resources = (Enumeration<URL>) bundle.findEntries(context, "*.*", true);
        while(resources.hasMoreElements()){
            URL resource = resources.nextElement();
            copyResource(coreDir, resource, context, override);
        }
    }
}
