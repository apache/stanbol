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

package org.apache.stanbol.contenthub.servicesapi.store.vocabulary;

import java.util.Date;

/**
 * Vocabulary class which provides constant properties to be used in the communication with Solr. Most of
 * these properties point to the fields defined in the <b>schema.xml</b> of Solr.
 * 
 * @author anil.sinaci
 * 
 */
public class SolrVocabulary {

    /**
     * Name of the unique ID field.
     */
    public static final String SOLR_FIELD_NAME_ID = "id";

    /**
     * Name of the field which holds the actual content.
     */
    public static final String SOLR_FIELD_NAME_CONTENT = "content";

    /**
     * Name of the field which holds the mime type (content type) of the content.
     */
    public static final String SOLR_FIELD_NAME_MIMETYPE = "mimeType";

    /**
     * Ending characters for dynamic fields of {@link String} type.
     */
    public static final String SOLR_DYNAMIC_FIELD_TEXT = "_t";

    /**
     * Ending characters for dynamic fields of {@link Long} type.
     */
    public static final String SOLR_DYNAMIC_FIELD_LONG = "_l";

    /**
     * Ending characters for dynamic fields of {@link Double} type.
     */
    public static final String SOLR_DYNAMIC_FIELD_DOUBLE = "_d";

    /**
     * Ending characters for dynamic fields of {@link Date} type.
     */
    public static final String SOLR_DYNAMIC_FIELD_DATE = "_dt";

    /**
     * "OR" keyword for Solr queries.
     */
    public static final String SOLR_OR = " OR ";
}
