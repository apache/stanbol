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
import java.util.Set;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queries.function.valuesource.IfFunction;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.Match.FieldLoader;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.Match.FieldType;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.cache.EntityCache;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.impl.ValueSourceAccessor;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.opensextant.solrtexttagger.TaggerFstCorpus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;

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
    private final RefCounted<SolrIndexSearcher> searcherRef;
    /*
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
        
        //get the corpusInfo
        CorpusInfo langCorpusInfo = config.getCorpus(language);
        CorpusInfo defaultCorpusInfo = config.getDefaultCorpus();
        
        //obtain the Solr Document Id field
        SchemaField idSchemaField = config.getIndex().getSchema().getUniqueKeyField();
        idField = idSchemaField.getName();
        solrDocfields.add(idField);

        //obtain the language specific fields for the session
        if(langCorpusInfo == null && defaultCorpusInfo == null){
            //this should not happen, because the canEnhance method of the 
            //engine should  already reject such calls
            throw new IllegalStateException("No FST Corpus configured for language '"
                +language+"' and also no default FST Corpus is present.!");
        }
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
        if(config.getTypeField() != null){
            this.typeField = config.getTypeField();
            solrDocfields.add(typeField);
        } else {
            this.typeField = null;
        }
        if(config.getRedirectField() != null){
            this.redirectField = config.getRedirectField();
            solrDocfields.add(redirectField);
        } else {
            this.redirectField = null;
        }
        if(config.getRankingField() != null){
            this.rankingField = config.getRankingField();
            solrDocfields.add(rankingField);
        } else {
            this.rankingField = null;
        }
        documentCacheRef = config.getEntityCacheManager().getCache(indexVersion);
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
        documentCacheRef.decref(); //clean up the DocumentCache reference
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
    
    public EntityCache getDocumentCache(){
        return documentCacheRef.get();
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
        synchronized (fstInfo) { // one at a time
            fstCorpus = fstInfo.getCorpus(); 
            if (fstCorpus == null) {
                if (fstInfo.isEnqueued()) {
                    throw new CorpusException("The FST corpus for language '"
                            + fstInfo.language + "' is enqueued for creation, but not yet "
                            + "available. Try at a  later point in time", null);
                }
                if (fstInfo.isFstCreationError()) {
                    throw new CorpusException(fstInfo.getErrorMessage(), null);
                }
                if (fstInfo.isFstFileError() && fstInfo.allowCreation) {
                    //try to recreate the FST corpus
                    if(config.getExecutorService() != null){
                        // TODO: this code should get moved to a CorpusManager class
                        config.getExecutorService().execute(
                            new CorpusCreationTask(config, fstInfo));
                        throw new CorpusException("The FST corpus for language '"
                                + fstInfo.language + "' was invalid and is now "
                                + "enqueued for re-creation. Retry at a  later "
                                + "point in time.", null);
                    } else {
                        throw new CorpusException(fstInfo.getErrorMessage(), null);
                    }
                }
            } else { //fstCorpus != null
                if(indexVersion != null && indexVersion.longValue() != fstCorpus.getIndexVersion()){
                    log.info("FST corpus for language '{}' is outdated ...", fstInfo.language);
                    if(fstInfo.isEnqueued()){
                        log.info("  ... already sheduled for recreation. "
                            + "Use outaded corpus for tagging");
                    } else if(fstInfo.allowCreation && config.getExecutorService() != null){
                        log.info("  ... initialise recreation");
                        config.getExecutorService().execute(
                            new CorpusCreationTask(config, fstInfo));
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
        }
        return fstCorpus;
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
            this.cache = documentCacheRef.get();
        }
        
        @Override
        public Map<FieldType,Object> load(int id) throws IOException {
            //load the Lucene Document for the id 
            Integer ID = Integer.valueOf(id);
            Document doc = sessionCache.get(ID);
            if(doc == null){
                doc = cache.get(ID);
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
                    cache.cache(ID, doc);
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
                    Set<UriRef> types = new HashSet<UriRef>();
                    for(String type : doc.getValues(typeField)){
                        types.add(new UriRef(type));
                    }
                    values.put(FieldType.type, types);
                }
                //load the redirects
                if(redirectField != null){
                    Set<UriRef> redirects = new HashSet<UriRef>();
                    for(String redirect : doc.getValues(redirectField)){
                        redirects.add(new UriRef(redirect));
                    }
                    values.put(FieldType.redirect, redirects);
                }
                //load the rankings
                if(rankingField != null){
                    Number num = doc.getField(rankingField).numericValue();
                    Double ranking;
                    if(num instanceof Double){
                        ranking = (Double)num;
                    } else if (num != null){
                        ranking = Double.valueOf(num.doubleValue());
                    } else { //num == null
                        String value = doc.get(rankingField);
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
    
}
