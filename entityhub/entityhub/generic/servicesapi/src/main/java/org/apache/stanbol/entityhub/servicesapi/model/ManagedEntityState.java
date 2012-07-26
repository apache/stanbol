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
package org.apache.stanbol.entityhub.servicesapi.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * Enumeration that defines the different states of Entities managed by
 * the Entityhub
 * @author Rupert Westenthaler
 *
 */
public enum ManagedEntityState {
    /**
     * This symbol is marked as removed
     */
    removed(RdfResourceEnum.entityStateRemoved.getUri()),
    /**
     * This symbol should no longer be moved. Usually there are one or more
     * new symbols that should be used instead of this one. See
     * {@link Symbol#getSuccessors()} for more information
     */
    depreciated(RdfResourceEnum.entityStateDepreciated.getUri()),
    /**
     * Indicates usually a newly created {@link Symbol} that needs some kind
     * of confirmation.
     */
    proposed(RdfResourceEnum.entityStateProposed.getUri()),
    /**
     * Symbols with that state are ready to be used.
     */
    active(RdfResourceEnum.entityStateActive.getUri()),
    ;
    private String uri;
    ManagedEntityState(String uri){
        this.uri = uri;
    }
    public String getUri(){
        return uri;
    }
    @Override
    public String toString() {
        return uri;
    }
    private static Map<String,ManagedEntityState> URI_TO_STATE;
    static {
        Map<String, ManagedEntityState> mappings = new HashMap<String, ManagedEntityState>();
        for(ManagedEntityState state : ManagedEntityState.values()){
            mappings.put(state.getUri(), state);
        }
        URI_TO_STATE = Collections.unmodifiableMap(mappings);
    }
    /**
     * Getter for the State based on the URI.
     * @param uri the URI
     * @return the State
     * @throws IllegalArgumentException if the parsed URI does not represent
     * a state
     */
    public static ManagedEntityState getState(String uri) throws IllegalArgumentException{
        ManagedEntityState state = URI_TO_STATE.get(uri);
        if(state == null){
            throw new IllegalArgumentException(String.format(
                "Unknown SymbolState URI %s (supported states URIs: %s)",
                uri,URI_TO_STATE.keySet()));
        }
        return state;
    }
    public static boolean isState(String uri){
        return URI_TO_STATE.containsKey(uri);
    }

}