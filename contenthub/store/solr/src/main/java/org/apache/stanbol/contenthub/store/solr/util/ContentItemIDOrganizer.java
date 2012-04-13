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
package org.apache.stanbol.contenthub.store.solr.util;

/**
 * This class contains methods to manage ids of content items<br>
 * TODO: This class may be merged with
 * {@link org.apache.stanbol.enhancer.servicesapi.helper.ContentItemHelper}
 * 
 * @author suat
 * 
 */
public class ContentItemIDOrganizer {
    public static final String CONTENT_ITEM_URI_PREFIX = "urn:content-item-";

    public static String attachBaseURI(String id) {
        if (!id.startsWith("urn:")) {
            id = CONTENT_ITEM_URI_PREFIX + id;
        }
        return id;
    }

    public static String detachBaseURI(String id) {
        if (id.startsWith(CONTENT_ITEM_URI_PREFIX)) {
            id = id.replaceFirst(CONTENT_ITEM_URI_PREFIX, "");
        }
        return id;
    }
}
