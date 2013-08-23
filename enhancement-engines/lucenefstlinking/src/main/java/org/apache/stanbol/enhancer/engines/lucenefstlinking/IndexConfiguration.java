package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.lucene.document.Document;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.cache.EntityCacheManager;
import org.apache.stanbol.enhancer.nlp.utils.LanguageConfiguration;
import org.opensextant.solrtexttagger.TaggerFstCorpus;

/**
 * Holds the configuration of the index used by the FST linking engine.
 * 
 * @author Rupert Westenthaler
 *
 */
public class IndexConfiguration {
    
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
     * FST corpus configuration
     */
    private Map<String,CorpusInfo> corpusInfos = new HashMap<String,CorpusInfo>();
    /**
     * {@link ExecutorService} used to create {@link TaggerFstCorpus} instances
     * at runtime.
     */
    protected ExecutorService executorService;
    /**
     * The encoding used by SolrFields (e.g. to define label fields for different
     * languages).
     */
    private FieldEncodingEnum fieldEncoding;
    /**
     * The instance used to retrieve/create the cache for Lucene {@link Document}s
     * of Entities.
     */
    private EntityCacheManager entityCacheManager;
    
    /**
     * The FST corpus used for linking regardless of the language of the
     * document
     */
    private CorpusInfo defaultFstCorpus;
   
    private final LanguageConfiguration fstConfig;

    public IndexConfiguration(LanguageConfiguration fstConfig, SolrCore index){
        if(fstConfig == null){
            throw new IllegalArgumentException("The parsed FST configuration MUST NOT be NULL!");
        }
        if(index == null || index.isClosed()){
            throw new IllegalArgumentException("The parsed SolrCore MUST NOT be NULL nore closed!");
        }
        this.fstConfig = fstConfig;
        this.index = index;
    }
    
    public CorpusInfo setDefaultCorpus(CorpusInfo corpus){
        CorpusInfo oldDefault = defaultFstCorpus;
        if(corpus != null){
            this.defaultFstCorpus = corpus;
        } else {
            this.defaultFstCorpus = null;
        }
        return oldDefault;
    }
    
    public CorpusInfo addCorpus(CorpusInfo corpus){
        if(corpus != null){
            return corpusInfos.put(corpus.language, corpus);
        } else {
            return null;
        }
    }
    
    public CorpusInfo removeCorpus(String language){
        return corpusInfos.remove(language);
    }
    /**
     * @return the fieldEncoding
     */
    public final FieldEncodingEnum getFieldEncoding() {
        return fieldEncoding;
    }

    /**
     * @param fieldEncoding the fieldEncoding to set
     */
    public final void setFieldEncoding(FieldEncodingEnum fieldEncoding) {
        this.fieldEncoding = fieldEncoding;
    }
    /**
     * @return the typeField
     */
    public final String getTypeField() {
        return typeField;
    }

    /**
     * @param typeField the typeField to set
     */
    public final void setTypeField(String typeField) {
        this.typeField = typeField;
    }
    /**
     * @return the redirectField
     */
    public final String getRedirectField() {
        return redirectField;
    }
    /**
     * @param redirectField the redirectField to set
     */
    public final void setRedirectField(String redirectField) {
        this.redirectField = redirectField;
    }
    /**
     * @return the rankingField
     */
    public final String getRankingField() {
        return rankingField;
    }
    /**
     * @param rankingField the rankingField to set
     */
    public final void setRankingField(String rankingField) {
        this.rankingField = rankingField;
    }

    public CorpusInfo getCorpus(String language) {
        return corpusInfos.get(language);
    }
    /**
     * Getter for the languages of all configured FST corpora
     * @return the languages of all configured FST corpora
     */
    public Set<String> getCorpusLanguages(){
        return Collections.unmodifiableSet(corpusInfos.keySet());
    }
    /**
     * Read-only collection of all {@link CorpusInfo}s defined for this
     * configuration.
     * @return
     */
    public Collection<CorpusInfo> getCorpora(){
        return Collections.unmodifiableCollection(corpusInfos.values());
    }
    
    public CorpusInfo getDefaultCorpus() {
        return defaultFstCorpus;
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
}
