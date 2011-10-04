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

package org.apache.stanbol.contenthub.servicesapi.search.execution;

/**
 * This is a base interface indicating the score of the resources. Search operation assigns a score to the
 * search results regardless of its type (i.e. {@link DocumentResource}, {@link ClassResource},
 * {@link IndividualResource}).
 * 
 * @author cihan
 * 
 */
public abstract interface Scored {

    /**
     * Returns the score of the resource.
     * 
     * @return The score of the resource.
     */
    double getScore();

    // TODO Freemarker rounds doubles automatically, this function will be removed once a solution is found
    String getScoreString();

    /**
     * Updates the score of the resource based on a weight. The score is updated according to a weight based
     * calculation.
     * 
     * @param score
     *            New score.
     * @param weight
     *            The weight of the new score.
     */
    void updateScore(Double score, Double weight);

}
