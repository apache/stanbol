package org.apache.stanbol.enhancer.engines.sentiment.summarize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.NlpAnnotations;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Section;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Token;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;
import org.apache.stanbol.enhancer.nlp.pos.LexicalCategory;
import org.apache.stanbol.enhancer.nlp.pos.PosTag;

/**
 * This class is used to allow adding negations to sentiments even if the
 * sentiment was already assigned to an SentimentInfo. In addition this class
 * stores the token for the sentiment AND the tokens causing the negations. No
 * support for multiple negations - meaning that the sentiment value is inverted
 * if 1..* negations are present.
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
    
    private final Token token;
    private final double value;
    private final Sentence sentence;
    private List<Token> negated;
    private List<Token> aboutness;
    private PosTag posTag;

    private int start;
    private int end;
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
    
    public void negate(Token token){
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
    protected final void setVerb(Token verb) {
        this.verb = verb;
        checkSpan(verb);
    }

    protected final void addAbout(Token noun) {
        if(aboutness == null){
            aboutness = new ArrayList<Token>(4);
        }
        aboutness.add(noun);
        checkSpan(noun);
    }
    /**
     * Checks the {@link #start} {@link #end} values against the span selected
     * by the parsed token
     * @param token
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
    public double getValue() {
        return negated == null ? value : value*-1;
    }

    public Token getToken() {
        return token;
    }
    public Sentence getSentence() {
        return sentence;
    }
    public AnalysedText getAnalysedText(){
        return token.getContext();
    }
    
    public List<Token> getNegates() {
        return negated == null ? Collections.EMPTY_LIST : negated;
    }

    /**
     * The Nouns or Pronoun(s) the Adjectives are about
     * @return
     */
    public List<Token> getAboutness() {
        return aboutness == null ? Collections.EMPTY_LIST : aboutness;
    }
    /**
     * The verb used to assign Adjectives to the Nouns (or Pronouns)
     * @return
     */
    public Token getVerb() {
        return verb;
    }
    
    public int getStart(){
        return start;
    }
    
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
