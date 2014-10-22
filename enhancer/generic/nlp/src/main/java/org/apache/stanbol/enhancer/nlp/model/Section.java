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
import java.util.Set;

/**
 * A {@link Span} that may enclose other Spans. Super type for {@link Chunk}s,
 * {@link Sentence}s and {@link AnalysedText}.<p>
 * As {@link Span} this is an meta (abstract) type. Implementations of this
 * Interface SHOULD BE abstract Classes. 
 */
public interface Section extends Span {

    /**
     * Iterates over all Span enclosed by this one that are of any of the
     * parsed Types.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @param types the {@link SpanTypeEnum types} of Spans included
     * @return sorted iterator over the selected Spans.
     */
    Iterator<Span> getEnclosed(Set<SpanTypeEnum> types);

    /**
     * Iterates over all enclosed Span within the parsed window. Only Spans
     * with on of the parsed types are returned. 
     * <p> 
     * The parsed window (start/end indexes) are relative to the section. If
     * the parsed window exceeds the Section the window adapted to the section.
     * This means that this method will never return Spans outside the section.
     * <p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @param types the {@link SpanTypeEnum types} of Spans included
     * @param startOffset the start offset relative to the start position of this {@link Section}
     * @param endOffset the end offset relative to the start position of this {@link Section}.
     * @return sorted iterator over the selected Spans.
     * @since 0.12.1
     */
    Iterator<Span> getEnclosed(Set<SpanTypeEnum> types, int startOffset, int endOffset);
    
    /**
     * Adds an Token relative to this Sentence
     * @param start the start of the token relative to the sentence
     * @param end
     * @return
     */
    Token addToken(int start, int end);

    /**
     * The Tokens covered by this Sentence.<p>
     * Returned Iterators MUST NOT throw {@link ConcurrentModificationException}
     * but consider additions of Spans.
     * @return the tokens
     */
    Iterator<Token> getTokens();

}
