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
package org.apache.stanbol.entityhub.indexing.core.normaliser;

import java.util.Map;

/**
 * This Interface provides the possibility to process score values provided for
 * Entities (e.g. to calculate the pageRank based on the number of incomming links
 * @author Rupert Westenthaler
 *
 */
public interface ScoreNormaliser {
    /**
     * Key used to configure an other {@link ScoreNormaliser} instance that
     * should be called before this instance processes the score. Values
     * MUST BE of type {@link ScoreNormaliser}.
     */
    String CHAINED_SCORE_NORMALISER = "chained";
    
    /**
     * -1 will be used a lot with implementations of this interface
     */
    public static final Float MINUS_ONE = Float.valueOf(-1f);
    /**
     * 0 will be used a lot with implementations of this interface
     */
    public static final Float ZERO = Float.valueOf(0f);

    void setConfiguration(Map<String,Object> config);
    /**
     * Normalises the parsed score value based on some algorithm.
     * @param score The score to normalise. <code>null</code> and values &lt; 0
     * MUST be ignored and returned as parsed.
     * @return <code>null</code> if no score can be calculated based on the
     * parsed value (especially if <code>null</code> is parsed as score).
     * Otherwise the normalized score where values &lt;0 indicate that the
     * entity should not be indexed.
     */
    Float normalise(Float score);

    /**
     * Returns the {@link ScoreNormaliser} instance that is chained to this one.
     * Parsed scores are first parsed to chained instances before they are
     * processed by current one.
     * @return the chained instance or <code>null</code> if none
     */
    ScoreNormaliser getChained();
}
