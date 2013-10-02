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
package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.CASE_SENSITIVE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_CASE_SENSITIVE_MATCHING_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_MATCHING_LANGUAGE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.ENTITY_TYPES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.TYPE_MAPPINGS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESSED_LANGUAGES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig.PROCESS_ONLY_PROPER_NOUNS_STATE;
import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.PROPERTY_NAME;
import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.apache.stanbol.commons.solr.IndexReference;
import org.apache.stanbol.commons.solr.RegisteredSolrServerTracker;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.cache.EntityCacheManager;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.cache.FastLRUCacheManager;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
/**
 * This is the OSGI component for the {@link FstLinkingEngine}. It is used to
 * manage the service configuration, tracks dependencies and handles the 
 * OSGI life cycle.  
 * @author Rupert Westenthaler
 *
 */
@Component(
    configurationFactory = true, 
    policy = ConfigurationPolicy.REQUIRE, // the baseUri is required!
    specVersion = "1.1", 
    metatype = true, 
    immediate = true,
    inherit = true)
@org.apache.felix.scr.annotations.Properties(value={
    @Property(name=PROPERTY_NAME), //the name of the engine
    @Property(name=FstLinkingEngineComponent.SOLR_CORE),
    @Property(name=IndexConfiguration.FIELD_ENCODING, options={
        @PropertyOption(
            value='%'+IndexConfiguration.FIELD_ENCODING+".option.none",
            name="None"),
        @PropertyOption(
            value='%'+IndexConfiguration.FIELD_ENCODING+".option.solrYard",
            name="SolrYard"),
        @PropertyOption(
            value='%'+IndexConfiguration.FIELD_ENCODING+".option.minusPrefix",
            name="MinusPrefix"),
        @PropertyOption(
            value='%'+IndexConfiguration.FIELD_ENCODING+".option.underscorePrefix",
            name="UnderscorePrefix"),
        @PropertyOption(
            value='%'+IndexConfiguration.FIELD_ENCODING+".option.minusSuffix",
            name="MinusSuffix"),
        @PropertyOption(
            value='%'+IndexConfiguration.FIELD_ENCODING+".option.underscoreSuffix",
            name="UnderscoreSuffix"),
        @PropertyOption(
            value='%'+IndexConfiguration.FIELD_ENCODING+".option.atPrefix",
            name="AtPrefix"),
        @PropertyOption(
            value='%'+IndexConfiguration.FIELD_ENCODING+".option.atSuffix",
            name="AtSuffix")
        },value="SolrYard"),
    @Property(name=IndexConfiguration.FST_CONFIG, cardinality=Integer.MAX_VALUE),
    @Property(name=IndexConfiguration.FST_FOLDER, 
    value=IndexConfiguration.DEFAULT_FST_FOLDER),
    @Property(name=IndexConfiguration.SOLR_TYPE_FIELD, value="rdf:type"),
    @Property(name=IndexConfiguration.SOLR_RANKING_FIELD, value="entityhub:entityRank"),
//  @Property(name=REDIRECT_FIELD,value="rdfs:seeAlso"),
//  @Property(name=REDIRECT_MODE,options={
//      @PropertyOption(
//          value='%'+REDIRECT_MODE+".option.ignore",
//          name="IGNORE"),
//      @PropertyOption(
//          value='%'+REDIRECT_MODE+".option.addValues",
//          name="ADD_VALUES"),
//      @PropertyOption(
//              value='%'+REDIRECT_MODE+".option.follow",
//              name="FOLLOW")
//      },value="IGNORE"),
    @Property(name=FstLinkingEngineComponent.FST_THREAD_POOL_SIZE,
        intValue=FstLinkingEngineComponent.DEFAULT_FST_THREAD_POOL_SIZE),
    @Property(name=FstLinkingEngineComponent.ENTITY_CACHE_SIZE, 
        intValue=FstLinkingEngineComponent.DEFAULT_ENTITY_CACHE_SIZE),
    @Property(name=SUGGESTIONS, intValue=DEFAULT_SUGGESTIONS),
    @Property(name=CASE_SENSITIVE,boolValue=DEFAULT_CASE_SENSITIVE_MATCHING_STATE),
    @Property(name=PROCESS_ONLY_PROPER_NOUNS_STATE, boolValue=DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE),
    @Property(name=PROCESSED_LANGUAGES, cardinality=Integer.MAX_VALUE,
        value={"*;lmmtip;uc=LINK;prob=0.75;pprob=0.75", // link multiple matchable tokens in chunks; link upper case words
               "de;uc=MATCH", //in German all Nouns are upper case
               "es;lc=Noun", //the OpenNLP POS tagger for Spanish does not support ProperNouns
               "nl;lc=Noun"}), //same for Dutch 
    @Property(name=DEFAULT_MATCHING_LANGUAGE,value=""),
    @Property(name=ENTITY_TYPES,cardinality=Integer.MAX_VALUE),
    @Property(name=TYPE_MAPPINGS,cardinality=Integer.MAX_VALUE, value={
        "dbp-ont:Organisation; dbp-ont:Newspaper; schema:Organization > dbp-ont:Organisation",
        "dbp-ont:Person; foaf:Person; schema:Person > dbp-ont:Person",
        "dbp-ont:Place; schema:Place; geonames:Feature > dbp-ont:Place",
        "dbp-ont:Work; schema:CreativeWork > dbp-ont:Work",
        "dbp-ont:Event; schema:Event > dbp-ont:Event",
        "schema:Product > schema:Product",
        "skos:Concept > skos:Concept"}),
//    @Property(name=DEREFERENCE_ENTITIES, boolValue=DEFAULT_DEREFERENCE_ENTITIES_STATE),
//    @Property(name=DEREFERENCE_ENTITIES_FIELDS,cardinality=Integer.MAX_VALUE,
//        value={"rdfs:comment","geo:lat","geo:long","foaf:depiction","dbp-ont:thumbnail"}),
    @Property(name=SERVICE_RANKING,intValue=0)
})
public class FstLinkingEngineComponent {

