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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.Match.FieldLoader;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.Match.FieldType;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.cache.EntityCache;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.opensextant.solrtexttagger.TaggerFstCorpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Profile created based on the {@link IndexConfiguration} for processing a
 * parsed ContentItem. <p>
 * 
 * @author Rupert Westenthaler
 *
 */
public class TaggingSession implements Closeable {
    
    private final Logger log = LoggerFactory.getLogger(TaggingSession.class);
    
    private String language;
    
    private Corpus langCorpus;
    
    private Corpus defaultCorpus;
    
    /**
     * The Solr document id field holding the URI of the Entity.
     */
    protected final String idField;
    
    /**
     * The Solr field holding the labels in the language of the current Document
     */
    protected final String labelField;
    
    protected final Language labelLang;
    /**
     * The Solr field holding the labels in the default matching language or
     * <code>null</code> if the same as {@link #labelField}
     */
    protected final String defaultLabelField;
    
    protected final Language defaultLabelLang;
    
    protected final Set<String> solrDocfields = new HashSet<String>();

    protected final IndexConfiguration config;
    
    protected final String typeField;
    protected final String redirectField;
    protected final String rankingField;
    
    /**
     * Used in the {@link LinkingModeEnum#NER} to store the {@link NerTag#getTag()}
     * and {@link NerTag#getType()} values for the span of the Named Entity.<p>
     * This information is collected by the {@link NamedEntityTokenFilter} while
     * iterating over the parsed text and is used in the processing of
     * {@link Tag}s to filter Entities based on their types. <p>
     * Not used in any linking mode other than <code>NER</code>
     */
    protected final NavigableMap<int[],Set<String>> entityMentionTypes = 
            new TreeMap<int[],Set<String>>(Tag.SPAN_COMPARATOR);
    
    private final RefCounted<SolrIndexSearcher> searcherRef;
    /**
     * Document Cache and session statistics for the cache
     */
    private RefCounted<EntityCache> documentCacheRef;
    private int docLoaded = 0;
    private int docCached = 0;
    private int docAppended = 0;
    //private final ValueSourceAccessor uniqueKeyCache;
    //private final Map<Integer,Match> matchPool = new HashMap<Integer,Match>(2048);
    private final FieldLoaderImpl fieldLoader;
    /**
     * The current version of the SolIndex (as reported by 
     * {@link DirectoryReader#getVersion()}) of the 
     * {@link IndexConfiguration#getIndex()}
     */
    private final Long indexVersion;

    
    TaggingSession(String language, IndexConfiguration config) throws CorpusException {
        this.language = language;
        this.config = config;
        //init the SolrIndexSearcher
        searcherRef = config.getIndex().getSearcher();
        SolrIndexSearcher searcher = searcherRef.get();
        DirectoryReader indexReader = searcher.getIndexReader();
        indexVersion = Long.valueOf(indexReader.getVersion());
        //check if the IndexConfiguration is up to date with the version of the index
        long confVersion = config.getVersion();
        if(confVersion != indexVersion){
            log.debug("> update IndexConfiguration (from: {} | to: {}",confVersion, indexVersion);
            config.update(indexVersion, searcher);
        }
        
        //get the corpusInfo
        CorpusInfo langCorpusInfo = config.getCorpus(language);
        log.debug("> language Corpus: {}", langCorpusInfo);
        CorpusInfo defaultCorpusInfo = config.getDefaultCorpus();
        log.debug("> default Corpus: {}", defaultCorpusInfo);
        
        //obtain the Solr Document Id field
        SchemaField idSchemaField = config.getIndex().getLatestSchema().getUniqueKeyField();
        idField = idSchemaField.getName();
        solrDocfields.add(idField);

        //obtain the language specific fields for the session
        if(langCorpusInfo != null){
            this.langCorpus = new Corpus(langCorpusInfo,
                obtainFstCorpus(indexVersion,langCorpusInfo));
            this.labelField = langCorpusInfo.storedField;
            solrDocfields.add(labelField);
            this.labelLang = langCorpusInfo.language == null || 
                    StringUtils.isBlank(langCorpusInfo.language) ? null : 
                        new Language(langCorpusInfo.language);
        } else {
            this.labelField = null;
            this.labelLang = null; 
        }
        if(defaultCorpusInfo != null && !defaultCorpusInfo.equals(langCorpusInfo)){
            this.defaultCorpus = new Corpus(defaultCorpusInfo,
                obtainFstCorpus(indexVersion,defaultCorpusInfo));
            this.defaultLabelField = defaultCorpusInfo.storedField;
            solrDocfields.add(defaultLabelField);
            this.defaultLabelLang = defaultCorpusInfo.language == null || 
                    StringUtils.isBlank(defaultCorpusInfo.language) ? null : 
                        new Language(defaultCorpusInfo.language);
        } else {
            this.defaultCorpus = null;
            this.defaultLabelField = null;
            this.defaultLabelLang = null;
        }
        if(this.defaultCorpus == null && this.langCorpus == null){
            throw new CorpusException("Unable to initialise a FST corpus for language '"
                + language+"'. Neigher the language specific Coprpus (field : "
                + langCorpusInfo != null ? langCorpusInfo.indexedField : "<undefined>" 
                + ") nor for the default language (field: " 
                + defaultCorpusInfo != null ? defaultCorpusInfo.indexedField : "<undefined>"
                + ") is currently available!",null);
        }
        if(config.getEncodedTypeField() != null){
            this.typeField = config.getEncodedTypeField();
            solrDocfields.add(typeField);
        } else {
            this.typeField = null;
        }
        if(config.getEncodedRedirectField() != null){
            this.redirectField = config.getEncodedRedirectField();
            solrDocfields.add(redirectField);
        } else {
            this.redirectField = null;
        }
        if(config.getEncodedRankingField() != null){
            this.rankingField = config.getEncodedRankingField();
            solrDocfields.add(rankingField);
        } else {
            this.rankingField = null;
        }
        if(config.getEntityCacheManager() != null){
            documentCacheRef = config.getEntityCacheManager().getCache(indexVersion);
        }
//        uniqueKeyCache = null; //no longer used.
//        uniqueKeyCache = new ValueSourceAccessor(searcher, idSchemaField.getType()
//            .getValueSource(idSchemaField, null));
        fieldLoader = new FieldLoaderImpl(searcher.getIndexReader());

    }
    /**
     * Used to instantiate {@link Match}es 
     * @param docId the Lucene document Id as returned by the FST corpus
     * @return the Match instance
     */
    public Match createMatch(int docId){
        return new Match(docId,fieldLoader);
    }
    
