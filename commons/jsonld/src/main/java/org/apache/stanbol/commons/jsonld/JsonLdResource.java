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

/**
 * @author Fabian Christ
 * 
 */
public class JsonLdResource {

    private String subject;
    private String profile;
    private List<String> types = new ArrayList<String>();
    
    // maps property names to JsonLD property objects
    private Map<String,JsonLdProperty> propertyMap = new HashMap<String,JsonLdProperty>();
    
    // map property names to types in case of singlevalued properties
    private Map<String,String> coercionMap = new HashMap<String,String>();

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public void addType(String type) {
        types.add(type);
    }

    public void addAllTypes(List<String> types) {
        this.types.addAll(types);
    }
    
    public List<String> getTypes() {
        return types;
    }

    public void putPropertyType(String property, String type) {
        JsonLdProperty p = this.propertyMap.get(property);
        if (p != null) {
            p.setType(type);
        }
        this.coercionMap.put(property, type);
    }

    public String getTypeOfProperty(String property) {
        return this.propertyMap.get(property).getType();
    }

    public Map<String,String> getCoerceMap() {
        return this.coercionMap;
    }

    public void putProperty(String property, Object value) {
        JsonLdProperty jldProperty = this.propertyMap.get(property);
        if (jldProperty == null) {
            jldProperty = new JsonLdProperty(property, value);
            String coercedType = this.coercionMap.get(property);
            jldProperty.setType(coercedType);
            propertyMap.put(property, jldProperty);
        }
        else {
            jldProperty.addSingleValue(value);
        }
    }
    
    public void putProperty(JsonLdProperty property) {
        String type = property.getType();
        if (type != null) {
            this.coercionMap.put(property.getName(), type);
        }
        else {
            this.coercionMap.remove(property.getName());
        }
        this.propertyMap.put(property.getName(), property);
    }

    public JsonLdProperty getPropertyValueIgnoreCase(String property) {
        for (String p : this.propertyMap.keySet()) {
            if (p.equalsIgnoreCase(property)) {
                return this.propertyMap.get(p);
            }
        }
        return null;
    }
    
    public JsonLdProperty getProperty(String property) {
        return this.propertyMap.get(property);
    }
    
    public Map<String,JsonLdProperty> getPropertyMap() {
        return this.propertyMap;
    }

    public boolean hasPropertyIgnorecase(String property) {
        for (String p : this.propertyMap.keySet()) {
            if (p.equalsIgnoreCase(property)) {
                return true;
            }
        }

        return false;
    }
}
