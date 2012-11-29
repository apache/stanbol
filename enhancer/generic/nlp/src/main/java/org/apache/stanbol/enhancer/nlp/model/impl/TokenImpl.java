package org.apache.stanbol.enhancer.nlp.model.impl;

import java.util.Arrays;

import org.apache.stanbol.enhancer.nlp.model.Span;
import org.apache.stanbol.enhancer.nlp.model.Token;


public final class TokenImpl extends SpanImpl implements Token {
    

//    protected TokenImpl(AnalysedTextImpl at, int start, int end){
//        super(at, SpanTypeEnum.Token,start, end);
//    }

    protected TokenImpl(AnalysedTextImpl at, Span relativeTo,int start, int end){
        super(at, SpanTypeEnum.Token, relativeTo, start, end);
    }
 
    @Override
    public String toString() {
        return String.format("%s: %s %s",type ,Arrays.toString(span), getSpan());
    }
}
