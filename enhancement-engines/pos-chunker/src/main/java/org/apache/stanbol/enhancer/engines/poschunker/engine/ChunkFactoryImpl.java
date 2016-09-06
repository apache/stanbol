/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

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
