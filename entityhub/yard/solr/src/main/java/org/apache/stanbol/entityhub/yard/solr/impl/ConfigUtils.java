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
package org.apache.stanbol.entityhub.yard.solr.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.entityhub.yard.solr.impl.install.IndexInstallerConstants;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Utility provides Methods that copy a configuration from a {@link Bundle}
 * and copy it to a directory.<p>
 * This is currently used by the {@link EmbeddedSolrPorovider} to initialise the
 * internally managed {@link EmbeddedSolrServer} and/or to add additional cores.
 * There are always two variants of the methods. The one taking a bundle as
 * parameter is supposed to be used when running within an OSGI environment. The
 * variant taking a Class object works outside of an OSGI environment.
 * @author Rupert Westenthaler
 *
 */
public final class ConfigUtils {
    private ConfigUtils(){}

    /**
     * Supported archive types.
     */
    public static final Map<String,String> SUPPORTED_SOLR_ARCHIVE_FORMAT;
    static {
        Map<String,String> cfm = new HashMap<String,String>();
        cfm.put("SOLR_INDEX_ARCHIVE_EXTENSION", "zip"); //the default if not specified
        cfm.put("gz", "gz");
        cfm.put("bz2", "bz2");
        cfm.put("zip", "zip");
        cfm.put("jar", "zip");
        cfm.put("ref", "properties"); //reference
        cfm.put("properties", "properties"); //also accept properties as references
        SUPPORTED_SOLR_ARCHIVE_FORMAT = Collections.unmodifiableMap(cfm);
    }
    /**
     * Use &lt;indexName&gt;.solrindex[.&lt;archiveType&gt;] as file name
     */
    public static final String SOLR_INDEX_ARCHIVE_EXTENSION = "solrindex";

