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

/**
 * This interface contains "getter" methods for a search result that can be passed in a {@link SearchResult}
 * object. A resultant document corresponds to a content item stored in Contenthub.
 * 
 * @author suat
 * 
 */
public interface DocumentResult {
    /**
     * Returns the URI of the content item corresponding to this search result.
     * 
     * @return URI of the search result
     */
    String getLocalId();

    /**
     * Returns the dereferencable URI of the content item corresponding to this search result. This URI of the
     * HTML interface of the content item.
     * 
     * @return Dereferencable URI of the search result
     */
    String getDereferencableURI();

    /**
     * Returns the mime type of the content item corresponding to this search result
     * 
     * @return Mime type of the search result
     */
    String getMimetype();

    /**
     * Returns the count of the enhancements of the content item corresponding to this search result
     * 
     * @return Enhancement count of the search result
     */
    long getEnhancementCount();

    /**
     * Returns the title of the content item corresponding to the this search result
     * 
     * @return Title of the search result
     */
    String getTitle();
}
