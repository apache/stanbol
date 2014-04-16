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
package org.apache.stanbol.entityhub.indexing.core.destination;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.stanbol.entityhub.indexing.core.config.IndexingConfig;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.CacheStrategy;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;

public final class OsgiConfigurationUtil {
    
    private OsgiConfigurationUtil() {/* Do not create instances of Util classes*/}
    
    private static final Logger log = LoggerFactory.getLogger(OsgiConfigurationUtil.class);
    
    public static final String DEFAULT_MAPPING_STATE = "proposed";
    public static final String DEFAULT_IMPORTED_ENTTIY_STATE = "proposed";
    public static final Object DEFAULT_EXPIRE_DURATION = Integer.valueOf(0);
    
    public static final String REFERENCED_SITE_COMPONENT_ID = "org.apache.stanbol.entityhub.site.referencedSite";
    public static final String CACHE_COMPONENT_ID = "org.apache.stanbol.entityhub.core.site.CacheImpl";
    
    private static final String CONFIG_ROOT = "config";
    private static final String[] CONFIG_PATH_ELEMENTS = new String[]{"org","apache","stanbol","data","site"};
    private static final String CONFIG_PATH;
    private static final String CONFIG_PACKAGE;
    static {
        StringBuilder path = new StringBuilder();
        StringBuilder java = new StringBuilder();
        for(String part : CONFIG_PATH_ELEMENTS){
            path.append(part);
            java.append(part);
            path.append(File.separatorChar);
            java.append('.');
        }
        CONFIG_PATH = path.toString();
        CONFIG_PACKAGE = java.toString();
    }
    
    public static void writeOsgiConfig(IndexingConfig indexingConfig,String name, Dictionary<String,Object> config) throws IOException{
        if(indexingConfig == null){
            throw new IllegalArgumentException("The parsed IndexingConfiguration MUST NOT be NULL");
        }
        if(name == null){
            throw new IllegalArgumentException("The parsed file name MUST NOT be NULL");
        }
        if(config == null){
            throw new IllegalArgumentException("The parsed configuration MUST NOT be NULL");
        }
        if(config.isEmpty()){
            throw new IllegalArgumentException("The parsed configuration MUST NOT be empty");
        }
        File configFile = new File(getConfigDirectory(indexingConfig),name);
        ConfigurationHandler.write(FileUtils.openOutputStream(configFile), config);
    }
    
