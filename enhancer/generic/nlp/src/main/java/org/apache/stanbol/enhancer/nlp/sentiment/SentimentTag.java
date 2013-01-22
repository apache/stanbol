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
package org.apache.stanbol.enhancer.nlp.sentiment;

import org.apache.stanbol.enhancer.nlp.model.annotation.Value;

/**
 * The sentiment {@link #POSITIVE} or {@link SentimentTag#NEGATIVE}. The
 * value is directly represented by the {@link Value#probability()}.
 * 
 * @author Rupert Westenthaler
 *
 */
public final class SentimentTag {

    /**
     * A positive sentiment tag
     */
    public static final SentimentTag POSITIVE = new SentimentTag(true);
    /**
     * A negative sentiment tag
     */
    public static final SentimentTag NEGATIVE = new SentimentTag(false);

    /**
     * positive if <code>true</code> otherwise negative.
     */
    private final boolean positive;

    /**
     * Singleton constructor
     */
    private SentimentTag(boolean positive){
        this.positive = positive;
    }
    
    /**
     * If the {@link Value#probability() sentiment} is positive
     */
    public final boolean isPositive() {
        return positive;
    }
    
    /**
     * If the {@link Value#probability() sentiment} is negative
     */
    public final boolean isNegative() {
        return !positive;
    }
    
    
}