    /**
     * The {@link SolrCore} is required to access the document ids for the Entities
     * as well as the analyzer chains of the fields used for the linking
     */
    public static final String SOLR_CORE = "enhancer.engines.linking.lucenefst.solrcore";
    
    /**
     * The size of the thread pool used to create FST models (default=1). Creating
     * such models does need a lot of memory. Expect values up to 10times of the
     * build model. So while this task can easily performed concurrently users need
     * to be aware that the process will occupy a lot of heap space (typically several
     * GBytes). If heap space is not an issue it is best to configure the value
     * based on the CPU cores available on the local host.<p>
     * This configuration has only an effect if runtime generation of FST modles
     * is enabled (either by default or for some FST by explicitly setting the 
     * '<code>{@link IndexConfiguration#PARAM_RUNTIME_GENERATION generate}=true</code>' parameter 
     * for some languages in the {@link IndexConfiguration#FST_CONFIG}.
     */
    public static final String FST_THREAD_POOL_SIZE = "enhancer.engines.linking.lucenefst.fstThreadPoolSize";
    /**
     * The default number of threads used to create FST models (default=1)
     */
    public static final int DEFAULT_FST_THREAD_POOL_SIZE = 1;    
    /**
     * Parameter used to configure the size of the Cache used to for Entity information.
     * While the FST linking is fully performed in memory this engine needs still to
     * load tagging relevant fields (labels, types, redirectes and entity ranking)
     * for matched entities from the disc. The EntityCache is a LRU cache for such
     * information.
     */
    public static final String ENTITY_CACHE_SIZE = "enhancer.engines.linking.lucenefst.entityCacheSize";
    /**
     * The default size of the Entity Cache is set to 65k entities.
     */
    public static final int DEFAULT_ENTITY_CACHE_SIZE = 65536;
    
    private final Logger log = LoggerFactory.getLogger(FstLinkingEngineComponent.class);
    /**
     * the name for the EnhancementEngine registered by this component
     */
    private String engineName;
    
    /**
     * used to resolve '{prefix}:{local-name}' used within the engines configuration
     */
    @Reference
    protected NamespacePrefixService prefixService;    

