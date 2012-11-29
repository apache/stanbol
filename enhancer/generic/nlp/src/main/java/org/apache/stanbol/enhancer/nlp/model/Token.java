package org.apache.stanbol.enhancer.nlp.model;



public interface Token extends Span {

    /**
     * Returns {@link SpanTypeEnum#Token}
     * @see Span#getType()
     * @see SpanTypeEnum#Token
     */
    SpanTypeEnum getType();

    
}