    public static ArchiveInputStream getArchiveInputStream(String solrArchiveName,InputStream is) throws IOException {
        String archiveFormat;
        String solrArchiveExtension = FilenameUtils.getExtension(solrArchiveName);
        if(solrArchiveExtension == null || solrArchiveExtension.isEmpty()){
            archiveFormat = solrArchiveName; //assume that the archiveExtension was parsed
        } else {
            archiveFormat = SUPPORTED_SOLR_ARCHIVE_FORMAT.get(solrArchiveExtension);
        }
        ArchiveInputStream ais;
        if("zip".equals(archiveFormat)){
            ais = new ZipArchiveInputStream(is);
        } else {
            if ("gz".equals(archiveFormat)) {
                    is = new GZIPInputStream(is);
            } else if ("bz2".equals(archiveFormat)) {
                    is = new BZip2CompressorInputStream(is);
            } else {
                throw new IllegalStateException("Unsupported compression format "+archiveFormat+" " +
                        "(implementation out of sync with Constants defined in "+IndexInstallerConstants.class.getName()+"). " +
                                "Please report this to stanbol-dev mailing list!");
            }
            ais = new TarArchiveInputStream(is);
        }
        return ais;
    }
    
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
     * @return the root directory of the solr configuration (same as parsed as 
     * rootDir)
     * @throws IOException On any IO error while coping the configuration
     * @throws IllegalStateException If the parsed bundle is in the 
     * {@link Bundle#UNINSTALLED}
     * state, the parsed rootDir does exist but is not a directory.
     * @throws IllegalArgumentException If <code>null</code> is parsed as bundle 
     * or rootDir or if the parsed bundle does not contain the required 
     * information to set up an configuration 
     */
    @SuppressWarnings("unchecked") //Enumeration<URL> required by OSGI specification
    public static File copyDefaultConfig(Bundle bundle, File rootDir,boolean override) throws IOException, IllegalStateException, IllegalArgumentException {
        if(bundle == null){
            throw new IllegalArgumentException("The parsed Bundle MUST NOT be NULL!");
        }
        if(rootDir == null){
            throw new IllegalArgumentException("The parsed root directory MUST NOT be NULL!");
        }
        if(rootDir.exists() && !rootDir.isDirectory()){
            throw new IllegalStateException("The parsed root directory "+rootDir.getAbsolutePath()+" extists but is not a directory!");
        }
        log.info(String.format("Copy Default Config from Bundle %s to %s (override=%s)",
            bundle.getSymbolicName(),rootDir.getAbsolutePath(),override));
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
     * Initialises the default configuration for the SolrYard based on data in
     * the parsed bundle. The configuration will be copied to the parsed root
     * directory.
     * @param clazzInArchive This class is used to identify the archive containing
     * the default configuration. Parsing <code>null</code> causes this class to
     * be used and therefore initialises the default configuration contained by
     * the SolrYard bundle.
     * @param rootDir the target directory for the configuration. 
     * @param override if true existing configurations are overridden.
     * @return the root directory of the solr configuration (same as parsed as rootDir)
     * @throws IOException On any IO error while coping the configuration
     * @throws IllegalStateException If the parsed rootDir does exist but is not
     * a directory.
     * @throws IllegalArgumentException iIf <code>null</code> is parsed as 
     * rootDir or if the parsed bundle does not contain the required information 
     * to set up an configuration 
     */
    public static File copyDefaultConfig(Class<?> clazzInArchive,File rootDir,boolean override) throws IOException, IllegalStateException, IllegalArgumentException {
        if(rootDir == null){
            throw new IllegalArgumentException("The parsed root directory MUST NOT be NULL!");
        }
        if(rootDir.exists() && !rootDir.isDirectory()){
            throw new IllegalStateException("The parsed root directory "+rootDir.getAbsolutePath()+" extists but is not a directory!");
        }
        File sourceRoot = getSource(clazzInArchive!=null?clazzInArchive:ConfigUtils.class); 
        if(sourceRoot.isFile()){
            ZipFile archive = new ZipFile(sourceRoot);
            log.info(String.format("Copy Default Config from jar-file %s to %s (override=%s)",
                sourceRoot.getName(),rootDir.getAbsolutePath(),override));
            try {
                for(Enumeration<ZipArchiveEntry> entries = (Enumeration<ZipArchiveEntry>)archive.getEntries();entries.hasMoreElements();){
                    ZipArchiveEntry entry = entries.nextElement();
                    if(entry.getName().startsWith(CONFIG_DIR)){
                        copyResource(rootDir, archive, entry, CONFIG_DIR, override);
                    }
                }
            } finally {
                //regardless what happens we need to close the archive!
                ZipFile.closeQuietly(archive);
            }
        } else { //load from file
            File source = new File(sourceRoot,CONFIG_DIR);
            if(source.exists() && source.isDirectory()){
                FileUtils.copyDirectory(source, rootDir);
            } else {
                throw new FileNotFoundException("The SolrIndex default config was not found in directory "+source.getAbsolutePath());
            }
        }
        return rootDir;
    }
    /**
     * Uses the {@link ClassLoader} of the parsed {@link Class} instance to locate
     * the jar file the class was loaded from.
     * @param clazz the class used as context to find the jar file
     * @return the archive the parsed class was loaded from
     * @throws IOException In case the jar file can not be accessed.
     */
    private static File getSource(Class<?> clazz) throws IOException {
        String classFileName = clazz.getName().replace('.', '/')+".class";
        URL classLocation = clazz.getClassLoader().getResource(classFileName);
        String classPath;
        try {
            classPath = new File(classLocation.toURI()).getAbsolutePath();
        } catch (Exception e) {
            throw new IOException("Unable to Access Source at location "+classLocation,e);
        }
        if(classPath.indexOf('!')>0){
            return new File(classPath.substring(0,classPath.indexOf('!')));
        } else {
            return new File(classPath.substring(0,classPath.length()-classFileName.length()));
        }
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
        String resourcePath = resource.toString();
        File file = prepairCopy(resourcePath, rootDir, context);
        if(file != null){
            boolean overrideState = false;
            if(file.exists() && override){
                FileUtils.deleteQuietly(file);
                overrideState = true;
            }
            if(!file.exists()) {
                FileUtils.copyURLToFile(resource, file);
                log.debug(String.format(" > %s %s",overrideState?"override":"copy",file));
            }
        } //else can not cppy logging already provided
    }
    /**
     * Variant of the copyResource method that used an entry of an archive as
     * source.
     * @param rootDir the directory used as target
     * @param archive the archive containing the parsed entry
     * @param entry the entry to copy to the target directory
     * @param context the context used to calculate the relative path of the
     * resource within the target directory
     * @param override if an existing resource within the target directory should
     * be deleted
     * @throws IOException in case of an error while reading or writing the resource
     */
    private static void copyResource(File rootDir,ZipFile archive, ZipArchiveEntry entry,String context,boolean override) throws IOException {
        File file = prepairCopy(entry.getName(), rootDir, context);
        if(file != null){
            boolean overrideState = false;
            if(file.exists() && override){
                FileUtils.deleteQuietly(file);
                overrideState = true;
            }
            if(!file.exists()) {
                OutputStream os = null;
                InputStream is = null;
                try {
                    os = FileUtils.openOutputStream(file);
                    is = archive.getInputStream(entry);
                    IOUtils.copy(is, os);
                    log.debug(String.format(" > %s %s",overrideState?"override":"copy",file));
                } finally {
                    IOUtils.closeQuietly(is);
                    IOUtils.closeQuietly(os);
                }
            }
        } //else can not cppy logging already provided
        
    }
    
    /**
     * Prepares the copying of a resource. The context is used to determine the
     * relative path of the resource. Than missing sub-directories are created
     * in the target directory. Finally the file instance representing this 
     * resource within the target directory is returned. 
     * @param resource The path to the resource. This need not be the full path.
     * It must only be ensured that the parsed context is contained. e.g. the
     * relative path of a resource within an archive provides enough context for
     * this method to work
     * @param targetDir the target directory
     * @param context the context to determine the relative path of the resource
     * within the target directory. The context MUST be part of the parsed
     * resource name. Otherwise this method will return <code>null</code>
     * @return the file representing the resource within the target directory.
     * In cases the context can not be found in the parsed resource this method 
     * returns <code>null</code>
     */
    private static File prepairCopy(String resource, File targetDir, String context) {
        if(!(context.charAt(context.length()-1) == '/')){
            context = context+'/';
        }
        int contextPos = resource.lastIndexOf(context);
        if(contextPos >=0){
            contextPos = contextPos+context.length();
        } else {
            log.warn(String.format("Context %s not found in resource %s -> ignored!",
                context,resource));
            return null;
        }
        String relativePath = resource.substring(contextPos);
        String[] relativePathElements = relativePath.split("/");
        File parentElement = targetDir;
        for(int i=0;i<relativePathElements.length-1;i++){
            File pathElement = new File(parentElement,relativePathElements[i]);
            if(!pathElement.exists()){
                pathElement.mkdir();
            }
            parentElement = pathElement;
        }
        File file = new File(parentElement,relativePathElements[relativePathElements.length-1]);
        return file;
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
     * @throws IllegalStateException If the parsed bundle is in the {@link Bundle#UNINSTALLED}
     * state, the parsed coreDir does exist but is not a directory.
     * @throws IllegalArgumentException if <code>null</code> is parsed as bundle or coreDir
     * or if the parsed bundle does not contain the required information to set 
     * up an configuration or the parsed coreName is empty.
     */
    @SuppressWarnings("unchecked") //Enumeration<URL> required by OSGI specification
    public static void copyCore(Bundle bundle, File coreDir, String coreName, boolean override) throws IOException,IllegalStateException,IllegalArgumentException{
        if(bundle == null){
            throw new IllegalArgumentException("The parsed Bundle MUST NOT be NULL!");
        }
        if(coreDir == null){
            throw new IllegalArgumentException("The parsed core directory MUST NOT be NULL!");
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
    /**
     * Copies a core from the parsed archive input stream to the target location
     * @param ais The input stream of the archive (not closed by this method)
     * @param coreDir the directory for the core
     * @param coreName the name of the core (used as context when reading relative
     * paths from the archive
     * @param override if existing files should be overridden
     * @throws IOException On any error while accessing the data of the archive
     * @throws IllegalArgumentException if any of the parameter is <code>null</code>
     * or if the coreDir exists but is not an directory or if the core name is
     * empty
     */
    public static void copyCore(ArchiveInputStream ais, File coreDir,String coreName,boolean override) throws IOException{
        if(ais == null){
            throw new IllegalArgumentException("The parsed ArchiveInputStream MUST NOT be NULL!");
        }
        if(coreDir == null){
            throw new IllegalArgumentException("The parsed core directory MUST NOT be NULL!");
        }
        if(coreDir.exists() && !coreDir.isDirectory()){
            throw new IllegalStateException("The parsed core directory "+coreDir.getAbsolutePath()+" extists but is not a directory!");
        }
        if(coreName== null || coreName.isEmpty()){
            throw new IllegalArgumentException("The parsed core name MUST NOT be NULL or empty!");
        }
        ArchiveEntry entry;
        while((entry = ais.getNextEntry())!=null){
            if(!entry.isDirectory()){
                copyArchiveEntry(ais, entry, coreDir, coreName, override);
                /*
                 * NOTE: Here we use the coreName as context (last argument to 
                 * prepairCopy(..)). This ensures that it matter if the archive 
                 * contains the data directly in the root or within an folder 
                 * with the name of the core.
                 */
            } //else - directories are created automatically and empty directories are not needed
        }
    }
    /**
     * Copy an Entry of an Archive to the target (File) within the Core Directory
     * @param ais the ArchiveInputStream
     * @param entry The Entry to copy
     * @param coreDir the root directory
     * @param context the context used to calculate the relative path of the
     * resource within the target directory
     * @param override if an existing resource within the target directory should
     * be deleted
     * @throws IOException in case of an error while reading or writing the resource
     */
    private static void copyArchiveEntry(ArchiveInputStream ais, ArchiveEntry entry, File coreDir, String context, boolean override) throws IOException {
        File file = prepairCopy(entry.getName(), coreDir, context);
        if(file != null){
            boolean overrideState = false;
            if(file.exists() && override){
                FileUtils.deleteQuietly(file);
                overrideState = true;
            }
            if(!file.exists()) {
                OutputStream os = null;
                try {
                    os= FileUtils.openOutputStream(file);
                    IOUtils.copy(ais, os);
                    log.debug(String.format(" > %s %s",overrideState?"override":"copy",file));
                }finally {
                    IOUtils.closeQuietly(os);
                }
            }
        } //else can not cppy logging already provided
    }
    /**
     * Copy the configuration of an core.
     * @param clazzInArchive This class is used to identify the archive containing
     * the default configuration. Parsing <code>null</code> causes this class to
     * be used and therefore initialises the default core configuration contained by
     * the SolrYard bundle.
     * @param coreDir the target directory for the core
     * @param coreName the core name or <code>null</code> to directly load the
     * configuration as present under {@value #CONFIG_DIR} in the bundle. This
     * property can be used if a bundle needs to provide multiple core
     * configurations
     * @param override if files in the target directory should be overridden
     * @throws IOException On any IO error while coping the configuration
     * @throws IllegalStateException If the parsed coreDir does exist but is not
     * a directory.
     * @throws IllegalArgumentException if <code>null</code> is parsed as coreDir
     * or if the parsed bundle does not contain the required information to set 
     * up an configuration or the parsed coreName is empty.
     */
    public static void copyCore(Class<?> clazzInArchive, File coreDir, String coreName, boolean override) throws IOException, IllegalArgumentException, IllegalStateException{
        if(coreDir == null){
            throw new IllegalArgumentException("The parsed core directory MUST NOT be NULL!");
        }
        if(coreDir.exists() && !coreDir.isDirectory()){
            throw new IllegalStateException("The parsed core directory "+coreDir.getAbsolutePath()+" extists but is not a directory!");
        }
        if(coreName!= null && coreName.isEmpty()){
            throw new IllegalArgumentException("The parsed core name MUST NOT be empty (However NULL is supported)!");
        }
        String context = CORE_CONFIG_DIR+(coreName!=null?'/'+coreName:"");
        File sourceRoot = getSource(clazzInArchive!=null?clazzInArchive:ConfigUtils.class);
        if(sourceRoot.isFile()){
            ZipFile archive = new ZipFile(sourceRoot);
            log.info(String.format("Copy core %s config from jar-file %s to %s (override=%s)",
                (coreName == null?"":coreName),sourceRoot.getName(),coreDir.getAbsolutePath(),override));
            try {
                for(Enumeration<ZipArchiveEntry> entries = (Enumeration<ZipArchiveEntry>)archive.getEntries();entries.hasMoreElements();){
                    ZipArchiveEntry entry = entries.nextElement();
                    if(entry.getName().startsWith(context)){
                        copyResource(coreDir, archive, entry, context, override);
                    }
                }
            } finally {
                //regardless what happens we need to close the archive!
                ZipFile.closeQuietly(archive);
            }
        } else { //load from file
            File source = new File(sourceRoot,context);
            if(source.exists() && source.isDirectory()){
                FileUtils.copyDirectory(source, coreDir);
            } else {
                throw new FileNotFoundException("The SolrIndex default config was not found in directory "+source.getAbsolutePath());
            }
        }
    }
    /**
     * Converts a parsed String to a File instance. The parsed string can be
     * formatted as file URL or as path
     * @param uriOrPath the file location as URL or path
     * @return the File
     */
    public static File toFile(String uriOrPath){
        File file = null;
        try {
            URI fileUri = new URI(uriOrPath);
            file = new File(fileUri);
        } catch (URISyntaxException e) {
            //not an URI -> ignore
        } catch (IllegalArgumentException e){
            //this happens if it is a URI but can not be converted to a file
            //still we should try to work with the parsed file ...
        }
        if(file == null){
            file = new File(uriOrPath);
        }
        return file;
    }
    
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * Methods for storing and loading configurations for uninitialised indexes
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */
    
    /**
     * The path to the directory used to store properties of uninitialised
     * referenced sites.<p>
     * Such sites are one that are created by using 
     * {@link #createSolrDirectory(String, String, Map)} but the
     * {@link DataFileProvider} does not yet provide the necessary data to
     * initialise the index.<p>
     * This directory will store properties files with the indexName as name,
     * properties as extension and the properties as value
     */
    private static final String UNINITIALISED_INDEX_DIRECTORY = "config/uninitialised-index";
    /**
     * Saves the configuration of an uninitialised index
     * @param context the context used to get the data storage
     * @param indexName the name of the uninitialised index
     * @param properties the properties of the uninitialised index
     * @throws IOException on any error while saving the configuration
     */
    public static void saveUninitialisedIndexConfig(ComponentContext context,String indexName,java.util.Properties properties) throws IOException {
        File uninstalledConfigDir = getUninitialisedSiteDirectory(context,true);
        File config = new File(uninstalledConfigDir,indexName+'.'+ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION+".ref");
        FileOutputStream out = null;
        if(properties == null){ //if no config is provided
            properties = new java.util.Properties();//save an empty one
        }
        try {
            out = new FileOutputStream(config);
            properties.store(out,null);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Returns the directory used to store the configurations of uninitialised
     * Solr Indexes
     * @param init if <code>true</code> the directory is created if needed
     * @return the directory 
     */
    public static File getUninitialisedSiteDirectory(ComponentContext context,boolean init) {
        if(context == null){
            throw new IllegalStateException("This functionality only works when running within an OSGI Environment");
        }
        File uninstalledConfigDir = context.getBundleContext().getDataFile(UNINITIALISED_INDEX_DIRECTORY);
        if(!uninstalledConfigDir.exists()){
            if(init){
                if(!uninstalledConfigDir.mkdirs()){
                    throw new IllegalStateException("Unable to create Directory "+
                        UNINITIALISED_INDEX_DIRECTORY+"for storing information of uninitialised Solr Indexes");
                }
            }
        } else if(!uninstalledConfigDir.isDirectory()){
            throw new IllegalStateException("The directory "+UNINITIALISED_INDEX_DIRECTORY+
                "for storing uninitialised Solr Indexes Information exists" +
                    "but is not a directory!");
        } //else -> it exists and is a dir -> nothing todo
        return uninstalledConfigDir;
    }
    /**
     * Loads the configurations of uninitialised Solr Indexes
     * @return the map with the index name as key and the properties as values
     * @throws IOException on any error while loading the configurations
     */
    public static Map<String,java.util.Properties> loadUninitialisedIndexConfigs(ComponentContext context) throws IOException {
        File uninstalledConfigDir = getUninitialisedSiteDirectory(context,false);
        Map<String,java.util.Properties> configs = new HashMap<String,java.util.Properties>();
        if(uninstalledConfigDir.exists()){
            for(String file : uninstalledConfigDir.list(new SuffixFileFilter(ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION+".ref"))){
                String indexName = file.substring(0,file.indexOf('.'));
                java.util.Properties props = new java.util.Properties();
                InputStream is = null;
                try {
                    is = new FileInputStream(new File(uninstalledConfigDir,file));
                    props.load(is);
                    configs.put(indexName, props);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }
        return configs;
    }
    /**
     * Removes the configuration for the index with the parsed name form the
     * list if uninitialised indexes
     * @param indexName the name of the index
     * @return if the file was deleted.
     */
    public static boolean removeUninitialisedIndexConfig(ComponentContext context,String indexName){
        File configFile = new File(getUninitialisedSiteDirectory(context,false),
            indexName+'.'+ConfigUtils.SOLR_INDEX_ARCHIVE_EXTENSION+".ref");
        return configFile.delete();
    }
}