    public void close(){
        //matchPool.clear(); //clean up the matchpool
        searcherRef.decref(); //clean up the Solr index searcher reference
        if(documentCacheRef != null){
            documentCacheRef.decref(); //clean up the DocumentCache reference
        }
    }
    /**
     * The language of this Session. This is typically the language detected for
     * the document.
     * @return the language of this Session
     */
    public String getLanguage() {
        return language;
    }
    
//    public String getTypeField() {
//        return config.getTypeField();
//    }
    
//    public String getRedirectField() {
//        return config.getRedirectField();
//    }
    
//    public String getDefaultLabelField() {
//        return defaultLabelField;
//    }
    
//    public Language getDefaultLabelLanguage() {
//        return defaultLabelLang;
//    }

//    public String getLabelField() {
//        return labelField;
//    }
    
//    public Language getLabelLanguage() {
//        return labelLang;
//    }

//    /**
//     * @return the langCorpus
//     */
//    public final CorpusInfo getLangCorpus() {
//        return langCorpusInfo;
//    }

//    /**
//     * @return the defaultCorpus
//     */
//    public final CorpusInfo getDefaultCorpus() {
//        return defaultCorpusInfo;
//    }


    public Corpus getDefaultCorpus() {
        return defaultCorpus;
    }
    
    public Corpus getLanguageCorpus() {
        return langCorpus;
    }
    
    public SolrIndexSearcher getSearcher() {
        return searcherRef.get();
    }
    
