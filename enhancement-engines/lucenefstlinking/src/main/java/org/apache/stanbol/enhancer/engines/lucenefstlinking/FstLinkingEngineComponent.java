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
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_DEREFERENCE_ENTITIES_STATE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_MATCHING_LANGUAGE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEFAULT_SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEREFERENCE_ENTITIES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.DEREFERENCE_ENTITIES_FIELDS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.ENTITY_TYPES;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.REDIRECT_FIELD;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.REDIRECT_MODE;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.SUGGESTIONS;
import static org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.TYPE_FIELD;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.util.NamedThreadFactory;
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
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.engine.EntityLinkingEngine;
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
    @Property(name=FstLinkingEngineComponent.FIELD_ENCODING, options={
        @PropertyOption(
            value='%'+FstLinkingEngineComponent.FIELD_ENCODING+".option.none",
            name="None"),
        @PropertyOption(
            value='%'+FstLinkingEngineComponent.FIELD_ENCODING+".option.solrYard",
            name="SolrYard"),
        @PropertyOption(
            value='%'+FstLinkingEngineComponent.FIELD_ENCODING+".option.minusPrefix",
            name="MinusPrefix"),
        @PropertyOption(
            value='%'+FstLinkingEngineComponent.FIELD_ENCODING+".option.underscorePrefix",
            name="UnderscorePrefix"),
        @PropertyOption(
            value='%'+FstLinkingEngineComponent.FIELD_ENCODING+".option.minusSuffix",
            name="MinusSuffix"),
        @PropertyOption(
            value='%'+FstLinkingEngineComponent.FIELD_ENCODING+".option.underscoreSuffix",
            name="UnderscoreSuffix"),
        @PropertyOption(
            value='%'+FstLinkingEngineComponent.FIELD_ENCODING+".option.atPrefix",
            name="AtPrefix"),
        @PropertyOption(
            value='%'+FstLinkingEngineComponent.FIELD_ENCODING+".option.atSuffix",
            name="AtSuffix")
        },value="SolrYard"),
    @Property(name=FstLinkingEngineComponent.FST_CONFIG, cardinality=Integer.MAX_VALUE),
    @Property(name=FstLinkingEngineComponent.FST_THREAD_POOL_SIZE,
        intValue=FstLinkingEngineComponent.DEFAULT_FST_THREAD_POOL_SIZE),
    @Property(name=FstLinkingEngineComponent.ENTITY_CACHE_SIZE, 
        intValue=FstLinkingEngineComponent.DEFAULT_ENTITY_CACHE_SIZE),
    @Property(name=FstLinkingEngineComponent.SOLR_TYPE_FIELD, value="rdf:type"),
    @Property(name=FstLinkingEngineComponent.SOLR_RANKING_FIELD, value="entityhub:entityRank"),
