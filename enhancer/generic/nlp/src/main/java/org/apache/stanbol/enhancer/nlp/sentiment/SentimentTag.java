package org.apache.stanbol.enhancer.nlp.sentiment;

import org.apache.stanbol.enhancer.nlp.model.annotation.Value;

/**
 * The sentiment {@link #POSITIVE} or {@link SentimentTag#NEGATIVE}. The
 * value is directly represented by the {@link Value#probability()}.
 * 
 * @author Rupert Westenthaler
 *
 */
public final class SentimentTag {

    /**
     * A positive sentiment tag
     */
    public static final SentimentTag POSITIVE = new SentimentTag(true);
    /**
     * A negative sentiment tag
     */
    public static final SentimentTag NEGATIVE = new SentimentTag(false);

    /**
     * positive if <code>true</code> otherwise negative.
     */
    private final boolean positive;

    /**
     * Singleton constructor
     */
    private SentimentTag(boolean positive){
        this.positive = positive;
    }
    
    /**
     * If the {@link Value#probability() sentiment} is positive
     */
    public final boolean isPositive() {
        return positive;
    }
    
    /**
     * If the {@link Value#probability() sentiment} is negative
     */
    public final boolean isNegative() {
        return !positive;
    }
    
    
}
