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
import java.util.List;

public class JsonLdProperty {

    private String name;
    
    private List<JsonLdPropertyValue> values = new ArrayList<JsonLdPropertyValue>();
    
    private String type;
    
    public JsonLdProperty(String name) {
        this.name = name;
    }
    
    public JsonLdProperty(String name, Object value) {
        this.name = name;
        this.values.add(new JsonLdPropertyValue(value));
    }
    
    public String getName() {
        return name;
    }
    
    public void addValue(JsonLdPropertyValue value) {
        this.values.add(value);
    }

    public void addSingleValue(Object value) {
        this.values.add(new JsonLdPropertyValue(value));
    }
    
    public List<JsonLdPropertyValue> getValues() {
        return this.values;
    }

    public String getType() {
        if (type == null) {
            // Compute the type by examine the value types
            boolean first = true;
            for (JsonLdPropertyValue value : this.values) {
                if (value.getType() != null) {
                    if (first) {
                        this.type = value.getType();
                        first = false;
                    }
                    else if (!this.type.equals(value.getType())) {
                        type = null;
                        break;
                    }
                }
            }
        }
        else {
            // If any value has another type than specified
            // by this type, we return NULL because
            // it's a multityped property.
            for (JsonLdPropertyValue value : this.values) {
                if (value.getType() != null) {
                    if (!type.equals(value.getType())) {
                        type = null;
                        break;
                    }
                }
            }
        }
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSingleValued() {
        return this.values.size() == 1;
    }
    
    public boolean isTyped() {
        return this.type != null;
    }
}
