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
import java.util.LinkedHashSet;

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


    private final Object value;
    private final Collection<String> dataTypeUris;

    public ValueConstraint(Object value) {
        this(value,null);
    }
    public ValueConstraint(Object value,Iterable<String> dataTypes) {
        super(ConstraintType.value);
        this.value = value;
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
        this.dataTypeUris = new LinkedHashSet<String>();
        if(dataTypes != null){
            for(String dataType : dataTypes){
                if(dataType != null && !dataType.isEmpty()){
                    dataTypeUris.add(dataType);
                }
            }
        }
        if(value == null && dataTypeUris.isEmpty()){
            throw new IllegalArgumentException("A value constraint MUST define at least a value or a valid - NOT NULL, NOT empty - data type uri!");
        }
        //it's questionable if we should do that at this position, because
        //components that process that constraint might have better ways to
        //do that and than they can not know if the user parsed a data type or
        //this code has calculated it based on the java type of the value!
//        if(dataTypeUris.isEmpty()){ //meaning value != null
//            for(DataTypeEnum dataType : DataTypeEnum.getAllDataTypes(value.getClass())){
//                dataTypeUris.add(dataType.getUri());
//            }
//        }
    }
    /**
     * Getter for the value
     * @return the value or <code>null</code> if the value is not constraint
     */
    public final Object getValue() {
        return value;
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
        return String.format("ValueConstraint[value=%s|types:%s]",value,dataTypeUris);
    }
}
