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
package org.apache.stanbol.enhancer.topic.api.training;

import org.apache.commons.lang.StringUtils;

import java.util.Collection;

/**
 * Data transfer object to pack the items of a multi-label text classification training set.
 */
public class Example {

    /**
     * Unique id of the document
     */
    public final String id;

    /**
     * Identifier of the labels (categories, topics, tags...) of the document. This is the target signal to
     * predict.
     * 
     * In practice this is expected to be a collection of String items but we do not enforce the cast to avoid
     * the GC overhead and be able to work with the native data-structures returned by SolrJ.
     */
    public final Collection<Object> labels;

    /**
     * Text fields of the document (could be headers, paragraphs, text extractions of PDF files...). Any
     * markup is assumed to have been cleaned up in some preprocessing step.
     * 
     * In practice this is expected to be a collection of String items but we do not enforce the cast to avoid
     * the GC overhead and be able to work with the native data-structures returned by SolrJ.
     */
    public final Collection<Object> contents;

    public Example(String id, Collection<Object> labelValues, Collection<Object> textValues) {
        this.id = id;
        this.labels = labelValues;
        this.contents = textValues;
    }

    /**
     * @return concatenated version of the content fields.
     */
    public String getContentString() {
        return StringUtils.join(contents, "\n\n");
    }
}
