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

import org.apache.stanbol.enhancer.nlp.model.annotation.Annotated;

/**
 * Represents a {@link #getSpan() span} [{@link #getStart() start},
 * {@link #getEnd() end}] within the {@link #getContext() text}. Spans also have
 * an assigned {@link #getType() type}. Possible types are defined within the
 * {@link SpanTypeEnum}.<p>
 * This is an meta (abstract) type. Implementations of this Interface 
 * SHOULD BE abstract Classes.
 */
public interface Span extends Annotated, Comparable<Span>{

    /**
     * The type of the Span
     * @return
     */
    SpanTypeEnum getType();

    /**
     * The start index of this span This is the absolute offset from the
     * {@link #getContext()}{@link AnalysedText#getText() .getText()}
     */
    int getStart();
    /**
     * The end index of this span. This is the absolute offset from the
     * {@link #getContext()}{@link AnalysedText#getText() .getText()}
     * @return
     */
    int getEnd();

    /**
     * The {@link AnalysedText} this Span was added to.
     * @return the AnalysedText representing the context of this Span
     */
    AnalysedText getContext();
    /**
     * The section of the text selected by this span
     * @return the selected section of the text
     */
    String getSpan();
    
}