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
package org.apache.stanbol.entityhub.jersey.grefine;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;

/**
 * Values can be simple JSON values or JSON objects with an 'id' and a
 * 'name'. This is mapped to {@link ReconcileValue} objects with an optional 
 * {@link #getId()} and a required {@link #getValue()}.<p>
 * The 'id' supports prefix:localname syntax for prefixes defined within the
 * {@link NamespaceEnum}
 * @author Rupert Westenthaler
 *
 */
public class ReconcileValue {
    private final String id;
    private final Object value;

    ReconcileValue(Object value){
        this(null,value);
    }
    ReconcileValue(String id, Object value){
        this.id = id;
        if(value == null){
            throw new IllegalArgumentException("The parsed value MUST NOT be NULL!");
        }
        this.value = value;
    }

    /**
     * The getter for the value of the 'id' property of the 'v' object
     * if present. This represents the value of fields that are already
     * successfully linked (reconciled) with some entity.
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }
    /**
     * Calls the {@link #toString()} method of the {@link #getValue()}
     */
    @Override
    public String toString() {
        return value.toString();
    }
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : value.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof ReconcileValue && ( //other is value
                (id != null && id.equals(((ReconcileValue) o).id)) || //ids are equals or 
                    (id == null && ((ReconcileValue)o).id == null //ids are null and
                    && value.equals(((ReconcileValue)o).value))); //values are equals
        
    }
}