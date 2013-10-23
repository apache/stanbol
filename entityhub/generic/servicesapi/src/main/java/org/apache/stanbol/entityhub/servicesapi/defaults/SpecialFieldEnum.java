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
package org.apache.stanbol.entityhub.servicesapi.defaults;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;

/**
 * Enumeration that defines fields that need to be treated specially by
 * the Stanbol Entityhub.<p>
 * All those field do use the {@link NamespaceEnum#entityhubQuery} namespace.
 * Entries of this enumeration are equals to the local name of the URIs.
 * and are also defined by the {@link RdfResourceEnum}. This enumeration is
 * intended to be used by {@link Yard} implementations to efficiently work with
 * special fields (e.g. decide if a field is a special field)
 * @author Rupert Westenthaler
 */
public enum SpecialFieldEnum {
    /**
     * The full text field. Union over all {@link Text} and String values
     * of any outgoing relation
     */
    fullText,
    /**
     * The semantic context field. Union over all {@link Reference} values 
     * of any outgoing relation
     */
    references
    ;
    
    private final String uri;
    private final String qname;
    
    private SpecialFieldEnum(){
        this.uri = NamespaceEnum.entityhubQuery.getNamespace()+name();
        this.qname = NamespaceEnum.entityhubQuery.getPrefix()+name();
    }
    
    public String getUri(){
        return uri;
    }
    public String getQName(){
        return qname;
    }
    public NamespaceEnum getNamespace(){
        return NamespaceEnum.entityhubQuery;
    }

    private static final Map<String,SpecialFieldEnum> name2field;
    static {
        Map<String,SpecialFieldEnum> map = new TreeMap<String,SpecialFieldEnum>();
        for(SpecialFieldEnum specialField : SpecialFieldEnum.values()){
            map.put(specialField.getUri(), specialField);
            map.put(specialField.getUri(), specialField);
            map.put(specialField.name(), specialField);
        }
        name2field = Collections.unmodifiableMap(map);
    }
    /**
     * Checks if the parsed name (local name or qname or full URI) is a
     * special field
     * @param name the local name, qname or URI
     * @return <code>true</code> if the parsed name references to a special field
     * or otherwise <code>false</code>
     */
    public static final boolean isSpecialField(String name){
        return getSpecialField(name) != null;
    }

    /**
     * Getter for the {@link SpecialFieldEnum} for the parsed name 
     * (local name or qname or full URI)
     * @param name the local name, qname or URI
     * @return the {@link SpecialFieldEnum} or <code>null</code> if the parsed
     * name does not refer to a special field.
     */
    public static SpecialFieldEnum getSpecialField(String name) {
        return name2field.get(name);
    }
}
