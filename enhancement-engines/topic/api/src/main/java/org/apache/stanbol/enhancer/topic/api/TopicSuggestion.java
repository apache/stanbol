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
package org.apache.stanbol.enhancer.topic.api;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Data transfer object for the individual topic classification results.
 */
public class TopicSuggestion {

    /**
     * The URI of the concept in the hierarchical conceptual scheme (that holds the broader relationship)
     */
    public final String conceptUri;

    /**
     * Reference to the broader concepts of this suggestion.
     */
    public final List<String> broader = new ArrayList<String>();

    /**
     * The (optional) URI of a resource that grounds this concepts in the real world. Can be null.
     */
    public final String primaryTopicUri;

    /**
     * The (positive) score of the suggestion: higher is better. Zero would mean unrelated. The absolute value
     * is meaningless: suggestions scores cannot be compared across different input text documents nor
     * distinct concept schemes.
     */
    public final float score;

    public TopicSuggestion(String conceptUri,
                           String primaryTopicUri,
                           Collection<? extends Object> broader,
                           float score) {
        this.conceptUri = conceptUri;
        this.primaryTopicUri = primaryTopicUri;
        if (broader != null) {
            for (Object broaderConcept : broader) {
                this.broader.add(broaderConcept.toString());
            }
        }
        this.score = score;
    }

    public TopicSuggestion(String conceptUri, float score) {
        this(conceptUri, null, null, score);
    }

    @Override
    public String toString() {
        return String.format("TopicSuggestion(\"%s\", [%s], %f)", conceptUri,
            StringUtils.join(broader, "\", \""), score);
    }
}