    public static TaggingSession createSession(IndexConfiguration indexConfig, 
            String language) throws CorpusException {
        TaggingSession session = new TaggingSession(language, indexConfig);
        return session;
    }
    /**
     * Getter for the EntityCache 
     * @return the cache or <code>null</code> if no one is configured
     */
    public EntityCache getDocumentCache(){
        return documentCacheRef != null ? documentCacheRef.get() : null;
    }
    /**
     * The number of Lucene Documents loaded form disc in this session so far
     * @return
     */
    public int getSessionDocLoaded(){
        return docLoaded;
    }
    /**
     * The number of Lucene Documents retrieved from the {@link #getDocumentCache()}
     * in this session so far
     * @return
     */
    public int getSessionDocCached(){
        return docCached;
    }
    /**
     * The number of Lucene Documents retrived from the {@link #getDocumentCache()},
     * but with missing fields from the Cache. For such documents the additional
     * fields (typically labels of different languages) where readed from disc and
     * added to the cached document.
     * @return
     */
    public int getSessionDocAppended(){
        return docAppended;
    }

    
    /**
     * Obtains the FST corpus for the parsed CorpusInfo. The other parameters
     * are just used for error messages in case this is not successful.
     * @param indexVersion the current version of the index
     * @param fstInfo the info about the corpus
     * @return the TaggerFstCorpus
     * @throws CorpusException if the requested corpus is currently not available
     */
    private TaggerFstCorpus obtainFstCorpus(Long indexVersion, CorpusInfo fstInfo) throws CorpusException {
        TaggerFstCorpus fstCorpus;
        fstCorpus = fstInfo.getCorpus(); 
        Future<TaggerFstCorpus> enqueuedCorpus = null;
        if (fstCorpus == null) {
            if (!fstInfo.allowCreation && fstInfo.isFstCreationError()) {
                throw new CorpusException(fstInfo.getErrorMessage(), null);
            }
            fstInfo.corpusLock.readLock().lock();
            try {
                enqueuedCorpus = fstInfo.getEnqueued();
            } finally {
                fstInfo.corpusLock.readLock().unlock();
            }
            if(enqueuedCorpus == null && //not enqueued
                    fstInfo.allowCreation){ 
                log.debug(" - enqueue creation of {}", fstInfo);
                enqueuedCorpus = enqueue(fstInfo);
            }
            if(enqueuedCorpus == null){
                throw new CorpusException("Unable to abtain Fst Corpus for " + fstInfo
                    + "(message: " + fstInfo.getErrorMessage() + ")!", null);
            }
        } else { //fstCorpus != null
            //check if the current FST corpus is up to date with the Solr index
            if(indexVersion != null && indexVersion.longValue() != fstCorpus.getIndexVersion()){
                log.debug(" - FST corpus for language '{}' is outdated", fstInfo.language);
                fstInfo.corpusLock.readLock().lock();
                try {
                    enqueuedCorpus = fstInfo.getEnqueued();
                } finally {
                    fstInfo.corpusLock.readLock().unlock();
                }
                if(enqueuedCorpus == null && //not already enqueued
                        fstInfo.allowCreation && config.getExecutorService() != null){
                    log.debug(" - enqueue creation of {}", fstInfo);
                    enqueuedCorpus = enqueue(fstInfo);
                } else {
                    log.warn("Unable to update outdated FST corpus for language '{}' "
                            + "because runtimeCreation is {} and ExecutorServic "
                            + "is {} available!", new Object[]{fstInfo.language,
                            fstInfo.allowCreation ? "enabled" : "disabled" ,
                            config.getExecutorService() == null ? "not" : ""});
                    log.warn("  ... please adapt the Engine configuration for up "
                        + "to date FST corpora!");
                }
            } else { //FST corpus is up to date with the current Solr index version
                log.debug("FST corpus for language '{}' is up to date", fstInfo.language);
            }
        }
        //TODO: maybe make this configurable
        int waitTime = fstCorpus == null ? 30 : 10; 
        if(enqueuedCorpus != null){ //we needed to build a new corpus
            try {
                log.debug(" - will wait max {}sec for creation of {}", waitTime, fstInfo);
                fstCorpus = enqueuedCorpus.get(waitTime, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); //recover interrupted state
            } catch (ExecutionException e) {
                log.warn("Unable to update outdated FST corpus " + fstInfo 
                    + " (message: " + fstInfo.getErrorMessage() + ")",e); 
            } catch (TimeoutException e) {
                if(fstCorpus != null){
                    log.debug("unable to build FST corpus for {} in time ({}sec). Will use "
                        + "previouse version ",fstInfo, waitTime);
                } else {
                    throw new CorpusException("Unable to build Fst Corpus for " + fstInfo
                        + "within " + waitTime+ "sec! Try again later.", null);
                }
            } catch (CancellationException e) {
                if(fstCorpus != null){
                    log.debug("building of  FST corpus for {} was cancelled. Will use "
                        + "previouse version.",fstInfo);
                } else {
                    throw new CorpusException("Building of FST Corpus " + fstInfo 
                        + "was cancelled!", null);
                }
            }
        }
        return fstCorpus;
    }
    /**
     * @param fstInfo
     * @return
     */
    private Future<TaggerFstCorpus> enqueue(CorpusInfo fstInfo) {
        Future<TaggerFstCorpus> enqueuedCorpus;
        fstInfo.corpusLock.writeLock().lock();
        try {
            enqueuedCorpus = fstInfo.getEnqueued(); //check again in write lock
            if(enqueuedCorpus == null){
                //enqueue for re-creation
                enqueuedCorpus = config.getExecutorService().submit(
                    new CorpusCreationTask(config, fstInfo));
                fstInfo.enqueued(enqueuedCorpus);;
            }
        } finally {
            fstInfo.corpusLock.writeLock().unlock();
        }
        return enqueuedCorpus;
    }
    /**
     * The current version of the SolrIndex as reported by the {@link IndexReader}
     * used by this TaggingSession.
     * @return the current version of the SolrIndex.
     */
    public Long getIndexVersion() {
        return indexVersion;
    }
    
