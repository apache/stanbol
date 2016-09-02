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

import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getAnalysedText;
import static org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper.getLanguage;
import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.getSelectionContext;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.DC_CONTRIBUTOR;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_END;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_ENTITY_REFERENCE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.ENHANCER_START;
import static org.apache.stanbol.enhancer.servicesapi.rdf.Properties.RDF_TYPE;
import static org.apache.stanbol.enhancer.servicesapi.rdf.TechnicalClasses.ENHANCER_TEXTANNOTATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.clerezza.commons.rdf.Language;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.PlainLiteralImpl;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.OpenBitSet;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.engine.EntityLinkingEngine;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkedEntity;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.TaggingSession.Corpus;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.AnalysedTextUtils;
import org.apache.stanbol.enhancer.nlp.ner.NerTag;
import org.apache.stanbol.enhancer.nlp.utils.NlpEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.apache.stanbol.enhancer.servicesapi.rdf.Properties;
import org.opensextant.solrtexttagger.TagClusterReducer;
import org.opensextant.solrtexttagger.Tagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FstLinkingEngine implements EnhancementEngine, ServiceProperties {

    private final Logger log = LoggerFactory.getLogger(FstLinkingEngine.class);

    /**
     * Use the same {@link ServiceProperties#ENHANCEMENT_ENGINE_ORDERING} as the
     * {@link EntityLinkingEngine#DEFAULT_ORDER}
     */
    public static final Integer ENGINE_ORDERING = EntityLinkingEngine.DEFAULT_ORDER;
    private static final Map<String,Object> SERVICE_PROPERTIES = Collections.unmodifiableMap(Collections
            .singletonMap(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING, (Object) ENGINE_ORDERING));

    private static final IRI ENHANCER_ENTITY_RANKING = new IRI(NamespaceEnum.fise + "entity-ranking");

    public static final IRI FISE_ORIGIN = new IRI(NamespaceEnum.fise + "origin");

    private final LiteralFactory literalFactory = LiteralFactory.getInstance();
    
    protected final String name;

    protected final LinkingModeEnum linkingMode;

    protected final TextProcessingConfig tpConfig;
    protected final EntityLinkerConfig elConfig;
    
    /**
     * Used in the {@link LinkingModeEnum#NER} to filter entities. For that configured
     * mappings for the {@link NerTag#getType()} and {@link NerTag#getTag()} values 
     * (the key) are mapped with the actual {@link Match#getTypes()} (the value set). 
     * The <code>null</code> value is interpreted as wildCard (any type matches). An
     * empty mapping is interpreted as an blacklist (do not lookup Named Entities
     * with that {@link NerTag#getType() type}/{@link NerTag#getTag() tag}
     */
    protected final Map<String,Set<String>> neTypeMappings;

    private IndexConfiguration indexConfig;

    public FstLinkingEngine(String name, LinkingModeEnum linkingMode, 
            IndexConfiguration indexConfig,
            TextProcessingConfig tpConfig, EntityLinkerConfig elConfig,
            Map<String,Set<String>> neTypeMappings) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor blank!");
        }
        this.name = name;
        if (indexConfig == null) {
            throw new IllegalArgumentException("The parsed IndexConfiguration MUST NOT be NULL!");
        }
        this.linkingMode = linkingMode == null ? LinkingModeEnum.values()[0] : linkingMode;
        this.indexConfig = indexConfig;
        if (tpConfig == null) {
            throw new IllegalArgumentException("The parsed Text Processing configuration MUST NOT be NULL");
        }
        this.tpConfig = tpConfig;
        if (elConfig == null) {
            throw new IllegalArgumentException("The parsed Entity Linking configuration MUST NOT be NULL");
        }
        this.elConfig = elConfig;
        if(linkingMode == LinkingModeEnum.NER && neTypeMappings == null){
            throw new IllegalArgumentException("The NamedEntity type mappings MUST NOT be NULL "
                    + "if the LinkingMode is NER!");
        }
        this.neTypeMappings = neTypeMappings;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String,Object> getServiceProperties() {
        return SERVICE_PROPERTIES;
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        log.trace("canEnhancer {}", ci.getUri());
        String language = getLanguage(this, ci, false);
        //(1) check if the language is enabled by the config
        if (language == null || !indexConfig.getFstConfig().isLanguage(language)) {
            log.debug("Engine {} ignores ContentItem {} becuase language {} is not condigured.",
                new Object[] {getName(), ci.getUri(), language});
            return CANNOT_ENHANCE;
        }
        //(2) check if we have a FST model for the language
        //NOTE: as STANBOL-1448 the index configuration is Solr index version
        //      dependent. This means that we can not use informations of the
        //      current IndexConfiguration to check if we have an FST model for
        //      the language of the requested document. Those information might
        //      be already out dated.
//        if(indexConfig.getCorpus(language) == null &&  //for the language
//        		indexConfig.getDefaultCorpus() == null){ //a default model
//            log.debug("Engine {} ignores ContentItem {} becuase no FST modles for language {} "
//            		+ "are available", new Object[] {getName(), ci.getUri(), language});
//                return CANNOT_ENHANCE;
//        }
        // we need a detected language, the AnalyzedText contentPart with
        // Tokens.
        AnalysedText at = AnalysedTextUtils.getAnalysedText(ci);
        if(at == null){
            if( linkingMode == LinkingModeEnum.PLAIN){
                return NlpEngineHelper.getPlainText(this, ci, false) != null ? ENHANCE_ASYNC : CANNOT_ENHANCE;
            } else {
                log.warn("Unable to process {} with engine name={} and mode={} "
                        + ": Missing AnalyzedText content part. Please ensure that "
                        + "NLP processing results are available before FST linking!", 
                        new Object[]{ci,name,linkingMode});
                return CANNOT_ENHANCE;
            }
        } else {
            if(linkingMode == LinkingModeEnum.PLAIN){
                return ENHANCE_ASYNC;
            } else if(at.getTokens().hasNext()){
                return ENHANCE_ASYNC;
            } else {
                log.warn("Unable to process {} with engine name={} and mode={} "
                    + "as the AnalyzedText does not contain any Tokens!", 
                    new Object[]{ci,name,linkingMode});
                return CANNOT_ENHANCE;
            }
        }
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at;
        if(linkingMode != LinkingModeEnum.PLAIN){
            //require AnalysedText contentPart
            at = getAnalysedText(this, ci, true);
        } else { //AnalysedText is optional in LinkingModeEnum.BASIC
            try {
                at = AnalysedTextUtils.getAnalysedText(ci);
            } catch (ClassCastException e) {
                //unexpected contentPart found under the URI expecting the AnalysedText
                at = null;
            }
        }
        final String content;
        if(at != null){ //we can get the content from the Analyzed text
            content = at.getSpan();
        } else { //no analyzed text ... read is from the text/plain blob
            try {
                content = ContentItemHelper.getText(
                    NlpEngineHelper.getPlainText(this, ci, true).getValue());
            } catch (IOException e) {
                throw new EngineException(this, ci, "Unable to access plain/text content!", e);
            }
        }
        log.debug("  > AnalysedText {}", at);
        String language = getLanguage(this, ci, true);
        log.debug("  > Language {}", language);
        if (log.isDebugEnabled()) {
            log.debug("computeEnhancements for ContentItem {} language {} text={}", new Object[] {
                    ci.getUri().getUnicodeString(), language, StringUtils.abbreviate(content, 100)});
        }
        // TODO: we need to do the same for the the default matching language
        TaggingSession session;
        try {
            session = TaggingSession.createSession(indexConfig, language);
        } catch (CorpusException e) {
            throw new EngineException(this, ci, e);
        }
        if(!session.hasCorpus()){
            //no corpus available for processing the request
            return;
        }
        long taggingStart = System.currentTimeMillis();
        final NavigableMap<int[],Tag> tags = new TreeMap<int[],Tag>(Tag.SPAN_COMPARATOR);
        try {
            //process the language of the document
            Corpus corpus = null;
            if(session.getLanguageCorpus() != null){
                corpus = session.getLanguageCorpus();
                long t = System.currentTimeMillis();
                int d = tag(content, at, session,corpus,tags);
                log.info(" - {}: fst: {}ms (callback: {}ms)", new Object[]{
                        corpus.getIndexedField(), System.currentTimeMillis()-t, d
                });
            }
            if(session.getDefaultCorpus() != null){
                if(corpus == null){
                    corpus = session.getDefaultCorpus();
                }
                long t = System.currentTimeMillis();
                int d = tag(content, at, session, session.getDefaultCorpus(),tags);
                log.info(" - {}: fst: {}ms (callback: {}ms)",new Object[]{
                        session.getDefaultCorpus().getIndexedField(), 
                        System.currentTimeMillis()-t, d});
            }
            long taggingEnd = System.currentTimeMillis();
            if(corpus == null){
                throw new EngineException(this,ci,"No FST corpus found to process contentItem "
                    + "language '"+session.getLanguage()+"'!",null);
            } else {
                if(session.getLanguageCorpus() != null && session.getDefaultCorpus() != null){
                    log.info(" - sum fst: {} ms", taggingEnd - taggingStart);
                }
            }
            int matches = match(content, tags.values(), session.entityMentionTypes);
            log.debug(" - loaded {} ({} loaded, {} cached, {} appended) Matches in {} ms", 
                    new Object[]{matches, session.getSessionDocLoaded(),
                        session.getSessionDocCached(), session.getSessionDocAppended(),
                        System.currentTimeMillis()-taggingEnd});
            if(log.isDebugEnabled() && session.getDocumentCache() != null){
                log.debug("EntityCache Statistics: {}", 
                    session.getDocumentCache().printStatistics());
            }
        } catch (IOException e) {
            throw new EngineException(this,ci,e);
        } finally {
            session.close();
        }
        if(log.isTraceEnabled()){
            log.trace("Tagged Entities:");
            for(Tag tag : tags.values()){
                log.trace("[{},{}]: {}", new Object[]{tag.getStart(),tag.getEnd(),tag.getMatches()});
            }
        }
        ci.getLock().writeLock().lock();
        try {
            writeEnhancements(ci,content,tags.values(),language, 
                elConfig.isWriteEntityRankings());
        } finally {
            ci.getLock().writeLock().unlock();
        }
        tags.clear(); //help the GC
    }

    private int match(String text, Collection<Tag> tags, Map<int[],Set<String>> emTypes) {
        log.trace("  ... process matches for {} extracted Tags:",tags.size());
        int matchCount = 0;
        Iterator<Tag> tagIt = tags.iterator();
        while(tagIt.hasNext()){
            Tag tag = tagIt.next();
            String anchor = text.substring(tag.getStart(), tag.getEnd());
            log.trace(" {}: '{}'", tag, anchor);
            tag.setAnchor(anchor);
            if(!elConfig.isCaseSensitiveMatching()){
                anchor = anchor.toLowerCase(Locale.ROOT);
            }
            
            int alength = anchor.length();
            List<Match> suggestions = new ArrayList<Match>(tag.getMatches().size());
            int i=1; //only for trace level debugging
            for(Match match : tag.getMatches()){
                if(log.isTraceEnabled()){
                    log.trace(" {}. {}", i++,  match.getUri());
                }
                matchCount++;
                final boolean filterType;
                if(linkingMode == LinkingModeEnum.NER){
                    Set<String> types = emTypes.get(new int[]{tag.getStart(), tag.getEnd()});
                    if(types == null){
                        log.warn(" - missing NE types for Named Entity [{},{}] {}!",
                            new Object[]{tag.getStart(), tag.getEnd(),tag.getAnchor()});
                        filterType = true;
                    } else {
                        filterType = filterByNamedEntityType(match.getTypes().iterator(), types);
                    }
                } else {
                    filterType = filterEntityByType(match.getTypes().iterator());
                }
                if(!filterType){
                    int distance = Integer.MAX_VALUE;
                    Literal matchLabel = null;
                    for(Iterator<Literal> it = match.getLabels().iterator(); it.hasNext() && distance > 0;){
                        Literal literal = it.next();
                        String label = literal.getLexicalForm();
                        int d;
                        if(!elConfig.isCaseSensitiveMatching()){
                            label = label.toLowerCase(Locale.ROOT);
                        }
                        d = StringUtils.getLevenshteinDistance(anchor, label);
                        if(d < distance){
                            distance = d;
                            matchLabel = literal;
                        }
                    }
                    if(distance == 0){
                        match.setMatch(1.0, matchLabel);
                    } else {
                        double length = Math.max(alength, matchLabel.getLexicalForm().length());
                        match.setMatch(1d - ((double)distance/length),matchLabel);
                    }
                    if(match.getScore() >= elConfig.getMinMatchScore()){
                        log.trace(" ... add suggestion: label: '{}'; conf: {}", 
                            matchLabel, match.getScore());
                        suggestions.add(match);
                    } else {
                        log.trace(" ... filtered because match score < {}", 
                            elConfig.getMinMatchScore());
                    }
                } else { //the type of the current Entity is blacklisted
                    log.trace("  ... filtered because of entity types");
                }
            }
            if(suggestions.isEmpty()){
                tagIt.remove(); // remove this tag as no match is left
            } else if(suggestions.size() > 1){ //if we have multiple suggestions
                //sort based on score
                Collections.sort(suggestions, Match.SCORE_COMPARATOR);
                int maxSuggestions = elConfig.getMaxSuggestions();
                if((suggestions.size() > maxSuggestions + 1) && 
                        elConfig.isIncludeSuggestionsWithSimilarScore()){
                    //include suggestions with similar score
                    double minIncludeScore = suggestions.get(maxSuggestions).getScore();
                    int numInclude = maxSuggestions + 1; //the next element
                    double actScore;
                    do {
                        actScore = suggestions.get(numInclude).getScore();
                        numInclude++; //increase for the next iteration
                    } while(numInclude < suggestions.size() && actScore >= minIncludeScore);
                    maxSuggestions = numInclude - 1;
                }
                //adapt score based on entity ranking
                if(elConfig.isRankEqualScoresBasedOnEntityRankings()){
                    adaptScoresForEntityRankings(suggestions);
                }
                if(log.isTraceEnabled()){ //log the suggestion information
                    log.trace("Suggestions:");
                    int si=1;
                    for(Match m : suggestions){
                        log.trace(" {}. {} - {} ({})", new Object[]{
                                si <= maxSuggestions ? si : "--",
                                m.getScore(),m.getMatchLabel(),m.getUri()});
                        si++;
                    }
                }
                //remove all suggestions > maxSuggestions
                if(suggestions.size() > maxSuggestions){
                    suggestions.subList(maxSuggestions,suggestions.size()).clear();
                }
            }
            tag.setSuggestions(suggestions);
        }
        return matchCount;
    }
    /**
     * Filter Entities based on matching the entity types with the named entity types.
     * The {@link #neTypeMappings} are used to convert named entity types to 
     * entity types. 
     * @param eTypes the types of the entity
     * @param neTypes the types of the named entity
     * @return
     */
    private boolean filterByNamedEntityType(Iterator<IRI> eTypes, Set<String> neTypes) {
        //first collect the allowed entity types
        Set<String> entityTypes = new HashSet<String>();
        for(String neType : neTypes){
            if(neType != null){
                Set<String> mappings = neTypeMappings.get(neType);
                if(mappings != null){
                    if(mappings.contains(null)){
                        //found an wildcard
                        return false; //do not filter
                    } else {
                        entityTypes.addAll(mappings);
                    }
                } //else no mapping for neType (tag or uri) present
            }
        }
        if(entityTypes.isEmpty()){
            return true; //no match possible .. filter
        }
        //second check the actual entity types against the allowed
        while(eTypes.hasNext()){
            IRI typeUri = eTypes.next();
            if(typeUri != null && entityTypes.contains(typeUri.getUnicodeString())){
                return false; //we found an match .. do not filter
            }
        }
        //no match found ... filter
        return true;
    }

    /**
     * Applies the configured entity type based filters
     * @param entityTypes
     * @return
     */
    private boolean filterEntityByType(Iterator<IRI> entityTypes){
        Map<IRI, Integer> whiteList = elConfig.getWhitelistedTypes();
        Map<IRI, Integer> blackList = elConfig.getBlacklistedTypes();
        Integer w = null;
        Integer b = null;
        while(entityTypes.hasNext()){
            IRI type = entityTypes.next();
            Integer act = whiteList.get(type);
            if(act != null){
                if(w == null || act.compareTo(w) < 0){
                    w = act;
                }
                if(act.intValue() == 0){
                    break;
                }
            }
            act = blackList.get(type);
            if(act != null){
                if(b == null || act.compareTo(b) < 0){
                    b = act;
                }
                if(act.intValue() == 0){
                    break;
                }
            }
        }
        if(w == null && b == null){
            return !elConfig.isDefaultWhitelistTypes();
        } else if(w != null){
            return b == null || w.compareTo(b) < 0 ? false : true;
        } else { //w == null && b != null
            return true; //filter
        }
    }
    /**
     * Uses the {@link Corpus} to tag the the {@link AnalysedText} and adds 
     * tagging results to the parsed tag map.
     * @param content the content to link
     * @param at the AnalyzedText. not required if {@link LinkingModeEnum#PLAIN}
     * @param session the tagging session of the text
     * @param corpus the corpus o the session to tag the content with
     * @param tags the Tags map used to store the tagging results
     * @return the time in milliseconds spent in the tag callback.
     * @throws IOException on any error while accessing the {@link SolrCore}
     */
    private int tag(final String content, final AnalysedText at, final TaggingSession session, 
            final Corpus corpus, final Map<int[],Tag> tags) throws IOException{
        final OpenBitSet matchDocIdsBS = new OpenBitSet(session.getSearcher().maxDoc());
        TokenStream baseTokenStream = corpus.getTaggingAnalyzer().tokenStream("", 
            new CharSequenceReader(content));
        final TokenStream tokenStream;
        final TagClusterReducer reducer;
        log.debug(" ... set up TokenStream and TagClusterReducer for linking mode {}", linkingMode);
        switch (linkingMode) {
            case PLAIN: //will link all tokens and search longest dominant right
                tokenStream = baseTokenStream;
                reducer = TagClusterReducer.LONGEST_DOMINANT_RIGHT;
                break;
            case NER:
                //this uses the NamedEntityTokenFilter as tokenStream and a
                //combination with the longest dominant right as reducer 
                NamedEntityTokenFilter neTokenFilter = new NamedEntityTokenFilter(
                    baseTokenStream, at, session.getLanguage(), neTypeMappings.keySet(),
                    session.entityMentionTypes);
                tokenStream = neTokenFilter;
                reducer = new ChainedTagClusterReducer(neTokenFilter,
                    TagClusterReducer.LONGEST_DOMINANT_RIGHT);
                break;
            case LINKABLE_TOKEN:
                //this uses the LinkableTokenFilter as tokenStream
                LinkableTokenFilter linkableTokenFilter = new LinkableTokenFilter(baseTokenStream, 
                    at, session.getLanguage(), tpConfig.getConfiguration(session.getLanguage()),
                    elConfig.getMinChunkMatchScore(), elConfig.getMinFoundTokens());
                //NOTE that the  LinkableTokenFilter implements longest dominant right
                // based on the matchable span of tags (instead of the whole span).
                reducer = new ChainedTagClusterReducer(
                    linkableTokenFilter,TagClusterReducer.ALL);
                tokenStream = linkableTokenFilter;
                break;
            default:
                throw new IllegalStateException("Unrecognized LinkingMode '"
                    + linkingMode + "! Please adapt implementation to changed Enumeration!");
        }
        log.debug(" - tokenStream: {}", tokenStream);
        log.debug(" - reducer: {} (class: {})", reducer, reducer.getClass().getName());
        
        //Now process the document
        final long[] time = new long[]{0};
        new Tagger(corpus.getFst(), tokenStream, reducer,session.isSkipAltTokens()) {
            
            @Override
            protected void tagCallback(int startOffset, int endOffset, long docIdsKey) {
                long start = System.nanoTime();
                if(log.isTraceEnabled()){
                    log.trace(" > tagCallback for {}", content.subSequence(startOffset, endOffset));
                }
                int[] span = new int[]{startOffset,endOffset};
                Tag tag = tags.get(span);
                if(tag == null){
                    tag = new Tag(span);
                    tags.put(span, tag);
                }
                // below caches, and also flags matchDocIdsBS
                Set<Match> matches = createMatches(docIdsKey);
                if(log.isTraceEnabled()){
                    log.trace("  - {} matches", matches.size());
                }
                tag.addIds(matches);
                long dif = System.nanoTime()-start;
                time[0] = time[0]+dif;
            }
            
            //NOTE: We can not use a cache, because we need to create different
            //      Match instances even for the same 'docIdsKey'. This is because
            //      the same result list might get generated for different
            //      surface forms in the text (e.g. if the SolrIndex is case
            //      insensitive, but the linking does consider the case when
            //      calculating the score). If we would use this cache Match
            //      instances would be used for several occurrences in the text
            //      and Match#getScore() values would get overridden when 
            //      processing those multiple occurrences.
            //Map<Long,Set<Match>> docIdsListCache = new HashMap<Long,Set<Match>>(1024);

            private Set<Match> createMatches(long docIdsKey) {
                IntsRef docIds = lookupDocIds(docIdsKey);
                Set<Match> matches = new HashSet<Match>(docIds.length);
                for (int i = docIds.offset; i < docIds.offset + docIds.length; i++) {
                    int docId = docIds.ints[i];
                    matchDocIdsBS.set(docId);// also, flip docid in bitset
                    matches.add(session.createMatch(docId));// translates here
                }
                return matches;
            }

        }.process();
        return (int)(time[0]/1000000);
    }
    /**
     * Adapts the scores of Matches with the same {@link Match#getScore() score}
     * but different {@link Match#getRanking() entity rankings} in a way that
     * suggestions with a higher ranking do have a slightly better score. The
     * score difference is never higher as <code>0.1</code>.
     * @param matches the matches
     */
    private void adaptScoresForEntityRankings(List<Match> matches) {
        List<Match> equalScoreList = new ArrayList<Match>(4);
        double score = 2f;
        for(Match match : matches){
            double actScore = match.getScore();
            if(score == actScore){
                equalScoreList.add(match);
            } else {
                if(equalScoreList.size() > 1){
                    adaptScoreForEntityRankings(equalScoreList, actScore);
                }
                score = actScore;
                equalScoreList.clear();
                equalScoreList.add(match);
            }
        }
        if(equalScoreList.size() > 1){
            adaptScoreForEntityRankings(equalScoreList,0);
        }
        //resort by score
        Collections.sort(matches, Match.SCORE_COMPARATOR);
    }
    /**
     * This method slightly adapts scores of Suggestions based on the Entity ranking.
     * It is used for Suggestions that would have the exact same score (e.g. 1.0) to
     * ensure ordering of the suggestions based on the rankings of the Entities
     * within the knowledge base linked against
     * @param equalScoreList Entities with the same {@link Suggestion#getScore()}
     * values. If this is not the case this method will change scores in unintended
     * ways
     * @param nextScore the score of the {@link Suggestion} with a lower score as the
     * list of suggestions parsed in the first parameter
     */
    private void adaptScoreForEntityRankings(List<Match> equalScoreList, double nextScore) {
        double score = equalScoreList.get(0).getScore();
        log.trace("  > Adapt Score of multiple Suggestions "
            + "with '{}' based on EntityRanking",score);
        //Adapt the score to reflect the entity ranking
        //but do not change order with entities of different
        //score. Also do not change the score more that 0.1
        //TODO: make the max change (0.1) configurable
        double dif = (Math.min(0.1, score-nextScore))/equalScoreList.size();
        Collections.sort(equalScoreList,Match.ENTITY_RANK_COMPARATOR);
        log.trace("    - keep socre of {} at {}", equalScoreList.get(0).getUri(), score);
        for(int i=1;i<equalScoreList.size();i++){
            score = score-dif;
            if(Match.ENTITY_RANK_COMPARATOR.compare(equalScoreList.get(i-1), 
                equalScoreList.get(i)) != 0){
                equalScoreList.get(i).updateScore(score);
                log.trace("    - set score of {} to {}", equalScoreList.get(i).getUri(), score);
            } else {
                double lastScore = equalScoreList.get(i-1).getScore();
                equalScoreList.get(i).updateScore(lastScore);
                log.trace("    - set score of {} to {}", equalScoreList.get(i).getUri(), lastScore);
            }
        }
    }
    
    /**
     * Writes the Enhancements for the {@link LinkedEntity LinkedEntities}
     * extracted from the parsed ContentItem
     * @param ci
     * @param tags
     * @param language
     */
    private void writeEnhancements(ContentItem ci, String text, Collection<Tag> tags, 
            String language, boolean writeRankings) {
        Language languageObject = null;
        if(language != null && !language.isEmpty()){
            languageObject = new Language(language);
        }
        
        Graph metadata = ci.getMetadata();
        for(Tag tag : tags){
            Collection<IRI> textAnnotations = new ArrayList<IRI>(tags.size());
            //first create the TextAnnotations for the Occurrences
            Literal startLiteral = literalFactory.createTypedLiteral(tag.getStart());
            Literal endLiteral = literalFactory.createTypedLiteral(tag.getEnd());
            //search for existing text annotation
            Iterator<Triple> it = metadata.filter(null, ENHANCER_START, startLiteral);
            IRI textAnnotation = null;
            while(it.hasNext()){
                Triple t = it.next();
                if(metadata.filter(t.getSubject(), ENHANCER_END, endLiteral).hasNext() &&
                        metadata.filter(t.getSubject(), RDF_TYPE, ENHANCER_TEXTANNOTATION).hasNext()){
                    textAnnotation = (IRI)t.getSubject();
                    break;
                }
            }
            if(textAnnotation == null){ //not found ... create a new one
                textAnnotation = EnhancementEngineHelper.createTextEnhancement(ci, this);
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_START, 
                    startLiteral));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_END, 
                    endLiteral));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_SELECTION_CONTEXT, 
                    new PlainLiteralImpl(getSelectionContext(text, tag.getAnchor(), 
                        tag.getStart()),languageObject)));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_SELECTED_TEXT, 
                    new PlainLiteralImpl(tag.getAnchor(),languageObject)));
                metadata.add(new TripleImpl(textAnnotation, 
                    Properties.ENHANCER_CONFIDENCE, 
                    literalFactory.createTypedLiteral(tag.getScore())));
            } else { //if existing add this engine as contributor
                metadata.add(new TripleImpl(textAnnotation, DC_CONTRIBUTOR, 
                    new PlainLiteralImpl(this.getClass().getName())));
            }
            //add dc:types (even to existing)
            for(IRI dcType : getDcTypes(tag.getSuggestions())){
                metadata.add(new TripleImpl(
                    textAnnotation, Properties.DC_TYPE, dcType));
            }
            textAnnotations.add(textAnnotation);
            //now the EntityAnnotations for the Suggestions
            for(Match match : tag.getSuggestions()){
                IRI entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, this);
                //should we use the label used for the match, or search the
                //representation for the best label ... currently its the matched one
                metadata.add(new TripleImpl(entityAnnotation, Properties.ENHANCER_ENTITY_LABEL, match.getMatchLabel()));
                metadata.add(new TripleImpl(entityAnnotation,ENHANCER_ENTITY_REFERENCE, 
                    new IRI(match.getUri())));
                for(IRI type : match.getTypes()){
                    metadata.add(new TripleImpl(entityAnnotation, 
                        Properties.ENHANCER_ENTITY_TYPE, type));
                }
                metadata.add(new TripleImpl(entityAnnotation,
                    Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(match.getScore())));
                //add the relation to the fise:TextAnnotation (the tag)
                metadata.add(new TripleImpl(entityAnnotation, Properties.DC_RELATION, textAnnotation));
                //write origin information
                if(indexConfig.getOrigin() != null){
                    metadata.add(new TripleImpl(entityAnnotation, FISE_ORIGIN, indexConfig.getOrigin()));
                }
                //TODO: add origin information of the EntiySearcher
