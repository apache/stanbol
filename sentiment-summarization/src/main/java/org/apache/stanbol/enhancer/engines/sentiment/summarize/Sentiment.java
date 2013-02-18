package org.apache.stanbol.enhancer.engines.sentiment.summarize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.stanbol.enhancer.nlp.model.Token;

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
    
    private Token token;
    private double value;
    private List<Token> negated;
    private List<Token> nouns;

    public Sentiment(Token token, double value) {
        this.token = token;
        this.value = value;
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
    }
    
    public double getValue() {
        return negated == null ? value : value*-1;
    }

    public Token getToken() {
        return token;
    }
    
    public List<Token> getNegates() {
        return negated == null ? Collections.EMPTY_LIST : negated;
    }

    public void noun(Token noun) {
        if(nouns == null){
            nouns = new ArrayList<Token>(4);
        }
        nouns.add(noun);
    }
    public List<Token> getNouns() {
        return nouns == null ? Collections.EMPTY_LIST : nouns;
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Sentiment ").append(token.getSpan()).append('@')
                .append(getValue()).append(" | negations: ").append(getNegates())
                .append(" | nouns: ").append(getNouns()).toString();
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