    /**
     * {@link FieldLoader} implementation used to create {@link Match} instances
     */
    private class FieldLoaderImpl implements FieldLoader {
        
        private static final String LOADED_FIELDS_FIELD_NAME = "__loadedFields__";
        
        private List<Field> loadedFieldsFields;
        
        private final IndexReader reader;
        /**
         * Cache similar to the {@link EntityCache}, but with a scope bound to
         * life cycle of this FieldLoaderImpl instance (a single TaggingSession).
         * This cache ensures the Lucene Documents are not loaded twice while
         * processing the same document (even if no EntiyCache is configured or
         * the size of the EntityCache is to small).
         */
        private final Map<Integer,Document> sessionCache = new HashMap<Integer,Document>();
        /**
         * The EntityCache instance that caches entity data over multiple sessions
         */
        private final EntityCache cache;
        
        public FieldLoaderImpl(IndexReader reader) {
            this.reader = reader;
            loadedFieldsFields = new ArrayList<Field>(solrDocfields.size());
            for(String loadedFieldName : solrDocfields){
                loadedFieldsFields.add(new StringField(LOADED_FIELDS_FIELD_NAME, 
                    loadedFieldName, Store.NO));
            }
            if(documentCacheRef != null){
                this.cache = documentCacheRef.get();
            } else {
                this.cache = null;
            }
        }
        