//                for(Entry<IRI,Collection<RDFTerm>> originInfo : entitySearcher.getOriginInformation().entrySet()){
//                    for(RDFTerm value : originInfo.getValue()){
//                        metadata.add(new TripleImpl(entityAnnotation, 
//                            originInfo.getKey(),value));
//                    }
//                }
                if(writeRankings){
                    Double ranking = match.getRanking();
                    if(ranking != null){
                        metadata.add(new TripleImpl(entityAnnotation, 
                            ENHANCER_ENTITY_RANKING,
                            literalFactory.createTypedLiteral(ranking)));
                    }
                }

                //TODO: dereferencing 
//                if(linkerConfig.isDereferenceEntitiesEnabled() &&
//                        dereferencedEntitis.add(entity.getUri())){ //not yet dereferenced
//                    //add all outgoing triples for this entity
//                    //NOTE: do not add all triples as there might be other data in the graph
//                    for(Iterator<Triple> triples = entity.getData().filter(entity.getUri(), null, null);
//                            triples.hasNext();metadata.add(triples.next()));
//                }
            }
        }
    }

    /**
     * Retrieves all {@link EntitySearcher#getEncodedTypeField()} values of the parsed
     * {@link Suggestion}s and than lookup the {@link NamespaceEnum#dcTerms dc}:type
     * values for the {@link LinkedEntity#getTypes()} by using the configured
     * {@link EntityLinkerConfig#getTypeMappings() types mappings} (and if
     * no mapping is found the {@link EntityLinkerConfig#getDefaultDcType() 
     * default} type.
     * @param conceptTypes The list of suggestions
     * @return the types values for the {@link LinkedEntity}
     */
    private Set<IRI> getDcTypes(List<Match> matches){
        if(matches == null || matches.isEmpty()){
            return Collections.emptySet();
        }
        Collection<IRI> conceptTypes = new HashSet<IRI>();
        double score = -1; //only consider types of the best ranked Entities
        for(Match match : matches){
            double actScore = match.getScore();
            if(actScore < score){
                break;
            }
            score = actScore;
            for(Iterator<IRI> types = match.getTypes().iterator(); 
                types.hasNext(); conceptTypes.add(types.next()));
        }
        Map<IRI,IRI> typeMappings = elConfig.getTypeMappings();
        Set<IRI> dcTypes = new HashSet<IRI>();
        for(IRI conceptType : conceptTypes){
            IRI dcType = typeMappings.get(conceptType);
            if(dcType != null){
                dcTypes.add(dcType);
            }
        }
        if(dcTypes.isEmpty() && elConfig.getDefaultDcType() != null){
            dcTypes.add(elConfig.getDefaultDcType());
        }
        return dcTypes;
    }
}
