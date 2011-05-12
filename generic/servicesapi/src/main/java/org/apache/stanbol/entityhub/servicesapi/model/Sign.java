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
 * A Sign links three things together
 * <ol>
 * <li>the <b>signifier</b> (ID) used to identify the sign
 * <li>the <b>description</b> (Representation) for the signified entity
 * <li>the <b>organisation</b> (Site) that provides this description
 * </ol>
 * 
 * @author Rupert Westenthaler
 *
 */
public interface Sign {
    /**
     * Enumeration over the different types of Signs defined by the Entityhub
     * @author Rupert Westenthaler
     *
     */
    enum SignTypeEnum {
        /**
         * The Sign - the default - type
         */
        Sign(RdfResourceEnum.Sign.getUri()),
        /**
         *  Symbols are Signs defined by this Entityhub instance
         */
        Symbol(RdfResourceEnum.Symbol.getUri()),
        /**
         * EntityMappings are signs that do map Signs defined/managed by referenced
         * Sites to Symbols.
         */
        EntityMapping(RdfResourceEnum.EntityMapping.getUri()),
        ;
        private String uri;
        SignTypeEnum(String uri){
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
        private static Map<String,SignTypeEnum> URI_TO_STATE;
        static {
            Map<String, SignTypeEnum> mappings = new HashMap<String, SignTypeEnum>();
            for(SignTypeEnum state : SignTypeEnum.values()){
                mappings.put(state.getUri(), state);
            }
            URI_TO_STATE = Collections.unmodifiableMap(mappings);
        }
        /**
         * Getter for the SignType based on the URI.
         * @param uri the URI
         * @return the State
         * @throws IllegalArgumentException if the parsed URI does not represent
         * a state
         */
        public static SignTypeEnum getType(String uri) throws IllegalArgumentException{
            SignTypeEnum state = URI_TO_STATE.get(uri);
            if(state == null){
                throw new IllegalArgumentException(String.format(
                    "Unknown SignType URI %s (supported states URIs: %s)",
                    uri,URI_TO_STATE.keySet()));
            }
            return state;
        }
        public static boolean isState(String uri){
            return URI_TO_STATE.containsKey(uri);
        }
    }
    /**
     * The default type of a {@link Sign} (set to {@link SignTypeEnum#Sign})
     */
    SignTypeEnum DEFAULT_SIGN_TYPE = SignTypeEnum.Sign;
    /**
     * The id (signifier) of this  sign.
     * @return the id
     */
    String getId();

    String SIGN_SITE = RdfResourceEnum.signSite.getUri();
    /**
     * Getter for the id of the referenced Site that defines/manages this sign.<br>
     * Note that the Entityhub allows that different referenced Sites
     * provide representations for the same id ({@link Sign#getId()}).
     * Therefore there may be different entity instances of {@link Sign} with
     * the same id but different representations.<br>
     * In other word different referenced Sites may manage representations by
     * using the same id.<br>
     * Note also, that the Entityhub assumes that all such representations
     * are equivalent and interchangeable. Therefore Methods that searches for
     * Entities on different Sites will return the first hit without searching
     * for any others.
     * @return the site of this Sign
     */
    String getSignSite();
    
    /**
     * The property used to store the type of the type
     */
    String SIGN_TYPE = RdfResourceEnum.signType.getUri();
    /**
     * Getter for the type of a sign. Subclasses may restrict values of this
     * property. (e.g. {@link #getType()} for {@link Symbol} always returns
     * {@link SignTypeEnum#Symbol})
     * @return the type
     */
    SignTypeEnum getType();

    /**
     * Getter for the {@link Representation} of that sign as defined/managed by the site
     * @return the representation
     */
    Representation getRepresentation();
}
