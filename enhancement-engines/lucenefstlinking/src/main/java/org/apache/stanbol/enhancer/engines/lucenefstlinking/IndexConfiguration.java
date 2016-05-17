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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.RDFTerm;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.cache.EntityCacheManager;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.opensextant.solrtexttagger.TaggerFstCorpus;
import org.opensextant.solrtexttagger.UnsupportedTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds the configuration of the index used by the FST linking engine.
 * 
 * @author Rupert Westenthaler
 *
 */
public class IndexConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(IndexConfiguration.class);
    
    private final SolrCore index;
    /**
     * The type field
     */
    private String typeField;

    /**
     * The redirect field
     */
    private String redirectField;
    /**
     * The entityRanking field
     */
    private String rankingField;

    /**
     * Used to sync access to {@link #corpusInfos}
     */
    private ReadWriteLock corpusInfoLock = new ReentrantReadWriteLock();
    /**
     * FST corpus configuration
     */
    private Map<String,CorpusInfo> corpusInfos;
    /**
     * {@link ExecutorService} used to create {@link TaggerFstCorpus} instances
     * at runtime.
     */
    protected ExecutorService executorService;
    /**
     * The encoding used by SolrFields (e.g. to define label fields for different
     * languages).
     */
    private final FieldEncodingEnum fieldEncoding;
    /**
     * The instance used to retrieve/create the cache for Lucene {@link Document}s
     * of Entities.
     */
    private EntityCacheManager entityCacheManager;
    
    private final LanguageConfiguration fstConfig;
    
    /**
     * If runtime generation is enabled by default (Note: explicitly configured
     * lanugages might override this)
     */
    private final boolean runtimeGeneration;

    /**
     * used to track if this index configuration is active
     */
    private boolean active = false;
    
    private long indexVersion = -1;

    private File fstDirectory;
    
    /**
     * The origin is added to <code>fise:TextAnnotation</code> created for
     * linked Entities. It is intended to be used for providing a reference to
     * dataset of the Entity. Both {@link IRI URI}s and {@link Literal}s can
     * be used here
     */
    private RDFTerm origin;

    /**
     * If alternate tokens (<code>posInc == 0</code>) can be skipped or if such
     * tokens should cause an {@link UnsupportedTokenException}.
     */
    private boolean skipAltTokens;

    /**
     * The default language
     */
    private String defaultLanguage;
    /**
     * If alternate tokens (<code>posInc == 0</code>) can be skipped or if such
     * tokens should cause an {@link UnsupportedTokenException}.
     * <p> 
     * While enabling this will allow to use FST linking with query time Lucene
     * {@link Analyzer}s that emit alternate tokens (e.g. the Kuromoji analyzers
     * for Japanese) but it also requires special care with index time 
     * {@link Analyzer} configurations. If enabled the index time analyzer MUST 
     * produce all possible tokens emited by the query time analyzer as only if
     * all such  combinations are added to the FST model skipped alternate 
     * tokens can not prevent mentions from being detected.
     * <p>
     * By default <code>skipAltTokens</code> is enabled for 
     * {@link FieldEncodingEnum#SolrYard} and deactivated for all other field
     * encoding setting. This is because all Solr <code>schema.xml</code> used
     * by the Stanbol Entityhub SolrYard ensure the requirement stated above.
     * For other Solr configurations users will neet to explicitly activate this.
     */
    public static final String SKIP_ALT_TOKENS = "enhancer.engines.linking.lucenefst.skipAltTokens";
    
    /**
     * Property used to configure the FieldName encoding of the SolrIndex. This
     * is mainly needed for label fields of different languages (e.g. by using 
     * the iso language code as prefix/suffix of Solr fields. However this also
     * adds support for SolrIndexes encoded as specified by the Stanbol
     * Entityhub SolrYard implementation. See {@link FieldEncodingEnum} for 
     * supported values
     */
    public static final String FIELD_ENCODING = "enhancer.engines.linking.lucenefst.fieldEncoding";
    /**
     * The name of the Solr field storing rankings for entities. Entities with a
     * higher value are considered as better (more popular).
     */
    public static final String SOLR_RANKING_FIELD = "enhancer.engines.linking.lucenefst.rankingField";
    /**
     * The name of the Solr field holding the entity type information
     */
    public static final String SOLR_TYPE_FIELD = "enhancer.engines.linking.lucenefst.typeField";
    /**
     * Language configuration defining the language, solr field and the name of the
     * FST file. The FST file is looked up using the {@link DataFileProvider}.
     */
    public static final String FST_CONFIG = "enhancer.engines.linking.lucenefst.fstconfig";
    /**
     * The folder used to store the FST files. The {@link DEFAULT_FST_FOLDER default} is 
     * '<code>${solr-data-dir}/fst</code>' - this is '<code>./fst</code>' relative to the
     * {@link SolrCore#getDataDir()} of the current SolrCore.
     */
    public static final String FST_FOLDER = "enhancer.engines.linking.lucenefst.fstfolder";
    /**
     * The default of the FST folder is '<code>${solr-data-dir}/fst</code>' - 
     * this is '<code>./fst</code>' relative to the {@link SolrCore#getDataDir()} 
     * of the current SolrCore.
     */
    public static final String DEFAULT_FST_FOLDER = "${solr-data-dir}/fst";
    /**
     * By default runtime generation for the FST is deactivated. Use the
     * {@link PARAM_RUNTIME_GENERATION} to enable it.
     */
    public static final boolean DEFAULT_RUNTIME_GENERATION = false;
    /**
     * Parameter that specifies if FST files are allowed to be generated at runtime.
     * Enabling this will require (1) write access to the SolrCore directory and
     * (2) a lot of Memory and CPU usage during the generation.
     */
    public static final String PARAM_RUNTIME_GENERATION = "generate";
    /**
     * Parameter used by the {@link IndexConfiguration#FST_CONFIG} to configure the solrField with
     * the stored labels. If not defined this defaults to the configured
     * {@link PARAM_FIELD}.
     */
    public static final String PARAM_STORE_FIELD = "stored";
    /**
     * Parameter used by the {@link IndexConfiguration#FST_CONFIG} to configure the Solr Field 
     * with the indexed labels used to buld the FST corpus.
     */
    public static final String PARAM_FIELD = "field";
    public static final String DEFAULT_FIELD = "rdfs:label";
    /**
     * Parameter used by the {@link IndexConfiguration#FST_CONFIG} to configure the name of the fst
     * file for a language
     */
    public static final String PARAM_FST = "fst";
    
    public IndexConfiguration(LanguageConfiguration fstConfig, SolrCore index, FieldEncodingEnum fieldEncoding, String defaultLanguage){
        if(fstConfig == null){
            throw new IllegalArgumentException("The parsed FST configuration MUST NOT be NULL!");
        }
        this.fstConfig = fstConfig;
        if(index == null || index.isClosed()){
            throw new IllegalArgumentException("The parsed SolrCore MUST NOT be NULL nore closed!");
        }
        //check if we have runtime generation
        String allowCreationString = fstConfig.getDefaultParameters().get(IndexConfiguration.PARAM_RUNTIME_GENERATION);
        if(allowCreationString == null){
            runtimeGeneration = IndexConfiguration.DEFAULT_RUNTIME_GENERATION;
        } else {
            runtimeGeneration = Boolean.parseBoolean(allowCreationString);
        }
        
        this.index = index;
        if(fieldEncoding == null){
            fieldEncoding = FieldEncodingEnum.None;
        }
        this.fieldEncoding = fieldEncoding;
        //In case of a SolrYard we can activate skipAltTokens (see javadoc for
        //#SKIP_ALT_TOKENS for more information)
        if(fieldEncoding == FieldEncodingEnum.SolrYard){
            this.skipAltTokens = true;
        } else {
            this.skipAltTokens = false;
        }
        this.defaultLanguage = defaultLanguage == null ? "" : defaultLanguage;
    }
    
    /**
     * Assumed to be called in a write lock on {@link #corpusInfoLock}
     * @param corpus
     */
    private void addCorpusInfo(CorpusInfo corpus){
        if(corpus != null){
            corpusInfos.put(corpus.language, corpus);
        }
    }
    
    protected CorpusInfo removeCorpus(String language){
        return corpusInfos.remove(language);
    }
    /**
     * @return the fieldEncoding
     */
    public final FieldEncodingEnum getFieldEncoding() {
        return fieldEncoding;
    }

    /**
     * @return the typeField
     */
    public final String getEncodedTypeField() {
        return typeField;
    }

    /**
     * Sets AND encodes the parsed value (based on the specified 
     * {@link #getFieldEncoding() FieldEncoding})
     * @param typeField the typeField to set
     */
    public final void setTypeField(String typeField) {
        this.typeField = typeField == null ? null :
            FieldEncodingEnum.encodeUri(typeField, fieldEncoding);
    }
    /**
     * @return the redirectField
     */
    public final String getEncodedRedirectField() {
        return redirectField;
    }
    /**
     * Sets AND encodes the parsed value (based on the specified 
     * {@link #getFieldEncoding() FieldEncoding})
     * @param redirectField the redirectField to set
     */
    public final void setRedirectField(String redirectField) {
        this.redirectField = redirectField == null ? null :
            FieldEncodingEnum.encodeUri(redirectField, fieldEncoding);
    }
    /**
     * @return the rankingField
     */
    public final String getEncodedRankingField() {
        return rankingField;
    }
    /**
     * Sets AND encodes the parsed value (based on the specified 
     * {@link #getFieldEncoding() FieldEncoding})
     * @param rankingField the rankingField to set
     */
    public final void setRankingField(String rankingField) {
        this.rankingField = rankingField == null ? null :
            FieldEncodingEnum.encodeFloat(rankingField, fieldEncoding);
    }
    /**
     * The version of the {@link #getIndex()} this configuration was built for.
     * @return the index version this configuration was built for.
     */
    public long getVersion() {
        return indexVersion;
    }
    /**
     * Returns the CorpusInfo for the parsed language. If the language has an
     * extension (e.g. en-US) it first tires to load the corpus for the exact
     * match and falls back to the main lanugage (en) if such a corpus does not
     * exist.
     * @param language the language
     * @return the corpus information or <code>null</code> if not present
     */
    public CorpusInfo getCorpus(String language) {
        corpusInfoLock.readLock().lock();
        try {
            CorpusInfo langCorpusInfo =  corpusInfos.get(language);
            if(langCorpusInfo == null && language.indexOf('-') > 0){
            	String rootLang = language.substring(0,language.indexOf('-'));
            	log.debug(" - no FST corpus for {}. Fallback to {}", language,rootLang);
            	langCorpusInfo =  corpusInfos.get(rootLang);
            }
            return langCorpusInfo;
        } finally {
            corpusInfoLock.readLock().unlock();
        }
    }
    /**
     * Getter for the languages of all configured FST corpora
     * @return a read-only copy of the languages of all configured FST corpora
     */
    public Set<String> getCorpusLanguages(){
        return Collections.unmodifiableSet(new HashSet<String>(corpusInfos.keySet()));
    }
    /**
     * Read-only collection of all {@link CorpusInfo}s defined for this
     * configuration.
     * @return a read only copy of the current {@link CorpusInfo}s
     */
    public Collection<CorpusInfo> getCorpora(){
        corpusInfoLock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(new ArrayList<CorpusInfo>(corpusInfos.values()));
        } finally {
            corpusInfoLock.readLock().unlock();
        }
    }
    
    /**
     * The {@link CorpusInfo} for the default laugnage
     * @return the default corpus or <code>null</code> if no corpus is available
     * for the default language
     */
    public CorpusInfo getDefaultCorpus() {
        corpusInfoLock.readLock().lock();
        try {
            return corpusInfos.get(defaultLanguage);
        } finally {
            corpusInfoLock.readLock().unlock();
        }
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
    
    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    /**
     * The FST configuration
     * @return
     */
    public LanguageConfiguration getFstConfig() {
        return fstConfig;
    }
    
    public SolrCore getIndex() {
        return index;
    }
    
    public void setEntityCacheManager(EntityCacheManager entityCacheManager) {
        this.entityCacheManager = entityCacheManager;
    }
    
    public EntityCacheManager getEntityCacheManager() {
        return entityCacheManager;
    }

    public File getFstDirectory() {
        return fstDirectory;
    }
    
    public void setFstDirectory(File fstDirectory) {
        this.fstDirectory = fstDirectory;
    }

    public void setOrigin(RDFTerm origin) {
        this.origin = origin;
    }
    /**
     * The Origin of the dataset or <code>null</code> if not defined. The
     * origin can be used to specify the dataset where the Entities described by
     * the configured FST originate from. If can be both an URI (e.g. 
     * <code>http://dbpedia.org</code>) or an literal "<code>dbpedia</code>").
     * If present the origin is added to any <code>fise:TextAnnotation</code>
     * created by the FstLinkingEngine with the property <code>fise:origin</code>
     * 
     * @return the origin or <code>null</code> if none is configured
     */
    public RDFTerm getOrigin() {
        return origin;
    }
    
    /**
     * Deactivates this {@link IndexConfiguration}
     */
    public void deactivate(){
        active = false;
    }
    
    /**
     * If this {@link IndexConfiguration} is still in sync with the version
     * of the {@link #getIndex() SolrCore}. This will return true if 
     * <code>{@link #isRuntimeGeneration()} == false </code>
     * @return <code>true</code> if still active. Otherwise <code>false</code>
     */
    public boolean isCurrent(){
        if(!runtimeGeneration){
            return true;
        } else {
            RefCounted<SolrIndexSearcher> searcherRef = index.getSearcher();
            try {
                long version = searcherRef.get().getIndexReader().getVersion();
                return indexVersion == version;
            } finally {
                searcherRef.decref();
            }
        }
    }

    private long getIndexVersion(){
        RefCounted<SolrIndexSearcher> searcherRef = index.getSearcher();
        try {
            return getIndexVersion(searcherRef.get());
        } finally {
            searcherRef.decref();
        }
    }
    
    private long getIndexVersion(SolrIndexSearcher searcher){
            return searcher.getIndexReader().getVersion();
    }
    
    public boolean isRuntimeGeneration() {
        return runtimeGeneration;
    }
    
    public boolean isActive() {
        return active;
    }
    
    /**
     * Activated this indexing configuration by inspecting the {@link SolrCore}
     * based on the provided configuration 
     * @return
     */
    public void activate() throws IOException {
        active = true;
        if(index == null){ //do we have an SolrCore
            throw new IllegalArgumentException("No SolrCore set for this configuration");
        }
        //if no fstDirectory is configured
        if(fstDirectory == null){ //use the default
            fstDirectory = new File(index.getDataDir(),"fst");
        }
        //init the fstDirectory (may throw IOException)
        if(fstDirectory.isFile()){
            throw new IOException("Default FST directory exists and "
                    + "is a File. Use #setFstDirectory() to set different one");
        } else if(!fstDirectory.exists()){
            FileUtils.forceMkdir(fstDirectory);
        }
        //acquire the initial index configuration
        update();
    }
    
    /**
     * Updates the configuration based on the  current version of the 
     * {@link #getIndex()}. If the SolrCore was not updated this will do
     * nothing.
     */
    public void update(){
        RefCounted<SolrIndexSearcher> searcherRef = index.getSearcher();
        try {
            update(getIndexVersion(searcherRef.get()), searcherRef.get());
        } finally {
            searcherRef.decref(); //decrease the count on the searcher
        }
    }
    /**
     * Version of {@link #update()} to be used in cases where the indexVersion
     * and a Solr searcher is already available in the calling method
     * @param indexVersion
     * @param searcher
     */
    protected void update(long indexVersion, SolrIndexSearcher searcher){
        assert searcher != null;
        assert searcher.getCore().equals(index);
        processFstConfig(indexVersion, searcher.getAtomicReader());
    }
    
    /**
     * This method combines the {@link #fstConfig} with the data present in the
     * {@link SolrCore}. 
     * <p>
     * As information for fields are only available when a
     * field was actually used by a document stored in the index one needs to
     * inspect the index after every change. 
     * <p>
     * An empty Solr index will result in
     * an empty {@link #corpusInfos} map. The first document with an value
     * for the English field will cause an {@link CorpusInfo} for the English
     * language to be created. As soon as the last document with an label for
     * a given language will be deleted the {@link CorpusInfo} for that language
     * will also disappear.
     * @param indexVersion the current version of the {@link #index} to process
     * the FST config for.
     * <p>
     * This method acquires a write lock on {@link #corpusInfoLock} while it
     * inspects the Solr index
     * @param indexReader The {@link AtomicReader} has access to the actual
     * fields present in the {@link SolrCore}. It is used to compare field
     * configurations in the {@link #fstConfig} with fields present in the Solr
     * {@link #index}.
     * @return If any {@link CorpusInfo FST configuration} where found during
     * inspecting the Solr {@link #index}
     */
    private boolean processFstConfig(long indexVersion, AtomicReader indexReader) {
        //first check if the Solr index was updated
        corpusInfoLock.readLock().lock();
        try {
            if(indexVersion == this.indexVersion){ //no update?
                return !corpusInfos.isEmpty(); //nothing to do
            }
        } finally {
            corpusInfoLock.readLock().unlock();
        }
        log.debug("> {} FST config for {} (FST dir: {})", 
            corpusInfos == null ? "create" : "update",
            index.getName(), fstDirectory.getAbsolutePath());
        
        boolean foundCorpus = false;

        corpusInfoLock.writeLock().lock();
        try {
            this.indexVersion = indexVersion;
            IndexSchema schema = index.getLatestSchema();
            Map<String,CorpusInfo> corpusInfosCopy;
            if(corpusInfos == null){ //first call
                corpusInfos = new HashMap<String,CorpusInfo>(); //init the field
                corpusInfosCopy = new HashMap<String,CorpusInfo>();
            } else {
                corpusInfosCopy = new HashMap<String,CorpusInfo>(corpusInfos);
                corpusInfos.clear(); //clear the old data
            }
            //(0) get basic parameters of the default configuration
            log.debug(" - default config");
            Map<String,String> defaultParams = fstConfig.getDefaultParameters();
            String fstName = defaultParams.get(IndexConfiguration.PARAM_FST);
            String indexField = defaultParams.get(IndexConfiguration.PARAM_FIELD);
            String storeField = defaultParams.get(IndexConfiguration.PARAM_STORE_FIELD);
            if(storeField == null){ 
                //apply indexField as default if indexField is NOT NULL
                storeField = indexField;
            }
            if(indexField == null){ //apply the defaults if null
                indexField = IndexConfiguration.DEFAULT_FIELD;
            }
            if(fstName == null){ //use default
                fstName = getDefaultFstFileName(indexField);
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
                Iterator<File> fstFiles = FileUtils.iterateFiles(fstDirectory, fstFilter, null);
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
                    fieldEncoding, "*");
                for(FieldInfo fieldInfo : fieldInfos){
                    //try to match the field names against the wildcard
                    if(FilenameUtils.wildcardMatch(fieldInfo.name, fieldWildcard)){
                        //for matches parse the language from the field name
                        String language = FieldEncodingEnum.parseLanguage(
                            fieldInfo.name, fieldEncoding, indexField);
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
                            File fstFile = new File(fstDirectory,fstFileName.toString());
                            //get the FieldType of the field from the Solr schema
                            FieldType fieldType = schema.getFieldTypeNoEx(fieldInfo.name);
                            if(fieldType != null){ //if the fieldType is present
                                if(runtimeGeneration || fstFile.isFile()){ //and FST is present or can be created
                                    //we need also to check if the stored field with
                                    //the labels is present
                                    //get the stored Field and check if it is present!
                                    String storeFieldName;
                                    if(storeField == null){ //storeField == indexField
                                        storeFieldName = fieldInfo.name;
                                    } else { // check that the storeField is present in the index
                                        storeFieldName = FieldEncodingEnum.encodeLanguage(
                                            storeField, fieldEncoding, language);
                                        FieldInfo storedFieldInfos = fieldInfos.fieldInfo(storeFieldName);
                                        if(storedFieldInfos == null){
                                            log.debug(" ... ignore language {} because Stored Field {} "
                                                    + "for IndexField {} does not exist! ", new Object[]{
                                                    language,storeFieldName,fieldInfo.name});
                                            storeFieldName = null;
                                        }
                                        
                                    }
                                    if(storeFieldName != null){ // == valid configuration
                                        CorpusInfo fstInfo = corpusInfosCopy.get(language);
                                        if(fstInfo == null || //new one
                                                !fstInfo.indexedField.equals(fieldInfo.name) || //index field compatible
                                                !fstInfo.storedField.equals(storeFieldName)){ //store field compatible
                                            CorpusInfo newFstInfo = new CorpusInfo(language, 
                                                fieldInfo.name, storeFieldName,  
                                                fieldType, fstFile, runtimeGeneration);
                                            log.debug(" ... {} {} ", fstInfo == null ? "create" : "update", newFstInfo);
                                            addCorpusInfo(newFstInfo);
                                            corpusInfosCopy.put(language, newFstInfo);
                                        } else { //no change in the SolrIndex ... use the exsisting CorpusInfo
                                            addCorpusInfo(fstInfo);
                                        }
                                        foundCorpus = true;
                                    }
                                } else {
                                    log.debug(" ... ignore language {} (field: {}) because "
                                        + "FST file '{}' does not exist and runtime creation "
                                        + "is deactivated!",new Object[]{ language,
                                                fieldInfo.name, fstFile.getAbsolutePath()});
                                }
                            } else {
                                log.debug(" ... ignore language {} becuase unknown fieldtype "
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
                Map<String,String> config = fstConfig.getParameters(language);
                String langIndexField = config.get(IndexConfiguration.PARAM_FIELD);
                String langStoreField = config.get(IndexConfiguration.PARAM_STORE_FIELD);
                String langFstFileName = config.get(IndexConfiguration.PARAM_FST);
                final boolean langAllowCreation;
                final String langAllowCreationString = config.get(IndexConfiguration.PARAM_RUNTIME_GENERATION);
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
                    langAllowCreation = runtimeGeneration;
                }
                //(2.b) check if the Solr field is present
                String encodedLangIndexField = FieldEncodingEnum.encodeLanguage(
                    langIndexField, fieldEncoding, language);
                String encodedLangStoreField = FieldEncodingEnum.encodeLanguage(
                    langStoreField, fieldEncoding, language);
                FieldInfo langIndexFieldInfo = fieldInfos.fieldInfo(encodedLangIndexField);
                if(langIndexFieldInfo != null){
                    FieldInfo langStoreFieldInfo = fieldInfos.fieldInfo(encodedLangStoreField);
                    if(langStoreFieldInfo != null){
                        FieldType fieldType = schema.getFieldTypeNoEx(langIndexFieldInfo.name);
                        if(fieldType != null){
                            //(2.c) check the FST file
                            File langFstFile = new File(fstDirectory,langFstFileName);
                            if(langFstFile.isFile() || langAllowCreation){
                                CorpusInfo langFstInfo = corpusInfosCopy.get(language);
                                if(langFstInfo == null || //new one
                                        !langFstInfo.indexedField.equals(encodedLangIndexField) || //index field compatible
                                        !langFstInfo.storedField.equals(encodedLangStoreField)){ //store field compatible
                                    CorpusInfo newLangFstInfo = new CorpusInfo(language, 
                                        encodedLangIndexField,encodedLangStoreField,
                                        fieldType, langFstFile, langAllowCreation);
                                    log.debug("   ... {} {} for explicitly configured language", 
                                        langFstInfo == null ? "create" : "update", newLangFstInfo);
                                    addCorpusInfo(newLangFstInfo);
                                } else { //we can use the existing instance
                                    addCorpusInfo(langFstInfo);
                                }
                                foundCorpus = true;
                            } else {
                                log.debug(" ... ignore explicitly configured language {} (field: {}) because "
                                        + "FST file '{}' does not exist and runtime creation "
                                        + "is deactivated!",new Object[]{ language,
                                                langIndexFieldInfo.name, langFstFile.getAbsolutePath()});
                            }
                        } else {
                            log.debug(" ... ignore explicitly configured language {} becuase unknown fieldtype "
                                    + "for SolrFied {}", language, langIndexFieldInfo.name);
                        }
                    } else {
                        log.debug(" ... ignore explicitly configured language {} because configured stored Field {} "
                                + "for IndexField {} does not exist! ", new Object[]{
                                language,langStoreField,langIndexFieldInfo.name});
                    }
                } else {
                    log.debug(" ... ignore explicitly configured language {} because configured field {} (encoded: {}) "
                        + "is not present in the SolrIndex!", new Object[]{
                                language, langIndexField, encodedLangIndexField });
                }
            }
        } finally {
            corpusInfoLock.writeLock().unlock();
        }
        return foundCorpus;
    }
    
    /**
     * Getter for the default FST file name based on the configured field
     * name. This method returns the '<code>{name}</code>' part of the
     * '<code>{name}.{lang}.fst</code>' name.
     * @param fstFieldName the field name.
     * @return the '<code>{name}</code>' part of the'<code>{name}.{lang}.fst</code>' name
     */
    private String getDefaultFstFileName(final String fstFieldName) {
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

    public boolean isSkipAltTokens() {
        return skipAltTokens;
    }

    public void setSkipAltTokens(boolean skipAltTokens) {
        this.skipAltTokens = skipAltTokens;
        
    }

}
