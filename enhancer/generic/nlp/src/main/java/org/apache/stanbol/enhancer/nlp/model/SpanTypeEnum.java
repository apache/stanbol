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

/**
 * Enumeration over different types - or roles - spans defined for an
 * {@link AnalysedText} may play.
 */
public enum SpanTypeEnum {
    /**
     * The Text as a whole
     */
    Text,
    /**
     * An section of the text (chapter, page, paragraph ...). NOTE: this
     * does NOT define types of sections.
     */
    TextSection,
    /**
     * An Sentence
     */
    Sentence,
    /**
     * A Chunk (e.g. a Noun Phrase) NOTE: this does NOT define types of
     * Chunks
     */
    Chunk,
    /**
     * A Token (e.g. a noun, verb, punctuation) NOTE: this does NOT define
     * types of Tokens
     */
    Token;
}