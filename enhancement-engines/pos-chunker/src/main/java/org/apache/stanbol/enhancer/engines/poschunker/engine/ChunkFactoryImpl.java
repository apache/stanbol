package org.apache.stanbol.enhancer.engines.poschunker.engine;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.stanbol.enhancer.engines.poschunker.PhraseBuilder;
import org.apache.stanbol.enhancer.engines.poschunker.PhraseBuilder.ChunkFactory;
import org.apache.stanbol.enhancer.nlp.model.AnalysedText;
import org.apache.stanbol.enhancer.nlp.model.Chunk;
import org.apache.stanbol.enhancer.nlp.model.Token;

/**
 * Implementation of the {@link ChunkFactory} interface used by the 
 * {@link PhraseBuilder} to create chunks
 * @author Rupert Westenthaler
 *
 */
public class ChunkFactoryImpl implements ChunkFactory{

    private final AnalysedText at;
    private final ReadWriteLock lock;
    
    public ChunkFactoryImpl(AnalysedText at, ReadWriteLock lock) {
        this.at = at;
        this.lock = lock;
    }
    
    @Override
    public Chunk createChunk(Token start, Token end) {
        if(start == null || end == null){
            throw new IllegalArgumentException("Parst start Token '" + start
                + "' and end Token '" + end +"' MUST NOT be NULL!");
        }
        lock.writeLock().lock();
        try {
            return at.addChunk(start.getStart(), end.getEnd());
        } finally {
            lock.writeLock().unlock();  
        }
    }

}
