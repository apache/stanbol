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
package org.apache.stanbol.entityhub.indexing.core.config;

import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_DESCRIPTION;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_ENTITY_DATA_ITERABLE;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_ENTITY_DATA_PROVIDER;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_ENTITY_ID_ITERATOR;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_ENTITY_POST_PROCESSOR;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_ENTITY_PROCESSOR;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_ENTITY_SCORE_PROVIDER;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_INDEXING_DESTINATION;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_INDEX_FIELD_CONFIG;
import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.KEY_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.namespaceprefix.service.StanbolNamespacePrefixService;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.indexing.core.normaliser.DefaultNormaliser;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexingConfig {
    private static final String DEFAULT_ROOT_PATH = "indexing";
    private static final String CONFIG_FOLDER = "config";
    private static final String CONFIG_PATH = DEFAULT_ROOT_PATH+File.separatorChar+CONFIG_FOLDER;
    private static final String SOURCE_FOLDER = "resources";
    private static final String SOURCE_PATH = DEFAULT_ROOT_PATH+File.separatorChar+SOURCE_FOLDER;
    private static final String DESTINATION_FOLDER = "destination";
    private static final String DISTRIBUTION_FOLDER = "dist";
    private static final String INDEXING_PROPERTIES = "indexing.properties";
    private static final String CONFIG_PARAM = "config";
    public static final String KEY_INDEXING_CONFIG = "indexingConfig";
    
    /**
     * Internally used to explain the syntax in the configuration file to parse parameters
     */
    private static final String SYNTAX_ERROR_MESSAGE = "{key}={value1},{param1}:{value1},{param2}:{value2};{value2},{param1}:{value1} ...";
    
    private static final Logger log = LoggerFactory.getLogger(IndexingConfig.class);
    private static final String DEFAULT_INDEX_FIELD_CONFIG_FILE_NAME = "indexFieldConfig.txt";
    
    public static final String DEFAULT_INDEXED_ENTITIES_ID_FILE_NAME = "indexed-entities-ids.zip";
    
    /**
     * This stores the context within the classpath to initialise missing
     * configurations and source based on the defaults in the classpath.
     * This might be a directory or an jar file. 
     * @see {@link #loadViaClasspath(String)}
     * @see #getConfigClasspathRootFolder()
     */
    private final File classPathRootDir;
    
    /**
     * The root directory for the indexing (defaults to {@link #DEFAULT_ROOT_PATH})
     */
    private final File rootDir;
//    /**
//     * The root directory for the configuration
//     */
//    private final File configDir;
//    /**
//     * The root directory for the resources (indexing source files)
//     */
//    private final File sourceDir;
//    /**
//     * The root directory for the files created during the indexing process
//     */
//    private final File destinationDir;
//    /**
//     * The root directory for the distribution files created in the finalisation
//     * phase of the indexing (e.g. The archive with the index,
//     * OSGI configuration, ...)
//     */
//    private final File distributionDir;
//    
//    /**
//     * Map between the relative paths stored in {@link #rootDir}, {@link #configDir},
//     * {@link #sourceDir}, {@link #destinationDir} and {@link #distributionDir}
//     * to the {@link File#getCanonicalFile()} counterparts as returned by the
//     * {@link #getRootFolder()} ... methods.
//     */
//    private final Map<File,File> canonicalDirs = new HashMap<File,File>();
    
    /**
     * The main indexing configuration as parsed form {@link #INDEXING_PROPERTIES}
     * file within the {@link #configDir}.
     */
    private final Map<String,Object> configuration;
    
    /**
     * The value of the {@link IndexingConstants#KEY_NAME} property
     */
    private String name;
    /**
     * The {@link EntityDataIterable} instance initialised based on the value
     * of the {@link IndexingConstants#KEY_ENTITY_DATA_ITERABLE} key or
     * <code>null</code> if not configured.
     * This variable uses lazy initialisation
     * @see #getDataIterable()
     */
    private EntityDataIterable entityDataIterable = null;
    /**
     * The {@link EntityDataProvider} instance initialised based on the value
     * of the {@link IndexingConstants#KEY_ENTITY_DATA_PROVIDER} key or
     * <code>null</code> if not configured.
     * This variable uses lazy initialisation
     * @see #getEntityDataProvider()
     */
    private EntityDataProvider entityDataProvider = null;

    /**
     * The {@link EntityIterator} instance initialised based on the value
     * of the {@link IndexingConstants#KEY_ENTITY_ID_ITERATOR} key or
     * <code>null</code> if not configured.
     * This variable uses lazy initialisation
     * @see #getEntityIdIterator()
     */
    private EntityIterator entityIdIterator = null;
    /**
     * The {@link EntityScoreProvider} instance initialised based on the value
     * of the {@link IndexingConstants#KEY_ENTITY_SCORE_PROVIDER} key or
     * <code>null</code> if not configured.
     * This variable uses lazy initialisation
     * @see #getEntityScoreProvider()
     */
    private EntityScoreProvider entityScoreProvider = null;
    /**
     * The {@link ScoreNormaliser} instance initialised based on the value
     * of the {@link IndexingConstants#KEY_SCORE_NORMALIZER} key or
     * <code>null</code> if not configured.
     * This variable uses lazy initialisation
     * @see #getNormaliser()
     */
    private ScoreNormaliser scoreNormaliser = null;
    /**
     * The {@link EntityProcessor}s initialised based on the value
     * of the {@link IndexingConstants#KEY_ENTITY_PROCESSOR} key or
     * <code>null</code> if not configured.
     * This variable uses lazy initialisation
     * @see #getEntityProcessor()
     */
    private List<EntityProcessor> entityProcessor = null;
    /**
     * The {@link EntityProcessor}s initialised based on the value
     * of the {@link IndexingConstants#KEY_ENTITY_POST_PROCESSOR} key or
     * <code>null</code> if not configured.
     * This variable uses lazy initialisation
     * @see #getEntityProcessor()
     */
    private List<EntityProcessor> entityPostProcessor = null;
    /**
     * The {@link IndexingDestination} instance initialised based on the value
     * of the {@link IndexingConstants#KEY_INDEXING_DESTINATION} key or
     * <code>null</code> if not configured.
     * This variable uses lazy initialisation
     * @see #getIndexingDestination()
     */
    private IndexingDestination indexingDestination = null;
    /**
     * The configuration of the fields/languages included/excluded in the index
     * as parsed based on the value of the 
     * {@link IndexingConstants#KEY_INDEX_FIELD_CONFIG} key.
     */
    private Collection<FieldMapping> fieldMappings;
    /**
     * offset to load resources via the classpath (only used for unit testing)
     */
    private String classpathResourceOffset;
    
    private NamespacePrefixService namespacePrefixService;
    
    /**
     * Creates an instance using {@link #DEFAULT_ROOT_PATH} (relative to the
     * working directory) as {@link #getIndexingFolder()} for the indexing
     */
    public IndexingConfig(){
        this(null);
    }
    /**
     * Creates an isntace using the parsed offset plus {@link #DEFAULT_ROOT_PATH}
     * as {@link #getIndexingFolder()} for the indexing
     * @param rootPath
     */
    public IndexingConfig(String rootPath){
        this(rootPath,null);
    }
    /**
     * Internally used for unit testing. Allows to parse an offset for loading
     * the indexer configuration from the classpath. Currently a protected
     * feature, but might be moved to the public API at a later point of time.
     * (would allow to include multiple default configurations via the
     * classpath).
     * @param rootPath
     * @param classpathOffset
     */
    protected IndexingConfig(String rootPath,String classpathOffset){
        this.classpathResourceOffset = classpathOffset;
        //first get the root
        File root;// = new File(System.getProperty("user.dir"));
        if(rootPath != null){
            root = new File(rootPath);
        } else {
            root = new File(".");
        }
        try {
            root = root.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get canonical file for "
                    +root,e);
        }
        log.info("Indexing Working Directory: {}",root.getAbsoluteFile());
        this.rootDir = root;
        File configDir = getConfigFolder();
        if(!configDir.getAbsoluteFile().isDirectory()){
            log.info(" > config directory {} does not exist",configDir);
            if(!configDir.getAbsoluteFile().mkdirs()){
                throw new IllegalStateException(
                    "Unable to create configuration folder '"+
                    configDir.getAbsolutePath()+"'!");
            } else {
                log.info("  - created");
            }
        }
        File sourceDir = getSourceFolder();
        if(!sourceDir.getAbsoluteFile().exists()){
            log.info(" > resource folder '{} does not exist ",sourceDir);
            if(!sourceDir.getAbsoluteFile().mkdirs()){
                throw new IllegalStateException(
                    "Unable to create resource folder '"+
                    sourceDir.getAbsolutePath()+"'!");
            } else {
                log.info("  - created");
            }
        }
        File destinationDir = getDestinationFolder();
        if(!destinationDir.getAbsoluteFile().exists()){
            log.debug(" > destination folder '{} does not exist ",destinationDir);
            if(!destinationDir.getAbsoluteFile().mkdirs()){
                throw new IllegalStateException(
                    "Unable to create target folder '"+
                    destinationDir.getAbsolutePath()+"'!");
            } else {
                log.debug("  - created");
            }
        }
        File distributionDir = getDistributionFolder();
        if(!distributionDir.getAbsoluteFile().exists()){
            log.debug(" > distribution folder '{} does not exist ",distributionDir);
            if(!distributionDir.getAbsoluteFile().mkdirs()){
                throw new IllegalStateException(
                    "Unable to create distribution '"+
                    destinationDir.getAbsolutePath()+"'!");
            } else {
                log.debug("  - created");
            }
        }
        //set up the root folder for the classpath
        this.classPathRootDir = getConfigClasspathRootFolder();
        log.info("Classpath Indexing Root {}",classPathRootDir);
        //read the prefixnamespace mappings
        try {
            initNamespacePrefixMapper();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get create NamespacePrefixMapper",e);
        }
        
        //check the main configuration
        this.configuration = loadConfig(INDEXING_PROPERTIES,true);
        Object value = configuration.get(KEY_NAME);
        if(value == null){
            throw new IllegalArgumentException("Indexing Configuration '"+
                INDEXING_PROPERTIES+"' is missing the required key "+KEY_NAME+"!");
        }
        this.name = value.toString();
        if(name.isEmpty()){
            throw new IllegalArgumentException("Invalid Indexing Configuration '"+
                INDEXING_PROPERTIES+"': The value for the parameter"+KEY_NAME+" MUST NOT be empty!");
        }
        value = configuration.get(KEY_INDEX_FIELD_CONFIG);
        if(value == null || value.toString().isEmpty()){
            value = DEFAULT_INDEX_FIELD_CONFIG_FILE_NAME;
        }
        
        final File indexFieldConfig = getConfigFile(value.toString());
        if(indexFieldConfig.isFile()){
            try {
                this.fieldMappings = FieldMappingUtils.parseFieldMappings(new Iterator<String>() {
                    LineIterator it = IOUtils.lineIterator(new FileInputStream(indexFieldConfig), "UTF-8");
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }
                    @Override
                    public String next() {
                        return it.nextLine();
                    }
                    @Override
                    public void remove() {
                        it.remove();
                    }
                },getNamespacePrefixService());
            } catch (IOException e) {
               throw new IllegalStateException("Unable to read Index Field Configuration form '"
                   +indexFieldConfig+"'!",e);
            }
        } else {
            throw new IllegalArgumentException("Invalid Indexing Configuration: " +
            		"IndexFieldConfiguration '"+indexFieldConfig+"' not found. " +
            		"Provide the missing file or use the '"+KEY_INDEX_FIELD_CONFIG+
            		"' in the '"+INDEXING_PROPERTIES+"' to configure a different one!");
        }
    }
    
    public NamespacePrefixService getNamespacePrefixService() {
        return namespacePrefixService;
    }
    /**
     * @param configDir
     * @throws IOException
     */
    private void initNamespacePrefixMapper() throws IOException {
        File nsPrefixMappings = getConfigFile("namespaceprefix.mappings");
        if(!nsPrefixMappings.isFile()){
            FileUtils.writeLines(nsPrefixMappings,"UTF-8",Arrays.asList(
                "# Syntax: '{prefix}\\t{namespace}\\n",
                "# where:",
                "#   {prefix} ... [0..9A..Za..z-_]",
                "#   {namespace} ... must end with '#' or '/' for URLs and ':' for URNs",
                "# one mapping per line, multiple prefixes for the same namespace allowed"));
        }
        namespacePrefixService = new StanbolNamespacePrefixService(nsPrefixMappings);
    }

    /**
     * Searches for a configuration file. If the configuration is not found
     * within the {@link #getConfigFolder()} than it searches the Classpath for
     * the configuration. If the configuration is found within the Classpath it
     * is copied the the configuration folder and than opened.<p>
     * The intension behind that is that the default values are provided within
     * the indexer archive but that the user can modify the configuration after
     * the first call.
     * @param configFile the name of the configuration file
     * @return
     * @throws IOException
     */
    public InputStream openConfig(String configFileName) throws IOException {
        return openResource(CONFIG_PATH,configFileName);
    }
    public InputStream openSource(String sourceFileName) throws IOException {
        return openResource(SOURCE_PATH,sourceFileName);
    }
    /**
     * Getter for the config file with the given name. If the file/directory is 
     * not present within the {@link #getConfigFolder()} it is searched via the 
     * classpath and created (if found).
     * @param configName
     * @return
     */
    public File getConfigFile(String configName) {
        return getResource(CONFIG_PATH, configName);
    }
    /**
     * Getter for the source file with the given name. If the file/directory is 
     * not present within the {@link #getSourceFolder()} it is searched via the 
     * classpath and created (if found).
     * @param configName
     * @return
     */
    public File getSourceFile(String configName) {
        return getResource(SOURCE_PATH, configName);
    }
    
    private InputStream openResource(String path,String fileName) throws IOException {
        File resource = getResource(path, fileName);
        InputStream in = null;
        if(resource.isFile()){
            in = new FileInputStream(resource);
        } //else not found -> return null
        return in;
    }

    /**
     * Searches for a resource with the parsed name in the parsed directory.
     * If it can not be found it tries to initialise it via the classpath.
     * @param root the (relative path) to the directory containing the file.
     * typically on of {@link #configDir} or {@link #sourceDir}.
     * @param fileName the name of the file (file or directory)
     * @return the absolute File or <code>null</code> if not found.
     */
    private File getResource(String path, String fileName) {
        File resourceDir = new File(getWorkingDirectory(),path);
        File resource = new File(resourceDir,fileName);
        log.info("request for RDFTerm {} (folder: {})",fileName,resourceDir);
        if(resource.getAbsoluteFile().exists()){
            log.info(" > rquested RDFTerm present");
        } else if(copyFromClasspath(new File(path,fileName))){
            log.info(" > rquested RDFTerm copied from Classpath ");
        } else {
            log.info(" > rquested RDFTerm not found");
        }
        return resource.getAbsoluteFile();
    }
    /**
     * This method copies Resources from the Classpath over to the target
     * resource. It supports both files and directories. In case of directories
     * all sub-directories and there files are copied.<p> 
     * One can not use {@link ClassLoader#getResource(String)} because it does
     * only support files and no directories.
     * @param resource the target resource (relative path also found in the jar)
     * @return <code>true</code> if the resource was found and copied.
     */
    private boolean copyFromClasspath(File resource){
        String resourcePath;
        if(classpathResourceOffset != null){
            String rs = resource.getPath();
            resourcePath = FilenameUtils.concat(classpathResourceOffset, rs);
        } else {
            resourcePath = resource.getPath();
        }
        if(classPathRootDir == null){ //not available
            return false;
        } else if(classPathRootDir.isDirectory()){ // loaded from directory
            File classpathResource = new File(classPathRootDir,resourcePath);
            try {
                if(classpathResource.isFile()){
                    FileUtils.copyFile(classpathResource, new File(getWorkingDirectory(),resource.getPath()));
                    return true;
                } else if(classpathResource.isDirectory()){
                    FileUtils.copyDirectory(classpathResource, new File(getWorkingDirectory(),resource.getPath()));
                    return true;
                } else {
                    return false;
                }
            } catch(IOException e){
                throw new IllegalStateException(
                    String.format("Unable to copy Configuration form classpath " +
                            "resource %s to target file %s!", 
                            classpathResource, resource.getAbsolutePath()),e);
            }
        } else { //loaded form a jar file
            boolean found = false;
            JarFile jar = null;
            //when loading from a jar file we need Unix style separators
            String unixResourcePath = FilenameUtils.separatorsToUnix(resourcePath);
            try {
                jar = new JarFile(classPathRootDir);
                //String resourceName = resource.getPath();
                Enumeration<JarEntry> entries = jar.entries();
                boolean completed = false;
                //we need to iterate over the entries because the resource might
                //refer to an file but missing the tailing '/'
                while(entries.hasMoreElements() && !completed){
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if(entryName.startsWith(resourcePath) || entryName.startsWith(unixResourcePath)){
                        log.info("found entry : {}[dir={}]",entryName,entry.isDirectory());
                        if((entryName.equals(resourcePath) || entryName.equals(unixResourcePath))
                                && !entry.isDirectory()){
                            //found the resource and it is an file -> copy and return
                            completed = true;
                        }
                        if(!entry.isDirectory()){ //copy a file
                            //still check if the target folder exist
                            //TODO: this depends on user.dir is root dir
                            File targetFolder = new File(getWorkingDirectory(),
                                FilenameUtils.getPathNoEndSeparator(entryName));
                            if(targetFolder.exists() || targetFolder.mkdirs()){
                                File outFile = new File(targetFolder,
                                    FilenameUtils.getName(entry.getName()));
                                InputStream is = jar.getInputStream(entry);
                                OutputStream os = new FileOutputStream(outFile);
                                IOUtils.copyLarge(is,os);
                                IOUtils.closeQuietly(is);
                                IOUtils.closeQuietly(os);
                                //found one resource
                                found = true;
                                log.info(" > created File {}",outFile);
                            } else {
                                throw new IllegalStateException("Unable to create" +
                                		"folder "+targetFolder);
                            }
                        } else { //directory
                            //TODO: this depends on user.dir is root dir
                            File targetFolder = new File(entryName);
                            if(!targetFolder.exists() && !targetFolder.mkdirs()){
                                throw new IllegalStateException("Unable to create" +
                                    "folder "+targetFolder);
                            } else { //created a directory
                                log.info(" > created Directory {}",targetFolder);
                                found = true;
                            }
                        }
                    } // else entry does not start with the parsed resource
                } //end while entries
            } catch (IOException e) {
               throw new IllegalStateException("Unable to copy resources from" +
               		"jar file "+classPathRootDir+"!",e);
            } finally {
                if(jar != null){
                    try {
                        jar.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
            return found;
        }
    }

    /**
     * First uses the {@link Thread#currentThread() current threads} class loader
     * to load the parsed resource. If not found the class loader of this class
     * is used.
     * @param resource the resource to load
     * @return the URL for the resource or <code>null</code> if not found
     */
    private static URL loadViaClasspath(String resource) {
        String unixResource = FilenameUtils.separatorsToUnix(resource);
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(
            unixResource);
        if(resourceUrl == null){
            resourceUrl = IndexingConfig.class.getClassLoader().getResource(
                unixResource);
        }
        return resourceUrl;
    }
    /**
     * Uses the Classpath to search for the File (maybe within a jar archive)
     * that is the root to load the config. This is needed in cases directories
     * are requested by the {@link #getResource(File, String)} methods because
     * the normal {@link ClassLoader#getResource(String)} method does not work
     * for directories.
     * @param clazz the class used as context to find the jar file
     * @return the archive the parsed class was loaded from
     * @throws IOException In case the jar file can not be accessed.
     */
    private File getConfigClasspathRootFolder() {
        //use the indexing.properties file as context
        //STANBOL-
        String contextResource;
        if(classpathResourceOffset != null){
            contextResource = FilenameUtils.concat(classpathResourceOffset, 
                CONFIG_PATH+File.separatorChar+INDEXING_PROPERTIES);
        } else {
            contextResource = CONFIG_PATH+File.separatorChar+INDEXING_PROPERTIES;
        }
        URL contextUrl = loadViaClasspath(contextResource);
        if(contextUrl == null){// if indexing.properties is not found via classpath
            log.info("No '{}' found via classpath. Loading RDFTerm via" +
            		"the classpath is deactivated.",
            		contextResource);
            return null;
        }
        String resourcePath;
        try {
            resourcePath = new File(contextUrl.toURI()).getAbsolutePath();
        } catch (Exception e) {
            //if we can not convert it to an URI, try directly with the URL
            //URLs with jar:file:/{jarPath}!{classPath} can cause problems
            //so try to parse manually by using the substring from the first
            //'/' to (including '!')
            String urlString;
            try {
                urlString = URLDecoder.decode(contextUrl.toString(),"UTF-8");
            } catch (UnsupportedEncodingException e1) {
                throw new IllegalStateException("Encoding 'UTF-8' is not supported",e);
            }
            int slashIndex =  urlString.indexOf('/');
            int exclamationIndex = urlString.indexOf('!');
            if(slashIndex >=0 && exclamationIndex > 0){
                resourcePath = urlString.substring(slashIndex, exclamationIndex+1);
                log.info("manually parsed plassPath: {} from {}",resourcePath,contextUrl);
            } else {
                //looks like there is an other reason than an URL as described above
                //so better to throw an exception than to guess ...
                throw new IllegalStateException("Unable to Access Source at location "+contextUrl,e);
            }
        }
        //now get the file for the root folder in the archive containing the config
        File classpathRoot;
        if(resourcePath.indexOf('!')>0){
            classpathRoot = new File(resourcePath.substring(0,resourcePath.indexOf('!')));
        } else {
            classpathRoot = new File(resourcePath.substring(0,resourcePath.length()-contextResource.length()));
        }
        return classpathRoot;
    }
    
    /**
     * Loads an {@link Properties} configuration from the parsed file and
     * returns it as Map
     * @param configFile the file
     * @param required if <code>true</code> an {@link IllegalArgumentException}
     * will be thrown if the config was not present otherwise an empty map will
     * be returned
     * @return The configuration as Map
     */
    private Map<String,Object> loadConfig(String configFile, boolean required) {
        //Uses an own implementation to parse key=value configuration
        //The problem with the java properties is that keys do not support
        //UTF-8, but some configurations might want to use URLs as keys!
        Map<String,Object> configMap = new HashMap<String,Object>();
        try {
            InputStream in = openConfig(configFile);
            if(in != null){
                LineIterator lines = IOUtils.lineIterator(in, "UTF-8");
                while(lines.hasNext()){
                    String line = lines.next();
                    if(!line.isEmpty()){
                        int indexOfEquals = line.indexOf('=');
                        String key = indexOfEquals > 0 ?
                                line.substring(0,indexOfEquals).trim():
                                    line.trim();
                        if(key.charAt(0) != '#' && key.charAt(0) != '!'){ //no comment
                            String value;
                            if(indexOfEquals > 0 && indexOfEquals < line.length()-1){
                                value = line.substring(indexOfEquals+1,line.length());
                            } else {
                                value = null;
                            }
                            configMap.put(key,value);
                        } // else ignore comments
                    } //else ignore empty lines
                }
            } else if(required){
                throw new IllegalArgumentException(
                    "Unable to find configuration file '"+
                    configFile+"'!");
            } else {//-> optional and not found -> return empty map
                log.info("Unable to find optional configuration {}",configFile);
            }
        } catch (IOException e) {
            if(required){
                throw new IllegalStateException(
                    "Unable to read configuration file '"+
                    configFile+"'!",e);
            } else {
                log.warn("Unable to read configuration file '"+configFile+"'!",e);
            }
        }
        // Old code that used java.util.Properties to load configurations!
//        Properties config = new Properties();
//        try {
//            config.load(new FileInputStream(configFile));
//        } catch (FileNotFoundException e) {
//            if(required){
//                throw new IllegalArgumentException(
//                    "Unable to find configuration file '"+
//                    configFile.getAbsolutePath()+"'!");
//            }
//        } catch (IOException e) {
//            if(required){
//                throw new IllegalStateException(
//                    "Unable to read configuration file '"+
//                    configFile.getAbsolutePath()+"'!",e);
//            }
//        }
//        if(config != null){
//            for(Enumeration<String> keys = (Enumeration<String>)config.propertyNames();keys.hasMoreElements();){
//                String key = keys.nextElement();
//                configMap.put(key, config.getProperty(key));
//            }
//        }
        return configMap;
    }
    /**
     * Getter for the working direcotry of the Indexing tool. (the directory
     * containing the /indexing folder). By defualt htis 
     * @return
     */
    public final File getWorkingDirectory(){
        return rootDir;
    }
    /**
     * Getter for the root folder used for the Indexing (root/indexing)
     * @return the root folder (containing the config, resources, target and dist folders)
     */
    public final File getIndexingFolder() {
        return new File(getWorkingDirectory(),DEFAULT_ROOT_PATH);
    }

    /**
     * The root folder for the configuration. Guaranteed to exist.
     * @return the root folder for the configuration
     */
    public final File getConfigFolder() {
        return new File(getIndexingFolder(),CONFIG_FOLDER);
    }

    /**
     * The root folder containing the resources used as input for the 
     * indexing process. Might not exist if no resources are available
     * @return the root folder for the resources
     */
    public final File getSourceFolder() {
        return new File(getIndexingFolder(),SOURCE_FOLDER);
    }

    /**
     * The root folder containing the files created by the indexing process.
     * Guaranteed to exist.
     * @return the target folder
     */
    public final File getDestinationFolder() {
        return new File(getIndexingFolder(),DESTINATION_FOLDER);
    }
    /**
     * The root folder for the distribution. Guaranteed to exist.
     * @return the distribution folder
     */
    public final File getDistributionFolder() {
        return new File(getIndexingFolder(),DISTRIBUTION_FOLDER);
    }
    /**
     * Getter for the name as configured by the {@link IndexingConstants#KEY_NAME}
     * by the main indexing configuration.
     * @return the name of this data source to index
     */
    public String getName() {
        return name;
    }
    /**
     * Getter for the description as configured by the {@link IndexingConstants#KEY_DESCRIPTION}
     * by the main indexing configuration.
     * @return the description of the data source to index or <code>null</code>
     * if not defined
     */
    public String getDescription(){
        Object value = configuration.get(KEY_DESCRIPTION);
        return value != null?value.toString():null;
    }
    /**
     * Getter for the failOnError as configured by the {@link IndexingConstants#KEY_FAIL_ON_ERROR_LOADING_RESOURCE}
     * by the main indexing configuration.
     * @return the boolean value of the failOnError parameter
     */
    public boolean isFailOnError(){
    	//by default failOnError is false to continue execution of the indexing tool
        boolean failOnError = false;
    	Object value = configuration.get(IndexingConstants.KEY_FAIL_ON_ERROR_LOADING_RESOURCE);
    	if(value != null && !value.toString().isEmpty()){
            failOnError = Boolean.parseBoolean(value.toString());
        }
    	return failOnError;
    }
    /**
     * The {@link ScoreNormaliser} as configured by the {@link IndexingConstants#KEY_SCORE_NORMALIZER}
     * by the main indexing configuration.
     * @return the configured {@link ScoreNormaliser} or a {@link DefaultNormaliser} if
     * this configuration is missing.
     */
    public ScoreNormaliser getNormaliser(){
        if(scoreNormaliser == null){
            initNormaliser();
        }
        return scoreNormaliser;
    }
    /**
     * The {@link EntityDataIterable} as configured by the {@link IndexingConstants#KEY_ENTITY_DATA_ITERABLE}
     * by the main indexing configuration.
     * @return the configured {@link EntityDataIterable} or a <code>null</code> if
     * this configuration is not present.
     */
    public EntityDataIterable getDataIterable(){
        if(entityDataIterable  != null){
            return entityDataIterable;
        } else if(configuration.containsKey(KEY_ENTITY_DATA_ITERABLE)){
            ConfigEntry config = parseConfigEntry(configuration.get(KEY_ENTITY_DATA_ITERABLE).toString());
            try {
                entityDataIterable = (EntityDataIterable)Class.forName(config.getClassName()).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid EntityDataIterable configuration '"+config.getConfigString()+"'!",e);
            }
            //add the configuration
            Map<String,Object> configMap = getComponentConfig(config, entityDataIterable.getClass().getSimpleName(), false);
            //add also the directly provided parameters
            configMap.putAll(config.getParams());
            entityDataIterable.setConfiguration(configMap);
            return entityDataIterable;
        } else {
            return null;
        }
    }
    public EntityIterator getEntityIdIterator() {
        if(entityIdIterator != null){
            return entityIdIterator;
        } else if(configuration.containsKey(KEY_ENTITY_ID_ITERATOR)){
            ConfigEntry config = parseConfigEntry(configuration.get(KEY_ENTITY_ID_ITERATOR).toString());
            try {
                entityIdIterator = (EntityIterator)Class.forName(config.getClassName()).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid EntityIterator configuration '"+config.getConfigString()+"'!",e);
            }
            //add the configuration
            Map<String,Object> configMap = getComponentConfig(config, entityIdIterator.getClass().getSimpleName(), false);
            //add also the directly provided parameters
            configMap.putAll(config.getParams());
            entityIdIterator.setConfiguration(configMap);
            return entityIdIterator;
        } else {
            return null;
        }
    }
    public EntityDataProvider getEntityDataProvider() {
        if(entityDataProvider != null){
            return entityDataProvider;
        } else if (configuration.containsKey(KEY_ENTITY_DATA_PROVIDER)){
            ConfigEntry config = parseConfigEntry(configuration.get(KEY_ENTITY_DATA_PROVIDER).toString());
            try {
                entityDataProvider = (EntityDataProvider)Class.forName(config.getClassName()).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid EntityDataProvider configuration '"+config.getConfigString()+"'!",e);
            }
            //add the configuration
            Map<String,Object> configMap = getComponentConfig(config, entityDataProvider.getClass().getSimpleName(), false);
            //add also the directly provided parameters
            configMap.putAll(config.getParams());
            entityDataProvider.setConfiguration(configMap);
            return entityDataProvider;
        } else {
            return null;
        }
    }
    public EntityScoreProvider getEntityScoreProvider() {
        if(entityScoreProvider != null){
            return entityScoreProvider;
        } else if (configuration.containsKey(KEY_ENTITY_SCORE_PROVIDER)){
            ConfigEntry config = parseConfigEntry(configuration.get(KEY_ENTITY_SCORE_PROVIDER).toString());
            try {
                entityScoreProvider = (EntityScoreProvider)Class.forName(config.getClassName()).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid EntityScoreProvider configuration '"+config.getConfigString()+"'!",e);
            }
            //add the configuration
            Map<String,Object> configMap = getComponentConfig(config, entityScoreProvider.getClass().getSimpleName(), false);
            //add also the directly provided parameters
            configMap.putAll(config.getParams());
            entityScoreProvider.setConfiguration(configMap);
            return entityScoreProvider;
        } else {
            return null;
        }
    }
    /**
     * The fields and languages included/excluded in the created index.<p>
     * NOTE: Currently this uses the {@link FieldMapping} class was initially
     * defined to be used as configuration for the {@link FieldMapper}. In
     * future this might change to an Interface that is more tailored to
     * defining the fields and languages included/excluded in the index and does
     * not allow to define mappings and data type conversions as the current one
     * @return
     */
    public Collection<FieldMapping> getIndexFieldConfiguration(){
        return fieldMappings;
    }
    /**
     * Getter for the list of {@link EntityProcessor}s or <code>null</code> if
     * none are configured.
     * @return
     */
    public List<EntityProcessor> getEntityProcessors() {
        if(entityProcessor != null){
            return entityProcessor;
        } else if (configuration.containsKey(KEY_ENTITY_PROCESSOR)){
            List<ConfigEntry> configs = parseConfigEntries(configuration.get(KEY_ENTITY_PROCESSOR).toString());
            List<EntityProcessor> processorList = new ArrayList<EntityProcessor>(configs.size());
            for(ConfigEntry config : configs){
                EntityProcessor processor;
                try {
                    processor = (EntityProcessor)Class.forName(config.getClassName()).newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid EntityProcessor configuration '"+config.getConfigString()+"'!",e);
                }
                //add the configuration
                Map<String,Object> configMap = getComponentConfig(config, processor.getClass().getSimpleName(), false);
                //add also the directly provided parameters
                configMap.putAll(config.getParams());
                processor.setConfiguration(configMap);
                processorList.add(processor);
            }
            if(!processorList.isEmpty()){ //do not set empty lists
                entityProcessor = Collections.unmodifiableList(processorList);
            }
            return entityProcessor;
        } else {
            return null;
        }
    }
    /**
     * Getter for the {@link EntityProcessor}s configured to be used for
     * post-processing or <code>null</code> if none.
     * @return
     */
    public List<EntityProcessor> getEntityPostProcessors(){
        if(entityPostProcessor != null){
            return entityPostProcessor;
        } else if(configuration.containsKey(KEY_ENTITY_POST_PROCESSOR)){
            List<ConfigEntry> configs = parseConfigEntries(configuration.get(KEY_ENTITY_POST_PROCESSOR).toString());
            List<EntityProcessor> postProcessorList = new ArrayList<EntityProcessor>(configs.size());
            for(ConfigEntry config : configs){
                EntityProcessor postProcessor;
                try {
                    postProcessor = (EntityProcessor)Class.forName(config.getClassName()).newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid EntityProcessor configuration '"+config.getConfigString()+"' for post-processing!",e);
                }
                //add the configuration
                Map<String,Object> configMap = getComponentConfig(config, postProcessor.getClass().getSimpleName(), false);
                //add also the directly provided parameters
                configMap.putAll(config.getParams());
                postProcessor.setConfiguration(configMap);
                postProcessorList.add(postProcessor);
            }
            if(!postProcessorList.isEmpty()){ //do not set empty lists
                entityPostProcessor = Collections.unmodifiableList(postProcessorList);
            }
            return entityPostProcessor;
        } else {
            return null;
        }
    }
    
    public IndexingDestination getIndexingDestination() {
        if(indexingDestination != null){
            return indexingDestination;
        } else if (configuration.containsKey(KEY_INDEXING_DESTINATION)){
            ConfigEntry config = parseConfigEntry(configuration.get(KEY_INDEXING_DESTINATION).toString());
            try {
                indexingDestination = (IndexingDestination)Class.forName(config.getClassName()).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid IndexingDestination configuration '"+config.getConfigString()+"'!",e);
            }
            //add the configuration
            Map<String,Object> configMap = getComponentConfig(config, indexingDestination.getClass().getSimpleName(), false);
            //add also the directly provided parameters
            configMap.putAll(config.getParams());
            indexingDestination.setConfiguration(configMap);
            return indexingDestination;
        } else {
            return null;
        }
    }
    public File getIndexedEntitiesIdsFile(){
        Object value = configuration.get(IndexingConstants.KEX_INDEXED_ENTITIES_FILE);
        if(value == null){
            return new File(getDestinationFolder(),DEFAULT_INDEXED_ENTITIES_ID_FILE_NAME);
        } else if (value.toString().isEmpty()){
            return null; //deactivate this feature;
        } else {
            return new File(getDestinationFolder(),value.toString());
        }
    }

    private void initNormaliser() {
        Object value = configuration.get(IndexingConstants.KEY_SCORE_NORMALIZER);
        if(value == null){
            this.scoreNormaliser = new  DefaultNormaliser();
        } else {
            ScoreNormaliser normaliser = null;
            ScoreNormaliser last = null;
            List<ConfigEntry> configs = parseConfigEntries(value.toString());
            for(int i=configs.size()-1;i>=0;i--){
                last = normaliser;
                normaliser = null;
                ConfigEntry config = configs.get(i);
                try {
                    normaliser = (ScoreNormaliser)Class.forName(config.getClassName()).newInstance();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid Normaliser configuration '"+config.getConfigString()+"'!",e);
                }
                Map<String,Object> normaliserConfig = getComponentConfig(config,normaliser.getClass().getSimpleName(),config.getParams().containsKey(CONFIG_PARAM));
                //add also the directly provided parameters
                normaliserConfig.putAll(config.getParams());
                if(last != null){
                    normaliserConfig.put(ScoreNormaliser.CHAINED_SCORE_NORMALISER, last);
                }
                normaliser.setConfiguration(normaliserConfig);
            }
            //set the normaliser!
            this.scoreNormaliser = normaliser;
        }
    }

    /**
     * Loads a configuration based on the value of the {@link #CONFIG_PARAM}
     * parameter of the parsed {@link ConfigEntry}.
     * @param configEntry
     * @param defaultName
     * @return
     */
    private Map<String,Object> getComponentConfig(ConfigEntry configEntry,String defaultName, boolean required) {
        //Removed support for parsing the relative path to the config file
        //because it was not used! (String relConfigPath was the first param)
//        File configDir;
//        if(relConfigPath == null || relConfigPath.isEmpty()){
//            configDir = this.configDir;
//        } else {
//            configDir = new File(this.configDir,relConfigPath);
//        }
//        //test also if relConfigPath = null, because also the root might not exist!
//        if(!configDir.isDirectory()){
//            if(required){
//                throw new IllegalArgumentException("The Configuration Directory '"+
//                    configDir+"' does not exist (or ist not a directory)!");
//            } else {
//                return new HashMap<String,Object>();
//            }
//        }
        //if the CONFIG_PARAM is present in the config we assume that a config is required
        String name = configEntry.getParams().get(CONFIG_PARAM);
        Map<String,Object> config = loadConfigFile(name == null ? defaultName : name, required);
        //we need to also add the key used to get (this) indexing config
        config.put(KEY_INDEXING_CONFIG, this);
        return config;
    }

    /**
     * Loads the config with the given name from the parsed directory and throwing
     * an {@link IllegalArgumentException} if the configuration is required but
     * not found
     * @param name the name (".properties" is appended if missing)
     * @param configDir the directory to look for the config
     * @param required if this config is required or optional
     * @return the key value mappings as map
     */
    private Map<String,Object> loadConfigFile(String name, boolean required) {
        Map<String,Object> loadedConfig;
        name = name.endsWith(".properties") ? name : name+".properties";
        loadedConfig = loadConfig(name,required);
        return loadedConfig;
    }

    private ConfigEntry parseConfigEntry(String config){
        return new ConfigEntry(config);
    }
    private List<ConfigEntry> parseConfigEntries(String config){
        List<ConfigEntry> configs = new ArrayList<ConfigEntry>();
        for(String configPart : config.split(";")){
            configs.add(parseConfigEntry(configPart));
        }
        return configs;
    }
    private final class ConfigEntry {
        private String configString;
        private String className;
        private Map<String,String> params;
        
        private ConfigEntry(String config){
            configString = config;
            String[] parts = config.split(",");
            className = parts[0];
            params = new HashMap<String,String>();
            if(parts.length>1){
                for(int i=1;i<parts.length;i++){
                    String[] param = parts[i].split(":"); //TODO: maybe use also "=" there
                    String value = null;
                    if(param.length>1){
                        value = parts[i].substring(parts[i].indexOf(':')+1);
                    }
                    params.put(param[0], value);
                }
            }
        }
        public final String getConfigString() {
            return configString;
        }
        public final String getClassName() {
            return className;
        }
        public final Map<String,String> getParams() {
            return params;
        }
    }
    /**
     * Can be used to look for a config within the configuration directory
     * of the {@link IndexingConfig}.
     * @param string the name of the configuration (".properties" is appended if
     * missing)
     * @param required if this is an required or optional configuration.
     * @return the key value mappings as map
     * @throws IllegalArgumentException if the configuration was not found and
     * <code>true</code> was parsed for required
     */
    public Map<String,Object> getConfig(String name,boolean required) throws IllegalArgumentException {
        return loadConfigFile(name, required);
    }
    /**
     * Getter for configured properties directly by the key. Typically used
     * to get Properties as defined by the {@link SiteConfiguration} interface
     * @param key the key of the property
     * @return the value or <code>null</code> if not present. Might also return
     * <code>null</code> in case the value <code>null</code> is set for the
     * requested property.
     */
    public Object getProperty(String key){
        return configuration.get(key);
    }
}
