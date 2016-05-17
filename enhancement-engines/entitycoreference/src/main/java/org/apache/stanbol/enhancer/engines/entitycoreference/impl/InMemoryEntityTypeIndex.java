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
package org.apache.stanbol.enhancer.engines.entitycoreference.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.clerezza.commons.rdf.IRI;

/**
 * Memory cache for storing often used Entity Type (Class) information.
 * 
 * @author Cristian Petroaca
 * 
 */
public class InMemoryEntityTypeIndex {
    /**
     * The index having as key the Uri of the class and the value the set of labels ordered by language.
     */
    private Map<IRI,Map<String,Set<String>>> index;

    public InMemoryEntityTypeIndex() {
        index = new HashMap<IRI,Map<String,Set<String>>>();
    }

    /**
     * Searches for a given class URI for the given language.
     * 
     * @param uri
     * @param language
     * @return
     */
    public Set<String> lookupEntityType(IRI uri, String language) {
        Map<String,Set<String>> langMap = index.get(uri);

        if (langMap != null) {
            return langMap.get(language);
        }

        return null;
    }

    /**
     * Adds a new class URI's labels for the given language.
     * 
     * @param uri
     * @param language
     * @param labels
     */
    public void addEntityType(IRI uri, String language, Set<String> labels) {
        Map<String,Set<String>> langMap = index.get(uri);

        if (langMap == null) {
            langMap = new HashMap<String,Set<String>>();
            index.put(uri, langMap);
        }

        langMap.put(language, labels);
    }
}