        @Override
        public Map<FieldType,Object> load(int id) throws IOException {
            //load the Lucene Document for the id 
            Integer ID = Integer.valueOf(id);
            Document doc = sessionCache.get(ID);
            if(doc == null){
                doc = cache != null ? cache.get(ID) : null;
                if(doc == null){
                    doc = reader.document(id, solrDocfields);
                    //if we read a doc from the index we need to add information about
                    //the fields we loaded (especially the languages of labels loaded
                    //NOTE that those information will never be stored in the index. They
                    //are only kept in-memory when caching this document.
                    for(Field loadedFieldsField : loadedFieldsFields){
                        doc.add(loadedFieldsField);
                    }
                    docLoaded++;
                    if(cache != null){
                        cache.cache(ID, doc);
                    }
                } else {
                    //we need to check if the fields of the cached doc are sufficient
                    //for the requested Solr Document fields
                    Set<String> fields = new HashSet<String>(solrDocfields);
                    String[] loaded = doc.getValues(LOADED_FIELDS_FIELD_NAME);
                    for(int i=0;i < loaded.length && !fields.isEmpty(); i++){
                        fields.remove(loaded[i]);
                    }
                    if(!fields.isEmpty()){ //we are missing some fields
                        //need to load it from the index
                        Document tmp = reader.document(id, fields);
                        //add the additional fields to the cached doc
                        for(IndexableField field : tmp.getFields()){
                            doc.add(field);
                        }
                        //also update the loaded fields
                        for(String loadedField : fields){
                            doc.add(new StringField(LOADED_FIELDS_FIELD_NAME, 
                                loadedField, Store.NO));
                        }
                        //NOTE: no need to update the cache, as we have updated the
                        //cached value.
                        //cache.cache(ID, doc);
                        docAppended++;
                    } else {
                        docCached++;
                    }
                    //and put the doc in the sessionCache
                }
                //add this doc to the session cache
                sessionCache.put(ID, doc);
            } //else { //document is in the session cache ... just use it
                //NOTE: The session cache has a minor side effect on the
                // EntityCache. Because multiple occurrences of an Entity
                // within the Document are not requested on the EntityCache
                // LRU based implementations will get slightly different
                // statistics. Assuming that the maximum size of the EntityCache
                // is >> as the number of Documents matching for the current Text
                // this effect can be considered as negligible.
            //}
            if(doc != null){
                Map<FieldType,Object> values = 
                        new EnumMap<Match.FieldType,Object>(FieldType.class);
                //load the ID
                values.put(FieldType.id, doc.get(idField));
                //load the labels
                Set<Literal> labels = new HashSet<Literal>();
                for(String label : doc.getValues(labelField)){
                    labels.add(new PlainLiteralImpl(label, labelLang));
                }
                if(defaultLabelField != null){
                    for(String label : doc.getValues(defaultLabelField)){
                        labels.add(new PlainLiteralImpl(label, defaultLabelLang));
                    }
                }
                values.put(FieldType.label, labels);
                //load the types
                if(typeField != null){
                    Set<IRI> types = new HashSet<IRI>();
                    for(String type : doc.getValues(typeField)){
                        types.add(new IRI(type));
                    }
                    values.put(FieldType.type, types);
                }
                //load the redirects
                if(redirectField != null){
                    Set<IRI> redirects = new HashSet<IRI>();
                    for(String redirect : doc.getValues(redirectField)){
                        redirects.add(new IRI(redirect));
                    }
                    values.put(FieldType.redirect, redirects);
                }
                //load the rankings
                if(rankingField != null){
                    IndexableField field = doc.getField(rankingField);
                    if(field != null) {
                        Number num = field.numericValue();
                        Double ranking;
                        if(num instanceof Double){
                            ranking = (Double)num;
                        } else if (num != null){
                            ranking = Double.valueOf(num.doubleValue());
                        } else { //num == null
                            String value = field.stringValue();
                            if(value != null){
                                try {
                                    ranking = Double.valueOf(value);
                                } catch (NumberFormatException e) {
                                    ranking = null;
                                }
                            } else {
                                ranking = null;
                            }
                        }
                        if(ranking != null){
                            values.put(FieldType.ranking, ranking);
                        }
                    }
                }
                return values;
            } else {
                throw new IOException("No document found for Lucene doc id '"+id+"'!");
            }
        }
    }

    public class Corpus {
        
        private CorpusInfo corpusInfo;
        private TaggerFstCorpus fst;

        Corpus(CorpusInfo corpusInfo, TaggerFstCorpus fst){
            this.corpusInfo = corpusInfo;
            this.fst = fst;
        }
        
        public String getLanugage(){
            return corpusInfo.language;
        }
        
        public Analyzer getAnalyzer(){
            return corpusInfo.analyzer;
        }
        public Analyzer getTaggingAnalyzer(){
            return corpusInfo.taggingAnalyzer;
        }
        
        public TaggerFstCorpus getFst(){
            return fst;
        }

        public String getIndexedField() {
            return corpusInfo.indexedField;
        }
        
        public String getStoredField(){
            return corpusInfo.storedField;
        }
    }

    public boolean isSkipAltTokens() {
        return config.isSkipAltTokens();
    }
    /**
     * If this session has a FST corpus for tagging
     * @return <code>true</code> if a language and/or a default corpus is available.
     * <code>false</code> if both are <code>null</code>
     */
    public boolean hasCorpus() {
        return langCorpus != null || defaultCorpus != null;
    }
    
}