    /**
     * Holds the FST configuration parsed to the engine
     */
    private LanguageConfiguration fstConfig;
    /**
     * The configured fstFolder. NOTE that the actual folder is determined in the
     * {@link #updateEngineRegistration(ServiceReference, SolrServer)} based on
     * the SolrCore.
     */
    private String fstFolder;
    /**
     * Holds the {@link TextProcessingConfig} parsed from the configuration of
     * this engine. <p>
     * NOTE: that by far not all configurations are supported. See documentation
     * for details
     */
    private TextProcessingConfig textProcessingConfig;
    /**
     * Holds the {@link EntityLinkerConfig} parsed from the configuration of
     * this engine. <p>
     * NOTE: that by far not all configurations are supported. See documentation
     * for details
     */
    private EntityLinkerConfig entityLinkerConfig;
    //SolrCore related fields
    /**
     * The reference to the configured SolrIndex parsed from the {@link #SOLR_CORE}
     * configuration
     * @see #SOLR_CORE
     */
    private IndexReference indexReference;
    /**
     * The OSGI {@link ServiceTracker} used to track the configured Solr core
     */
    private RegisteredSolrServerTracker solrServerTracker;
    /**
     * The ServiceReference of the {@link SolrCore} registered as OSGI service
     * and tracked by the {@link #solrServerTracker}. This is used to
     * check if the SolrCore has changed on OSGI service events
     */
    private ServiceReference solrServerReference;
    /**
     * The {@link SolrCore} used for FST linking. This is set based on OSGI
     * events provided by the {@link #solrServerTracker}
     */
    private SolrCore solrCore;

    /**
     * Holds the OSGI service registration for the {@link FstLinkingEngine}
     */
    private ServiceRegistration engineRegistration;
    /**
     * Holds the metadata registered with the {@link #engineRegistration}
     */
    Dictionary<String,Object> engineMetadata;
    /**
     * The bundle context for this component. Also used to track dependencies
     * and register the {@link #engineRegistration}
     */
    private BundleContext bundleContext;
    
    /**
     * Thread pool used for the runtime creation of FST modles.
     * @see #FST_THREAD_POOL_SIZE
     * @see #DEFAULT_FST_THREAD_POOL_SIZE
     */
    private ExecutorService fstCreatorService;

    /**
     * The field name in the configured Solr index holding type information for
     * Entities.
     */
    private String solrTypeField;
    
    /**
     * The field name in the configured Solr index holding ranking information for
     * Entities. values are expected to be floating point numbers.
     */
    private String solrRankingField;
    /**
     * The fieldEncoding used by the configured SolrIndex for field names.
     * @see FieldEncodingEnum
     */
    private FieldEncodingEnum fieldEncoding;
    
    /**
     * Cache used for Lucene {@link Document}s recently loaded from the index.
     * The size can be configured by using the {@link #ENTITY_CACHE_SIZE}
     * configuration parameter.
     * @see #ENTITY_CACHE_SIZE
     * @see #DEFAULT_ENTITY_CACHE_SIZE
     */
    private EntityCacheManager documentCacheFactory;

    private IndexConfiguration indexConfig;

    private Boolean skipAltTokensConfig;
    
    /**
     * Default constructor as used by OSGI. This expects that 
     * {@link #activate(ComponentContext)} is called before usage
     */
    public FstLinkingEngineComponent() {
    }

    @Activate
    @SuppressWarnings("unchecked")
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        log.info("activate {}",getClass().getSimpleName());
        this.bundleContext = ctx.getBundleContext();
        Dictionary<String,Object> properties = ctx.getProperties();
        //(0) The name for the Enhancement Engine and the basic metadata
        Object value = properties.get(PROPERTY_NAME);
        if(value == null || value.toString().isEmpty()){
            throw new ConfigurationException(PROPERTY_NAME, "The EnhancementEngine name MUST BE configured!");
        } else {
            this.engineName = value.toString();
        }
        engineMetadata = new Hashtable<String,Object>();
        engineMetadata.put(PROPERTY_NAME, this.engineName);
        value = properties.get(Constants.SERVICE_RANKING);
        engineMetadata.put(Constants.SERVICE_RANKING, value == null ? Integer.valueOf(0) : value);

        //(1) parse the TextProcessing configuration
        //TODO: decide if we should use the TextProcessingConfig for this engine
        textProcessingConfig = TextProcessingConfig.createInstance(properties);
        entityLinkerConfig = EntityLinkerConfig.createInstance(properties, prefixService);
        
