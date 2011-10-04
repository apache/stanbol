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

package org.apache.stanbol.contenthub.core.utils;

import org.apache.stanbol.contenthub.servicesapi.search.vocabulary.SearchVocabulary;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.Filter;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * 
 * @author cihan
 * 
 */
public final class Filters {

    public static final Filter<Resource> CLASS_RESOURCE_FILTER = new Filter<Resource>() {
        @Override
        public boolean accept(Resource o) {
            return o.hasProperty(RDF.type, SearchVocabulary.CLASS_RESOURCE);
        }
    };

    public static final Filter<Resource> INDIVIDUAL_RESOURCE_FILTER = new Filter<Resource>() {
        @Override
        public boolean accept(Resource o) {
            return o.hasProperty(RDF.type, SearchVocabulary.INDIVIDUAL_RESOURCE);
        }
    };

    public static final Filter<Resource> DOCUMENT_RESOURCE_FILTER = new Filter<Resource>() {
        @Override
        public boolean accept(Resource o) {
            return o.hasProperty(RDF.type, SearchVocabulary.DOCUMENT_RESOURCE);
        }
    };

    public static final Filter<Resource> EXTERNAL_RESOURCE_FILTER = new Filter<Resource>() {
        @Override
        public boolean accept(Resource o) {
            return o.hasProperty(RDF.type, SearchVocabulary.EXTERNAL_RESOURCE);
        }
    };

    private Filters() {

    }
}
