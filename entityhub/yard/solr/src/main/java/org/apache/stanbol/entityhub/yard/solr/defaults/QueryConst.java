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
package org.apache.stanbol.entityhub.yard.solr.defaults;

import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.yard.solr.impl.SolrYard;


/**
 * Defines parameters used by the {@link FieldQuery} implementation of the
 * SolrYard. Some of those might also be supported by the {@link SolrYard}
 * configuration to set default values<p>
 * 
 * @author Rupert Westenthaler
 *
 */
public final class QueryConst {
    private QueryConst(){/*do not allow instances*/}
    
    /**
     * Property allowing to enable/disable the generation of Phrase queries for
     * otional query terms (without wildcards). Values are expected to be
     * {@link Boolean}
     */
    public static final String PHRASE_QUERY_STATE = "stanbol.entityhub.yard.solr.query.phraseQuery";
    /**
     * The default state for the {@link #PHRASE_QUERY_STATE} (default: false)
     */
    public static final Boolean DEFAULT_PHRASE_QUERY_STATE = Boolean.FALSE;
    /**
     * Property allowing to set a query time boost for certain query terms.
     * Values are expected to be floating point values grater than zero.
     */
    public static final String QUERY_BOOST = "stanbol.entityhub.yard.solr.query.boost";
}
