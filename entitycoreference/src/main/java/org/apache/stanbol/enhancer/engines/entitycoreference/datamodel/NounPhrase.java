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
package org.apache.stanbol.enhancer.engines.entitycoreference.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.apache.stanbol.enhancer.nlp.model.Span;

/**
 * Encapsulates span and sentence information about a noun phrase.
 * 
 * @author Cristian Petroaca
 * 
 */
public class NounPhrase {
    /**
     * The {@link Span} which represents this noun phrase.
     */
    private Span chunk;

    /*
     * TODO - should use Set instead?
     */
    /**
     * The {@link Span}s - tokens - which make up this noun phrase.
     */
    private List<Span> tokens;

    /**
     * The {@link Span}s contained in this noun phrase which represent Ners.
     */
    private List<Span> nerChunks;

    /**
     * The sentence index in which this noun phrase is found.
     */
    private int sentenceNo;

    public NounPhrase(Span chunk, int sentenceNo) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }

        this.chunk = chunk;
        this.tokens = new ArrayList<Span>();
        this.nerChunks = new ArrayList<Span>();
        this.sentenceNo = sentenceNo;
    }

    /**
     * Gets the chunk representing this noun phrase.
     * 
     * @return
     */
    public Span getChunk() {
        return chunk;
    }

    /**
     * Adds a new token which is found in this noun phrase.
     * 
     * @param token
     */
    public void addToken(Span token) {
        /*
         * TODO - validate token boundaries within this noun phrase.
         */
        tokens.add(token);
    }

    /**
     * Gets the list of tokens which make up this noun phrase.
     * 
     * @return
     */
    public List<Span> getTokens() {
        return tokens;
    }

    /**
     * Adds a new NER chunk which is found within this noun phrase.
     * 
     * @param chunk
     */
    public void addNerChunk(Span chunk) {
        /*
         * TODO - validate NER boundaries within this noun phrase.
         */
        nerChunks.add(chunk);
    }

    /**
     * Gets the list of NERs within this noun phrase.
     * 
     * @return
     */
    public List<Span> getNerChunks() {
        return nerChunks;
    }

    /**
     * Determines whether this noun phrase's {@link Span} contains the given {@link Span}.
     * 
     * @param span
     * @return
     */
    public boolean containsSpan(Span span) {
        return (span.getStart() >= chunk.getStart() && span.getEnd() <= chunk.getEnd());
    }

    /**
     * Determines whether this noun phrase has NERs.
     * 
     * @return
     */
    public boolean hasNers() {
        return nerChunks.size() > 0;
    }

    /**
     * Returns the sentence index in which this noun phrase is found.
     * 
     * @return
     */
    public int getSentenceNo() {
        return this.sentenceNo;
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + chunk.hashCode();
        result = prime * result + tokens.hashCode();
        result = prime * result + nerChunks.hashCode();

        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        NounPhrase other = (NounPhrase) obj;

        return chunk.equals(other.chunk) && tokens.equals(other.tokens) && nerChunks.equals(other.nerChunks)
               && sentenceNo == other.sentenceNo;
    }
}
