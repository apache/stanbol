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

package org.apache.stanbol.contenthub.search.engines.enhancement.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author cihan
 * 
 */
public class EnhancementPool {

    private static Map<String,EnhancementRepresentation> enhancements = new HashMap<String,EnhancementRepresentation>();
    private static Map<String,EntityRepresentation> entities = new HashMap<String,EntityRepresentation>();

    public static EnhancementRepresentation getEnhancementRepresentation(String label, String document) {
        String key = label + document;
        if (!enhancements.containsKey(key)) {
            EnhancementRepresentation er = new EnhancementRepresentation(label, document);
            enhancements.put(key, er);
        }
        return enhancements.get(key);
    }

    public static EntityRepresentation getEntityRepresentation(String ref, String label, String document) {
        String key = ref + document;
        EnhancementRepresentation er = getEnhancementRepresentation(label, document);
        if (!entities.containsKey(key)) {
            EntityRepresentation etr = new EntityRepresentation(ref);
            er.getExternalResources().add(etr);
            entities.put(key, etr);
        }
        return entities.get(key);
    }

    public static Collection<EnhancementRepresentation> getAll() {
        return enhancements.values();
    }

    public static void clear() {
        enhancements.clear();
        entities.clear();
    }
}
