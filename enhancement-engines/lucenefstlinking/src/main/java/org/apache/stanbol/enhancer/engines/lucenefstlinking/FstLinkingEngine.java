package org.apache.stanbol.enhancer.engines.lucenefstlinking;

import static org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion.ENTITY_RANK_COMPARATOR;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.clerezza.rdf.core.Language;
import org.apache.clerezza.rdf.core.Literal;
import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Resource;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.PlainLiteralImpl;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.OpenBitSet;
import org.apache.solr.core.SolrCore;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.TextProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.engine.EntityLinkingEngine;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkedEntity;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.LinkedEntity.Occurrence;
import org.apache.stanbol.enhancer.engines.lucenefstlinking.TaggingSession.Corpus;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
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

    private final LiteralFactory literalFactory = LiteralFactory.getInstance();
    
    protected final String name;

    protected final TextProcessingConfig tpConfig;
    protected final EntityLinkerConfig elConfig;

    private IndexConfiguration indexConfig;


    public FstLinkingEngine(String name, IndexConfiguration indexConfig,
            TextProcessingConfig tpConfig, EntityLinkerConfig elConfig) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL nor blank!");
        }
        this.name = name;
        if (indexConfig == null) {
            throw new IllegalArgumentException("The parsed IndexConfiguration MUST NOT be NULL!");
        }
        this.indexConfig = indexConfig;
        if (tpConfig == null) {
            throw new IllegalArgumentException("The parsed Text Processing configuration MUST NOT be NULL");
        }
        this.tpConfig = tpConfig;
        if (elConfig == null) {
            throw new IllegalArgumentException("The parsed Entity Linking configuration MUST NOT be NULL");
        }
        this.elConfig = elConfig;
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
        if (language == null || !indexConfig.getFstConfig().isLanguage(language)) {
            log.debug("Engine {} ignores ContentItem {} becuase language {} is not condigured.",
                new Object[] {getName(), ci.getUri(), language});
            return CANNOT_ENHANCE;
        }
        // we need a detected language, the AnalyzedText contentPart with
        // Tokens.
        AnalysedText at = getAnalysedText(this, ci, false);
        return at != null && at.getTokens().hasNext() ? ENHANCE_ASYNC : CANNOT_ENHANCE;
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        AnalysedText at = getAnalysedText(this, ci, true);
        log.debug("  > AnalysedText {}", at);
        String language = getLanguage(this, ci, true);
        log.debug("  > Language {}", language);
        if (log.isDebugEnabled()) {
            log.debug("computeEnhancements for ContentItem {} language {} text={}", new Object[] {
                    ci.getUri().getUnicodeString(), language, StringUtils.abbreviate(at.getSpan(), 100)});
        }
        // TODO: we need to do the same for the the default matching language
        TaggingSession session;
        try {
            session = TaggingSession.createSession(indexConfig, language);
        } catch (CorpusException e) {
            throw new EngineException(this, ci, e);
        }
        long taggingStart = System.currentTimeMillis();
        final NavigableMap<int[],Tag> tags = new TreeMap<int[],Tag>(Tag.SPAN_COMPARATOR);
        try {
            //process the language of the document
            Corpus corpus = null;
            if(session.getLanguageCorpus() != null){
                corpus = session.getLanguageCorpus();
                long t = System.currentTimeMillis();
                int d = tag(at, session,corpus,tags);
                log.info(" - {}: fst: {}ms (callback: {}ms)", new Object[]{
                        corpus.getIndexedField(), System.currentTimeMillis()-t, d
                });
            }
            if(session.getDefaultCorpus() != null){
                if(corpus == null){
                    corpus = session.getDefaultCorpus();
                }
                long t = System.currentTimeMillis();
                int d = tag(at, session, session.getDefaultCorpus(),tags);
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
            log.debug("Process Matches for {} extragted Tags:",tags.size());
            int matches = match(at,tags.values());
            //thr remaining code is logging only
            if(log.isTraceEnabled()){
                String text = at.getSpan();
                for(Tag tag : tags.values()){
                    log.trace(" {}: '{}'", tag, text.subSequence(tag.getStart(), tag.getEnd()));
                    int i=1;
                    for(Match match : tag.getSuggestions()){
                        log.trace(" {}. {} - {} ({})", new Object[]{
                                i++, match.getScore(),  match.getMatchLabel(), match.getUri()});
                    }
                }
            }
            log.info(" - loaded {} ({} loaded, {} cached, {} appended) Matches in {} ms", 
                    new Object[]{matches, session.getSessionDocLoaded(),
                        session.getSessionDocCached(), session.getSessionDocAppended(),
                        System.currentTimeMillis()-taggingEnd});
            if(log.isDebugEnabled()){
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
            writeEnhancements(ci,at.getSpan(),tags.values(),language);
        } finally {
            ci.getLock().writeLock().unlock();
        }
    }

    private int match(AnalysedText at, Collection<Tag> tags) {
        int matchCount = 0;
        String text = at.getSpan();
        Iterator<Tag> tagIt = tags.iterator();
        while(tagIt.hasNext()){
            Tag tag = tagIt.next();
            String anchor = text.substring(tag.getStart(), tag.getEnd());
            tag.setAnchor(anchor);
            if(!elConfig.isCaseSensitiveMatching()){
                anchor = anchor.toLowerCase(Locale.ROOT);
            }
            
            int alength = anchor.length();
            List<Match> suggestions = new ArrayList<Match>(tag.getMatches().size());
            for(Match match : tag.getMatches()){
                matchCount++;
                if(!filterEntityByType(match.getTypes().iterator())){
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
                    suggestions.add(match);
                } //else the type of the current Entity is blacklisted
            }
            if(suggestions.isEmpty()){
                tagIt.remove(); // remove this tag as no match is left
            } else if(suggestions.size() > 1){ //if we have multiple suggestions
                //sort based on score
                Collections.sort(suggestions, Match.SCORE_COMPARATOR);
                //adapt score based on entity ranking
                adaptScoresForEntityRankings(suggestions);
                //cut the list on the maximum nuber of suggestions
                if(suggestions.size() > elConfig.getMaxSuggestions()){
                    suggestions = suggestions.subList(0, elConfig.getMaxSuggestions());
                }
            }
            tag.setSuggestions(suggestions);
        }
        return matchCount;
    }
    /**
     * Applies the configured entity type based filters
     * @param entityTypes
     * @return
     */
    private boolean filterEntityByType(Iterator<UriRef> entityTypes){
        Map<UriRef, Integer> whiteList = elConfig.getWhitelistedTypes();
        Map<UriRef, Integer> blackList = elConfig.getBlacklistedTypes();
        Integer w = null;
        Integer b = null;
        while(entityTypes.hasNext()){
            UriRef type = entityTypes.next();
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
     * @param at the AnalyzedText
     * @param session the tagging session of the text
     * @param corpus the corpus o the session to tag the content with
     * @param tags the Tags map used to store the tagging results
     * @return the time in milliseconds spent in the tag callback.
     * @throws IOException on any error while accessing the {@link SolrCore}
     */
    private int tag(AnalysedText at, final TaggingSession session, 
            final Corpus corpus, final Map<int[],Tag> tags) throws IOException {
        final OpenBitSet matchDocIdsBS = new OpenBitSet(session.getSearcher().maxDoc());
        TokenStream baseTokenStream = corpus.getAnalyzer().tokenStream("", 
            new CharSequenceReader(at.getText()));
        TokenStream linkableTokenStream = new LinkableTokenFilterStream(baseTokenStream, 
            at, session.getLanguage(), tpConfig.getConfiguration(session.getLanguage()));
        final long[] time = new long[]{0};
        new Tagger(corpus.getFst(), linkableTokenStream, TagClusterReducer.NO_SUB) {
            
            @Override
            protected void tagCallback(int startOffset, int endOffset, long docIdsKey) {
                long start = System.nanoTime();
                int[] span = new int[]{startOffset,endOffset};
                Tag tag = tags.get(span);
                if(tag == null){
                    tag = new Tag(span);
                    tags.put(span, tag);
                }
                // below caches, and also flags matchDocIdsBS
                tag.addIds(createMatches(docIdsKey));
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
        log.debug("  > Adapt Score of multiple Suggestions "
            + "with '{}' based on EntityRanking",score);
        //Adapt the score to reflect the entity ranking
        //but do not change order with entities of different
        //score. Also do not change the score more that 0.1
        //TODO: make the max change (0.1) configurable
        double dif = (Math.min(0.1, score-nextScore))/equalScoreList.size();
        Collections.sort(equalScoreList,Match.ENTITY_RANK_COMPARATOR);
        log.debug("    - keep socre of {} at {}", equalScoreList.get(0).getUri(), score);
        for(int i=1;i<equalScoreList.size();i++){
            score = score-dif;
            if(Match.ENTITY_RANK_COMPARATOR.compare(equalScoreList.get(i-1), 
                equalScoreList.get(i)) != 0){
                equalScoreList.get(i).updateScore(score);
                log.debug("    - set score of {} to {}", equalScoreList.get(i).getUri(), score);
            } else {
                double lastScore = equalScoreList.get(i-1).getScore();
                equalScoreList.get(i).updateScore(lastScore);
                log.debug("    - set score of {} to {}", equalScoreList.get(i).getUri(), lastScore);
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
    private void writeEnhancements(ContentItem ci, String text, Collection<Tag> tags, String language) {
        Language languageObject = null;
        if(language != null && !language.isEmpty()){
            languageObject = new Language(language);
        }
        
        MGraph metadata = ci.getMetadata();
        for(Tag tag : tags){
            Collection<UriRef> textAnnotations = new ArrayList<UriRef>(tags.size());
            //first create the TextAnnotations for the Occurrences
            Literal startLiteral = literalFactory.createTypedLiteral(tag.getStart());
            Literal endLiteral = literalFactory.createTypedLiteral(tag.getEnd());
            //search for existing text annotation
            Iterator<Triple> it = metadata.filter(null, ENHANCER_START, startLiteral);
            UriRef textAnnotation = null;
            while(it.hasNext()){
                Triple t = it.next();
                if(metadata.filter(t.getSubject(), ENHANCER_END, endLiteral).hasNext() &&
                        metadata.filter(t.getSubject(), RDF_TYPE, ENHANCER_TEXTANNOTATION).hasNext()){
                    textAnnotation = (UriRef)t.getSubject();
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
            for(UriRef dcType : getDcTypes(tag.getSuggestions())){
                metadata.add(new TripleImpl(
                    textAnnotation, Properties.DC_TYPE, dcType));
            }
            textAnnotations.add(textAnnotation);
            //now the EntityAnnotations for the Suggestions
            for(Match match : tag.getSuggestions()){
                UriRef entityAnnotation = EnhancementEngineHelper.createEntityEnhancement(ci, this);
                //should we use the label used for the match, or search the
                //representation for the best label ... currently its the matched one
                metadata.add(new TripleImpl(entityAnnotation, Properties.ENHANCER_ENTITY_LABEL, match.getMatchLabel()));
                metadata.add(new TripleImpl(entityAnnotation,ENHANCER_ENTITY_REFERENCE, 
                    new UriRef(match.getUri())));
                for(UriRef type : match.getTypes()){
                    metadata.add(new TripleImpl(entityAnnotation, 
                        Properties.ENHANCER_ENTITY_TYPE, type));
                }
                metadata.add(new TripleImpl(entityAnnotation,
                    Properties.ENHANCER_CONFIDENCE, literalFactory.createTypedLiteral(match.getScore())));
                //add the relation to the fise:TextAnnotation (the tag)
                metadata.add(new TripleImpl(entityAnnotation, Properties.DC_RELATION, textAnnotation));
                //TODO: add origin information of the EntiySearcher
//                for(Entry<UriRef,Collection<Resource>> originInfo : entitySearcher.getOriginInformation().entrySet()){
//                    for(Resource value : originInfo.getValue()){
//                        metadata.add(new TripleImpl(entityAnnotation, 
//                            originInfo.getKey(),value));
//                    }
//                }
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
     * Retrieves all {@link EntitySearcher#getTypeField()} values of the parsed
     * {@link Suggestion}s and than lookup the {@link NamespaceEnum#dcTerms dc}:type
     * values for the {@link LinkedEntity#getTypes()} by using the configured
     * {@link EntityLinkerConfig#getTypeMappings() types mappings} (and if
     * no mapping is found the {@link EntityLinkerConfig#getDefaultDcType() 
     * default} type.
     * @param conceptTypes The list of suggestions
     * @return the types values for the {@link LinkedEntity}
     */
    private Set<UriRef> getDcTypes(List<Match> matches){
        if(matches == null || matches.isEmpty()){
            return Collections.emptySet();
        }
        Collection<UriRef> conceptTypes = new HashSet<UriRef>();
        double score = -1; //only consider types of the best ranked Entities
        for(Match match : matches){
            double actScore = match.getScore();
            if(actScore < score){
                break;
            }
            score = actScore;
            for(Iterator<UriRef> types = match.getTypes().iterator(); 
                types.hasNext(); conceptTypes.add(types.next()));
        }
        Map<UriRef,UriRef> typeMappings = elConfig.getTypeMappings();
        Set<UriRef> dcTypes = new HashSet<UriRef>();
        for(UriRef conceptType : conceptTypes){
            UriRef dcType = typeMappings.get(conceptType);
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