    private static Dictionary<String,Object> createSiteConfig(IndexingConfig indexingConfig){
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        //basic properties
        //we use the name as ID
        config.put(SiteConfiguration.ID, indexingConfig.getName());
        //also set the id as name
        config.put(SiteConfiguration.NAME, indexingConfig.getName());
        //config.put(SiteConfiguration.NAME, indexingConfig.getName());
        if(indexingConfig.getDescription() != null && !indexingConfig.getDescription().isEmpty()){
            config.put(SiteConfiguration.DESCRIPTION, indexingConfig.getDescription());
        }
        //the cache
        //name the Cache is the same as for the Yard.
        config.put(ReferencedSiteConfiguration.CACHE_ID, getYardID(indexingConfig));
        config.put(ReferencedSiteConfiguration.CACHE_STRATEGY, CacheStrategy.all);
        //Entity Dereferencer (optional)
        if(addProperty(ReferencedSiteConfiguration.ACCESS_URI, config, indexingConfig)){
            addProperty(ReferencedSiteConfiguration.ENTITY_DEREFERENCER_TYPE, config, indexingConfig,
                "Referenced Site for " + indexingConfig.getName() +
                " (including a full local Cache)");
        }
        //Entity Searcher (optional)
        if(addProperty(ReferencedSiteConfiguration.QUERY_URI, config, indexingConfig)){
            addProperty(ReferencedSiteConfiguration.ENTITY_SEARCHER_TYPE, config, indexingConfig);
        }
        //General Properties
        addProperty(SiteConfiguration.DEFAULT_EXPIRE_DURATION, config, indexingConfig,DEFAULT_EXPIRE_DURATION);
        addProperty(SiteConfiguration.DEFAULT_MAPPING_STATE, config, indexingConfig,DEFAULT_MAPPING_STATE);
        addProperty(SiteConfiguration.DEFAULT_SYMBOL_STATE, config, indexingConfig,DEFAULT_IMPORTED_ENTTIY_STATE);
        //the entity prefix is optional and may be an array
        addPropertyValues(SiteConfiguration.ENTITY_PREFIX, config, indexingConfig);
        //add the Field Mappings when entities of this Site are imported to the
        //entityhub. This may be the same mappings as used for the Cache however
        //they may be also different.
        Object value = indexingConfig.getProperty(SiteConfiguration.SITE_FIELD_MAPPINGS);
        if(value != null){
            File fieldMappingConfig = indexingConfig.getConfigFile(value.toString());
            if(fieldMappingConfig != null){
                try {
                    config.put(SiteConfiguration.SITE_FIELD_MAPPINGS, 
                        FileUtils.readLines(fieldMappingConfig, "UTF-8"));
                } catch (IOException e) {
                    log.warn(String.format("Unable to read Field Mappings for Referenced Site " +
                    		"configuration"),e);
                }
            } else {
                log.warn("Unable to load configured Field Mappings for Reference Site " +
                		"{}={}",SiteConfiguration.SITE_FIELD_MAPPINGS,value);
            }
        }
        //set other optional properties
        addProperty(SiteConfiguration.SITE_ATTRIBUTION, config, indexingConfig);
        addProperty(SiteConfiguration.SITE_ATTRIBUTION_URL, config, indexingConfig);
        addPropertyValues(SiteConfiguration.SITE_LICENCE_NAME, config, indexingConfig);
        addPropertyValues(SiteConfiguration.SITE_LICENCE_TEXT, config, indexingConfig);
        addPropertyValues(SiteConfiguration.SITE_LICENCE_URL, config, indexingConfig);
        return config;
    }
    
    private static Dictionary<String,Object> createCacheConfig(IndexingConfig indexingConfig){
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        //a cache needs to provide the ID of the Yard
        String yardId = getYardID(indexingConfig);
        config.put(Cache.ID, yardId);
        config.put(Cache.NAME, indexingConfig.getName()+" Cache");
        config.put(Cache.DESCRIPTION, "Cache for the "+indexingConfig.getName()+
            " Referenced Site using the "+yardId+".");
        config.put(Cache.CACHE_YARD, getYardID(indexingConfig));
        //additinal Mappings:
        // This can be used to define what information are store to the cache
        // if an Entity is updated from a remote site.
        // If not present the mappings used by the Yard are used. This default
        // is sufficient for full indexes as created by the indexing utils
        // therefore we need not to deal with additional mappings here
        return config;
    }
    /**
     * Adds the configurations as defined by the Yard Interface. Configurations
     * of specific Yard implementations might need to add additional 
     * parameters. <p>
     * This also ensures that the ID of the Yard is the same as referenced by the
     * configurations for the referenced site and the cache.
     * @param indexingConfig
     * @return
     */
    public static Dictionary<String,Object> createYardConfig(IndexingConfig indexingConfig){
        Dictionary<String,Object> config = new Hashtable<String,Object>();
        config.put(Yard.ID, getYardID(indexingConfig));
        config.put(Yard.NAME, indexingConfig.getName()+" Index");
        config.put(Yard.DESCRIPTION,"Full local index for the Referenced Site \""+indexingConfig.getName()+"\".");
        return config;
    }    
    public static void writeSiteConfiguration(IndexingConfig indexingConfig) throws IOException {
        String siteConfigFileName = REFERENCED_SITE_COMPONENT_ID + 
            "-" + indexingConfig.getName()+".config";
        writeOsgiConfig(indexingConfig,siteConfigFileName, createSiteConfig(indexingConfig));
    }
    
