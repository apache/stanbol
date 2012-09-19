package org.apache.stanbol.enhancer.nlp.model.impl;

import java.util.Iterator;

import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Sentence;
import org.apache.stanbol.enhancer.nlp.model.Span;


public final class SentenceImpl extends SectionImpl implements Sentence {

    
    protected SentenceImpl(AnalysedTextImpl at, Span relativeTo,int start, int end){
        super(at, SpanTypeEnum.Sentence, relativeTo, start, end);
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Sentence#addChunk(int, int)
     */
    @Override
    public ChunkImpl addChunk(int start, int end){
        return register(new ChunkImpl(context, this, start, end));
    }
    
    /* (non-Javadoc)
     * @see org.apache.stanbol.enhancer.nlp.model.impl.Sentence#getChunks()
     */
    @Override
    public Iterator<Chunk> getChunks(){
        return filter(Chunk.class);
    }
}
