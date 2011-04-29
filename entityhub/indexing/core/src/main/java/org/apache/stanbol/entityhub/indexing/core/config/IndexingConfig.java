package org.apache.stanbol.entityhub.indexing.core.config;

import static org.apache.stanbol.entityhub.indexing.core.config.IndexingConstants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.stanbol.entityhub.core.mapping.FieldMappingUtils;
import org.apache.stanbol.entityhub.indexing.core.EntityDataIterable;
import org.apache.stanbol.entityhub.indexing.core.EntityDataProvider;
import org.apache.stanbol.entityhub.indexing.core.EntityIterator;
import org.apache.stanbol.entityhub.indexing.core.EntityProcessor;
import org.apache.stanbol.entityhub.indexing.core.EntityScoreProvider;
import org.apache.stanbol.entityhub.indexing.core.IndexingDestination;
import org.apache.stanbol.entityhub.indexing.core.normaliser.DefaultNormaliser;
import org.apache.stanbol.entityhub.indexing.core.normaliser.ScoreNormaliser;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexingConfig {
    private static final String DEFAULT_ROOT_PATH = "indexing";
    private static final String CONFIG_PATH = "config";
    private static final String SOURCE_PATH = "resources";
    private static final String DESTINATION_PATH = "destination";
    private static final String DISTRIBUTION_PATH = "dist";
    private static final String INDEXING_PROERTIES = "indexing.properties";
    private static final String CONFIG_PARAM = "config";
    public static final String KEY_INDEXING_CONFIG = "indexingConfig";
    
    /**
     * Internally used to explain the syntax in the configuration file to parse parameters
     */
    private static final String SYNTAX_ERROR_MESSAGE = "{key}={value1},{param1}:{value1},{param2}:{value2};{value2},{param1}:{value1} ...";
    
    private static final Logger log = LoggerFactory.getLogger(IndexingConfig.class);
    private static final String DEFAULT_INDEX_FIELD_CONFIG_FILE_NAME = "indexFieldConfig.txt";
    
    private final File rootDir;
    private final File configDir;
    private final File sourceDir;
    private final File destinationDir;
    private final File distributionDir;
    private final Map<String,Object> configuration;
    
    private String name;

    private EntityDataIterable entityDataIterable = null;
    private EntityDataProvider entityDataProvider = null;

    private EntityIterator entityIdIterator = null;
    private EntityScoreProvider entityScoreProvider = null;
    
    private ScoreNormaliser scoreNormaliser = null;
    
    private EntityProcessor entityProcessor = null;
    
    private IndexingDestination indexingDestination = null;
    /**
     * The configuration of the fields/languages included/excluded in the index.
     */
    private Collection<FieldMapping> fieldMappings;
    
    public IndexingConfig(){
        this(null);
    }
    
    public IndexingConfig(String rootPath){
        //first get the root
        File root = new File(System.getProperty("user.dir"));
        if(rootPath != null){
            File parsed = new File(rootPath);
            if(!parsed.isAbsolute()){
                root = new File(root,rootPath); //add parsed to "user.dir"
            } else {
                root = parsed; //use the parsed absolute path
            }
        }
        //now we need to add the name of the root folder
        root = new File(root,DEFAULT_ROOT_PATH);
        //check if root exists
        if(!root.isDirectory()){
            throw new IllegalArgumentException(
                "The root folder for the indexing '"+root.getAbsolutePath()+
                "' does not exist!");
        } else {
            this.rootDir = root;
        }
        //check also for the config
        this.configDir = new File(root,CONFIG_PATH);
        if(!configDir.isDirectory()){
            throw new IllegalArgumentException(
                "The root folder for the indexing configuration '"+
                root.getAbsolutePath()+"' does not exist!");
        }
        this.sourceDir = new File(root,SOURCE_PATH);
        if(!sourceDir.exists()){
            log.info("The resource folder '"+sourceDir.getAbsolutePath()+
                "' (typically containing the sources used for indexing) does not exist");
            log.info(" - this might be OK if no (local) resources are needed for the indexing");
        }
        this.destinationDir = new File(root,DESTINATION_PATH);
        if(!destinationDir.exists()){
            if(!destinationDir.mkdirs()){
                throw new IllegalStateException(
                    "Unable to create target folder for indexing '"+
                    destinationDir.getAbsolutePath()+"'!");
            }
        }
        this.distributionDir = new File(root,DISTRIBUTION_PATH);
        if(!distributionDir.exists()){
            if(!distributionDir.mkdirs()){
                throw new IllegalStateException(
                    "Unable to create distribution folder for indexing '"+
                    destinationDir.getAbsolutePath()+"'!");
            }
        }
        //check the main configuration
        File indexingConfigFile = new File(this.configDir,INDEXING_PROERTIES);
        this.configuration = loadConfig(indexingConfigFile,true);
        Object value = configuration.get(KEY_NAME);
        if(value == null){
            throw new IllegalArgumentException("Indexing Configuration '"+
                indexingConfigFile+"' is missing the required key "+KEY_NAME+"!");
        }
        this.name = value.toString();
        if(name.isEmpty()){
            throw new IllegalArgumentException("Invalid Indexing Configuration '"+
                indexingConfigFile+"': The value for the parameter"+KEY_NAME+" MUST NOT be empty!");
        }
        value = configuration.get(KEY_INDEX_FIELD_CONFIG);
        if(value == null || value.toString().isEmpty()){
            value = DEFAULT_INDEX_FIELD_CONFIG_FILE_NAME;
        }
        final File indexFieldConfig = new File(configDir,value.toString());
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
                });
            } catch (IOException e) {
               throw new IllegalStateException("Unable to read Index Field Configuration form '"
                   +indexFieldConfig+"'!",e);
            }
        } else {
            throw new IllegalArgumentException("Invalid Indexing Configuration: " +
            		"IndexFieldConfiguration '"+indexFieldConfig+"' not found. " +
            		"Provide the missing file or use the '"+KEY_INDEX_FIELD_CONFIG+
            		"' in the '"+indexingConfigFile+"' to configure a different one!");
        }
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
    @SuppressWarnings("unchecked")
    private Map<String,Object> loadConfig(File configFile, boolean required) {
        //Uses an own implementation to parse key=value configuration
        //The problem with the java properties is that keys do not support
        //UTF-8, but some configurations might want to use URLs as keys!
        Map<String,Object> configMap = new HashMap<String,Object>();
        try {
            LineIterator lines = IOUtils.lineIterator(new FileInputStream(configFile), "UTF-8");
            while(lines.hasNext()){
                String line = (String)lines.next();
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
        } catch (FileNotFoundException e) {
            if(required){
                throw new IllegalArgumentException(
                    "Unable to find configuration file '"+
                    configFile.getAbsolutePath()+"'!");
            }
        } catch (IOException e) {
            if(required){
                throw new IllegalStateException(
                    "Unable to read configuration file '"+
                    configFile.getAbsolutePath()+"'!",e);
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
     * Getter for the root folder used for the Indexing
     * @return the root folder (containing the config, resources, target and dist folders)
     */
    public final File getRootFolder() {
        return rootDir;
    }

    /**
     * The root folder for the configuration. Guaranteed to exist.
     * @return the root folder for the configuration
     */
    public final File getConfigFolder() {
        return configDir;
    }

    /**
     * The root folder containing the resources used as input for the 
     * indexing process. Might not exist if no resources are available
     * @return the root folder for the resources
     */
    public final File getSourceFolder() {
        return sourceDir;
    }

    /**
     * The root folder containing the files created by the indexing process.
     * Guaranteed to exist.
     * @return the target folder
     */
    public final File getDestinationFolder() {
        return destinationDir;
    }
    /**
     * The root folder for the distribution. Guaranteed to exist.
     * @return the distribution folder
     */
    public final File getDistributionFolder() {
        return distributionDir;
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
    public EntityDataIterable getDataInterable(){
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
        } else if(configuration.containsKey(KEY_ENTITY_ID_ITERATPR)){
            ConfigEntry config = parseConfigEntry(configuration.get(KEY_ENTITY_ID_ITERATPR).toString());
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
    public EntityProcessor getEntityProcessor() {
        if(entityProcessor != null){
            return entityProcessor;
        } else if (configuration.containsKey(KEY_ENTITY_PROCESSOR)){
            ConfigEntry config = parseConfigEntry(configuration.get(KEY_ENTITY_PROCESSOR).toString());
            try {
                entityProcessor = (EntityProcessor)Class.forName(config.getClassName()).newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid EntityProcessor configuration '"+config.getConfigString()+"'!",e);
            }
            //add the configuration
            Map<String,Object> configMap = getComponentConfig(config, entityProcessor.getClass().getSimpleName(), false);
            //add also the directly provided parameters
            configMap.putAll(config.getParams());
            entityProcessor.setConfiguration(configMap);
            return entityProcessor;
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
        Map<String,Object> config = loadConfig(name == null ? defaultName : name, configDir, required);
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
    private Map<String,Object> loadConfig(String name, File configDir, boolean required) {
        Map<String,Object> loadedConfig;
        name = name.endsWith(".properties")? name : name+".properties";
        if(name == null){
            if(required){
                throw new IllegalArgumentException("Missing required parameter'"+
                    CONFIG_PARAM+"' Syntax: '"+SYNTAX_ERROR_MESSAGE +"'!");
            } else {
                return new HashMap<String,Object>();
            }
        }
        File configFile = new File(configDir,name);
        loadedConfig = loadConfig(configFile,required);
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
    private class ConfigEntry {
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
        return loadConfig(name, configDir, required);
    }
}
