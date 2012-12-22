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

import static org.apache.stanbol.enhancer.nlp.NlpAnnotations.POS_ANNOTATION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.PlainLiteral;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.TripleImpl;
import org.apache.stanbol.enhancer.engines.entitylinking.Entity;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcher;
import org.apache.stanbol.enhancer.engines.entitylinking.EntitySearcherException;
import org.apache.stanbol.enhancer.engines.entitylinking.LabelTokenizer;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.config.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.entitylinking.config.LanguageProcessingConfig;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.ProcessingState.TokenData;
import org.apache.stanbol.enhancer.engines.entitylinking.impl.Suggestion.MATCH;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.servicesapi.rdf.NamespaceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityLinker {
    
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
    
    private Integer lookupLimit;
    
    private LabelTokenizer labelTokenizer;
    

    public EntityLinker(AnalysedText analysedText, String language,
                        LanguageProcessingConfig textProcessingConfig,
                        EntitySearcher entitySearcher,
                        EntityLinkerConfig linkerConfig,
                        LabelTokenizer labelTokenizer) {
        //this.analysedText = analysedText;
        this.entitySearcher = entitySearcher;
        this.linkerConfig = linkerConfig;
        this.textProcessingConfig = textProcessingConfig;
        this.labelTokenizer = labelTokenizer;
        this.state = new ProcessingState(analysedText,language,textProcessingConfig,linkerConfig);
        this.lookupLimit  = Math.max(10,linkerConfig.getMaxSuggestions()*2);
    }
    /**
     * Steps over the sentences, chunks, tokens of the {@link #sentences}
     */
    public void process() throws EntitySearcherException {
        //int debugedIndex = 0;
        while(state.next()) {
            TokenData token = state.getToken();
            if(log.isDebugEnabled()){
                log.debug("--- preocess Token {}: {} (lemma: {} | pos:{}) chunk: {}",
                    new Object[]{token.index,token.token.getSpan(),
                                 token.morpho != null ? token.morpho.getLemma() : "none", 
                                 token.token.getAnnotations(POS_ANNOTATION),
                                 token.inChunk != null ? 
                                         (token.inChunk.chunk + " "+ token.inChunk.chunk.getSpan()) : 
                                             "none"});
            }
            List<String> searchStrings = new ArrayList<String>(linkerConfig.getMaxSearchTokens());
            searchStrings.add(token.getTokenText());
            //Determine the range we are allowed to search for tokens
            final int minIncludeIndex;
            final int maxIndcludeIndex;
            //NOTE: testing has shown that using Chunks to restrict search for
            //      additional matchable tokens does have an negative impact on
            //      recall. Because of that this restriction is for now deactivated
            boolean restrirctContextByChunks = false; //TODO: maybe make configurable
            if(token.inChunk != null && !textProcessingConfig.isIgnoreChunks() &&
                    restrirctContextByChunks){
                minIncludeIndex = Math.max(
                    state.getConsumedIndex()+1, 
                    token.inChunk.startToken);
                maxIndcludeIndex = token.inChunk.endToken;
            } else {
                maxIndcludeIndex = state.getTokens().size() - 1;
                minIncludeIndex = state.getConsumedIndex() + 1;
            }
            int prevIndex,pastIndex; //search away from the currently active token
            int distance = 0;
            do {
                distance++;
                prevIndex = token.index-distance;
                pastIndex = token.index+distance;
                if(minIncludeIndex <= prevIndex){
                    TokenData prevToken = state.getTokens().get(prevIndex);
                    if(log.isDebugEnabled()){
                        log.debug("    {} {}:'{}' (lemma: {} | pos:{})",new Object[]{
                            prevToken.isMatchable? '+':'-',prevToken.index,
                            prevToken.token.getSpan(),
                            prevToken.morpho != null ? prevToken.morpho.getLemma() : "none",
                            prevToken.token.getAnnotations(POS_ANNOTATION)
                        });
                    }
                    if(prevToken.isMatchable){
                        searchStrings.add(0,prevToken.getTokenText());
                    }
                }
                if(maxIndcludeIndex >= pastIndex){
                    TokenData pastToken = state.getTokens().get(pastIndex);
                    if(log.isDebugEnabled()){
                        log.debug("    {} {}:'{}' (lemma: {} | pos:{})",new Object[]{
                            pastToken.isMatchable? '+':'-',pastToken.index,
                            pastToken.token.getSpan(),
                            pastToken.morpho != null ? pastToken.morpho.getLemma() : "none",
                            pastToken.token.getAnnotations(POS_ANNOTATION)
                        });
                    }
                    if(pastToken.isMatchable){
                        searchStrings.add(pastToken.getTokenText());
                    }
                }
            } while(searchStrings.size() < linkerConfig.getMaxSearchTokens() && distance <
                    linkerConfig.getMaxSearchDistance() &&
                    (prevIndex > minIncludeIndex || pastIndex < maxIndcludeIndex));
            //we might have an additional element in the list
            if(searchStrings.size() > linkerConfig.getMaxSearchTokens()){
                searchStrings = searchStrings.subList( //the last part of the list
                    searchStrings.size()-linkerConfig.getMaxSearchTokens(), 
                    searchStrings.size());
            }
            log.debug("  >> searchStrings {}",searchStrings);
            //search for Entities
            List<Suggestion> suggestions = lookupEntities(searchStrings);
            if(!suggestions.isEmpty()){
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
                    }
                    //Filter matches with less than config.getMinFoundTokens()
                    //if matchcount is less than of the best match
                    if(matchCount < bestMatchCount &&
                            matchCount < linkerConfig.getMinFoundTokens()){
                        it.remove();
                    } else { //calculate the score
                        //how good is the current match in relation to the best one
                        double spanScore = matchCount/bestMatchCount;
                        suggestion.setScore(spanScore*spanScore*suggestion.getLabelMatch().getMatchScore());
                    }
                }
                Suggestion oldBestRanked = suggestions.get(0); //for debugging
                //resort by score
                Collections.sort(suggestions, Suggestion.SCORE_COMPARATOR);
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
                //remove all suggestions > config.maxSuggestions
                if(suggestions.size() > linkerConfig.getMaxSuggestions()){
                    suggestions.subList(linkerConfig.getMaxSuggestions(),suggestions.size()).clear();
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
                }
                int start = suggestions.get(0).getLabelMatch().getStart();
                int span = suggestions.get(0).getLabelMatch().getSpan();
                //Store the linking results
                String selectedText = state.getTokenText(start,span);
                //float score;
                LinkedEntity linkedEntity = linkedEntities.get(selectedText);
                if(linkedEntity == null){
                    linkedEntity = new LinkedEntity(selectedText,
                        suggestions, getLinkedEntityTypes(suggestions.subList(0, 1)));
                    linkedEntities.put(selectedText, linkedEntity);
                }
                linkedEntity.addOccurrence(state.getSentence(), 
                    //NOTE: The end Token is "start+span-1"
                    state.getTokens().get(start).token, state.getTokens().get(start+span-1).token);
                //set the next token to process to the next word after the
                //currently found suggestion
                state.setConsumed(start+span-1);
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
    private Set<UriRef> getLinkedEntityTypes(Collection<Suggestion> suggestions){
        Collection<UriRef> conceptTypes = new HashSet<UriRef>();
        for(Suggestion suggestion : suggestions){
            for(Iterator<UriRef> types = 
                suggestion.getEntity().getReferences(linkerConfig.getTypeField()); 
                types.hasNext();conceptTypes.add(types.next()));
        }
        Map<UriRef,UriRef> typeMappings = linkerConfig.getTypeMappings();
        Set<UriRef> dcTypes = new HashSet<UriRef>();
        for(UriRef conceptType : conceptTypes){
            UriRef dcType = typeMappings.get(conceptType);
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
        Iterator<UriRef> redirects = result.getReferences(linkerConfig.getRedirectField());
        switch (linkerConfig.getRedirectProcessingMode()) {
            case ADD_VALUES:
                MGraph entityData = result.getData();
                UriRef entityUri = result.getUri();
                while(redirects.hasNext()){
                    UriRef redirect = redirects.next();
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
                    UriRef redirect = redirects.next();
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
     * @param searchStrings the list of {@link Token#getText() words} to search
     * entities for.
     * @return The sorted list with the suggestions.
     * If there are no suggestions an empty list will be returned.
     * @throws EntitySearcherException 
     */
    private List<Suggestion> lookupEntities(List<String> searchStrings) throws EntitySearcherException {
        Set<String> languages = new HashSet<String>();
        languages.add(linkerConfig.getDefaultLanguage());
        languages.add(state.getLanguage());
        int countryCodeIndex = state.getLanguage() == null ? -1 : state.getLanguage().indexOf('-');
        if(countryCodeIndex >= 2){
            languages.add(state.getLanguage().substring(0,countryCodeIndex));
        }
        Collection<? extends Entity> results;
        results = entitySearcher.lookup(linkerConfig.getNameField(),
            linkerConfig.getSelectedFields(),
            searchStrings, 
            languages.toArray(new String[languages.size()]),
            lookupLimit);
        log.debug("   - found {} entities ...",results.size());
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        for(Entity result : results){ 
            log.debug("    > {}",result.getId());
            Suggestion suggestion = matchLabels(result);
            log.debug("      < {}",suggestion);
            if(suggestion.getMatch() != MATCH.NONE){
                suggestions.add(suggestion);
            }                    
        }
        //sort the suggestions
        if(suggestions.size()>1){
            Collections.sort(suggestions,Suggestion.MATCH_TYPE_SUGGESTION_COMPARATOR);
        }
        //TODO: Work in Progress feature ... allowing to refine search if no
        //      suggestion is found but results where present
        //      However this would need full limit/offset support for the
        //      EntitySearcher. (rwesten 2012-05-21)
//        Integer maxResults = entitySearcher.getLimit();
//        if(maxResults == null){
//            maxResults = 1; //fall back to 1 if limit is not known
//        }
//        if(suggestions.isEmpty() && //if no suggestions where found
//                results.size() >= maxResults && //but the query had max results
//                //than the actual entity might not be within the first LIMIT results
//                searchStrings.size() > 1){ //if multiple words where used for the search
//            //try again with only a single word
//            suggestions = lookupEntities(Collections.singletonList(searchStrings.get(0)));
//            
//        }
        //remove all elements > config.getMaxSuggestions()
        return suggestions;
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
    private Suggestion matchLabels(Entity entity) {
        String curLang = state.getLanguage(); //language of the current sentence
        String defLang = linkerConfig.getDefaultLanguage(); //configured default language 
        String mainLang;
        int countryCodeIndex = state.getLanguage() == null ? -1 : state.getLanguage().indexOf('-');
        Collection<PlainLiteral> mainLangLabels;
        if(countryCodeIndex >= 2){
            mainLang = state.getLanguage().substring(0,countryCodeIndex);
            mainLangLabels = new ArrayList<PlainLiteral>();
        } else {
            mainLang = curLang;
            mainLangLabels = Collections.emptyList();
        }
        Iterator<PlainLiteral> labels = entity.getText(linkerConfig.getNameField());
        Suggestion match = new Suggestion(entity);
        Collection<PlainLiteral> defaultLabels = new ArrayList<PlainLiteral>();
        boolean matchedLangLabel = false;
        while(labels.hasNext()){
            PlainLiteral label = labels.next();
            String lang = label.getLanguage() != null ? label.getLanguage().toString() : null;
            if((lang == null && curLang == null) ||
                    (lang != null && curLang != null && lang.equalsIgnoreCase(curLang))){
                matchLabel(match, label);
                matchedLangLabel = true;
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
            for(PlainLiteral mainLangLabel : mainLangLabels){
                matchLabel(match, mainLangLabel);
                matchedLangLabel = true;
            }
        }
        //use only labels in the default language if there is
        // * no label in the current language or
        // * no MATCH was found in the current language
        if(!matchedLangLabel || match.getMatch() == MATCH.NONE){
            for(PlainLiteral defaultLangLabel : defaultLabels){
                matchLabel(match, defaultLangLabel);
            }
        }
        return match;
    }
    
    /**
     * @param suggestion
     * @param label
     */
    private void matchLabel(Suggestion suggestion, PlainLiteral label) {
        String text = label.getLexicalForm();
        String lang = label.getLanguage() == null ? null : label.getLanguage().toString();
        if(!linkerConfig.isCaseSensitiveMatching()){
            text = text.toLowerCase(); //TODO use language of label for Locale
        }
        //Tokenize the label and remove remove tokens without alpha numerical chars
        String[] unprocessedLabelTokens = labelTokenizer.tokenize(text, lang); 
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
                unprocessedLabelTokens[i-offset] = unprocessedLabelTokens[i];
            }
        }
        String[] labelTokens;
        if(offset == 0){
            labelTokens = unprocessedLabelTokens;
        } else {
            labelTokens = new String[unprocessedLabelTokens.length-offset];
            System.arraycopy(unprocessedLabelTokens, 0, labelTokens, 0, labelTokens.length);
        }
        Set<String> labelTokenSet = new HashSet<String>(
                Arrays.asList(labelTokens));
        int foundProcessableTokens = 0;
        int foundTokens = 0;
        float foundTokenMatch = 0;
        //ensure the correct order of the tokens in the suggested entity
        boolean search = true;
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
                currentTokenText = currentToken.getTokenText();
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
                            labelTokenSet.remove(labelTokenText);
                        }
                    }
                }
                if(!found){
                    //search for a match in the wrong order
                    //currently only exact matches (for testing)
                    if(found = labelTokenSet.remove(currentTokenText)){
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
                    notFound++;
                    if(currentToken.isMatchable || notFound > linkerConfig.getMaxNotFound()){
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
        search = true;
        while(search && labelIndex >= 0 && currentIndex > state.getConsumedIndex()){
            String labelTokenText = labelTokens[labelIndex];
            if(labelTokenSet.contains(labelTokenText)){ //still not matched
                currentToken = state.getTokens().get(currentIndex);
                currentTokenText = currentToken.getTokenText();
                if(!linkerConfig.isCaseSensitiveMatching()){
                    currentTokenText = currentTokenText.toLowerCase();
                }
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
                    labelTokenSet.remove(labelTokenText);
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
            //matched tokens only within the span of the first/last processable token
            //Matching rules
            // - if less than config#minTokenFound() than accept only EXACT
            // - override PARTIAL matches with FULL/EXACT matches only if
            //   foundTokens of the PARTIAL match is > than of the FULL/EXACT
            //   match (this will be very rare
            String currentText = state.getTokenText(firstFoundIndex,coveredTokens);
            if(linkerConfig.isCaseSensitiveMatching() ? currentText.equals(text) : currentText.equalsIgnoreCase(text)){ 
                labelMatch = new LabelMatch(firstFoundIndex, coveredTokens, label);
            } else {
                if(foundTokens == labelTokens.length && foundTokens == coveredTokens){
                    //if all token matched set found to covered: May be lower because only
                    //processable tokens are counted, but FULL also checks
                    //of non-processable!
                    foundTokens = coveredTokens;
                    foundProcessableTokens = coveredProcessableTokens;
                }
                labelMatch = new LabelMatch(firstProcessableFoundIndex, coveredProcessableTokens, 
                    foundProcessableTokens,foundTokensWithinCoveredProcessableTokens,
                    foundTokenMatch/foundTokens,label,labelTokens.length);
            }
            if(labelMatch.getLabelScore() >= linkerConfig.getMinLabelScore() && 
                    labelMatch.getTextScore() >= linkerConfig.getMinTextScore() && 
                    labelMatch.getMatchScore() >= linkerConfig.getMinMatchScore()){
                suggestion.addLabelMatch(labelMatch);
            }
        } //else NO tokens found -> nothing to do
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

}
