package org.apache.stanbol.enhancer.nlp.model;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

public interface Sentence extends Section {

    /**
     * Returns {@link SpanTypeEnum#Sentence}
     * @see Span#getType()
     * @see SpanTypeEnum#Sentence
     */
    SpanTypeEnum getType();

    /**
     * Adds an Chunk relative to this Sentence
     * @param start the start of the chunk relative to the sentence
     * @param end
     * @return
     */
    Chunk addChunk(int start, int end);


    /**
     * The Chunks covered by this Sentence<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the chunks
     */
    Iterator<Chunk> getChunks();

}