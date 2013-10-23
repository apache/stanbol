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
package org.apache.stanbol.commons.stanboltools.datafileprovider.impl.tracking;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Reference to a DataFile (name and bundleSymbolicName) that implements
 * {@link #hashCode()} and {@link #equals(Object)} so that it can be used
 * with the java Collections API
 * @author Rupert Westenthaler
 *
 */
public final class DataFileReference {
    
    private final String name;
    private final String bundleSymbolicName;
    private final Map<String,String> properties;

    protected DataFileReference(String name){
        this(null,name,null);
    }
    protected DataFileReference(String bundleSymbolicName, String name){
        this(bundleSymbolicName,name,null);
    }
    protected DataFileReference(String name,Map<String,String> properties){
        this(null,name,properties);
    }
    protected DataFileReference(String bundleSymbolicName, String name, Map<String,String> properties){
        if(name == null || name.isEmpty()){
            throw new IllegalStateException("The name of tracked Resources MUST NOT be NULL nor empty!");
        }
        this.name = name;
        this.bundleSymbolicName = bundleSymbolicName;
        if(properties == null){
            this.properties = Collections.emptyMap();
        } else {
            this.properties = Collections.unmodifiableMap(properties);
        }
    }
    /**
     * The name of the resource
     * @return the name (ensured to be not <code>null</code> nor empty)
     */
    public final String getName() {
        return name;
    }
    /**
     * @return the bundleSymbolicName
     */
    public final String getBundleSymbolicName() {
        return bundleSymbolicName;
    }
    @Override
    public int hashCode() {
        return name.hashCode() + (bundleSymbolicName != null ? bundleSymbolicName.hashCode() : 0);
    }
    @Override
    public boolean equals(Object o) {
        return o instanceof DataFileReference && ((DataFileReference)o).name.equals(name) &&
            (bundleSymbolicName == null ? //if null check if others is also null
                ((DataFileReference)o).bundleSymbolicName == null : //else check for equality
                    bundleSymbolicName.equals(((DataFileReference)o).bundleSymbolicName));
    }
    @Override
    public String toString() {
        return '['+name+(bundleSymbolicName != null?'@'+bundleSymbolicName : "")+']';
    }
    /**
     * @return the properties
     */
    public Map<String,String> getProperties() {
        return properties;
    }
}