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
package org.apache.stanbol.enhancer.nlp.model;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.apache.clerezza.commons.rdf.IRI;
import org.apache.stanbol.enhancer.servicesapi.Blob;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;

/**
 * Provides access to NLP processing results of the <code>text/plain</code>
 * {@link Blob} of an ContentItem. Intended to be
 * {@link ContentItem#addPart(org.apache.clerezza.commons.rdf.IRI, Object) added
 * as ContentPart} by using {@link #ANALYSED_TEXT_URI}.
 * @see ContentItem#addPart(IRI, Object)
 */
public interface AnalysedText extends Section{

    
    /**
     * The {@link IRI} used to register the {@link AnalysedText} instance
     * as {@link ContentItem#addPart(org.apache.clerezza.commons.rdf.IRI, Object) 
     * ContentPart} to the {@link ContentItem}
     */
    public static final IRI ANALYSED_TEXT_URI = new IRI("urn:stanbol.enhancer:nlp.analysedText");

    /**
     * Returns {@link SpanTypeEnum#Text}
     * @see Span#getType()
     * @see SpanTypeEnum#Text
     */
    SpanTypeEnum getType();

    /**
     * Adds an Sentence
     * @param start the start index
     * @param end the end index
     * @return the Sentence
     */
    Sentence addSentence(int start, int end);

    /**
     * Adds an Chunk
     * @param start the start of the chunk
     * @param end
     * @return
     */
    Chunk addChunk(int start, int end);

    /**
     * All sentences of the Analysed texts.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return
     */
    Iterator<Sentence> getSentences();

    /**
     * All Chunks of this analysed text.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the chunks
     */
    Iterator<Chunk> getChunks();

    /**
     * Getter for the text.
     * @return 
     */
    CharSequence getText();
    
    /**
     * The analysed {@link Blob}. Typically {@link Blob#getMimeType()} will be
     * <code>text/plain</code>.
     * @return the analysed {@link Blob} instance.
     */
    Blob getBlob();
    
    
}