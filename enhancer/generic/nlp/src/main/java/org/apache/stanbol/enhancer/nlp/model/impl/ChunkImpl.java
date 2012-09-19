package org.apache.stanbol.enhancer.nlp.model.impl;

import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Span;


public final class ChunkImpl extends SectionImpl implements Chunk {

    protected ChunkImpl(AnalysedTextImpl at, Span relativeTo,int start, int end){
        super(at,SpanTypeEnum.Chunk,relativeTo,start,end);
    }
        
}
