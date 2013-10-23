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
package org.apache.stanbol.commons.jsonld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.codehaus.jettison.json.JSONArray;

public class JsonLdProfile extends JsonLdCommon {

    private Map<String,List<String>> typesMap = new HashMap<String,List<String>>();

    public void addType(String property, String type) {
        if (this.typesMap.get(property) == null) {
            this.typesMap.put(property, new ArrayList<String>());
        }
        List<String> types = this.typesMap.get(property);
        types.add(type);
    }
    
    public void addTypes(String property, List<String> types) {
        if (this.typesMap.get(property) == null) {
            this.typesMap.put(property, new ArrayList<String>());
        }
        this.typesMap.get(property).addAll(types);
    }

    public List<String> getTypes(String property) {
        return this.typesMap.get(property);
    }
    
    public Set<String> getTypes() {
        return typesMap.keySet();
    }

    public String toString() {
        return JsonSerializer.toString(this.createJsonMap());
    }

    public String toString(int indent) {
        return JsonSerializer.toString(this.createJsonMap(), indent);
    }
    
    private Map<String,Object> createJsonMap() {
        Map<String,Object> json = null;
        try {
            json = createJson();
        } catch (ShorteningException e) {
            // problems while using the shortening algorithm
            this.setUseCuries(true);
            this.usedNamespaces.clear();
            try {
                json = createJson();
            } catch (ShorteningException e1) {
                // ignore this
            }
        }
        
        return json;
    }

    private Map<String,Object> createJson() throws ShorteningException {
        Map<String,Object> json = new TreeMap<String,Object>(new JsonComparator());

        // put the namespaces
        Map<String,Object> contextObject = new TreeMap<String,Object>(new JsonComparator());
        for (String ns : namespacePrefixMap.keySet()) {
            contextObject.put(namespacePrefixMap.get(ns), ns);
        }

        // put types
        Map<String,Object> typesObject = new TreeMap<String,Object>(new JsonComparator());
        for (String property : this.typesMap.keySet()) {
            List<String> types = this.typesMap.get(property);
            if (types.size() == 1) {
                typesObject.put(shortenURI(property), shortenURI(types.get(0)));
            } else {
                JSONArray typesArray = new JSONArray();
                for (String type : types) {
                    typesArray.put(shortenURI(type));
                }
                typesObject.put(shortenURI(property), typesArray);
            }
        }
        contextObject.put(JsonLdCommon.TYPES, typesObject);

        json.put(JsonLdCommon.CONTEXT, contextObject);

        return json;
    }
}