    public static void writeCacheConfiguration(IndexingConfig indexingConfig) throws IOException {
        String cacheFileName = CACHE_COMPONENT_ID + 
            "-" + indexingConfig.getName()+".config";
        writeOsgiConfig(indexingConfig,cacheFileName, createCacheConfig(indexingConfig));
    }
    
    /**
     * Getter for default ID of the yard based on the value of 
     * {@link IndexingConfig#getName()}
     * @param config the IndexingConfig
     * @return the default ID of the yard based on the value of 
     * {@link IndexingConfig#getName()}
     */
    public static String getYardID(IndexingConfig config){
        return config.getName()+"Index";
    }
    
    private static boolean addPropertyValues(String key, Dictionary<String,Object> config, IndexingConfig indexingConfig){
        Object value = indexingConfig.getProperty(key);
        if(value != null && !value.toString().isEmpty()) {
            config.put(key, value.toString().split(";"));
            return true;
        } else {
            return false;
        }
    }
    
    private static boolean addProperty(String key, Dictionary<String,Object> config, IndexingConfig indexingConfig){
        return addProperty(key, config, indexingConfig, null);
    }
    private static boolean addProperty(String key, Dictionary<String,Object> config, IndexingConfig indexingConfig,Object defaultValue){
        Object value = indexingConfig.getProperty(key);
        if(value != null || defaultValue != null){
            config.put(key, value != null ? value : defaultValue);
            return true;
        } else {
            return false;
        }
    }
    /**
     * Getter for the Directory that need to contain all Files to be included
     * in the OSGI Bundle.
     * @param config the indexing configuration
     * @return the directory (created if not already existing)
     * @throws IOException If the directory could not be created
     */
    public static File getConfigDirectory(IndexingConfig config) throws IOException{
        File configRoot = new File(config.getDestinationFolder(),CONFIG_ROOT);
        File siteConfigDir = new File(configRoot,CONFIG_PATH+config.getName().toLowerCase());
        if(!siteConfigDir.isDirectory()){
            if(!siteConfigDir.mkdirs()){
                throw new IOException("Unable to create config Directory "+siteConfigDir);
            }
        }
        return siteConfigDir;
    }
    
    public static void createBundle(IndexingConfig config){
        Builder builder = new Builder();
        builder.setProperty("Install-Path",
            FilenameUtils.separatorsToUnix(CONFIG_PATH) //see STANBOL-768
                + config.getName().toLowerCase());
        builder.setProperty(Builder.EXPORT_PACKAGE,CONFIG_PACKAGE+config.getName().toLowerCase());
        builder.setProperty(Builder.BUNDLE_CATEGORY, "Stanbol Data");
        builder.setProperty(Builder.BUNDLE_NAME, "Apache Stanbol Data: "+config.getName());
        builder.setProperty(Builder.CREATED_BY, "Apache Stanbol Entityhub Indexing Utils");
        builder.setProperty(Builder.BUNDLE_VENDOR, "Apache Stanbol (Incubating)");//TODO make configureable
        builder.setProperty(Builder.BUNDLE_VERSION, "1.0.0");
        builder.setProperty(Builder.BUNDLE_DESCRIPTION, "Bundle created for import of the referenced site "
            + config.getName() +" into the Apache Stanbol Entityhub");
        builder.setProperty(Builder.BUNDLE_SYMBOLICNAME, CONFIG_PACKAGE+config.getName().toLowerCase());
        try {
            builder.addClasspath(new File(config.getDestinationFolder(),CONFIG_ROOT));
        } catch (IOException e) {
            log.warn("(builder.addClasspath) Unable to build OSGI Bundle for Indexed Referenced Site "+config.getName(),e);
            builder.close();
            //?? why not throwing an exception here ??
            return;
        }
        try {
            Jar jar = builder.build();
            jar.write(new File(config.getDistributionFolder(),
                CONFIG_PACKAGE+config.getName()+"-1.0.0.jar"));
        } catch (Exception e) {
            log.warn("(builder.build) Unable to build OSGI Bundle for Indexed Referenced Site "+config.getName(),e);
            //?? why not throwing an exception here ??
			return;
		} finally {
			builder.close();
		}
    }
}
