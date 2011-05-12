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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

public interface EntityMapping extends Sign {
    /**
     * The default state for newly created instances if not otherwise configured
     */
    MappingState DEFAULT_MAPPING_STATE = MappingState.proposed;
    /**
     * Enumeration that defines the different states of {@link EntityMapping}
     * instances.
     * @author Rupert Westenthaler
     *
     */
    enum MappingState {
        /**
         * Mapping the entity to the symbol was rejected by some user/process.
         * Such mappings MUST NOT be used in any application context other than
         * some administrative interfaces.
         */
        rejected(RdfResourceEnum.mappingStateRejected.getUri()),
        /**
         * Indicated, that a mapping of the entity to the symbol is proposed.
         * Such mappings still wait for some kind of confirmation to be fully
         * established. Based on the application context it might already be
         * OK to used them.
         */
        proposed(RdfResourceEnum.mappingStateProposed.getUri()),
        /**
         * This indicates that this mapping has expired. This indicated, that
         * this mapping was once {@link MappingState#confirmed} but now waits for
         * some confirmation activity. Based on the application context it might
         * still be OK to use mappings with that state.
         */
        expired(RdfResourceEnum.mappingStateExpired.getUri()),
        /**
         * Indicated, that this mapping is fully valied and can be used in any
         * application context
         */
        confirmed(RdfResourceEnum.mappingStateConfirmed.getUri()),
        ;
        private String uri;
        MappingState(String uri){
            this.uri = uri;
        }
        public String getUri(){
            return uri;
        }
        @Override
        public String toString() {
            return uri;
        }
        // ---- reverse Mapping based on URI ----
        private static Map<String,MappingState> URI_TO_STATE;
        static {
            Map<String, MappingState> mappings = new HashMap<String, MappingState>();
            for(MappingState state : MappingState.values()){
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
        public static MappingState getState(String uri) throws IllegalArgumentException{
            MappingState state = URI_TO_STATE.get(uri);
            if(state == null){
                throw new IllegalArgumentException(String.format(
                    "Unknown MappingState URI %s (supported states URIs: %s)",
                    uri,URI_TO_STATE.keySet()));
            }
            return state;
        }
        public static boolean isState(String uri){
            return URI_TO_STATE.containsKey(uri);
        }
    }
    //    /**
//     * Getter for the identifier.
//     * @return the identifier
//     */
//    String getId();
    /**
     * The key to be used for the id of the mapped entity
     */
    String ENTITY_ID = RdfResourceEnum.mappedEntity.toString();

    /**
     * Getter for the ID of the entity
     * @return the mapped entity
     */
    String getEntityId();
    /**
     * The key to be used for the id of the mapped symbol
     */
    String SYMBOL_ID = RdfResourceEnum.mappedSymbol.toString();
    /**
     * Getter for the ID of the symbol
     * @return the symbol the entity is mapped to
     */
    String getSymbolId();
    /**
     * The key to be used for the state of the MappedEntity instance
     */
    String STATE = RdfResourceEnum.hasMappingState.toString();

    /**
     * The state of this mapping
     * @return the state
     */
    MappingState getState();
    /**
     * Setter for the mapping state
     * @param state the new state
     * @throws IllegalArgumentException if the parsed state is <code>null</code>
     */
    void setState(MappingState state) throws IllegalArgumentException;

    /**
     * The property used to hold the expires date of the representation (if any)
     */
    String EXPIRES = RdfResourceEnum.expires.getUri();
    /**
     * Getter for the date this representation expires. If this representation
     * does not expire this method returns <code>null</code>.
     * @return the expire date or <code>null</code> if not applicable.
     */
    Date getExpires();
    /**
     * Setter for the expire date for this representation.
     * @param date the date or <code>null</code> if this representation does not
     * expire
     */
    void setExpires(Date date);
//    /**
//     * Getter for the Representation of this EntityMapping
//     * @return The representation
//     */
//    Representation getRepresentation();

}
