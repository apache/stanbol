package org.apache.stanbol.enhancer.nlp.model;

import org.apache.stanbol.enhancer.nlp.model.annotation.Annotated;

/**
 * Represents a {@link #getSpan() span} [{@link #getStart() start},
 * {@link #getEnd() end}] within the {@link #getContext() text}. Spans also have
 * an assigned {@link #getType() type}. Possible types are defined within the
 * {@link SpanTypeEnum}.<p>
 * This is an meta (abstract) type. Implementations of this Interface 
 * SHOULD BE abstract Classes.
 */
public interface Span extends Annotated, Comparable<Span>{

    /**
     * Enumeration over different types - or roles - spans defined for an
     * {@link AnalysedText} may play.
     */
    public static enum SpanTypeEnum {
        /**
         * The Text as a whole
         */
        Text,
        /**
         * An section of the text (chapter, page, paragraph ...). NOTE: this
         * does NOT define types of sections.
         */
        TextSection,
        /**
         * An Sentence
         */
        Sentence,
        /**
         * A Chunk (e.g. a Noun Phrase) NOTE: this does NOT define types of
         * Chunks
         */
        Chunk,
        /**
         * A Token (e.g. a noun, verb, punctuation) NOTE: this does NOT define
         * types of Tokens
         */
        Token;
    }
    /**
     * The type of the Span
     * @return
     */
    SpanTypeEnum getType();

    /**
     * The start index of this span This is the absolute offset from the
     * {@link #getContext()}{@link AnalysedText#getText() .getText()}
     */
    int getStart();
    /**
     * The end index of this span. This is the absolute offset from the
     * {@link #getContext()}{@link AnalysedText#getText() .getText()}
     * @return
     */
    int getEnd();

    /**
     * The {@link AnalysedText} this Span was added to.
     * @return the AnalysedText representing the context of this Span
     */
    AnalysedText getContext();
    /**
     * The section of the text selected by this span
     * @return the selected section of the text
     */
    String getSpan();
    
}