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
package org.apache.stanbol.enhancer.engines.entitylinking.impl;

import static org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion.ENTITY_RANK_COMPARATOR;

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

import org.apache.clerezza.commons.rdf.Triple;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.Literal;
import org.apache.clerezza.commons.rdf.impl.utils.TripleImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcherException;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion.MATCH;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityLinker {
    
    private static final int MIN_SEARCH_LIMIT = 10;

    private final Logger log = LoggerFactory.getLogger(EntityLinker.class);
    
    private final EntityLinkerConfig linkerConfig;
    private final LanguageProcessingConfig textProcessingConfig;
    //private final AnalysedText analysedText;
    private final EntitySearcher entitySearcher;
    /**
     * The state of the current processing
     */
    private final ProcessingState state;
    /**
     * The map holding the results of the linking process
     */
    private final Map<String,LinkedEntity> linkedEntities = new HashMap<String,LinkedEntity>();
    
    //private Integer lookupLimit;
    
    private LabelTokenizer labelTokenizer;

    private LinkingStateAware linkingStateAware;

    final int minSearchResults;
    
    //Language configuration
    final String documentLang;
    final String defaultLang;
    final String documentMainLang;
    
    private Statistic textProcessingStats = new Statistic("Text Processing");
    private Statistic lookupStats = new Statistic("Vocabulary Lookup");
    private int cacheHits = 0;
    private int numQueryResults = 0;
    private int numFilteredResults = 0;
    private Statistic matchingStats = new Statistic("Label Matching");
    private Statistic rankingStats = new Statistic("Suggestion Ranking");
//    private Statistic test = new Statistic("test1");
//    private Statistic test2_ = new Statistic("test2");
//    private int numLabels = 0;
    private long processingTime = -1;

    private Map<List<String>,List<Entity>> lookupCache;


    public EntityLinker(AnalysedText analysedText, String language,
                        LanguageProcessingConfig textProcessingConfig,
                        EntitySearcher entitySearcher,
                        EntityLinkerConfig linkerConfig,
                        LabelTokenizer labelTokenizer) {
        this(analysedText,language,textProcessingConfig,entitySearcher,linkerConfig,labelTokenizer,null);
    }
    public EntityLinker(AnalysedText analysedText, String language,
                LanguageProcessingConfig textProcessingConfig,
                EntitySearcher entitySearcher,
                EntityLinkerConfig linkerConfig,
                LabelTokenizer labelTokenizer, LinkingStateAware linkingStateAware) {
        //this.analysedText = analysedText;
        this.lookupCache = new HashMap<List<String>,List<Entity>>();
        this.entitySearcher = entitySearcher;
        this.linkerConfig = linkerConfig;
        this.textProcessingConfig = textProcessingConfig;
        this.labelTokenizer = labelTokenizer;
        this.state = new ProcessingState(analysedText,language,textProcessingConfig);
        minSearchResults = entitySearcher.getLimit() == null ? MIN_SEARCH_LIMIT : 
            Math.max(MIN_SEARCH_LIMIT,entitySearcher.getLimit());
        //this.lookupLimit  = Math.max(minResults,linkerConfig.getMaxSuggestions()*3);
        this.linkingStateAware = linkingStateAware;
        //init the language settings
        this.documentLang = state.getLanguage();
        this.defaultLang = linkerConfig.getDefaultLanguage();
        int countryCodeIndex = documentLang == null ? -1 : documentLang.indexOf('-');
        if(countryCodeIndex >= 2){
            documentMainLang = documentLang.substring(0,countryCodeIndex);
        } else {
            documentMainLang = null;
        }

    }
    /**
     * Steps over the sentences, chunks, tokens of the {@link #sentences}
     */
    public void process() throws EntitySearcherException {
        long startTime = System.currentTimeMillis();
        //int debugedIndex = 0;
        Section sentence = null;
        textProcessingStats.begin();
        while(state.next()) {
            //STANBOL-1070: added linkingStateAware callbacks for components that
            //   need to react on the state of the Linking process
            if(linkingStateAware != null){
                if(!state.getSentence().equals(sentence)){
                    if(sentence != null){
                        linkingStateAware.endSection(sentence);
                    }
                    sentence = state.getSentence(); //set the next sentence
                    linkingStateAware.startSection(sentence); //notify its start
                }
                linkingStateAware.startToken(state.getToken().token); //notify the current token
            }
            TokenData token = state.getToken();
            if(log.isDebugEnabled()){
                log.debug("--- preocess Token {}: {} (lemma: {}) linkable={}, matchable={} | chunk: {}",
                    new Object[]{token.index,token.getTokenText(),token.getTokenLemma(),
                        token.isLinkable, token.isMatchable, token.inChunk != null ? 
                                (token.inChunk.chunk + " "+ token.inChunk.chunk.getSpan()) : "none"});
            }
            List<TokenData> searchStrings = new ArrayList<TokenData>(linkerConfig.getMaxSearchTokens());
            getSearchString(token);
            searchStrings.add(token);
            //Determine the range we are allowed to search for tokens
            final int minIncludeIndex;
            final int maxIndcludeIndex;
            int consumedIndex = state.getConsumedIndex();
            //NOTE: testing has shown that using Chunks to restrict search for
            //      additional matchable tokens does have an negative impact on
            //      recall. Because of that this restriction is for now deactivated
//            if(token.inChunk != null && !textProcessingConfig.isIgnoreChunks()){
//                minIncludeIndex = token.inChunk.getStartTokenIndex();
//                maxIndcludeIndex = token.inChunk.getEndTokenIndex();
//                log.debug("  - restrict context to chunk[{}, {}]",
//                    minIncludeIndex, maxIndcludeIndex);
//            } else {
                maxIndcludeIndex = state.getTokens().size() - 1;
                minIncludeIndex = 0;
//            }
            int prevIndex = token.index;
            int pastIndex = token.index;
            int pastNonMatchable = 0;
            int prevNonMatchable = 0;
            int distance = 0;
            do { 
                distance++;//keep track of the distance
                //get the past token at the given distance (However ignore
                //non AlphaNumeric tokens when calculating the distance)
                pastIndex++;
                TokenData pastToken = null;
                while(pastToken == null && maxIndcludeIndex >= pastIndex &&
                        pastNonMatchable <= 1){
                    TokenData td = state.getTokens().get(pastIndex);
                    if(td.hasAlphaNumeric){
                        pastToken = td;
                    } else {
                        pastIndex++;
                    }
                }
                //get the previous token at the given distance (However ignore
                //non AlphaNumeric tokens when calculating the distance)
                prevIndex--;
                TokenData prevToken = null;
                while(prevToken == null && minIncludeIndex <= prevIndex &&
                        //allow one nonMatchable token if prevIndex > the last
                        //consumed one and zero nonMatchable if prevIndex is <=
                        //the last consumed one
                        ((prevIndex > consumedIndex && prevNonMatchable <= 1) ||
                                prevIndex <= consumedIndex && prevNonMatchable < 1)){
                    TokenData td = state.getTokens().get(prevIndex);
                    if(td.hasAlphaNumeric){
                        prevToken = td;
                    } else {
                        prevIndex--;
                    }
                }
                //now that we know the tokens at this distance check if they are matchable
                //Fist the past token
                if(pastToken != null){
                    if(log.isDebugEnabled()){
                        log.debug("    {} {}:'{}' (lemma: {}) linkable={}, matchable={}",new Object[]{
                                pastToken.isMatchable? '+':'-',pastToken.index,
                                pastToken.getTokenText(), pastToken.getTokenLemma(),
                                pastToken.isLinkable, pastToken.isMatchable
                        });
                    }
                    if(pastToken.isMatchable){
                        searchStrings.add(pastToken);
                    } else {
                        pastNonMatchable++;
                    }
                }
                //Second in the previous token
                if(prevToken != null){
                    if(log.isDebugEnabled()){
                        log.debug("    {} {}:'{}' (lemma: {}) linkable={}, matchable={}",new Object[]{
                            prevToken.isMatchable? '+':'-',prevToken.index,
                            prevToken.getTokenText(), prevToken.getTokenLemma(),
                            prevToken.isLinkable, prevToken.isMatchable
                        });
                    }
                    if(prevToken.isMatchable){
                        getSearchString(prevToken);
                        searchStrings.add(0,prevToken);
                    } else {
                        prevNonMatchable++;
                    }
                }
            } while(searchStrings.size() < linkerConfig.getMaxSearchTokens() && distance <
                    linkerConfig.getMaxSearchDistance() &&
                    (prevIndex > minIncludeIndex || pastIndex < maxIndcludeIndex) &&
                    (prevNonMatchable <= 1 || pastNonMatchable <= 1));
            //we might have an additional element in the list
            if(searchStrings.size() > linkerConfig.getMaxSearchTokens()){
                searchStrings = searchStrings.subList( //the last part of the list
                    searchStrings.size()-linkerConfig.getMaxSearchTokens(), 
                    searchStrings.size());
            }
            if(log.isDebugEnabled()){
                List<String> list = new ArrayList<String>(searchStrings.size());
                for(TokenData dt : searchStrings){
                    list.add(dt.token.getSpan());
                }
                log.debug("  >> searchStrings {}",list);
            }
            textProcessingStats.complete();
            //search for Entities
            List<Suggestion> suggestions = lookupEntities(searchStrings);
            //Treat partial matches that do match more as the best FULL match
            //differently
            List<Suggestion> partialMatches = new ArrayList<Suggestion>();
            if(!suggestions.isEmpty()){
                rankingStats.begin();
                //update the suggestions based on the best match
                int bestMatchCount = suggestions.get(0).getLabelMatch().getMatchCount();
                Iterator<Suggestion> it = suggestions.iterator();
                while(it.hasNext()){
                    Suggestion suggestion = it.next();
                    //suggestions that match less tokens as the best match
                    //need to be updated to PARTIAL
                    int matchCount = suggestion.getLabelMatch().getMatchCount();
                    if(matchCount < bestMatchCount){
                        suggestion.setMatch(MATCH.PARTIAL);
                    } else if( matchCount > bestMatchCount){ //selects more tokens
                        partialMatches.add(suggestion); //but only a PARTIAL MATCH
                        it.remove(); //remove from the main suggestion list
                    }
                    //Filter matches with less than config.getMinFoundTokens()
                    //if matchcount is less than of the best match
                    if(matchCount < bestMatchCount &&
                            matchCount < linkerConfig.getMinFoundTokens()){
                        it.remove();
                    } else { //calculate the score
                        //how good is the current match in relation to the best one
                        double spanScore = matchCount >= bestMatchCount ? 1.0d : 
                            matchCount/(double)bestMatchCount;
                        suggestion.setScore(spanScore*spanScore*suggestion.getLabelMatch().getMatchScore());
                    }
                }
                Suggestion oldBestRanked = suggestions.get(0); //for debugging
                //resort by score
                Collections.sort(suggestions, Suggestion.SCORE_COMPARATOR);
                Collections.sort(partialMatches, Suggestion.SCORE_COMPARATOR);
                //this should never happen ... but the
                //matchcount of the best match MUST NOT change
                //after the sort by score!
                if(bestMatchCount != suggestions.get(0).getLabelMatch().getMatchCount()){
                    log.warn("The match count for the top Ranked Suggestion for {} " +
                            "changed after resorting based on Scores!",
                        state.getTokenText(suggestions.get(0).getLabelMatch().getStart(),bestMatchCount));
                    log.warn("  originalbest   : {}",oldBestRanked);
                    log.warn(" currnet ranking : {}",suggestions);
                    log.warn("  ... this will result in worng confidence values relative to the best match");
                }
                int maxSuggestions = linkerConfig.getMaxSuggestions();
                if((suggestions.size() + 1) > maxSuggestions && 
                        linkerConfig.isIncludeSuggestionsWithSimilarScore()){
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
                //remove all suggestions > maxSuggestions
                if(suggestions.size() > maxSuggestions){
                    suggestions.subList(maxSuggestions,suggestions.size()).clear();
                }
                //adapt equals rankings based on the entity rank
                if(linkerConfig.isRankEqualScoresBasedOnEntityRankings()){
                    adaptScoresForEntityRankings(suggestions);
                    adaptScoresForEntityRankings(partialMatches);
                }
                if(log.isDebugEnabled()){
                    log.debug("  >> Suggestions:");
                    int i=0;
                    for(Suggestion s : suggestions){
                        log.debug("   - {}: {}",i,s);
                        i++;
                    }
                }
                //process redirects
                if(linkerConfig.getRedirectProcessingMode() != RedirectProcessingMode.IGNORE){
                    for(Suggestion suggestion : suggestions){
                        processRedirects(suggestion);
                    }
                    for(Suggestion suggestion : partialMatches){
                        processRedirects(suggestion);
                    }
                }
                //create LinkedEntities for the main suggestions
                int start = suggestions.get(0).getLabelMatch().getStart();
                int span = suggestions.get(0).getLabelMatch().getSpan();
                //Store the linking results
                String selectedText = state.getTokenText(start,span);
                //float score;
                LinkedEntity linkedEntity = linkedEntities.get(selectedText);
                if(linkedEntity == null){
                    linkedEntity = new LinkedEntity(selectedText,
                        suggestions, getLinkedEntityTypes(suggestions));
                    linkedEntities.put(selectedText, linkedEntity);
                } // else Assumption: The list of suggestions is the SAME
                linkedEntity.addOccurrence(state.getSentence(), 
                    //NOTE: The end Token is "start+span-1"
                    state.getTokens().get(start).token, state.getTokens().get(start+span-1).token);
                //In case of a FULL or EXACT MATCH we can set the next token to process to the next 
                //word after the currently found suggestion
                if(suggestions.get(0).getMatch().ordinal() >= MATCH.FULL.ordinal()){
                    state.setConsumed(start+span-1);
                }
                //create LinkedEntities for partial matches
                //TODO: maybe we need to group partial matches based on their
                //      selected Tokens and only group those suggestions that do
                //      select the same span in the Text. Currently all are grouped
                //      based on those that does select the most tokens.
                if(!partialMatches.isEmpty()){
                    start = partialMatches.get(0).getLabelMatch().getStart();
                    span = partialMatches.get(0).getLabelMatch().getSpan();
                    selectedText = state.getTokenText(start, span);
                    linkedEntity = linkedEntities.get(selectedText);
                    if(linkedEntity == null){
                        linkedEntity = new LinkedEntity(selectedText,
                            partialMatches, getLinkedEntityTypes(suggestions));
                        linkedEntities.put(selectedText, linkedEntity);
                    } // else Assumption: The list of suggestions is the SAME
                    linkedEntity.addOccurrence(state.getSentence(), 
                        //NOTE: The end Token is "start+span-1"
                        state.getTokens().get(start).token, state.getTokens().get(start+span-1).token);
                }
                rankingStats.complete();
            } // else suggestions are empty
            if(linkingStateAware != null){
                linkingStateAware.endToken(state.getToken().token);
            }
            textProcessingStats.begin();
        }
        textProcessingStats.cancel(); //do not count the last call
        if(linkingStateAware != null && sentence != null){
            linkingStateAware.endSection(sentence);
        }
        this.processingTime = System.currentTimeMillis()-startTime;
    }
    /**
     * @param suggestions
     */
    private void adaptScoresForEntityRankings(List<Suggestion> suggestions) {
        List<Suggestion> equalScoreList = new ArrayList<Suggestion>(4);
        double score = 2f;
        for(Suggestion s : suggestions){
            double actScore = s.getScore();
            if(score == actScore){
                equalScoreList.add(s);
            } else {
                if(equalScoreList.size() > 1){
                    adaptScoreForEntityRankings(equalScoreList, actScore);
                }
                score = actScore;
                equalScoreList.clear();
                equalScoreList.add(s);
            }
        }
        if(equalScoreList.size() > 1){
            adaptScoreForEntityRankings(equalScoreList,0);
        }
        //resort by score
        Collections.sort(suggestions, Suggestion.SCORE_COMPARATOR);
    }
    /**
     * Helper that extracts the 
     * @param token
     */
    private String getSearchString(TokenData token) {
        String searchString = linkerConfig.isLemmaMatching() ? token.getTokenLemma() :
            token.getTokenText();
        if(searchString == null){
            searchString = token.getTokenText();
        }
        return searchString;
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
    private void adaptScoreForEntityRankings(List<Suggestion> equalScoreList, double nextScore) {
        double score = equalScoreList.get(0).getScore();
        log.debug("  > Adapt Score of multiple Suggestions "
            + "with '{}' based on EntityRanking",score);
        //Adapt the score to reflect the entity ranking
        //but do not change order with entities of different
        //score. Also do not change the score more that 0.1
        //TODO: make the max change (0.1) configurable
        double dif = (Math.min(0.1, score-nextScore))/equalScoreList.size();
        Collections.sort(equalScoreList,ENTITY_RANK_COMPARATOR);
        log.debug("    - keep socre of {} at {}", equalScoreList.get(0).getEntity().getId(), score);
        for(int i=1;i<equalScoreList.size();i++){
            score = score-dif;
            if(ENTITY_RANK_COMPARATOR.compare(equalScoreList.get(i-1), 
                equalScoreList.get(i)) != 0){
                equalScoreList.get(i).setScore(score);
                log.debug("    - set score of {} at {}", equalScoreList.get(i).getEntity().getId(), score);
            } else {
                double lastScore = equalScoreList.get(i-1).getScore();
                equalScoreList.get(i).setScore(lastScore);
                log.debug("    - set score of {} at {}", equalScoreList.get(i).getEntity().getId(), lastScore);
            }
        }
    }
    /**
     * After {@link #process()}ing this returns the entities linked for the
     * parsed {@link AnalysedContent}.
     * @return the linked entities
     */
    public final Map<String,LinkedEntity> getLinkedEntities() {
        return linkedEntities;
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
    private Set<IRI> getLinkedEntityTypes(Collection<Suggestion> suggestions){
        Collection<IRI> conceptTypes = new HashSet<IRI>();
        double score = -1; //only consider types of the best ranked Entities
        for(Suggestion suggestion : suggestions){
            double actScore = suggestion.getScore();
            if(actScore < score){
                break;
            }
            for(Iterator<IRI> types = 
                suggestion.getEntity().getReferences(linkerConfig.getTypeField()); 
                types.hasNext();conceptTypes.add(types.next()));
        }
        Map<IRI,IRI> typeMappings = linkerConfig.getTypeMappings();
        Set<IRI> dcTypes = new HashSet<IRI>();
        for(IRI conceptType : conceptTypes){
            IRI dcType = typeMappings.get(conceptType);
            if(dcType != null){
                dcTypes.add(dcType);
            }
        }
        if(dcTypes.isEmpty() && linkerConfig.getDefaultDcType() != null){
            dcTypes.add(linkerConfig.getDefaultDcType());
        }
        return dcTypes;
    }
    /**
     * Processes {@link EntitySearcher#getRedirectField() redirect field} values for
     * the parsed suggestions based on the {@link RedirectProcessingMode}
     * as configured in the {@link #config}.<p>
     * The results of this method are stored within the parsed {@link Suggestion}s
     * @param suggestion The suggestion to process.
     * @throws EntitySearcherException 
     */
    private void processRedirects(Suggestion suggestion) throws EntitySearcherException {
        //if mode is IGNORE -> nothing to do
        if(linkerConfig.getRedirectProcessingMode() == RedirectProcessingMode.IGNORE){
            return;
        }
        //in case results for queries are locally cached it might be the case
        //that some/all of the results do already redirects processed.
        //therefore there is a small internal state that stores this information
        if(suggestion.isRedirectedProcessed()){
            return; //Redirects for ResultMatch are already processed ... ignore
        }
        Entity result = suggestion.getResult();
        Iterator<IRI> redirects = result.getReferences(linkerConfig.getRedirectField());
        switch (linkerConfig.getRedirectProcessingMode()) {
            case ADD_VALUES:
                Graph entityData = result.getData();
                IRI entityUri = result.getUri();
                while(redirects.hasNext()){
                    IRI redirect = redirects.next();
                    if(redirect != null){
                        Entity redirectedEntity = entitySearcher.get(redirect,
                            linkerConfig.getSelectedFields());
                        if(redirectedEntity != null){
                            for(Iterator<Triple> data = redirectedEntity.getData().filter(
                                redirectedEntity.getUri(), null, null);data.hasNext();){
                                Triple t = data.next();
                                entityData.add(new TripleImpl(entityUri,t.getPredicate(),t.getObject()));
                            }
                        }
                        //set that the redirects where searched for this result
                        suggestion.setRedirectProcessed(true);
                    }
                }
            case FOLLOW:
                while(redirects.hasNext()){
                    IRI redirect = redirects.next();
                    if(redirect != null){
                        Entity redirectedEntity = entitySearcher.get(redirect,
                            linkerConfig.getSelectedFields());
                        if(redirectedEntity != null){
                            suggestion.setRedirect(redirectedEntity);
                        }
                    }
                }
            default: //nothing to do
        }
    }
    /**
     * Searches for Entities in the {@link #entitySearcher} corresponding to the
     * {@link Token#getText() words} of the current {@link #state position} in
     * the text.
     * @param searchTokens the list of {@link Token#getText() words} to search
     * entities for.
     * @return The sorted list with the suggestions.
     * If there are no suggestions an empty list will be returned.
     * @throws EntitySearcherException 
     */
    private List<Suggestion> lookupEntities(List<TokenData> searchTokens) throws EntitySearcherException {
        Set<String> languages = new HashSet<String>();
        languages.add(linkerConfig.getDefaultLanguage());
        languages.add(state.getLanguage());
        int countryCodeIndex = state.getLanguage() == null ? -1 : state.getLanguage().indexOf('-');
        if(countryCodeIndex >= 2){
            languages.add(state.getLanguage().substring(0,countryCodeIndex));
        }
        List<String> searchStrings = new ArrayList<String>(searchTokens.size());
        for(Iterator<TokenData> it = searchTokens.iterator();it.hasNext();){
            searchStrings.add(getSearchString(it.next()));
        }
        String[] languageArray = languages.toArray(new String[languages.size()]);
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        //check if we have the search strings in the cache
        List<Entity> results = lookupCache.get(searchStrings);
        if(results != null){ //query is cached
            cacheHits++;
            //match the cached results
            for(Entity result : results){
                processLookupResult(searchTokens, result, suggestions);
            }
        } else { // we need to perform a new query
            results = new ArrayList<Entity>();
            //perform the lookup with the parsed parameter
            int numResults = performLookup(searchStrings, languageArray, suggestions, searchTokens, results);
            //cache the results
            lookupCache.put(searchStrings, results);
            //if no match where found in the result .. fallback to a search for the
            //current token
            if(suggestions.isEmpty() && numResults > 0 && searchStrings.size() > 1){
                //there where results, but no one matched ...
                //   ... it is most likely a case where the used search terms are
                //       not releated. So try to query for the active token only
                log.debug("   > No match for '{}' searchStrings ... ", searchStrings);
                searchStrings = Collections.singletonList(getSearchString(state.getToken()));
                searchTokens = Collections.singletonList(state.getToken());
                results = lookupCache.get(searchStrings);
                if(results != null){ //query is cached
                    cacheHits++;
                    //match the cached results
                    for(Entity result : results){
                        processLookupResult(searchTokens, result, suggestions);
                    }
                } else {
                    results = new ArrayList<Entity>();
                    log.debug("     ... fallback to search for active token '{}' ...",searchStrings);
                    performLookup(searchStrings, languageArray, suggestions, searchTokens, results);
                    //cache the results of the fall-back query
                    lookupCache.put(searchStrings, results);
                }
            }
        }
        //sort the suggestions
        if(suggestions.size()>1){
            Collections.sort(suggestions,Suggestion.MATCH_TYPE_SUGGESTION_COMPARATOR);
        }
        return suggestions;
    }
    /**
     * @param searchStrings
     * @param languageArray
     * @param suggestions
     * @param searchTokens
     * @param queryResults the unprocessed results of the query for the parsed
     * parameters. This is used to cache results of queries. This avoid issuing
     * the same query twice for a analysed document.
     * string.
     * @return
     * @throws EntitySearcherException
     */
    private int performLookup(List<String> searchStrings, String[] languageArray,
            List<Suggestion> suggestions, List<TokenData> searchTokens, 
            List<Entity> queryResults) throws EntitySearcherException {
        int minProcessedResults = linkerConfig.getMaxSuggestions()*3;
        int lookupLimit = Math.max(MIN_SEARCH_LIMIT, linkerConfig.getMaxSuggestions()*2*searchTokens.size());
        int maxResults = lookupLimit*2;
        int offset = 0;
        int numFiltered = 0;
        boolean moreResultsAvailable = true;
        int numResults = 0;
        //search for entities until
        // (1) we have more as MAX_SUGGESTION results
        // (2) no more results are available
        // (3) the number of processed Entities is smaller as two times the
        //     suggestions
        // (4) the number of requested Entities is smaller as two times the
        //     lookup limit.
        //NOTE: making multiple requests can decrease the performance a lot.
        //      Because of that those limits assure that no more than two
        //      requests are made for the same lookup.
        while(suggestions.size() < linkerConfig.getMaxSuggestions() &&
                moreResultsAvailable && (numResults-numFiltered) < (minProcessedResults) &&
                numResults < maxResults){
            Collection<? extends Entity> results;
            log.debug("   > request entities [{}-{}] entities ...",offset,(offset+lookupLimit));
            lookupStats.begin(); //keep statistics
            results = entitySearcher.lookup(linkerConfig.getNameField(),
                linkerConfig.getSelectedFields(), searchStrings, languageArray,
                lookupLimit, offset);
            lookupStats.complete();
            log.debug("      < found {} entities ...",results.size());
            //queries might return more as the requested results
            moreResultsAvailable = results.size() >= lookupLimit;
            numResults = numResults + results.size();
            offset = numResults;
            matchingStats.begin();
            for(Entity result : results){ 
                if(log.isDebugEnabled()){
                    log.debug("    > {} (ranking: {})",result.getId(),result.getEntityRanking());
                }
                numQueryResults++;
                //white/black list based entity type filtering (STANBOL-1111)
                if(!linkerConfig.isEntityTypeFilteringActive() || 
                        !filterEntity(result.getReferences(linkerConfig.getTypeField()))){
                    //a valid query result
                    queryResults.add(result);
                    //now match the result against the current position in the text
                    processLookupResult(searchTokens, result, suggestions);
                } else { //do not process Entities with a filtered type
                    numFilteredResults++; //global statistics
                    numFiltered++;
                }
            }
            matchingStats.complete();
            //sort the suggestions
        }
        return numResults;
    }
    /**
     * Processes the parsed entity lookup results and adds suggestions to the
     * parsed suggestion list
     * @param result the result to process
     * @param suggestions the suggestions
     * @return the number of filtered results
     */
    private void processLookupResult(List<TokenData> searchTokens, Entity result, List<Suggestion> suggestions) {
        Suggestion suggestion = matchLabels(searchTokens, result);
        if(suggestion.getMatch() != MATCH.NONE){
            if(log.isDebugEnabled()){
                log.debug("      + {}",suggestion);
            }
            suggestions.add(suggestion);
        } else {
            log.debug("      - no match");
        }
    }
    
    public boolean filterEntity(Iterator<IRI> entityTypes){
        Map<IRI, Integer> whiteList = linkerConfig.getWhitelistedTypes();
        Map<IRI, Integer> blackList = linkerConfig.getBlacklistedTypes();
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
            return !linkerConfig.isDefaultWhitelistTypes();
        } else if(w != null){
            return b == null || w.compareTo(b) < 0 ? false : true;
        } else { //w == null && b != null
            return true; //filter
        }
    }
    
    /**
     * Matches the labels of the parsed {@link Representation} with the Tokens of
     * the texts (beginning with the currently active 
     * {@link ProcessingState#getToken() token}).<p>
     * The field used to get the labels is retrieved from 
     * {@link EntitySearcher#getNameField()}. Only labels with no language or the
     * language of the current sentence are considered. If less than 
     * {@link EntityLinkerConfig#getMinFoundTokens()} tokens match with an
     * label the Concept is only considered to match if the label is
     * {@link String#equalsIgnoreCase(String)} to the text covered by the
     * matched token(s). Otherwise also {@link MATCH#FULL} and {@link MATCH#PARTIAL}
     * results are allowed.
     * @param entity The entity including at least the data for the
     * {@link EntitySearcher#getNameField()} property.
     * @return The result of the matching.
     */
    private Suggestion matchLabels(List<TokenData> searchTokens, Entity entity) {
        String curLang = documentLang; //language of the current sentence
        String defLang = defaultLang; //configured default language 
        String mainLang = documentMainLang;
        Collection<Literal> mainLangLabels;
        if(documentMainLang != null){
            mainLang = documentMainLang;
            mainLangLabels = new ArrayList<Literal>();
        } else {
            mainLang = documentLang;
            mainLangLabels = Collections.emptyList();
        }
        Iterator<Literal> labels = entity.getText(linkerConfig.getNameField());
        Suggestion match = new Suggestion(entity);
        Collection<Literal> defaultLabels = new ArrayList<Literal>();
        boolean matchedLangLabel = false;
        //avoid matching multiple labels with the exact same lexical.
        Set<String> matchedLabels = new HashSet<String>();
        while(labels.hasNext()){
            Literal label = labels.next();
            //numLabels++;
            String lang = label.getLanguage() != null ? label.getLanguage().toString() : null;
            String text = label.getLexicalForm();
            //if case-insensitive matching ... compare lower case versions
            if(!linkerConfig.isCaseSensitiveMatching()){
                text = text.toLowerCase(Locale.ROOT);
            }
            if((lang == null && curLang == null) ||
                    (lang != null && curLang != null && lang.equalsIgnoreCase(curLang))){
                if(!matchedLabels.contains(text)){
                    matchLabel(searchTokens, match, label);
                    matchedLabels.add(text);
                    matchedLangLabel = true;
                } else if(!matchedLangLabel){
                    matchedLangLabel = true; //found a equivalent label in the matchlang
                }
            } else if((lang == null && mainLang == null) ||
                    (lang != null && mainLang != null && lang.equalsIgnoreCase(mainLang))){
                mainLangLabels.add(label);
            } else if((lang == null && defLang == null) ||
                    (lang != null && defLang != null && lang.startsWith(defLang))){
                defaultLabels.add(label);
            }
        }
        //try to match main language labels
        if(!matchedLangLabel || match.getMatch() == MATCH.NONE){
            for(Literal mainLangLabel : mainLangLabels){
                if(!matchedLabels.contains(mainLangLabel.getLexicalForm())){
                    matchLabel(searchTokens, match, mainLangLabel);
                    matchedLabels.add(mainLangLabel.getLexicalForm());
                    matchedLangLabel = true;
                }
            }
        }
        //use only labels in the default language if there is
        // * no label in the current language or
        // * no MATCH was found in the current language
        if(!matchedLangLabel || match.getMatch() == MATCH.NONE){
            for(Literal defaultLangLabel : defaultLabels){
                if(!matchedLabels.contains(defaultLangLabel.getLexicalForm())){
                    matchLabel(searchTokens, match, defaultLangLabel);
                    matchedLabels.add(defaultLangLabel.getLexicalForm());
                }
            }
        }
        return match;
    }
    
    /**
     * @param suggestion
     * @param label
     */
    private void matchLabel(List<TokenData> searchTokens, Suggestion suggestion, Literal label) {
//        test.begin();
        String text = label.getLexicalForm();
        String lang = label.getLanguage() == null ? null : label.getLanguage().toString();
        if(!linkerConfig.isCaseSensitiveMatching()){
            text = text.toLowerCase(); //TODO use language of label for Locale
        }
        //Tokenize the label and remove remove tokens without alpha numerical chars
        String[] unprocessedLabelTokens = labelTokenizer != null ? 
                labelTokenizer.tokenize(text, lang) : null; 
        if(unprocessedLabelTokens == null){ //no tokenizer available
            log.info("Unable to tokenize {} language texts. Will process untokenized label {}",
                state.getLanguage(),text);
            unprocessedLabelTokens = new String[]{text}; //there is already a warning
        }
        int offset = 0;
        for(int i=0;i<unprocessedLabelTokens.length;i++){
            boolean hasAlphaNumericChar = Utils.hasAlphaNumericChar(unprocessedLabelTokens[i]);
            if(!hasAlphaNumericChar){
                offset++;
            } else if(offset > 0){
                String token = unprocessedLabelTokens[i];
                token = StringUtils.replaceChars(token,".","");
                unprocessedLabelTokens[i-offset] = token;
            }
        }
        String[] labelTokens;
        if(offset == 0){
            labelTokens = unprocessedLabelTokens;
        } else {
            labelTokens = new String[unprocessedLabelTokens.length-offset];
            System.arraycopy(unprocessedLabelTokens, 0, labelTokens, 0, labelTokens.length);
        }
        //holds the tokens and their position within the label. NOTE that the same
        //token may appear multiple times in the label (e.g. "Da Da Bing"
        Map<String,List<Integer>> labelTokenMap = new HashMap<String, List<Integer>>();
        for(int i=0;i < labelTokens.length; i++){
            List<Integer> tokenIndexes = labelTokenMap.get(labelTokens[i]);
            if(tokenIndexes == null){
                tokenIndexes = new ArrayList<Integer>(2);
                labelTokenMap.put(labelTokens[i], tokenIndexes);
            }
            tokenIndexes.add(Integer.valueOf(i));
        }
        NavigableMap<Integer, String> matchedLabelTokens = new TreeMap<Integer,String>();
        int foundProcessableTokens = 0;
        int foundTokens = 0;
        float foundTokenMatch = 0;
        //ensure the correct order of the tokens in the suggested entity
        boolean search = true;
        boolean activeTokenNotMatched = true;
        int firstFoundIndex = -1;
        int firstProcessableFoundIndex = -1;
        int lastFoundIndex = -1;
        int lastProcessableFoundIndex = -1;
        int firstFoundLabelIndex = -1;
        int lastfoundLabelIndex = -1;
        TokenData currentToken;
        String currentTokenText;
        int currentTokenLength;
        int notFound = 0;
        
        int matchedTokensNotWithinProcessableTokenSpan = 0;
        int foundTokensWithinCoveredProcessableTokens = 0;
        float minTokenMatchFactor = linkerConfig.getMinTokenMatchFactor();
        //search for matches within the correct order
        for(int currentIndex = state.getToken().index;
                currentIndex < state.getTokens().size() 
                && search ;currentIndex++){
            currentToken = state.getTokens().get(currentIndex);
            if(currentToken.hasAlphaNumeric){
                currentTokenText = linkerConfig.isLemmaMatching() ? 
                        currentToken.getTokenLemma() : currentToken.getTokenText();
                if(currentTokenText == null) { //no lemma available
                    currentTokenText = currentToken.getTokenText(); //fallback to text
                }
                //ignore '.' in tokens to ensure that 'D.C.' matches 'DC' ...
                currentTokenText = StringUtils.replaceChars(currentTokenText,".","");
                if(!linkerConfig.isCaseSensitiveMatching()){
                    currentTokenText = currentTokenText.toLowerCase();
                }
                currentTokenLength = currentTokenText.length();
                boolean found = false;
                float matchFactor = 0f;
                //iteration starts at the next token after the last matched one
                //so it is OK to skip tokens in the label, but not within the text
                for(int i = lastfoundLabelIndex+1;!found && i < labelTokens.length;i ++){
                    String labelTokenText = labelTokens[i];
                    int labelTokenLength = labelTokenText.length();
                    float maxLength = currentTokenLength > labelTokenLength ? currentTokenLength : labelTokenLength;
                    float lengthDif = Math.abs(currentTokenLength - labelTokenLength);
                    if((lengthDif/maxLength)<=(1-minTokenMatchFactor)){ //this prevents unnecessary string comparison 
                        int matchCount = compareTokens(currentTokenText, labelTokenText);
                        if(matchCount/maxLength >= minTokenMatchFactor){
                            lastfoundLabelIndex = i; //set the last found index to the current position
                            found = true; //set found to true -> stops iteration
                            matchFactor = matchCount/maxLength; //how good is the match
                            //remove matched labels from the set to disable them for
                            //a later random oder search
                            Integer labelTokenIndex = getLabelTokenIndex(labelTokenText, i, labelTokenMap);
                            matchedLabelTokens.put(labelTokenIndex, labelTokenText);
                        }
                    }
                }
                if(!found){
                    //search for a match in the wrong order
                    //currently only exact matches (for testing)
                    Integer index = getLabelTokenIndex(currentTokenText, lastfoundLabelIndex+1, labelTokenMap);
                    if(index != null){
                        matchedLabelTokens.put(index, currentTokenText);
                        found = true;
                        matchFactor = 0.7f;
                    }
                }
                //int found = text.indexOf(currentToken.getText().toLowerCase());
                if(found){ //found
                    if(currentToken.isMatchable){
                        foundProcessableTokens++; //only count processable Tokens
                        if(firstProcessableFoundIndex < 0){
                            firstProcessableFoundIndex = currentIndex;
                        }
                        lastProcessableFoundIndex = currentIndex;
                        foundTokensWithinCoveredProcessableTokens++;
                        if(matchedTokensNotWithinProcessableTokenSpan > 0){
                            foundTokensWithinCoveredProcessableTokens = foundTokensWithinCoveredProcessableTokens +
                                    matchedTokensNotWithinProcessableTokenSpan;
                            matchedTokensNotWithinProcessableTokenSpan = 0;
                        }
                    } else {
                        matchedTokensNotWithinProcessableTokenSpan++;
                    }
                    foundTokens++;
                    foundTokenMatch = foundTokenMatch + matchFactor; //sum up the matches
                    if(firstFoundIndex < 0){
                        firstFoundIndex = currentIndex;
                        firstFoundLabelIndex = lastfoundLabelIndex;
                    }
                    lastFoundIndex = currentIndex;
                } else { //not found
                    if(state.getToken().index == currentToken.index){
                        //the currently active Token MUST BE matched
                        search = false;
                        activeTokenNotMatched = true;
                    }
                    notFound++;
                    //stop forward search if
//                    if(currentToken.isMatchable || notFound > linkerConfig.getMaxNotFound()){
                    if(!searchTokens.contains(currentToken)){
                        //stop as soon as a token that needs to be processed is
                        //not found in the label or the maximum number of tokens
                        //that are not processable are not found
                        search = false; 
                    }
                }
            } // else token without alpha or numeric characters are not processed
        }
        //search backwards for label tokens until firstFoundLabelIndex if there
        //are unconsumed Tokens in the sentence before state.getTokenIndex
        int currentIndex = state.getToken().index-1;
        int labelIndex = firstFoundLabelIndex-1;
        notFound = 0;
        matchedTokensNotWithinProcessableTokenSpan = 0;
        if(activeTokenNotMatched){ //do not search backwards if the active token
            //was not found
            search = true;
        }
        while(search && labelIndex >= 0 && currentIndex >= 0){// && currentIndex > state.getConsumedIndex()){
            String labelTokenText = labelTokens[labelIndex];
            if(labelTokenMap.containsKey(labelTokenText)){ //still not matched
                currentToken = state.getTokens().get(currentIndex);
                currentTokenText = linkerConfig.isLemmaMatching() ? 
                        currentToken.getTokenLemma() : currentToken.getTokenText();
                if(currentTokenText == null) { //no lemma available
                    currentTokenText = currentToken.getTokenText(); //fallback to text
                }
                if(!linkerConfig.isCaseSensitiveMatching()){
                    currentTokenText = currentTokenText.toLowerCase();
                }
                currentTokenText = StringUtils.replaceChars(currentTokenText,".","");
                currentTokenLength = currentTokenText.length();
                boolean found = false;
                float matchFactor = 0f;
                int labelTokenLength = labelTokenText.length();
                float maxLength = currentTokenLength > labelTokenLength ? currentTokenLength : labelTokenLength;
                float lengthDif = Math.abs(currentTokenLength - labelTokenLength);
                if((lengthDif/maxLength)<=(1-minTokenMatchFactor)){ //this prevents unnecessary string comparison 
                    int matchCount = compareTokens(currentTokenText, labelTokenText);
                    if(matchCount/maxLength >= minTokenMatchFactor){
                        found = true; //set found to true -> stops iteration
                        matchFactor = matchCount/maxLength; //how good is the match
                    }
                }
                if(found){ //found
                    if(currentToken.isMatchable){
                        foundProcessableTokens++; //only count processable Tokens
                        if(lastProcessableFoundIndex < 0){ //if last is not yet set
                            lastProcessableFoundIndex = currentIndex;
                        }
                        firstProcessableFoundIndex = currentIndex;
                        foundTokensWithinCoveredProcessableTokens++;
                        if(matchedTokensNotWithinProcessableTokenSpan > 0){
                            foundTokensWithinCoveredProcessableTokens = foundTokensWithinCoveredProcessableTokens +
                                    matchedTokensNotWithinProcessableTokenSpan;
                            matchedTokensNotWithinProcessableTokenSpan = 0;
                        }
                    } else {
                        matchedTokensNotWithinProcessableTokenSpan++;
                    }
                    foundTokens++;
                    foundTokenMatch = foundTokenMatch + matchFactor; //sum up the matches
                    firstFoundIndex = currentIndex;
                    labelIndex--; 
                    Integer foundIndex = getLabelTokenIndex(labelTokenText, currentIndex, labelTokenMap);
                    matchedLabelTokens.put(foundIndex, labelTokenText);
                } else {
                    notFound++;
                    if(currentToken.isMatchable || notFound > linkerConfig.getMaxNotFound()){
                        //stop as soon as a token that needs to be processed is
                        //not found in the label or the maximum number of tokens
                        //that are not processable are not found
                        search = false; 
                    }
                }
                currentIndex --;
            } else { //this token is already matched ...
                labelIndex--; //try the next one
            }
        }
        if(foundProcessableTokens > 0) { //if any Token has matched
            //Now we make a second round to search tokens that match in the wrong order
            //e.g. if given and family name of persons are switched
            final LabelMatch labelMatch;
            int coveredTokens = lastFoundIndex-firstFoundIndex+1;
            int coveredProcessableTokens = lastProcessableFoundIndex-firstProcessableFoundIndex+1;
            //check if we lookup Entities within a processable chunk
            final float chunkMatchScore;
            if(!textProcessingConfig.isIgnoreChunks() &&
                    state.getToken().inChunk != null &&  //there is a chunk
                    state.getToken().inChunk.isProcessable){ //the chunk is processable
                ChunkData cd = state.getToken().inChunk;
                List<TokenData> tokens = state.getTokens();
                if(log.isTraceEnabled()){
                    log.trace("  ... checking match with chunk {}: {}", 
                        cd.chunk, cd.chunk.getSpan());
                }
                int cstart = cd.getMatchableStart() >= 0 ? cd.getMatchableStart() :
                    firstProcessableFoundIndex;
                int cend = cd.getMatchableEndChar();
                //if the match does not cover the whole chunk
                if(cstart < firstProcessableFoundIndex || cend > lastProcessableFoundIndex){ 
                    int foundInChunk = 0;
                    int numInChunk = 0;
                    for(int i = cd.matchableStart; i <= cd.matchableEnd ; i++){
                        TokenData td = tokens.get(i);
                        if(td.isMatchable){
                            numInChunk++;
                            if(i >= firstProcessableFoundIndex &&
                                    i <= lastProcessableFoundIndex){
                                foundInChunk++;
                            }
                        }
                    }
                    chunkMatchScore = (float) foundInChunk / (float) numInChunk;
                    log.trace("  ... label matches {} of {} matchable token in Chunk", 
                        foundInChunk, numInChunk);
                } else { //matches the whole chunk
                    chunkMatchScore = 1f;
                }
            } else { //no chunk (or ignoreChuncks == true) .. set chunkMatchScore to 1f
                chunkMatchScore = 1f;
            }
            //matched tokens only within the span of the first/last processable token
            //Matching rules
            // - if less than config#minTokenFound() than accept only EXACT
            // - override PARTIAL matches with FULL/EXACT matches only if
            //   foundTokens of the PARTIAL match is > than of the FULL/EXACT
            //   match (this will be very rare
            String currentText = state.getTokenText(firstFoundIndex,coveredTokens);
            if(chunkMatchScore == 1f && //the whole chunk matches
                    (linkerConfig.isCaseSensitiveMatching() ? currentText.equals(text) : currentText.equalsIgnoreCase(text))){ 
                labelMatch = new LabelMatch(firstFoundIndex, coveredTokens, label);
            } else if(chunkMatchScore >= linkerConfig.getMinChunkMatchScore()){
                int coveredLabelTokens = matchedLabelTokens.lastKey().intValue() -
                        matchedLabelTokens.firstKey().intValue() + 1;
                if(foundTokens == labelTokens.length && foundTokens == coveredTokens){
                    //if all token matched set found to covered: May be lower because only
                    //processable tokens are counted, but FULL also checks
                    //of non-processable!
                    foundTokens = coveredTokens;
                    foundProcessableTokens = coveredProcessableTokens;
                }
                labelMatch = new LabelMatch(firstProcessableFoundIndex, coveredProcessableTokens, 
                    foundProcessableTokens,foundTokensWithinCoveredProcessableTokens,
                    foundTokenMatch/(float)foundTokens,label,labelTokens.length, coveredLabelTokens);
            } else {
                if(log.isTraceEnabled()){ //trace level logging for STANBOL-1211
                    List<TokenData> tokens = state.getTokens();
                    int start = tokens.get(firstProcessableFoundIndex).token.getStart();
                    int end = tokens.get(lastProcessableFoundIndex).token.getEnd();
                    CharSequence content = state.getToken().token.getContext().getText();
                    CharSequence match = content.subSequence(start, end);
                    ChunkData cd = state.getToken().inChunk;
                    int cStart = tokens.get(cd.matchableStart).token.getStart();
                    int cEnd = tokens.get(cd.matchableEnd).token.getEnd();
                    CharSequence context = content.subSequence(cStart, cEnd);
                    log.trace(" - filter match '{}'@[{},{}] because it does only match "
                            + "{}% (min: {}%) of the matchable Tokens in Chunk '{}'@[{},{}]",
                            new Object[]{match, start, end, Math.round(chunkMatchScore*100),
                                    Math.round(linkerConfig.getMinChunkMatchScore()*100),
                                    context, cStart, cEnd});
                }
                labelMatch = null;
            }
            if(labelMatch != null &&
                    labelMatch.getLabelScore() >= linkerConfig.getMinLabelScore() && 
                    labelMatch.getTextScore() >= linkerConfig.getMinTextScore() && 
                    labelMatch.getMatchScore() >= linkerConfig.getMinMatchScore()){
                log.trace(" + add suggestion {}", labelMatch);
                suggestion.addLabelMatch(labelMatch);
            }
        } //else NO tokens found -> nothing to do
//        test.complete();
    }
    /**
     * Utility Method that searches for the Index of the parsed label token text
     * within the labelTokenMap. Matched tokens are removed from the parsed
     * LabelTokenMap <p>
     * NOTE: This is necessary, because in cases where Labels do contain the same
     * token twice, it might not be always clear which token is the matching one.
     * Especially if the order of the Tokens in the Text does not exactly match
     * the order within the Label. This Method tries always to find the matching
     * token closest to the parsed currentIndex.
     * It iterates backwards to prefer Tokens that occur later as the current index
     * in the tokenized label.
     * @param labelTokenText the text of the current labelToken
     * @param currentIndex the current index of the processing (or if not known
     * the last matched index of an token within the label
     * @param labelTokenMap the Map holding tokens as key and a list of occurrences
     * as values or <code>null</code> if no Token with the parsed labelTokenText
     * was present as key in the parsed labelTokenMap
     * @return the index of the selected label token
     */
    private Integer getLabelTokenIndex(String labelTokenText, int currentIndex,
            Map<String,List<Integer>> labelTokenMap) {
        List<Integer> tokenIndexes = labelTokenMap.get(labelTokenText);
        if(tokenIndexes == null){
            return null;
        }
        //try to remove the closest index in the map 
        Integer labelTokenIndex = Integer.valueOf(currentIndex);
        //search the closest position
        int closest = Integer.MAX_VALUE;
        int closestIndex = -1;
        for(int p = tokenIndexes.size()-1; p >= 0; p--){
            Integer index = tokenIndexes.get(p);
            int dif = Math.abs(index.intValue()-currentIndex);
            if(Math.abs(index.intValue()-currentIndex) < closest){
                closest = dif;
                closestIndex = p;
                labelTokenIndex = index;
                if(closest == 0){
                    break;
                }
            }
        }
        tokenIndexes.remove(closestIndex);
        if(tokenIndexes.isEmpty()){
            labelTokenMap.remove(labelTokenText);
        }
        return labelTokenIndex;
    }
    /**
     * Compares to token with each other and returns the longest match. The 
     * tokens are compared from the beginning and from the end.
     * @param token1 the first token
     * @param token2 the second token
     * @return the number of matching chars
     */
    private int compareTokens(String token1,String token2){
        int l1 = token1.length(); //length of the first token
        int l2 = token2.length(); //length of the second token
        //in case of same length check for equals first
        if(l1 == l2 && token1.equals(token2)){ 
            return l1;
        }
        int ml = l1>l2?l2:l1; //minimum length of a token
        if(ml == 0){
            return ml;
        }
        int f = 0; //forward match count + 1
        int b = 0; //backward match count + 1
        boolean match = true; //still matches
        while(match && f < ml){
            match = token1.charAt(f) == token2.charAt(f);
            f++;
        }
        if(!match){
            f--;
        }
        if(f < ml){
            match = true;
            while(match && b < ml){
                b++;
                match = token1.charAt(l1-b) == token2.charAt(l2-b);
            }
            if(!match){
                b--;
            }
        }
        return f > b ? f : b;
    }
    /**
     * This logs the statistics about the processing process
     * @param log the logger used to log the statistics
     */
    public void logStatistics(Logger log){
        log.info("EntityLinking Statistics:");
        double textProcessingDuration = textProcessingStats.getDuration();
        double lookupDuration = lookupStats.getDuration();
        double matchingDuration = matchingStats.getDuration();
        double rankingDuration = rankingStats.getDuration();
        double other = processingTime-textProcessingDuration-lookupDuration-matchingDuration;
        log.info("    - overal: {}ms (text processing: {}%, lookup: {}%, matching {}%, ranking {}%, other {}%)", new Object[]{
                processingTime, 
                Math.round(textProcessingDuration*100/(double)processingTime),
                Math.round(lookupDuration*100/(double)processingTime),
                Math.round(matchingDuration*100/(double)processingTime),
                 Math.round(rankingDuration*100/(double)processingTime),
                Math.round(other*100/(double)processingTime),
        });
        textProcessingStats.printStatistics(log);
        lookupStats.printStatistics(log);
        float cacheHitPercentage = lookupStats.count > 0 ? //avoid division by zero
                cacheHits*100f/(float)lookupStats.count : Float.NaN;
        log.info("    - cache hits: {} ({}%)",cacheHits,cacheHitPercentage);
        log.info("      - {} query results ({} filtered - {}%)",
            new Object[]{numQueryResults,numFilteredResults, 
                numFilteredResults*100f/(float)numQueryResults});
        matchingStats.printStatistics(log);
        rankingStats.printStatistics(log);
//        test.printStatistics(log);
//        test2.printStatistics(log);
    }

}
