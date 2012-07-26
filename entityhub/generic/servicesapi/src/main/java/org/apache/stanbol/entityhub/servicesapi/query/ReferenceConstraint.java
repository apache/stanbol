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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.defaults.DataTypeEnum;


public class ReferenceConstraint extends ValueConstraint {

    /**
     * The references. Same as returned by {@link ValueConstraint#getValues()}
     * but in a Set of generic type string
     */
    private Set<String> references;
    
    public ReferenceConstraint(String reference) {
        this(reference != null ? Collections.singleton(reference) : null);
    }
    public ReferenceConstraint(Collection<String> references) {
        this(references,null);
    }
    public ReferenceConstraint(Collection<String> references, MODE mode) {
        super(references,Arrays.asList(DataTypeEnum.Reference.getUri()),mode);
        if(references == null){
            throw new IllegalArgumentException("Parsed Reference(s) MUST NOT be NULL");
        }
        //we need to copy the values over, because in Java one can not cast
        //Set<Object> to Set<String>
        Set<String> r = new LinkedHashSet<String>(getValues().size());
        for(Object value : getValues()){
            r.add((String)value);
        }
        this.references = Collections.unmodifiableSet(r);
    }

//    /**
//     * Getter for the first parsed Reference
//     * @return the reference
//     */
//    public String getReference() {
//        return (String)getValue();
//    }
    /**
     * Getter for the Reference
     * @return the reference
     */
    public Set<String> getReferences() {
        return references;
    }


}
