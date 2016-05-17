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
package org.apache.stanbol.enhancer.engines.keywordextraction.impl;

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

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Token;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.Suggestion.MATCH;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.AnalysedContent;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntitySearcher;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityLinker {
    
    private final Logger log = LoggerFactory.getLogger(EntityLinker.class);

    private final EntityLinkerConfig config;
    private final AnalysedContent content;
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
    /**
     * After {@link #process()}ing this returns the entities linked for the
     * parsed {@link AnalysedContent}.
     * @return the linked entities
     */
    public final Map<String,LinkedEntity> getLinkedEntities() {
        return linkedEntities;
    }
    public EntityLinker(AnalysedContent content,EntitySearcher taxonomy,EntityLinkerConfig config){
        if(config == null){
            throw new IllegalArgumentException("The parsed TaxonomyLinkerConfig MUST NOT be NULL!");
        }
        if(taxonomy == null){
            throw new IllegalArgumentException("The parsed Taxonomy MUST NOT be NULL!");
        }
        if(content == null){
            throw new IllegalArgumentException("The parsed AnalysedContent MUST NOT be NULL!");
        }
        this.content = content;
        this.entitySearcher = taxonomy;
        this.config = config;
        this.state = new ProcessingState(content.getAnalysedText());
        this.lookupLimit  = Math.max(10,config.getMaxSuggestions()*2);
    }
    /**
     * Steps over the sentences, chunks, tokens of the {@link #sentences}
     */
    public void process() throws EngineException {
        int debugedIndex = 0;
        while(state.next()) {
            if(log.isDebugEnabled() && (state.getTokenIndex() > debugedIndex || state.getTokenIndex() ==  0)){
                debugedIndex = state.getTokenIndex();
                Token token = state.getToken();
                log.debug(" {} {} (pos:{}|prop:{})",new Object[]{
                    isProcessableToken(token)? '+':'-',
                    token.getText(),token.getPosTags(),token.getPosProbabilities()
                });
            }
            if(isProcessableToken(state.getToken())){
                List<String> searchStrings = new ArrayList<String>(config.getMaxSearchTokens());
                searchStrings.add(state.getToken().getText());
                //get the list of all tokens that can possible be matched
                int includeTokenIndex = state.getTokenIndex();
                includeTokenIndex++;
                while(searchStrings.size() < config.getMaxSearchTokens() && //more search strings
                        (includeTokenIndex <= (state.getChunk() != null ? //still within
                                state.getChunk().getEnd() : //the chunk
                                    state.getSentence().getTokens().size()-1))){ //or sentence
                    Token included = state.getSentence().getTokens().get(includeTokenIndex);
                    if(log.isDebugEnabled()  && includeTokenIndex > debugedIndex){
                        debugedIndex = includeTokenIndex;
                        log.debug(" {} {} (pos:{}|prop:{})",new Object[]{
                            isProcessableToken(included)? '+':'-',
                            included.getText(),included.getPosTags(),included.getPosProbabilities()
                        });
                    }
                    includeTokenIndex++;
                    if(isProcessableToken(included)){
                        searchStrings.add(included.getText());
                    }
                }
                //search for Entities
                List<Suggestion> suggestions = lookupEntities(searchStrings);
                if(!suggestions.isEmpty()){
                    //update the suggestions based on the best match
                    int bestMatchCount = suggestions.get(0).getMatchCount();
                    Iterator<Suggestion> it = suggestions.iterator();
                    while(it.hasNext()){
                        Suggestion suggestion = it.next();
                        //suggestions that match less tokens as the best match
                        //need to be updated to PARTIAL
                        if(suggestion.getMatchCount() < bestMatchCount){
                            suggestion.setMatch(MATCH.PARTIAL);
                        }
                        //Filter matches with less than config.getMinFoundTokens()
                        //if matchcount is less than of the best match
                        if(suggestion.getMatchCount() < bestMatchCount &&
                                suggestion.getMatchCount() < config.getMinFoundTokens()){
                            it.remove();
                        } else { //calculate the score
                            double suggestionMatchScore = suggestion.getMatchCount()*suggestion.getMatchScore();
                            //how good is the current match in relation to the best one
                            double spanScore = suggestion.getMatchCount()/bestMatchCount;
                            //how good is the match to the span selected by this suggestion
                            double textScore = suggestionMatchScore/suggestion.getSpan();
                            //how good is the match in relation to the tokens of the suggested label
                            double labelScore = suggestionMatchScore/suggestion.getLabelTokenCount();
                            suggestion.setScore(spanScore*spanScore*textScore*labelScore);
                        }
                    }
                    Suggestion oldBestRanked = suggestions.get(0); //for debugging
                    //resort by score
                    Collections.sort(suggestions, Suggestion.SCORE_COMPARATOR);
                    //this should never happen ... but the
                    //matchcount of the best match MUST NOT change
                    //after the sort by score!
                    if(bestMatchCount != suggestions.get(0).getMatchCount()){
                        log.warn("The match count for the top Ranked Suggestion for {} " +
                        		"changed after resorting based on Scores!",
                            state.getTokenText(suggestions.get(0).getStart(),bestMatchCount));
                        log.warn("  originalbest   : {}",oldBestRanked);
                        log.warn(" currnet ranking : {}",suggestions);
                        log.warn("  ... this will result in worng confidence values relative to the best match");
                    }
                    //remove all suggestions > config.maxSuggestions
                    if(suggestions.size() > config.getMaxSuggestions()){
                        suggestions.subList(config.getMaxSuggestions(),suggestions.size()).clear();
                    }
                    
                    //process redirects
                    if(config.getRedirectProcessingMode() != RedirectProcessingMode.IGNORE){
                        for(Suggestion suggestion : suggestions){
                            processRedirects(suggestion);
                        }
                    }
                    int start = suggestions.get(0).getStart();
                    int span = suggestions.get(0).getSpan();
                    //Store the linking results
                    String selectedText = state.getTokenText(start,span);
                    //float score;
                    LinkedEntity linkedEntity = linkedEntities.get(selectedText);
                    if(linkedEntity == null){
                        linkedEntity = new LinkedEntity(selectedText,
                            suggestions, getLinkedEntityTypes(suggestions.subList(0, 1)));
                        linkedEntities.put(selectedText, linkedEntity);
                    }
                    linkedEntity.addOccurrence(
                        state.getSentence(), start, span);
                    //set the next token to process to the next word after the
                    //currently found suggestion
                    state.setConsumed(start+span-1);
                }
                
            } //else do not process this token
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
    private Set<IRI> getLinkedEntityTypes(Collection<Suggestion> suggestions){
        Collection<String> conceptTypes = new HashSet<String>();
        for(Suggestion suggestion : suggestions){
            for(Iterator<Reference> types = 
                suggestion.getRepresentation().getReferences(config.getTypeField()); 
                types.hasNext();conceptTypes.add(types.next().getReference()));
        }
        Map<String,IRI> typeMappings = config.getTypeMappings();
        Set<IRI> dcTypes = new HashSet<IRI>();
        for(String conceptType : conceptTypes){
            IRI dcType = typeMappings.get(conceptType);
            if(dcType != null){
                dcTypes.add(dcType);
            }
        }
        if(dcTypes.isEmpty() && config.getDefaultDcType() != null){
            dcTypes.add(config.getDefaultDcType());
        }
        return dcTypes;
    }
    /**
     * Processes {@link EntitySearcher#getRedirectField() redirect field} values for
     * the parsed suggestions based on the {@link RedirectProcessingMode}
     * as configured in the {@link #config}.<p>
     * The results of this method are stored within the parsed {@link Suggestion}s
     * @param suggestion The suggestion to process.
     */
    private void processRedirects(Suggestion suggestion) {
        //if mode is IGNORE -> nothing to do
        if(config.getRedirectProcessingMode() == RedirectProcessingMode.IGNORE){
            return;
        }
        //in case results for queries are locally cached it might be the case
        //that some/all of the results do already redirects processed.
        //therefore there is a small internal state that stores this information
        if(suggestion.isRedirectedProcessed()){
            return; //Redirects for ResultMatch are already processed ... ignore
        }
        Representation result = suggestion.getResult();
        Iterator<Reference> redirects = result.getReferences(config.getRedirectField());
        switch (config.getRedirectProcessingMode()) {
            case ADD_VALUES:
                while(redirects.hasNext()){
                    Reference redirect = redirects.next();
                    if(redirect != null){
                        Representation redirectedEntity = entitySearcher.get(redirect.getReference(),
                            config.getSelectedFields());
                        if(redirectedEntity != null){
                            for(Iterator<String> fields = redirectedEntity.getFieldNames();fields.hasNext();){
                                String field = fields.next();
                                result.add(field, redirectedEntity.get(field));
                            }
                        }
                        //set that the redirects where searched for this result
                        suggestion.setRedirectProcessed(true);
                    }
                }
            case FOLLOW:
                while(redirects.hasNext()){
                    Reference redirect = redirects.next();
                    if(redirect != null){
                        Representation redirectedEntity = entitySearcher.get(redirect.getReference(),
                            config.getSelectedFields());
                        if(redirectedEntity != null){
                            //copy the original result score
                            redirectedEntity.set(RdfResourceEnum.resultScore.getUri(),
                                result.get(RdfResourceEnum.resultScore.getUri()));
                            //set the redirect
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
     */
    private List<Suggestion> lookupEntities(List<String> searchStrings) throws EngineException {
        Collection<? extends Representation> results;
        try {
            results = entitySearcher.lookup(config.getNameField(),config.getSelectedFields(),
            searchStrings, state.getSentence().getLanguage(),config.getDefaultLanguage());
        } catch (RuntimeException e) {
            throw new EngineException(e.getMessage(),e);
        }
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        for(Representation result : results){ 
            Suggestion match = matchLabels(result);
            if(match.getMatch() != MATCH.NONE){
                suggestions.add(match);
            }                    
        }
        //sort the suggestions
        if(suggestions.size()>1){
            Collections.sort(suggestions,Suggestion.DEFAULT_SUGGESTION_COMPARATOR);
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
     * @param rep The representation including at least the data for the
     * {@link EntitySearcher#getNameField()} property.
     * @return The result of the matching.
     */
    private Suggestion matchLabels(Representation rep) {
        String curLang = state.getLanguage(); //language of the current sentence
        String defLang = config.getDefaultLanguage(); //configured default language 
//        Iterator<Text> labels = rep.get(config.getNameField(), //get all labels
//            state.getLanguage(), //in the current language
//            config.getDefaultLanguage()); //and the default language
        Iterator<Text> labels = rep.getText(config.getNameField());
        Suggestion match = new Suggestion(rep);
        Collection<Text> defaultLabels = new ArrayList<Text>();
        boolean matchedCurLangLabel = false;
        while(labels.hasNext()){
            Text label = labels.next();
            String lang = label.getLanguage();
            if((lang == null && curLang == null) ||
                    (lang != null && curLang != null && lang.startsWith(curLang))){
                matchLabel(match, label);
                matchedCurLangLabel = true;
            } else if((lang ==null && defLang == null) ||
                    (lang != null && defLang != null && lang.startsWith(defLang))){
                defaultLabels.add(label);
            }
        }
        //use only labels in the default language if there is
        // * no label in the current language or
        if(!matchedCurLangLabel){// || match.getMatch() == MATCH.NONE){
            for(Text defaultLangLabel : defaultLabels){
                matchLabel(match, defaultLangLabel);
            }
        }
        return match;
    }
    
    /**
     * @param match
     * @param label
     */
    private void matchLabel(Suggestion match, Text label) {
        String text = label.getText();
        if(!config.isCaseSensitiveMatching()){
            text = text.toLowerCase(); //TODO use language of label for Locale
        }
        //Tokenize the label and remove remove tokens without alpha numerical chars
        String[] unprocessedLabelTokens = content.tokenize(text);
        int offset = 0;
        for(int i=0;i<unprocessedLabelTokens.length;i++){
            boolean hasAlpha = false;
            for(int j=0;!hasAlpha && j<unprocessedLabelTokens[i].length();j++){
                hasAlpha = Character.isLetterOrDigit(unprocessedLabelTokens[i].charAt(j));
            }
            if(!hasAlpha){
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
        int lastFoundIndex = -1;
        int firstFoundLabelIndex = -1;
        int lastfoundLabelIndex = -1;
        Token currentToken;
        String currentTokenText;
        int currentTokenLength;
        int notFound = 0;
        float minTokenMatchFactor = config.getMinTokenMatchFactor();
        //search for matches within the correct order
        for(int currentIndex = state.getTokenIndex();
                currentIndex < state.getSentence().getTokens().size() 
                && search ;currentIndex++){
            currentToken = state.getSentence().getTokens().get(currentIndex);
            if(currentToken.hasAplhaNumericChar()){
                currentTokenText = currentToken.getText();
                if(!config.isCaseSensitiveMatching()){
                    currentTokenText = currentTokenText.toLowerCase();
                }
                currentTokenLength = currentTokenText.length();
                boolean isProcessable = isProcessableToken(currentToken);
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
                    if(isProcessable){
                        foundProcessableTokens++; //only count processable Tokens
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
                    if(isProcessable || notFound > config.getMaxNotFound()){
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
        int currentIndex = state.getTokenIndex()-1;
        int labelIndex = firstFoundLabelIndex-1;
        notFound = 0;
        search = true;
        while(search && labelIndex >= 0 && currentIndex > state.getConsumedIndex()){
            String labelTokenText = labelTokens[labelIndex];
            if(labelTokenSet.remove(labelTokenText)){ //still not matched
                currentToken = state.getSentence().getTokens().get(currentIndex);
                boolean isProcessable = isProcessableToken(currentToken);
                currentTokenText = currentToken.getText();
                if(!config.isCaseSensitiveMatching()){
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
                    if(isProcessable){
                        foundProcessableTokens++; //only count processable Tokens
                    }
                    foundTokens++;
                    foundTokenMatch = foundTokenMatch + matchFactor; //sum up the matches
                    firstFoundIndex = currentIndex;
                    currentIndex --;
                } else {
                    notFound++;
                    if(isProcessable || notFound > config.getMaxNotFound()){
                        //stop as soon as a token that needs to be processed is
                        //not found in the label or the maximum number of tokens
                        //that are not processable are not found
                        search = false; 
                    }
                }
            }
            labelIndex--; 
        }
        //Now we make a second round to search tokens that match in the wrong order
        //e.g. if given and family name of persons are switched
        MATCH labelMatch; 
        int coveredTokens = lastFoundIndex-firstFoundIndex+1;
        float labelMatchScore = (foundTokenMatch/(float)labelTokens.length);
        //Matching rules
        // - if less than config#minTokenFound() than accept only EXACT
        // - override PARTIAL matches with FULL/EXACT matches only if
        //   foundTokens of the PARTIAL match is > than of the FULL/EXACT
        //   match (this will be very rare
        if(foundProcessableTokens > 0 && match.getMatchCount() <= foundProcessableTokens) {
            String currentText = state.getTokenText(firstFoundIndex,coveredTokens);
            if(config.isCaseSensitiveMatching() ? currentText.equals(text) : currentText.equalsIgnoreCase(text)){ 
                labelMatch = MATCH.EXACT;
                //set found to covered: May be lower because only
                //processable tokens are counted, but Exact also checks
                //of non-processable!
                foundTokens = coveredTokens;
            } else if((foundProcessableTokens >= config.getMinFoundTokens() ||
                    //NOTE (rwesten, 2012-05-21): Do not check if all covered
                    //  Tokens are found, but if all Tokens of the Label are
                    //  matched! (STANBOL-622)
                    //foundTokens == coveredTokens) && 
                    foundTokens >= labelTokens.length) &&
                    labelMatchScore >= 0.6f){
                //same as above
                //if(foundTokens == coveredTokens){
                if(foundTokens == labelTokens.length && foundTokens == coveredTokens){
                    labelMatch = MATCH.FULL;
                } else {
                    labelMatch = MATCH.PARTIAL;
                }
            } else {
                labelMatch = MATCH.NONE;
            }
            if(labelMatch != MATCH.NONE){
                if(match.getMatchCount() < foundProcessableTokens ||
                        match.getMatchCount() == foundProcessableTokens && 
                        labelMatch.ordinal() > match.getMatch().ordinal()){
                    match.updateMatch(labelMatch, firstFoundIndex, coveredTokens, foundTokens,
                        foundTokenMatch/foundTokens,label,labelTokens.length);
                } //else this match is not better as the existing one
            } //else ignore labels with MATCH.NONE
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
    
    /**
     * Checks if the current token of {@link #state} is processable. 
     * @param token the {@link Token} to check.
     * @return <code>true</code> if the parsed token needs to be processed.
     * Otherwise <code>false</code>
     */
    private boolean isProcessableToken(Token token) {
        Boolean processToken = null;
        String[] posTags = token.getPosTags();
        double[] posProb = token.getPosProbabilities();
        if(posTags != null){
            int i=0;
            do {
                processToken = content.processPOS(posTags[i],posProb[i]);
                i++;
            } while(processToken == null && i<posTags.length);
        }
        if(processToken == null) {
             processToken = token.getText().length() >= config.getMinSearchTokenLength();
        }
        return processToken;
    }
}
