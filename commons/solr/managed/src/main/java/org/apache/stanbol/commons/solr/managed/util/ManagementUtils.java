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
package org.apache.stanbol.commons.solr.managed.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.FileNameUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.commons.solr.SolrConstants;
import org.apache.stanbol.commons.solr.managed.IndexMetadata;
import org.apache.stanbol.commons.solr.managed.ManagedIndexState;
import org.apache.stanbol.commons.solr.utils.ConfigUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManagementUtils {

    /**
     * Private constructor to restrict instantiation
     */
    private ManagementUtils() {}

   /**
     * The logger
     */
    private static final Logger log = LoggerFactory.getLogger(ManagementUtils.class);
    /**
     * Substitutes ${property.name} with the values retrieved via <ul>
     * <li> {@link BundleContext#getProperty(String)} or
     * <li> {@link System#getProperty(String, String)} if the parsed
     * {@link BundleContext} is <code>null</code>
     * </ul>
     * Substitutes with an empty string if the property is not present. If
     * the substitution does not end with {@link File#separatorChar}, than it is
     * appended to allow easily creating paths relative to root directory available
     * as property regardless if the property includes/excludes the final
     * separator char.
     * <p>
     * Nested substitutions are NOT supported. However multiple substitutions are supported.
     * <p>
     * If someone knows a default implementation feel free to replace!
     * 
     * @param value
     *            the value to substitute
     * @param bundleContext
     *            If not <code>null</code> the {@link BundleContext#getProperty(String)} is used instead of
     *            the {@link System#getProperty(String)}. By that it is possible to use OSGI only properties
     *            for substitution.
     * @return the substituted value
     */
    public static String substituteProperty(String value, BundleContext bundleContext) {
        int prevAt = 0;
        int foundAt = 0;
        StringBuilder substitution = new StringBuilder();
        while ((foundAt = value.indexOf("${", prevAt)) >= prevAt) {
            substitution.append(value.substring(prevAt, foundAt));
            String propertyName = value.substring(foundAt + 2, value.indexOf('}', foundAt));
            String propertyValue = bundleContext == null ? // if no bundleContext is available
            System.getProperty(propertyName) : // use the System properties
                    bundleContext.getProperty(propertyName);
            if(propertyValue != null) {
                substitution.append(propertyValue);
                if(propertyValue.charAt(propertyValue.length()-1) != File.separatorChar){
                    substitution.append(File.separatorChar);
                }
            } //else nothing to append
            prevAt = foundAt + propertyName.length() + 3; // +3 -> "${}".length
        }
        substitution.append(value.substring(prevAt, value.length()));
        return substitution.toString();
    }
    /**
     * An instance of the {@link ArchiveStreamFactory}
     */
    public static final  ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
    /**
     * An instance of the compressor stream factory
     */
    public static final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
    /**
     * Tries to create an {@link ArchiveInputStream} based on the parsed {@link InputStream}.
     * First the provided resource name is used to detect the type of the archive.
     * if that does not work, or the parsed resource name is <code>null</code> the
     * stream is created by using the auto-detection of the archive type.
     * @param resourceName the name of the resource or <code>null</code>
     * @param is the {@link InputStream}
     * @return the {@link ArchiveInputStream}
     * @throws ArchiveException if the {@link InputStream} does not represented any
     * supported Archive type
     */
    public static ArchiveInputStream getArchiveInputStream(String resourceName, InputStream is) throws ArchiveException{
        if(is == null){
            return null;
        }
        String extension = resourceName == null ? null : 
            FilenameUtils.getExtension(resourceName);
        if(!is.markSupported()){
            is = new BufferedInputStream(is);
        }
        InputStream as;
        if(!"zip".equalsIgnoreCase(extension)){ //if not a zip file (the default)
            //we need to first check if this is a compressed stream
            try {
                as =  compressorStreamFactory.createCompressorInputStream(extension,is);
                extension = "tar"; // assume tar archives
            } catch (CompressorException e) {
                try {
                    as = compressorStreamFactory.createCompressorInputStream(is);
                    extension = "tar"; // assume tar archives
                } catch (CompressorException e1) {
                    //not a compression stream?
                    as = is;
                }
            }
        } else { //zip ... this is already an archive stream
            as = is;
        }
        if(extension != null){
            try {
                return archiveStreamFactory.createArchiveInputStream(extension, as);
            } catch (ArchiveException e) {
                //ignore
            }
        }
        //try to detect
        return archiveStreamFactory.createArchiveInputStream(is);
    }
    /**
     * Getter for the name of the index within the current 
     * {@link IndexMetadata#getArchive() archive} set to load the index data
     * from. If no archive is set (e.g. if the {@link ArchiveInputStream} was
     * directly parsed, than the {@link IndexMetadata#getIndexName() index name}
     * directly is used as default.
     * @param metadata the {@link IndexMetadata}
     * @return the name of the index within the indexArchive used to load the
     * data from. In other words the relative path to the index data within the
     * index archive.
     */
    public static String getArchiveCoreName(final IndexMetadata metadata) {
        String name = metadata.getIndexName();
        String archiveCoreName = metadata.getArchive();
        if(archiveCoreName == null){
            archiveCoreName = name;
        } else {
            //the name of the core in the archive MUST BE the same as
            //the name of the archive excluding .solrindex.{archive-format}
            int split = archiveCoreName.indexOf('.');
            if(split>0){
                archiveCoreName = archiveCoreName.substring(0,split);
            }
        }
        return archiveCoreName;
    }
//    /**
//     * Parses the name of the Core from an IndexReference (file url, file path,
//     * index name or server:indexname)
//     * @param indexRef the parsed indexRef
//     * @return
//     */
//    public static String getCoreNameForIndexRef(String indexRef,String serverName) {
//        
//        String[] parsedRef = ConfigUtils.parseSolrServerReference(indexRef);
//        String coreName;
//        if(parsedRef[0] != null && !parsedRef[0].equals(serverName)){
//            coreName = null; //other server
//        } else {
//            coreName = parsedRef[1];
//            if(coreName == null || coreName.isEmpty()){
//                log.warn("The parsed index reference '"+indexRef+"' does not define a valid core name!");
//            }
//        }
//        return coreName;
//    }
    /**
     * Creates and initialises a {@link IndexMetadata} instance based on the
     * parsed {@link SolrCore}
     * @param core the {@link SolrCore}
     * @param serverName the name of the server
     * @return the initialised {@link IndexMetadata}
     */
    public static IndexMetadata getMetadata(SolrCore core, String serverName){
        if(core == null){
            return null;
        }
        IndexMetadata metadata = new IndexMetadata();
        if(serverName != null){
            metadata.setServerName(serverName);
        }
        metadata.setSynchronized(false);
        updateMetadata(metadata, core);
        return metadata;
    }
    /**
     * Updates the parsed {@link IndexMetadata} instance based on the
     * properties of the parsed {@link SolrCore}.<p>
     * This sets the state, index name and the directory.
     * @param metadata the {@link IndexMetadata} to update
     * @param core the core
     */
    public static void updateMetadata(IndexMetadata metadata, SolrCore core){
        if(metadata == null || core == null){
            return;
        }
        metadata.setState(ManagedIndexState.ACTIVE);
        metadata.setIndexName(core.getName());
        metadata.setDirectory(core.getCoreDescriptor().getInstanceDir());
    }
    /**
     * Updates the parsed metadata based on the properties of the 
     * {@link ServiceReference}.<p>
     * This updates the index name, server name, and the directory value based
     * on the corresponding keys as defined in {@link SolrConstants}
     * @param metadata the metadata to update
     * @param coreRef the ServiceReference used to update the metadata
     */
    public static void updateMetadata(IndexMetadata metadata, ServiceReference coreRef){
        if(metadata == null || coreRef == null){
            return;
        }
//        if(SolrCore.class.getName().equals(coreRef.getProperty(Constants.OBJECTCLASS))){
        String value = (String)coreRef.getProperty(SolrConstants.PROPERTY_CORE_DIR);
        if(value != null){
            metadata.setDirectory(value);
        }
        value = (String) coreRef.getProperty(SolrConstants.PROPERTY_CORE_NAME);
        if(value != null){
            metadata.setIndexName(value);
        }
        value = (String) coreRef.getProperty(SolrConstants.PROPERTY_SERVER_NAME);
        if(value != null){
            metadata.setServerName(value);
        }
//        } //else parsed service Reference does not refer to a SolrCore
    }
}
