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

package org.apache.stanbol.enhancer.engines.dereference;

/**
 * Exception thrown if the parsed dereference configuration is not valid.
 * Messages should indicate the {@link DereferenceContext} field as well as
 * the Dereferencer implementation
 * @author Rupert Westenthaler
 *
 */
public class DereferenceConfigurationException extends IllegalArgumentException {

    private static final long serialVersionUID = 1431844013656980310L;
    private final Class<? extends EntityDereferencer> dereferencer;
    private final String property;

    
    public DereferenceConfigurationException(String reason, 
            Class<? extends EntityDereferencer> dereferencer, String property){
        this(reason, null, dereferencer, property);
    }
    
    public DereferenceConfigurationException(Throwable cause, 
            Class<? extends EntityDereferencer> dereferencer, String property){
        this(null, cause, dereferencer, property);
    }
    
    public DereferenceConfigurationException(String reason, Throwable cause, 
            Class<? extends EntityDereferencer> dereferencer, String property){
        super(new StringBuilder("IllegalConfiguration for ")
        .append(dereferencer == null ? "Dereferencer <unkown>" : dereferencer.getClass().getSimpleName())
        .append(" and property '").append(property == null ? "<unknwon>" : property)
        .append("': ").append(reason != null ? reason : "").toString(), cause);
        this.dereferencer = dereferencer;
        this.property = property;
    }
    
    public Class<? extends EntityDereferencer> getDereferencer() {
        return dereferencer;
    }
    
    public String getProperty() {
        return property;
    }
    
}
