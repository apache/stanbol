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
package org.apache.stanbol.contenthub.servicesapi.search.featured;

import java.util.List;

import org.apache.solr.client.solrj.response.FacetField.Count;

/**
 * This interface defines the structure of facets that are obtained from underlying Solr index for the search
 * operation
 * 
 * @author suat
 * 
 */
public interface FacetResult {
    /**
     * Returns the full name of the facet.
     * 
     * @return
     */
    String getName();

    /**
     * Returns the name of the facet to be used in the HTML interface.
     * 
     * @return
     */
    String getHtmlName();

    /**
     * Returns values regarding this facet in a {@link List} of {@link Count}s.
     * 
     * @return
     */
    List<Count> getValues();
}
