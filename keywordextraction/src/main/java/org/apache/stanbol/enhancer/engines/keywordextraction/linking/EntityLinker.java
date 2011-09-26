package org.apache.stanbol.enhancer.engines.keywordextraction.linking;

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

import opennlp.tools.util.Span;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText.Token;
import org.apache.stanbol.enhancer.engines.keywordextraction.impl.ProcessingState;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.EntityLinkerConfig.RedirectProcessingMode;
import org.apache.stanbol.enhancer.engines.keywordextraction.linking.Suggestion.MATCH;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

public class EntityLinker {

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
    }
    /**
     * Steps over the sentences, chunks, tokens of the {@link #sentences}
     */
    public void process(){
        while(state.next()) {
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
                            //how good is the current match in relation to the best one
                            double spanScore = ((double)suggestion.getMatchCount())/bestMatchCount;
                            //how good is the match to the span selected by this suggestion
                            double textScore = ((double)suggestion.getMatchCount())/suggestion.getSpan();
                            //how good is the match in relation to the tokens of the suggested label
                            double labelScore = ((double)suggestion.getMatchCount()/suggestion.getLabelTokenCount());
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
                        //TODO: change this to a warning (like to have exceptions during debugging)
                        throw new IllegalStateException(String.format(
                            "The match count for the top Ranked Suggestion for %s changed after resorting based on Scores! (original: %s, currnet %s)",
                            state.getTokenText(bestMatchCount),oldBestRanked,suggestions));
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
                    int span = suggestions.get(0).getSpan();
                    //Store the linking results
                    String selectedText = state.getTokenText(span);
                    //float score;
                    LinkedEntity linkedEntity = linkedEntities.get(selectedText);
                    if(linkedEntity == null){
                        linkedEntity = new LinkedEntity(selectedText,
                            suggestions, getLinkedEntityTypes(suggestions.subList(0, 1)));
                        linkedEntities.put(selectedText, linkedEntity);
                    }
                    linkedEntity.addOccurrence(
                        state.getSentence(), state.getTokenIndex(), span);
                    //set the next token to process to the next word after the
                    //currently found suggestion
                    state.setNextToken(state.getTokenIndex()+span);
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
    private Set<UriRef> getLinkedEntityTypes(Collection<Suggestion> suggestions){
        Collection<String> conceptTypes = new HashSet<String>();
        for(Suggestion suggestion : suggestions){
            for(Iterator<Reference> types = 
                suggestion.getRepresentation().getReferences(config.getTypeField()); 
                types.hasNext();conceptTypes.add(types.next().getReference()));
        }
        Map<String,UriRef> typeMappings = config.getTypeMappings();
        Set<UriRef> dcTypes = new HashSet<UriRef>();
        for(String conceptType : conceptTypes){
            UriRef dcType = typeMappings.get(conceptType);
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
    private List<Suggestion> lookupEntities(List<String> searchStrings) {
        Collection<? extends Representation> results = entitySearcher.lookup(
            config.getNameField(),config.getSelectedFields(),
            searchStrings, state.getSentence().getLanguage(),config.getDefaultLanguage());
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
        while(labels.hasNext()){
            Text label = labels.next();
            String lang = label.getLanguage();
            //check the language of the current label
            //NOTE: Stirng.startWith is used to match'en-GB' with 'en'
            if((lang == null && ( //if lang is null
                            defLang == null || //default lang is null
                            curLang == null)) //or current lang is null
                    || (lang != null && ( //if lang is not null
                            //NOTE: starsWith does not like parsing NULL
                            curLang != null && lang.startsWith(curLang) || //match with default
                            defLang != null && lang.startsWith(defLang)) //or match with current
                        ) //end or
                    ){ //end if
                String text = label.getText().toLowerCase();
                List<String> labelTokens = Arrays.asList(content.tokenize(text));
                int foundTokens = 0;
                //ensure the correct order of the tokens in the suggested entity
                int foundInLabelIndex = 0;
                boolean search = true;
                int lastFoundIndex = -1;
                Token currentToken;
                int maxNotFound = 1; //TODO make configureable
                int notFound = 0;
                for(int currentIndex = state.getTokenIndex();currentIndex < state.getSentence().getTokens().size() && search;currentIndex++){
                    currentToken = state.getSentence().getTokens().get(currentIndex);
                    boolean isProcessable = isProcessableToken(currentToken);
                    int found = text.indexOf(currentToken.getText().toLowerCase());
                    if(found>=foundInLabelIndex){ //found
                        if(isProcessable){
                            foundTokens++; //only count processable Tokens
                        }
                        //TODO: maybe move this also in the "isProcessable" ...
                        foundInLabelIndex = found+currentToken.getText().length();
                        lastFoundIndex = currentIndex;
                    } else { //not found
                        notFound++;
                        if(isProcessable || notFound > maxNotFound){
                            //stop as soon as a token that needs to be processed is
                            //not found in the label or the maximum number of tokens
                            //that are not processable are not found
                            search = false; 
                        }
                    } //else it is OK if non processable tokens are not found
                } 
                MATCH labelMatch; 
                int coveredTokens = lastFoundIndex-state.getTokenIndex()+1;
                //Matching rules
                // - if less than config#minTokenFound() than accept only EXACT
                // - override PARTIAL matches with FULL/EXACT matches only if
                //   foundTokens of the PARTIAL match is > than of the FULL/EXACT
                //   match (this will be very rare
                if(foundTokens > 0 && match.getMatchCount() <= foundTokens) {
                    String currentText = state.getTokenText(coveredTokens);
                    if(currentText.equalsIgnoreCase(label.getText())){ 
                        labelMatch = MATCH.EXACT;
                        //set found to covered: May be lower because only
                        //processable tokens are counted, but Exact also checks
                        //of non-processable!
                        foundTokens = coveredTokens;
                    } else if(foundTokens >= config.getMinFoundTokens()){
                        if(foundTokens == coveredTokens){
                            labelMatch = MATCH.FULL;
                        } else {
                            labelMatch = MATCH.PARTIAL;
                        }
                    } else {
                        labelMatch = MATCH.NONE;
                    }
                    if(labelMatch != MATCH.NONE){
                        if(match.getMatchCount() < foundTokens ||
                                match.getMatchCount() < foundTokens && 
                                labelMatch.ordinal() > match.getMatch().ordinal()){
                            match.updateMatch(labelMatch, coveredTokens, foundTokens,label,labelTokens.size());
                        } //else this match is not better as the existing one
                    } //else ignore labels with MATCH.NONE
                } //else NO tokens found -> nothing to do
            } // else worng language
        }
        return match;
    }

    /**
     * Checks if the current token of {@link #state} is processable. 
     * @param token the {@link Token} to check.
     * @return <code>true</code> if the parsed token needs to be processed.
     * Otherwise <code>false</code>
     */
    private boolean isProcessableToken(Token token) {
        Boolean processToken = null;
        if(token.getPosTag() != null){
            processToken = content.processPOS(token.getPosTag());
        }
        if(processToken == null) {
             processToken = token.getText().length() >= config.getMinSearchTokenLength();
        }
        return processToken;
    }
}
