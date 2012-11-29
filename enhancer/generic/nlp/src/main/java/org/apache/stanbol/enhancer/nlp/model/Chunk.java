package org.apache.stanbol.enhancer.nlp.model;


public interface Chunk extends Section {

    /**
     * Returns {@link SpanTypeEnum#Chunk}
     * @see Span#getType()
     * @see SpanTypeEnum#Chunk
     */
    SpanTypeEnum getType();
    
}