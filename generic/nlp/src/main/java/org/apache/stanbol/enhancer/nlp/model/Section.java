package org.apache.stanbol.enhancer.nlp.model;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

/**
 * A {@link Span} that may enclose other Spans. Super type for {@link Chunk}s,
 * {@link Sentence}s and {@link AnalysedText}.<p>
 * As {@link Span} this is an meta (abstract) type. Implementations of this
 * Interface SHOULD BE abstract Classes. 
 */
public interface Section extends Span {

    /**
     * Iterates over all Span enclosed by this one that are of any of the
     * parsed Types.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @param types the {@link SpanTypeEnum types} of Spans included
     * @return sorted iterator over the selected Spans.
     */
    Iterator<Span> getEnclosed(Set<SpanTypeEnum> types);

    /**
     * Adds an Token relative to this Sentence
     * @param start the start of the token relative to the sentence
     * @param end
     * @return
     */
    Token addToken(int start, int end);

    /**
     * The Tokens covered by this Sentence.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the tokens
     */
    Iterator<Token> getTokens();

}