        //(2) parse the configured IndexReference
        value = properties.get(SOLR_CORE);
        if(value == null){
            throw new ConfigurationException(SOLR_CORE, "Missing required configuration of the SolrCore");
        } else {
            indexReference = IndexReference.parse(value.toString());
        }
        value = properties.get(IndexConfiguration.FIELD_ENCODING);
        if(value == null){
            throw new ConfigurationException(IndexConfiguration.FIELD_ENCODING, "Missing required configuration of the Solr Field Encoding");
        } else {
            try {
                fieldEncoding = FieldEncodingEnum.valueOf(value.toString().trim());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(IndexConfiguration.FIELD_ENCODING, "The configured " 
                        + "FieldEncoding MUST BE a member of "
                        + Arrays.toString(FieldEncodingEnum.values()), e);
            }
        }
        value = properties.get(IndexConfiguration.SKIP_ALT_TOKENS);
        if(value instanceof Boolean){
            skipAltTokensConfig = ((Boolean)value);
        } else if(value != null){
            skipAltTokensConfig = new Boolean(value.toString());
        } // else no config -> will use the default
        
        //(4) init the FST configuration
        //We can create the default configuration only here, as it depends on the
        //name of the solrIndex
        String defaultConfig = "*;" 
                + IndexConfiguration.PARAM_FST + "=" + indexReference.getIndex() + ";"
                + IndexConfiguration.PARAM_FIELD + "=" + IndexConfiguration.DEFAULT_FIELD;
        fstConfig = new LanguageConfiguration(IndexConfiguration.FST_CONFIG, new String[]{defaultConfig});
        //now set the actual configuration parsed to the engine
        value = properties.get(IndexConfiguration.FST_CONFIG);
        if(value != null && !StringUtils.isBlank(value.toString())){
            fstConfig.setConfiguration(properties);
        } //else keep the default
        
        value = properties.get(IndexConfiguration.FST_FOLDER);
        if(value instanceof String){
            this.fstFolder = ((String)value).trim();
            if(this.fstFolder.isEmpty()){
                this.fstFolder = null;
            }
        } else if(value == null){
            this.fstFolder = null;
        } else {
            throw new ConfigurationException(IndexConfiguration.FST_FOLDER, "Values MUST BE of type String"
                + "(found: "+value.getClass().getName()+")!");
        }
        
        //(5) Create the ThreadPool used for the runtime creation of FST models
        value = properties.get(FST_THREAD_POOL_SIZE);
        int tpSize;
        if(value instanceof Number){
            tpSize = ((Number)value).intValue();
        } else if(value != null){
            try {
                tpSize = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(FST_THREAD_POOL_SIZE, 
                    "Unable to parse the integer FST thread pool size from the "
                    + "configured "+value.getClass().getSimpleName()+" '" 
                    + value+"'!",e);
            }
        } else {
            tpSize = -1;
        }
        if(tpSize <= 0){ //if configured value <= 0 we use the default
            tpSize = DEFAULT_FST_THREAD_POOL_SIZE;
        }
        //build a ThreadFactoryBuilder for low priority daemon threads that
        //do use a meaningful name
        ThreadFactoryBuilder tfBuilder = new ThreadFactoryBuilder();
        tfBuilder.setDaemon(true);//should be stopped if the VM closes
        tfBuilder.setPriority(Thread.MIN_PRIORITY); //low priority
        tfBuilder.setNameFormat(engineName+"-FstRuntimeCreation-thread-%d");
        if(fstCreatorService != null && !fstCreatorService.isTerminated()){
            //NOTE: We can not call terminateNow, because to interrupt threads
            //      here would also close FileChannels used by the SolrCore
            //      and produce java.nio.channels.ClosedByInterruptException
            //      exceptions followed by java.nio.channels.ClosedChannelException
            //      on following calls to affected files of the SolrIndex.
            
            //Because of that we just log a warning and let uncompleted tasks
            //complete!
            log.warn("some items in a previouse FST Runtime Creation Threadpool have "
                + "still not finished!");
        }
        fstCreatorService = Executors.newFixedThreadPool(tpSize,tfBuilder.build());
        
