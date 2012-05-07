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
package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint.MODE;

/**
 * A constraint the filters/selects based on the value and/or the parsed
 * dataTypes. A valid constraint MUST define a value OR valid data type. A
 * valid data type is defined as a String that is NOT NULL and NOT empty.<p>
 * If the collection of data types is <code>null</code> components processing
 * this constraint are encouraged to deduct the data types based on the type
 * of the value.
 * @author Rupert Westenthaler
 *
 */
public class ValueConstraint extends Constraint {

    /**
     * The mode how multiple values are treated
     */
    public static enum MODE {
        /**
         * Any of the parsed values is sufficient to select an entity. Similar
         * to UNION in SPARQL
         */
        any,
        /**
         * All parsed values must be present.
         */
        all
    };
    /**
     * The default {@link MODE} is {@link MODE#any}
     */
    public static final MODE DEFAULT_MODE = MODE.any;
    private final Set<Object> values;
    private final Collection<String> dataTypeUris;
    private final MODE mode;

    
    public ValueConstraint(Object value) {
        this(value,null);
    }
    public ValueConstraint(Object value,Iterable<String> dataTypes) {
        this(value,dataTypes,null);
    }
    public ValueConstraint(Object value,Iterable<String> dataTypes,MODE mode) {
        super(ConstraintType.value);
        if(value == null){
            this.values = null;
        } else if(value instanceof Iterable<?>){
            Set<Object> v = new LinkedHashSet<Object>();
            @SuppressWarnings("unchecked")
            Iterable<Object> values = (Iterable<Object>)value;
            for(Object val : values){
                if(val != null){
                    v.add(val);
                }
            }
            if(v.isEmpty()){
                throw new IllegalArgumentException("The values MUST BE NULL or " +
                		"contain at least a single NOT NULL value MUST BE parsed!");
            }
            this.values = Collections.unmodifiableSet(v);
        } else { //single value
            this.values = Collections.singleton(value);
        }
        /*
         * Implementation NOTE:
         *   We need to use a LinkedHashSet here to
         *    1) ensure that there are no duplicates and
         *    2) ensure ordering of the parsed constraints
         *   Both is important: Duplicates might result in necessary calculations
         *   and ordering might be important for users that expect that the
         *   dataType parsed as first is the first used for processing (e.g.
         *   when specifying acceptable data types for a field, one would expect
         *   that values that need to be converted are preferable converted to
         *   the datatype specified as first)
         */
        if(dataTypes != null){
            Set<String> dataTypeUris = new LinkedHashSet<String>();
            for(String dataType : dataTypes){
                if(dataType != null && !dataType.isEmpty()){
                    dataTypeUris.add(dataType);
                }
            }
            if(dataTypeUris.isEmpty()){
                throw new IllegalArgumentException("At least a single NOT NULL and " +
                        "not empty data type uri MUST BE parsed (NULL will trigger " +
                        "detection of the data type based on the parsed value(s))!");
            }
            this.dataTypeUris = Collections.unmodifiableSet(dataTypeUris);
        } else {
            this.dataTypeUris = null;
        }
        this.mode = mode == null ? DEFAULT_MODE : mode;
    }
    /**
     * Getter for the first parsed value
     * @return the value or <code>null</code> if the value is not constraint
     */
//    public final Object getValue() {
//        return values.iterator().next();
//    }
    /**
     * Getter for the value
     * @return the value or <code>null</code> if the value is not constraint
     */
    public final Set<Object> getValues() {
        return values;
    }
    /**
     * Getter for the {@link MODE} of this ValueConstraint
     * @return the mode
     */
    public MODE getMode() {
        return mode;
    }
    /**
     * Getter for the list of the parsed data types URIs
     * @return the list of dataType URIs or an empty list if not defined.
     */
    public final Collection<String> getDataTypes() {
        return dataTypeUris;
    }
    @Override
    public String toString() {
        return String.format("ValueConstraint[values=%s|types:%s]",values,dataTypeUris);
    }
}
