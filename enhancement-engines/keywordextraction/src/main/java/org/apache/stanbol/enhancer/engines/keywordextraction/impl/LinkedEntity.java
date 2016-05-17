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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import opennlp.tools.util.Span;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.commons.opennlp.TextAnalyzer.AnalysedText;

/**
 * The occurrence of an detected Entity within the content. <p>
 * Note that this class already stores the information in a structure as needed
 * to write Enhancements as defined by the upcoming 2nd version of the
 * Apache Stanbol Enhancement Structure (EntityAnnotation, TextOccurrence and
 * EntitySuggestion). However it can also be used to write
 * TextAnnotations and EntityAnnotations as defined by the 1st version
 * @author Rupert Westenthaler
 *
 */
public class LinkedEntity {
    /**
     * An mention of an linked entity within the text
     * @author Rupert Westenthaler
     *
     */
    public final class Occurrence {
        /**
         * The maximum number of chars until that the current sentence is used
         * as context for TextOcccurrences. If the sentence is longer a area of
         * {@link #CONTEXT_TOKEN_COUNT} before and after the current selected
         * text is used as context.<p>
         * This is especially important in case no sentence detector is available
         * for the current content. Because in this case the whole text is
         * parsed as a single Sentence.
         * TODO: Maybe find a more clever way to determine the context
         */
        public static final int MAX_CONTEXT_LENGTH = 200;
        /**
         * The number of tokens surrounding the current selected text used to
         * calculate the context if the current sentence is longer than
         * {@link #MAX_CONTEXT_LENGTH} chars.<p>
         * This is especially important in case no sentence detector is available
         * for the current content. Because in this case the whole text is
         * parsed as a single Sentence.
         * TODO: Maybe find a more clever way to determine the context
         */
        public static final int CONTEXT_TOKEN_COUNT = 5;
        private final int start;
        private final int end;
        private final String context;

        private Occurrence(AnalysedText sentence,int token) {
            this(sentence,token,1);
        }
        private Occurrence(AnalysedText sentence,int startToken,int tokenSpan){
            this.start = sentence.getOffset()+sentence.getTokens().get(startToken).getStart();
            this.end = sentence.getOffset()+sentence.getTokens().get(startToken+tokenSpan-1).getEnd();
            String context = sentence.getText();
            if(context.length() > MAX_CONTEXT_LENGTH){
                Span contextTokenSpan = new Span(
                    Math.max(0, startToken-CONTEXT_TOKEN_COUNT),
                    Math.min(startToken+tokenSpan+CONTEXT_TOKEN_COUNT, sentence.getTokens().size())-1);
                context = context.substring(sentence.getTokens().get(contextTokenSpan.getStart()).getStart(),
                    sentence.getTokens().get(contextTokenSpan.getEnd()).getEnd());
            }
            this.context = context;
        }
        /**
         * The context (surrounding text) of the occurrence.
         * @return
         */
        public String getContext() {
            return context;
        }
        /**
         * The start index of the occurrence
         * @return the start index relative to the start of the text 
         */
        public int getStart() {
            return start;
        }
        /**
         * the end index of the occurrence
         * @return the end index relative to the start of the text
         */
        public int getEnd() {
            return end;
        }
        /**
         * The selected text of this occurrence. Actually returns the value
         * of {@link LinkedEntity#getSelectedText()}, because th
         * @return
         */
        public String getSelectedText(){
            return LinkedEntity.this.getSelectedText();
        }
        @Override
        public String toString() {
            return start+","+end;
        }
        @Override
        public int hashCode() {
            return context.hashCode()+start+end;
        }
        @Override
        public boolean equals(Object arg0) {
            return arg0 instanceof Occurrence && 
                ((Occurrence)arg0).start == start &&
                ((Occurrence)arg0).end == end &&
                ((Occurrence)arg0).context.equals(context);
        }
    }
    private final String selectedText;
    private final Set<IRI> types;
    private final List<Suggestion> suggestions;
    private final Collection<Occurrence> occurrences = new ArrayList<Occurrence>();
    private final Collection<Occurrence> unmodOccurrences = Collections.unmodifiableCollection(occurrences);
    /**
     * Creates a new LinkedEntity for the parsed parameters
     * @param selectedText the selected text
     * @param suggestions the entity suggestions
     * @param types the types of the linked entity. 
     */
    protected LinkedEntity(String selectedText, List<Suggestion> suggestions, Set<IRI> types) {
        this.suggestions = Collections.unmodifiableList(suggestions);
        this.selectedText = selectedText;
        this.types = Collections.unmodifiableSet(types);
    }
   /**
     * Creates a new Linked Entity including the first {@link Occurrence}
     * @param sentence the sentence (context) for the occurrence.
     * @param startToken the index of the start token
     * @param tokenSpan the number of token included in this span
     * @param suggestions the entity suggestions
     * @param types the types of the linked entity. 
     */
    protected LinkedEntity(AnalysedText sentence,int startToken,int tokenSpan, 
                           List<Suggestion> suggestions, Set<IRI> types) {
        this(sentence.getText().substring(
            sentence.getTokens().get(startToken).getStart(), 
            sentence.getTokens().get(tokenSpan).getEnd()),suggestions,types);
        addOccurrence(sentence, startToken,tokenSpan);
    }
    /**
     * Getter for the selected text
     * @return the selected text
     */
    public String getSelectedText() {
        return selectedText;
    }
    
    /**
     * Getter for read only list of types
     * @return the types
     */
    public Set<IRI> getTypes() {
        return types;
    }
    /**
     * Adds an new Occurrence
     * @param sentence the analysed sentence
     * @param startToken the start token
     * @param tokenSpan the number of tokens included in this span
     * @return the new Occurrence also added to {@link #getOccurrences()}
     */
    protected Occurrence addOccurrence(AnalysedText sentence,int startToken,int tokenSpan){
        Occurrence o = new Occurrence(sentence, startToken, tokenSpan);
        occurrences.add(o);
        return o;
    }
    /**
     * Getter for the read only list of Occurrences
     * @return the occurrences
     */
    public Collection<Occurrence> getOccurrences(){
        return unmodOccurrences;
    }
    /**
     * Getter for the read only list of Suggestions
     * @return the suggestions
     */
    public List<Suggestion> getSuggestions(){
        return suggestions;
    }
    
    /**
     * Getter for the Score
     * @return The score of the first element in {@link #getSuggestions()} or 
     * <code>0</code> if there are no suggestions
     */
    public double getScore(){
        return suggestions.isEmpty() ? 0f : suggestions.get(0).getScore();
    }
    
    /**
     * Only considers the {@link #getSelectedText()}, because it is assumed that
     * for the same selected text there MUST BE always the same suggestions with
     * the same types and occurrences.
     */
    @Override
    public int hashCode() {
        return selectedText.hashCode();
    }
    /**
     * Only considers the {@link #getSelectedText()}, because it is assumed that
     * for the same selected text there MUST BE always the same suggestions with
     * the same types and occurrences.
     */
    @Override
    public boolean equals(Object arg0) {
        return arg0 instanceof LinkedEntity && 
        ((LinkedEntity)arg0).selectedText.equals(selectedText);
    }
    @Override
    public String toString() {
        return selectedText+'@'+occurrences+"->"+suggestions;
    }
}