        //(6) Parse the EntityCache config
        int ecSize;
        value = properties.get(ENTITY_CACHE_SIZE);
        if(value instanceof Number){
            ecSize = ((Number)value).intValue();
        } else if (value != null){
            try {
                ecSize = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                throw new ConfigurationException(ENTITY_CACHE_SIZE, 
                    "Unable to parse the integer EntityCacheSize from the "
                    + "configured "+value.getClass().getSimpleName()+" '" 
                    + value+"'!",e);
            }
        } else {
            ecSize = -1;
        }
        if(ecSize < 0){
            documentCacheFactory = new FastLRUCacheManager(DEFAULT_ENTITY_CACHE_SIZE);
        } else if(ecSize == 0){
            documentCacheFactory = null;
        } else {
            documentCacheFactory = new FastLRUCacheManager(ecSize);
        }
        
        //(7) parse the Entity type field
        value = properties.get(IndexConfiguration.SOLR_TYPE_FIELD);
        if(value == null || StringUtils.isBlank(value.toString())){
            solrTypeField = null;
        } else {
            solrTypeField = value.toString().trim();
        }
        //(8) parse the Entity Ranking field
        value = properties.get(IndexConfiguration.SOLR_RANKING_FIELD);
        if(value == null){
            solrRankingField = null;
        } else {
            solrRankingField = value.toString().trim();
        }
        
