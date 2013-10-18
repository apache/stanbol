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
package org.apache.stanbol.enhancer.engines.sentiment.summarize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

/**
 * This class is used to represents a {@link Token} that holds a Sentiment in the
 * context of a {@link Sentence}. Sentiment might be {@link #addNegate(Token) negated}
 * and be {@link #addAbout(Token) assigned} to a Noun or Pronoun via a
 * {@link #getVerb() Verb}. The {@link #getStart()} and {@link #getEnd()} values
 * return the span selected by this Sentiment. This are the lowest start and
 * highest end values of any token related with this sentiment. Those spans are
 * used by the {@link SentimentPhrase} class for clustering {@link Sentiment}s
 * to phrases.
 * 
 * @author Rupert Westenthaler
 *
 */
public class Sentiment {
    
    /**
     * Minimum POS tag confidence so that the annotated POS is used without
     * considering the {@link #PREF_LEX_CAT}
     */
    private static final double MIN_POS_CONF = 0.85;
    /**
     * if the confidence of the main POS tag is lower then {@link #MIN_POS_CONF},
     * than all POS tags are searched for the an POS annotation compatible with
     * {@link #PREF_LEX_CAT}.
     */
    private static final Set<LexicalCategory> PREF_LEX_CAT = EnumSet.of(LexicalCategory.Adjective);
    /**
     * The token holding the sentiment
     */
    private final Token token;
    /**
     * The (not negated) value of the sentiment
     */
    private final double value;
    /**
     * The Sentence of the {@link #token}
     */
    private final Sentence sentence;
    /**
     * List of tokens that negate this sentiment. <code>null</code> if no
     * negation was added
     */
    private List<Token> negated;
    /**
     * The Nouns and/or Pronouns this sentiment is about. <code>null</code> if
     * no aboutness is defined
     */
    private List<Token> aboutness;
    /**
     * The PosTag of the of the {@link #token}
     */
    private final PosTag posTag;

    /**
     * The start position of this sentiment. This is the lowest start of any
     * token added to this sentiment. This field is set by {@link #checkSpan(Token)}
     */
    private int start;
    /**
     * The end position of this sentiment. This is the highest end of any
     * token added to this sentiment. This field is set by {@link #checkSpan(Token)}
     */
    private int end;
    /**
     * The verb assigning this sentiment to the Nouns and/or Pronouns added
     * by {@link #addAbout(Token)}.
     */
    private Token verb;
    
    /**
     * The Token with the sentiment, the value of the sentiment and optionally
     * the Sentence for the token
     * @param token
     * @param value
     * @param sentence
     */
    public Sentiment(Token token, double value, Sentence sentence) {
        this.token = token;
        this.value = value;
        this.sentence = sentence;
        this.start = token.getStart();
        this.end = token.getEnd();
        List<Value<PosTag>> tags = token.getAnnotations(NlpAnnotations.POS_ANNOTATION);
        PosTag posTag = null;
        if(tags != null && !tags.isEmpty()){
            for(Value<PosTag> tag : tags){
                if(tag.probability() == Value.UNKNOWN_PROBABILITY ||
                        tag.probability() >= MIN_POS_CONF || 
                        !Collections.disjoint(tag.value().getCategories(),PREF_LEX_CAT)){
                    posTag = tag.value();
                    break;
                }
            }
            if(posTag == null){
                posTag = tags.get(0).value();
            }
            if(posTag.hasCategory(LexicalCategory.Noun)){
                addAbout(token); //add the token also as noun
            }
            if(posTag.hasCategory(LexicalCategory.Verb)){
                setVerb(token);
            }
        }
        this.posTag = posTag;
    }
    /**
     * Adds an Token that negates this Sentiment
     * @param token the token
     */
    protected void addNegate(Token token){
        if(negated == null){ //most of the time a singeltonList will do
            negated = Collections.singletonList(token);
        } else if(negated.size() == 1){
            List<Token> l = new ArrayList<Token>(4);
            l.add(negated.get(0));
            l.add(token);
            negated = l;
        }
        checkSpan(token);
    }
    protected void setVerb(Token verb) {
        this.verb = verb;
        checkSpan(verb);
    }

    protected void addAbout(Token noun) {
        if(aboutness == null){
            aboutness = new ArrayList<Token>(4);
        }
        aboutness.add(noun);
        checkSpan(noun);
    }
    /**
     * Checks the {@link #start} {@link #end} values against the span selected
     * by the parsed token.<p>
     * This method is called by all others that do add tokens.
     * @param token the added token
     */
    private void checkSpan(Token token) {
        if(start > token.getStart()){
            start = token.getStart();
        }
        if(end < token.getEnd()){
            end = token.getEnd();
        }
    }

    /**
     * The POS tag of the Token with a sentiment.
     * @return
     */
    public PosTag getPosTag() {
        return posTag;
    }
    /**
     * The Sentiment value (considering possible negations)
     * @return the sentiment value
     */
    public double getValue() {
        return negated == null ? value : value*-1;
    }
    /**
     * The Token holding the sentiment
     * @return the token
     */
    public Token getToken() {
        return token;
    }
    public Sentence getSentence() {
        return sentence;
    }
    /**
     * The {@link AnalysedText Text}
     * @return the text
     */
    public AnalysedText getAnalysedText(){
        return token.getContext();
    }
    /**
     * The tokens negating this Sentiment
     * @return the tokens or an empty list if none
     */
    public List<Token> getNegates() {
        return negated == null ? Collections.<Token>emptyList() : negated;
    }

    /**
     * The Nouns or Pronoun(s) the Sentiment is about
     * @return the tokens or an empty list if none.
     */
    public List<Token> getAboutness() {
        return aboutness == null ? Collections.<Token>emptyList() : aboutness;
    }
    /**
     * The verb used to assign Adjectives to the Nouns (or Pronouns)
     * @return
     */
    public Token getVerb() {
        return verb;
    }
    /**
     * The start position of this sentiment. This is the lowest start of any
     * token linked to this sentiment
     * @return the start position
     */
    public int getStart(){
        return start;
    }
    /**
     * The end position of this sentiment. This is the highest end of any
     * token linked to this sentiment
     * @return the end position
     */
    public int getEnd(){
        return end;
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Sentiment [").append(start).append(',').append(end).append("]:")
                .append(token).append('@')
                .append(getValue()).append(" | negations: ").append(getNegates())
                .append(" | about: ").append(getAboutness()).append(" | verb: ").append(verb).toString();
    }
    @Override
    public int hashCode() {
        return token.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Sentiment && token.equals(((Sentiment)obj).token)
                && value == ((Sentiment)obj).value && ((negated == null 
                && ((Sentiment)obj).negated == null) || (negated != null 
                && !negated.isEmpty() && ((Sentiment)obj).negated != null &&
                !((Sentiment)obj).negated.isEmpty()));
    }

}
