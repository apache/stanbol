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
package org.apache.stanbol.enhancer.servicesapi;

import java.util.Map;

/**
 * Stanbol Enhancer components might implement this interface to parse additional
 * properties to other components.
 *
 * @author Rupert Westenthaler
 */
public interface ServiceProperties {

    /**
     * Getter for the properties defined by this service.
     * @return An unmodifiable map of properties defined by this service
     */
    Map<String,Object> getServiceProperties();

    //TODO review the definition of constants
    /**
     * Property Key used to define the order in which {@link EnhancementEngine}s are
     * called by the {@link EnhancementJobManager}. This property expects a
     * single {@link Integer} as value
     */
    String ENHANCEMENT_ENGINE_ORDERING = "org.apache.stanbol.enhancer.engine.order";

    /**
     * Ordering values >= this value indicate, that an enhancement engine
     * dose some pre processing on the content
     */
    Integer ORDERING_PRE_PROCESSING = 200;

    /**
     * Ordering values < {@link ServiceProperties#ORDERING_PRE_PROCESSING} and
     * >= this value indicate, that an enhancement engine performs operations
     * that are only dependent on the parsed content.<p>
     * <b>NOTE:</b> the NLP processing specific orderings that are defined
     * within this span
     * @see #ORDERING_NLP_LANGAUGE_DETECTION
     * @see #ORDERING_NLP_SENTENCE_DETECTION
     * @see #ORDERING_NLP_TOKENIZING
     * @see #ORDERING_NLP_POS
     * @see #ORDERING_NLP_CHUNK
     * @See #ORDERING_NLP_LEMMATIZE
     */
    Integer ORDERING_CONTENT_EXTRACTION = 100;

    /**
     * Ordering values < {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     * and >= this value indicate, that an enhancement engine performs operations
     * on extracted features of the content. It can also extract additional
     * enhancement by using the content, but such features might not be
     * available to other engines using this ordering range
     */
    Integer ORDERING_EXTRACTION_ENHANCEMENT = 1;

    /**
     * The default ordering uses {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}
     * -1 . So by default EnhancementEngines are called after all engines that
     * use an value within the ordering range defined by
     * {@link ServiceProperties#ORDERING_EXTRACTION_ENHANCEMENT}
     */
    Integer ORDERING_DEFAULT = 0;

    /**
     * Ordering values < {@link ServiceProperties#ORDERING_DEFAULT} and >= this
     * value indicate that an enhancement engine performs post processing
     * operations on existing enhancements.
     */
    Integer ORDERING_POST_PROCESSING = -100;
    
    /* -------
     * NLP processing orderings (all within the ORDERING_CONTENT_EXTRACTION range
     * -------
     */
    /**
     * Ordering values < {@link #ORDERING_PRE_PROCESSING} and >=
     * {@link #ORDERING_NLP_LANGAUGE_DETECTION} are reserved for engines that detect
     * the language of an content
     */
    Integer ORDERING_NLP_LANGAUGE_DETECTION = ServiceProperties.ORDERING_CONTENT_EXTRACTION + 90;
    /**
     * Ordering values < {@link #ORDERING_NLP_LANGAUGE_DETECTION} and >=
     * {@link #ORDERING_NLP_SENTENCE_DETECTION} are reserved for engines that extract
     * sections within the text content
     */
    Integer ORDERING_NLP_SENTENCE_DETECTION = ServiceProperties.ORDERING_CONTENT_EXTRACTION + 80;
    /**
     * Ordering values < {@link #ORDERING_NLP_SENTENCE_DETECTION} and >=
     * {@link #ORDERING_NLP_TOKENIZING} are reserved for engines that tokenize
     * the text
     */
    Integer ORDERING_NLP_TOKENIZING = ServiceProperties.ORDERING_CONTENT_EXTRACTION + 70;
    /**
     * Ordering values < {@link #ORDERING_NLP_TOKENIZING} and >=
     * {@link #ORDERING_NLP_POS} are reserved for engines that perform
     * POS (Part of Speech) tagging
     */
    Integer ORDERING_NLP_POS = ServiceProperties.ORDERING_CONTENT_EXTRACTION + 60;
    /**
     * Ordering values < {@link #ORDERING_NLP_POS} and >=
     * {@link #ORDERING_NLP_CHUNK} are reserved for engines that annotate
     * Chunks (such as Noun Phrases) in an text.
     */
    Integer ORDERING_NLP_CHUNK = ServiceProperties.ORDERING_CONTENT_EXTRACTION + 50;
    /**
     * Ordering values < {@link #ORDERING_NLP_CHUNK} and >=
     * {@link #ORDERING_NLP_LEMMATIZE} are reserved for engines that lemmatize
     * texts.<p>
     */
    Integer ORDERING_NLP_LEMMATIZE = ServiceProperties.ORDERING_CONTENT_EXTRACTION + 40;
    /**
     * Ordering values < {@link #ORDERING_NLP_LEMMATIZE} and >=
     * {@link #ORDERING_NLP_NER} are reserved for engines that do perform 
     * Named Entity Recognition (NER)<p>
     */
    Integer ORDERING_NLP_NER = ServiceProperties.ORDERING_CONTENT_EXTRACTION + 30;
}