        //(9) start tracking the SolrCore
        try {
            solrServerTracker = new RegisteredSolrServerTracker(
                bundleContext, indexReference, null){
                
                @Override
                public void removedService(ServiceReference reference, Object service) {
                    updateEngineRegistration(solrServerTracker.getServiceReference(), null); 
                    super.removedService(reference, service);
                }
                

                @Override
                public void modifiedService(ServiceReference reference, Object service) {
                    updateEngineRegistration(solrServerTracker.getServiceReference(), null); 
                    super.modifiedService(reference, service);
                }
                
                @Override
                public SolrServer addingService(ServiceReference reference) {
                    SolrServer server = super.addingService(reference);
                    if(solrCore != null){
                        log.warn("Multiple SolrServer for IndexLocation {} available!",
                            indexReference);
                    } else {
                        updateEngineRegistration(reference, server);
                    }
                    return server;
                }
            };
        } catch (InvalidSyntaxException e) {
            throw new ConfigurationException(SOLR_CORE, "parsed SolrCore name '"
                +value.toString()+"' is invalid (expected: '[{server-name}:]{indexname}'");
        }
        solrServerTracker.open();
    }
    
    /**
     * This will be called on each <ul>
     * <li>update to the Component configuration (activate, deactivate)
     * <li>updates on the SolrCore
     * </ul>
     * on any detected change it will update the registered EnhancementEngine.<p>
     * This also initialises the FST configuration.
     * @param reference the ServiceRefernece for the SolrServer or <code>null</code>
     * in case the service is no longer available.
     * @param server the SolrServer (or <code>null</code>
     */
    protected void updateEngineRegistration(ServiceReference reference, SolrServer server) {
        if(reference != null && server == null){
            server = solrServerTracker.getService(reference);
        }
        if(reference == null && this.indexReference == null){
            return; //nothing to do
        }
        BundleContext bundleContext = this.bundleContext;
        synchronized (this) { //init one after the other in case of multiple calls
            SolrCore core;
            IndexConfiguration indexConfig; // the indexConfig build by this call
            try {
                if(bundleContext == null){ //already deactivated
                    return; //NOTE: unregistering is done in finally block
                }
                if(reference != null){
                    if(reference.equals(this.solrServerReference)){
                      //use the current core
                        core = solrCore;
                    } else { //get the SolrCore from the EmbeddedSolrServer
                        core = getSolrCore(server);
                    }
                } else {//SolrCore not available
                    core = null;
                }
                if(core == null){ //no SolrCore
                    return; //NOTE: unregistering is done in finally block
                } //else - we do have a SolrCore
                //File fstDir = new File(dataDir,"fst");
                //now collect the FST configuration
                indexConfig = new IndexConfiguration(fstConfig, core, fieldEncoding);
                indexConfig.setTypeField(solrTypeField);
                indexConfig.setRankingField(solrRankingField);
                //set fields parsed in the activate method
                indexConfig.setExecutorService(fstCreatorService);
                indexConfig.setRedirectField(null);//TODO add support
                //NOTE: the FST cofnig is processed even if the SolrCore has not changed
                //      because their might be config changes and/or new FST files in the
                //      FST directory of the SolrCore.
                indexConfig.setFstDirectory(getFstDirectory(core, fstFolder));
                //set the DocumentCacheFactory
                indexConfig.setEntityCacheManager(documentCacheFactory);
                if(skipAltTokensConfig != null){
                    indexConfig.setSkipAltTokens(skipAltTokensConfig);
                }
                //create a new searcher for creating FSTs
                boolean foundCorpus;
                try {
                    foundCorpus = indexConfig.activate();
                }catch (RuntimeException e) { //in case of any excpetion
                    unregisterEngine(); //unregister current engine and clean up
                    throw e; //re-throw 
                }
                if(!foundCorpus){
                    unregisterEngine(); //unregister current engine and clean up
                    throw new IllegalStateException("Processing of the FST configuration " +
                    		"was not successfull for any language. See WARN level loggings " +
                    		"for more details!");
                } else { //some FST corpora initialised
                    if(log.isInfoEnabled()){ //log the initialised languages
                        Set<String> langSet = new HashSet<String>(indexConfig.getCorpusLanguages());
                        if(langSet.remove(null)){ //replace the null for the default language
                            langSet.add(""); //with an empty string
                        }
                        String[] langArray = langSet.toArray(new String[langSet.size()]);
                        Arrays.sort(langArray,String.CASE_INSENSITIVE_ORDER);
                        log.info(" ... initialised FST corpora for languages {}",
                            Arrays.toString(langArray));
                    }
                }
            } finally {
                //in any case (even an Exception) ensure that the current
                //engine registration is unregistered and the currentyl used
                //SolrCore is unregistered!
                unregisterEngine();
            }
            //check if we need to create some FST files
            for(CorpusInfo fstInfo : indexConfig.getCorpora()){
                //check if the fst does not exist and the fstInfo allows creation
                if(!fstInfo.fst.exists() && fstInfo.allowCreation){
                    //create a task on the FST corpus creation service
                    fstCreatorService.execute(new CorpusCreationTask(indexConfig, fstInfo));
                }
            }
            //set the default linking corpora
            String defaultLanguage = entityLinkerConfig.getDefaultLanguage();
            if(defaultLanguage == null){
                defaultLanguage = ""; //FST uses an empty string for the default
            }
            CorpusInfo defaultCoprous = indexConfig.getCorpus(defaultLanguage);
            log.info(" ... set '{}' as default FST Corpus: {}", defaultCoprous.language, defaultCoprous);
            indexConfig.setDefaultCorpus(defaultCoprous);
            //set the index configuration to the field;
            this.indexConfig = indexConfig;
            FstLinkingEngine engine = new FstLinkingEngine(engineName, indexConfig,
                textProcessingConfig, entityLinkerConfig);
            String[] services = new String [] {
                    EnhancementEngine.class.getName(),
                    ServiceProperties.class.getName()};
            this.engineRegistration = bundleContext.registerService(services,engine, engineMetadata);
            this.solrServerReference = reference;
            this.solrCore = core;
        }

        
    }
    /**
     * Resolves the directory to store the FST models based on the configured
     * {@link IndexConfiguration#FST_FOLDER}. Also considering the name of the SolrServer and
     * SolrCore
     * @param core
     * @param fstFolderConfig
     * @return
     */
    private File getFstDirectory(SolrCore core, String fstFolderConfig) {
        StrSubstitutor substitutor = new StrSubstitutor(new SolrCoreStrLookup(
            indexReference, core, bundleContext));
        substitutor.setEnableSubstitutionInVariables(true);
        String folderStr = substitutor.replace(fstFolderConfig);
        if(folderStr.indexOf("${") > 0){
            folderStr = substitutor.replace(folderStr);
        }
        //convert separators to the current OS
        folderStr = FilenameUtils.separatorsToSystem(folderStr);
        File fstDir = new File(folderStr);
        if(!fstDir.isDirectory()){ //create the FST directory
            try {
                FileUtils.forceMkdir(fstDir);
            } catch (IOException e) {
                unregisterEngine(); //unregister current engine and clean up
                throw new IllegalStateException("Unable to create Directory for"
                        + "storing the FST files at location '"+fstDir+"'.");
            }
        }
        
        return fstDir;
    }

    /**
     * unregisters the Engines service registration, closes the SolrCore and
     * rests the fields. If no engine is registered this does nothing!
     */
    private void unregisterEngine() {
        //use local copies for method calls to avoid concurrency issues
        ServiceRegistration engineRegistration = this.engineRegistration;
        if(engineRegistration != null){
            engineRegistration.unregister();
            this.engineRegistration = null; //reset the field
        }
        solrServerReference = null;
        SolrCore solrServer = this.solrCore;
        if(solrServer != null){
            solrServer.close(); //decrease the reference count!!
            this.solrCore = null; //rest the field
        }
        //deactivate the index configuration if present
        if(indexConfig != null){
            indexConfig.deactivate();
            indexConfig = null;
        }
    }

    /**
     * Internal helper to get th SolrCore from the tracked SolrServer. This
     * assumes that tracked SolrServers are of type {@link EmbeddedSolrServer}.
     * @param server the SolrServer
     * @return the SolrCore or <code>null</code> if <code>null</code> is parsed
     * as server.
     * @throws IllegalStateException if the parsed {@link SolrServer} is not an
     * {@link EmbeddedSolrServer} or it does not contain the configured SolrCore 
     */
    private SolrCore getSolrCore(SolrServer server) {
        SolrCore core;
        if(server != null){
            if(server instanceof EmbeddedSolrServer){
                core = ((EmbeddedSolrServer)server).getCoreContainer().getCore(
                    indexReference.getIndex());
                if(core == null){
                    throw new IllegalStateException("Solr CoreContainer for IndexRef '"
                            + indexReference + "'is missing the expected SolrCore '"
                            + indexReference.getIndex() + "' (present: "
                            + ((EmbeddedSolrServer)server).getCoreContainer().getCoreNames()
                            + ")!");
                }
            } else {
                core = null;
                throw new IllegalStateException("Unable to use '" 
                    + server.getClass().getSimpleName() + "' (indexRef: " 
                    +indexReference+") because it is not an EmbeddedSolrServer!");
            }
        } else {
            core = null;
        }
        return core;
    }



    /**
     * Deactivates this components. 
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        if(solrServerTracker != null){
            //closing the tracker will also cause registered engines to be
            //unregistered as service (see #updateEngineRegistration())
            solrServerTracker.close();
            solrServerTracker = null;
        }
        if(fstCreatorService != null){
            //we MUST NOT call shutdownNow(), because this would close
            //low level Solr FileChannels.
            fstCreatorService.shutdown();
            //do not set NULL, as we want to warn users an re-activation if old
            //threads are still running.
        }
        indexReference = null;
        engineMetadata = null;
        textProcessingConfig = null;
        entityLinkerConfig = null;
        bundleContext = null;
        skipAltTokensConfig = null;
    }
    
    /**
     * {@link StrSubstitutor} {@link StrLookup} implementation used for
     * determining the directory for storing FST files based on the configured
     * {@link IndexConfiguration#FST_FOLDER} configuration.
     * @author Rupert Westenthaler
     *
     */
    private static class SolrCoreStrLookup extends StrLookup {

        private final BundleContext bc;
        private final SolrCore core;
        private final IndexReference indexRef;

        public SolrCoreStrLookup(IndexReference indexRef, SolrCore core, BundleContext bc) {
            this.indexRef = indexRef;
            this.core = core;
            this.bc = bc;
        }
        
        @Override
        public String lookup(String key) {
            if("solr-data-dir".equals(key)){
                return core.getDataDir();
            } else if("solr-index-dir".equals(key)){
                return core.getIndexDir();
            } else if("solr-server-name".equals(key)){
                return indexRef.getServer();
            } else if("solr-core-name".equals(key)){
                return core.getName();
            } else {
                return bc.getProperty(key);
            }
        }
        
    }
    
}
