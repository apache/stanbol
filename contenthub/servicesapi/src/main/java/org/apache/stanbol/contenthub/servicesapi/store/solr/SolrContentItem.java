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

package org.apache.stanbol.contenthub.servicesapi.store.solr;

import java.util.List;
import java.util.Map;

import org.apache.stanbol.enhancer.servicesapi.ContentItem;

/**
 * Defines a {@link ContentItem} to be stored in Solr.
 * <p>
 * Solr provides a mechanism to apply faceted search operations. Facets are passed as
 * <code>key:[value1,value2]</code> pairs. These facets are named as <b>constraints</b> within
 * {@link SolrContentItem}.
 * 
 * @author meric.taze
 * 
 */
public interface SolrContentItem extends ContentItem {

    /**
     * Retrieves the constraints (facets) which can be applied on this content item in the search operation.
     * 
     * @return A map of constraints (facets).
     */
    Map<String,List<Object>> getConstraints();

    /**
     * Retrieves the title of this content item
     * 
     * @return title {@link String}
     */
    String getTitle();
}