//    @Property(name=REDIRECT_FIELD,value="rdfs:seeAlso"),
//    @Property(name=REDIRECT_MODE,options={
//        @PropertyOption(
//            value='%'+REDIRECT_MODE+".option.ignore",
//            name="IGNORE"),
//        @PropertyOption(
//            value='%'+REDIRECT_MODE+".option.addValues",
//            name="ADD_VALUES"),
//        @PropertyOption(
//                value='%'+REDIRECT_MODE+".option.follow",
//                name="FOLLOW")
//        },value="IGNORE"),
    @Property(name=TYPE_FIELD,value="rdf:type"),
    @Property(name=ENTITY_TYPES,cardinality=Integer.MAX_VALUE),
    @Property(name=SUGGESTIONS, intValue=DEFAULT_SUGGESTIONS),
    @Property(name=CASE_SENSITIVE,boolValue=DEFAULT_CASE_SENSITIVE_MATCHING_STATE),
    @Property(name=PROCESS_ONLY_PROPER_NOUNS_STATE, boolValue=DEFAULT_PROCESS_ONLY_PROPER_NOUNS_STATE),
    @Property(name=PROCESSED_LANGUAGES, cardinality=Integer.MAX_VALUE,
        value={"*;lmmtip;uc=LINK;prob=0.75;pprob=0.75", // link multiple matchable tokens in chunks; link upper case words
               "de;uc=MATCH", //in German all Nouns are upper case
               "es;lc=Noun", //the OpenNLP POS tagger for Spanish does not support ProperNouns
               "nl;lc=Noun"}), //same for Dutch 
    @Property(name=DEFAULT_MATCHING_LANGUAGE,value=""),
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
    public static final String SOLR_CORE = "enhancer.engines.linking.solrfst.solrcore";
    
    /**
     * Language configuration defining the language, solr field and the name of the
     * FST file. The FST file is looked up using the {@link DataFileProvider}.
     */
    public static final String FST_CONFIG = "enhancer.engines.linking.solrfst.fstconfig";
    /**
     * The name of the Solr field holding the entity type information
     */
    public static final String SOLR_TYPE_FIELD = "enhancer.engines.linking.solrfst.typeField";
    /**
     * The name of the Solr field storing rankings for entities. Entities with a
     * higher value are considered as better (more popular).
     */
    public static final String SOLR_RANKING_FIELD = "enhancer.engines.linking.solrfst.rankingField";
    /**
     * Property used to configure the FieldName encoding of the SolrIndex. This
     * is mainly needed for label fields of different languages (e.g. by using 
     * the iso language code as prefix/suffix of Solr fields. However this also
     * adds support for SolrIndexes encoded as specified by the Stanbol
     * Entityhub SolrYard implementation. See {@link FieldEncodingEnum} for 
     * supported values
     */
    public static final String FIELD_ENCODING = "enhancer.engines.linking.solrfst.fieldEncoding";
    /**
     * Parameter used by the {@link #FST_CONFIG} to configure the Solr Field 
     * with the indexed labels used to buld the FST corpus.
     */
    public static final String PARAM_FIELD = "field";
    /**
     * Parameter used by the {@link #FST_CONFIG} to configure the solrField with
     * the stored labels. If not defined this defaults to the configured
     * {@link #PARAM_FIELD}.
     */
    public static final String PARAM_STORE_FIELD = "stored";
    
    public static final String DEFAULT_FIELD = "rdfs:label";
    /**
     * Parameter used by the {@link #FST_CONFIG} to configure the name of the fst
     * file for a language
     */
    public static final String PARAM_FST = "fst";
    /**
     * Parameter that specifies if FST files are allowed to be generated at runtime.
     * Enabling this will require (1) write access to the SolrCore directory and
     * (2) a lot of Memory and CPU usage during the generation.
     */
    public static final String PARAM_RUNTIME_GENERATION = "generate";
    /**
     * By default runtime generation for the FST is deactivated. Use the
     * {@link #PARAM_RUNTIME_GENERATION} to enable it.
     */
    public static final boolean DEFAULT_RUNTIME_GENERATION = false;
    /**
     * The size of the thread pool used to create FST models (default=1). Creating
     * such models does need a lot of memory. Expect values up to 10times of the
     * build model. So while this task can easily performed concurrently users need
     * to be aware that the process will occupy a lot of heap space (typically several
     * GBytes). If heap space is not an issue it is best to configure the value
     * based on the CPU cores available on the local host.<p>
     * This configuration has only an effect if runtime generation of FST modles
     * is enabled (either by default or for some FST by explicitly setting the 
     * '<code>{@link #PARAM_RUNTIME_GENERATION generate}=true</code>' parameter 
     * for some languages in the {@link #FST_CONFIG}.
     */
    public static final String FST_THREAD_POOL_SIZE = "enhancer.engines.linking.solrfst.fstThreadPoolSize";
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
    public static final String ENTITY_CACHE_SIZE = "enhancer.engines.linking.solrfst.entityCacheSize";
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
        value = properties.get(FIELD_ENCODING);
        if(value == null){
            throw new ConfigurationException(FIELD_ENCODING, "Missing required configuration of the Solr Field Encoding");
        } else {
            try {
                fieldEncoding = FieldEncodingEnum.valueOf(value.toString().trim());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException(FIELD_ENCODING, "The configured " 
                        + "FieldEncoding MUST BE a member of "
                        + Arrays.toString(FieldEncodingEnum.values()), e);
            }
        }
        
        //(4) init the FST configuration
        //We can create the default configuration only here, as it depends on the
        //name of the solrIndex
        String defaultConfig = "*;" 
                + PARAM_FST + "=" + indexReference.getIndex() + ";"
                + PARAM_FIELD + "=" + DEFAULT_FIELD;
        fstConfig = new LanguageConfiguration(FST_CONFIG, new String[]{defaultConfig});
        //now set the actual configuration parsed to the engine
        value = properties.get(FST_CONFIG);
        if(value != null && !StringUtils.isBlank(value.toString())){
            fstConfig.setConfiguration(properties);
        } //else keep the default
        
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
        value = properties.get(SOLR_TYPE_FIELD);
        if(value == null || StringUtils.isBlank(value.toString())){
            solrTypeField = null;
        } else {
            String typeField = value.toString();
            solrTypeField = StringUtils.isBlank(typeField) ? null : 
                FieldEncodingEnum.encodeUri(typeField.trim(),fieldEncoding);
        }
        //(8) parse the Entity Ranking field
        value = properties.get(SOLR_RANKING_FIELD);
        if(value == null){
            solrRankingField = null;
        } else {
            String rankingField = value.toString();
            solrRankingField = StringUtils.isBlank(rankingField) ? null : 
                FieldEncodingEnum.encodeFloat(rankingField.trim(),fieldEncoding);
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
                //NOTE: the FST cofnig is processed even if the SolrCore has not changed
                //      because their might be config changes and/or new FST files in the
                //      FST directory of the SolrCore.
                String dataDir = core.getDataDir();
                File fstDir = new File(dataDir,"fst");
                if(!fstDir.isDirectory()){ //create the FST directory
                    try {
                        FileUtils.forceMkdir(fstDir);
                    } catch (IOException e) {
                        unregisterEngine(); //unregister current engine and clean up
                        throw new IllegalStateException("Unable to create Directory for"
                                + "storing the FST files within the SolrCore data dir.");
                    }
                }
                //now collect the FST configuration
                indexConfig = new IndexConfiguration(fstConfig, core);
                //set fields parsed in the activate method
                indexConfig.setFieldEncoding(fieldEncoding);
                indexConfig.setExecutorService(fstCreatorService);
                indexConfig.setTypeField(solrTypeField);
                indexConfig.setRankingField(solrRankingField);
                indexConfig.setRedirectField(null);//TODO add support
                //set the DocumentCacheFactory
                indexConfig.setEntityCacheManager(documentCacheFactory);
                //create a new searcher for creating FSTs
                RefCounted<SolrIndexSearcher> searcherRef = core.getSearcher(true, true, null);
                boolean foundCorpus;
                try {
                    foundCorpus = processFstConfig(indexConfig, fstDir, searcherRef.get().getAtomicReader());
                }catch (RuntimeException e) { //in case of any excpetion
                    unregisterEngine(); //unregister current engine and clean up
                    throw e; //re-throw 
                } finally {
                    searcherRef.decref(); //decrease the count on the searcher
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
     * This method combines the {@link #fstConfig} with the data present in the
     * {@link SolrCore}.
     * @param fstDir The directory used to look for the FST files
     * @param indexReader The {@link AtomicReader} has access to the actual
     * fields present in the {@link SolrCore}. It is used to compare field
     * configurations in the {@link #fstConfig} with fields present in the solr
     * index.
     * @param indexConfig the {@link IndexConfiguration} used to store the FST
     * configuration
     */
    private boolean processFstConfig(IndexConfiguration indexConfig, File fstDir, AtomicReader indexReader) {
        log.info("> process FST config for {} (FST dir: {})",indexReference,
            fstDir.getAbsolutePath());
        IndexSchema schema = indexConfig.getIndex().getSchema();
        boolean foundCorpus = false;
        //(0) get basic parameters of the default configuration
        log.info(" - default config");
        Map<String,String> defaultParams = fstConfig.getDefaultParameters();
        String fstName = defaultParams.get(PARAM_FST);
        final String indexField = defaultParams.get(PARAM_FIELD);
        final String storeField = defaultParams.get(PARAM_STORE_FIELD);
        if(fstName == null){ //use default
            fstName = getDefaultFstFileName(indexField);
        }
        final boolean allowCreation;
        String allowCreationString = defaultParams.get(PARAM_RUNTIME_GENERATION);
        if(allowCreationString == null){
            allowCreation = DEFAULT_RUNTIME_GENERATION;
        } else {
            allowCreation = Boolean.parseBoolean(allowCreationString);
        }
        //This are all fields actually present in the index (distinguished with
        //those defined in the schema). This also includes actual instances of
        //dynamic field definition in the schema.
        FieldInfos fieldInfos = indexReader.getFieldInfos(); //we need this twice
        
        //(1) in case the fstConfig uses a wildcard we need to search for
        //    languages present in the SolrIndex. For that we use the indexReader
        //    to get the FieldInfos and match them against FST files in the FST
        //    directory and FieldType definitions in the schema of the SolrCore
        //NOTE: this needs only do be done if wildcards are enabled in the fstConfig
        if(fstConfig.useWildcard()){ 
            //(1.a) search for present FST files in the FST directory
            Map<String,File> presentFstFiles = new HashMap<String,File>();
            WildcardFileFilter fstFilter = new WildcardFileFilter(
                fstName+".*.fst");
            @SuppressWarnings("unchecked")
            Iterator<File> fstFiles = FileUtils.iterateFiles(fstDir, fstFilter, null);
            while(fstFiles.hasNext()){
                File fstFile = fstFiles.next();
                String fstFileName = fstFile.getName();
                //files are named such as "{name}.{lang}.fst"
                String language = FilenameUtils.getExtension(
                    FilenameUtils.getBaseName(fstFileName));
                presentFstFiles.put(language, fstFile);
            }
            //(1.b) iterate over the fields in the Solr index and search for 
            //      matches against the configured indexField name
            String fieldWildcard = FieldEncodingEnum.encodeLanguage(indexField,
                indexConfig.getFieldEncoding(), "*");
            for(FieldInfo fieldInfo : fieldInfos){
                //try to match the field names against the wildcard
                if(FilenameUtils.wildcardMatch(fieldInfo.name, fieldWildcard)){
                    //for matches parse the language from the field name
                    String language = FieldEncodingEnum.parseLanguage(
                        fieldInfo.name, indexConfig.getFieldEncoding(), indexField);
                    if(language != null && //successfully parsed language
                            //is current language is enabled? 
                            fstConfig.isLanguage(language) &&
                            //is there no explicit configuration for this language?
                            !fstConfig.getExplicitlyIncluded().contains(language)){
                        //generate the FST file name
                        StringBuilder fstFileName = new StringBuilder(fstName);
                        if(!language.isEmpty()){
                            fstFileName.append('.').append(language);
                        }
                        fstFileName.append(".fst");
                        File fstFile = new File(fstDir,fstFileName.toString());
                        //get the FieldType of the field from the Solr schema
                        FieldType fieldType = schema.getFieldTypeNoEx(fieldInfo.name);
                        if(fieldType != null){ //if the fieldType is present
                            if(allowCreation || fstFile.isFile()){ //and FST is present or can be created
                                //we need also to check if the stored field with
                                //the labels is present
                                //get the stored Field and check if it is present!
                                String storeFieldName;
                                if(storeField == null){ //storeField == indexField
                                    storeFieldName = fieldInfo.name;
                                } else { // check that the storeField is present in the index
                                    storeFieldName = FieldEncodingEnum.encodeLanguage(
                                        storeField, indexConfig.getFieldEncoding(), language);
                                    FieldInfo storedFieldInfos = fieldInfos.fieldInfo(storeFieldName);
                                    if(storedFieldInfos == null){
                                        log.warn(" ... ignore language {} because Stored Field {} "
                                                + "for IndexField {} does not exist! ", new Object[]{
                                                language,storeFieldName,fieldInfo.name});
                                        storeFieldName = null;
                                    }
                                    
                                }
                                if(storeFieldName != null){ // == valid configuration
                                    CorpusInfo fstInfo = new CorpusInfo(language, 
                                        fieldInfo.name, storeFieldName,  
                                        fieldType.getAnalyzer(), fstFile, allowCreation);
                                    log.debug(" ... init {} ", fstInfo);
                                    indexConfig.addCorpus(fstInfo);
                                    foundCorpus = true;
                                }
                            } else {
                                log.warn(" ... ignore language {} (field: {}) because "
                                    + "FST file '{}' does not exist and runtime creation "
                                    + "is deactivated!",new Object[]{ language,
                                            fieldInfo.name, fstFile.getAbsolutePath()});
                            }
                        } else {
                            log.warn(" ... ignore language {} becuase unknown fieldtype "
                                + "for SolrFied {}",language,fieldInfo.name);
                        }
                    } //else the field matched the wildcard, but has not passed the
                    //encoding test.
                } //Solr field does not match the field definition in the config
            } // end iterate over all fields in the SolrIndex
        } //else Wildcard not enabled in the fstConfig
        
        //(2) process explicit configuration for configured languages
        for(String language : fstConfig.getExplicitlyIncluded()){
            //(2.a) get the language specific config (with fallback to default)
            Map<String,String> config = fstConfig.getLanguageParams(language);
            String langIndexField = config.get(PARAM_FIELD);
            String langStoreField = config.get(PARAM_STORE_FIELD);
            String langFstFileName = config.get(PARAM_FST);
            final boolean langAllowCreation;
            final String langAllowCreationString = config.get(PARAM_RUNTIME_GENERATION);
            if(langIndexField != null){
                //also consider explicit field names as default for the fst name
                if(langFstFileName == null){
                    StringBuilder fileName = new StringBuilder(
                        getDefaultFstFileName(langIndexField));
                    if(!language.isEmpty()){
                        fileName.append('.').append(language);
                    }
                    fileName.append(".fst");
                    langFstFileName = fileName.toString();
                }
            } else {
                langIndexField = indexField;
            }
            if(langStoreField == null){ //fallbacks
                if(storeField != null){ //first to default store field
                    langStoreField = storeField;
                } else { //else to the lang index field
                    langStoreField = langIndexField;
                }
            }
            if(langFstFileName == null){ //no fstFileName config
                // ... use the default
                langFstFileName = new StringBuilder(fstName).append('.')
                        .append(language).append(".fst").toString(); 
            }
            if(langAllowCreationString != null){
                langAllowCreation = Boolean.parseBoolean(langAllowCreationString);
            } else {
                langAllowCreation = allowCreation;
            }
            //(2.b) check if the Solr field is present
            String encodedLangIndexField = FieldEncodingEnum.encodeLanguage(
                langIndexField, indexConfig.getFieldEncoding(), language);
            String encodedLangStoreField = FieldEncodingEnum.encodeLanguage(
                langStoreField, indexConfig.getFieldEncoding(), language);
            FieldInfo langIndexFieldInfo = fieldInfos.fieldInfo(encodedLangIndexField);
            if(langIndexFieldInfo != null){
                FieldInfo langStoreFieldInfo = fieldInfos.fieldInfo(encodedLangStoreField);
                if(langStoreFieldInfo != null){
                    FieldType fieldType = schema.getFieldTypeNoEx(langIndexFieldInfo.name);
                    if(fieldType != null){
                        //(2.c) check the FST file
                        File langFstFile = new File(fstDir,langFstFileName);
                        if(langFstFile.isFile() || langAllowCreation){
                            CorpusInfo langFstInfo = new CorpusInfo(language, 
                                encodedLangIndexField,encodedLangStoreField,
                                fieldType.getAnalyzer(), langFstFile, langAllowCreation);
                            log.debug("   ... add {} for explicitly configured language", langFstInfo);
                            indexConfig.addCorpus(langFstInfo);
                            foundCorpus = true;
                        } else {
                            log.warn(" ... ignore language {} (field: {}) because "
                                    + "FST file '{}' does not exist and runtime creation "
                                    + "is deactivated!",new Object[]{ language,
                                            langIndexFieldInfo.name, langFstFile.getAbsolutePath()});
                        }
                    } else {
                        log.warn(" ... ignore language {} becuase unknown fieldtype "
                                + "for SolrFied {}", language, langIndexFieldInfo.name);
                    }
                } else {
                    log.warn(" ... ignore language {} because configured stored Field {} "
                            + "for IndexField {} does not exist! ", new Object[]{
                            language,langStoreField,langIndexFieldInfo.name});
                }
            } else {
                log.warn(" ... ignore language {} because configured field {} (encoded: {}) "
                    + "is not present in the SolrIndex!", new Object[]{
                            language, langIndexField, encodedLangIndexField });
            }
        }
        return foundCorpus;
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
     * Getter for the default FST file name based on the configured field
     * name. This method returns the '<code>{name}</code>' part of the
     * '<code>{name}.{lang}.fst</code>' name.
     * @param fstFieldName the field name.
     * @return the '<code>{name}</code>' part of the'<code>{name}.{lang}.fst</code>' name
     */
    private static String getDefaultFstFileName(final String fstFieldName) {
        String fstName;
        if(!StringUtils.isAlphanumeric(fstFieldName)) {
            StringBuilder escaped = new StringBuilder(fstFieldName.length());
            for(int i = 0; i < fstFieldName.length();i++){
                int codepoint = fstFieldName.codePointAt(i);
                if(Character.isLetterOrDigit(codepoint)){
                    escaped.appendCodePoint(codepoint);
                } else {
                    escaped.append('_');
                }
            }
            fstName = escaped.toString();
        } else {
            fstName = fstFieldName;
        }
        return fstName;
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
    }
    
}
