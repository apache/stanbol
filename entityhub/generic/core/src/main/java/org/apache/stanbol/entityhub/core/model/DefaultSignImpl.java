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
package org.apache.stanbol.entityhub.core.model;

import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSignImpl implements Sign{

    Logger log = LoggerFactory.getLogger(DefaultSignImpl.class);
    
    protected final Representation representation;
    private final String signSite;

//    public DefaultSignImpl(Representation representation) {
//        if(representation == null){
//            throw new IllegalArgumentException("NULL value ist not allowed for the Representation");
//        }
//        if(representation.getFirstReference(SIGN_SITE) == null){
//            throw new IllegalStateException("Parsed Representation does not define the required field"+SIGN_SITE+"!");
//        }
//        this.representation = representation;
//    }
    protected DefaultSignImpl(String siteId,Representation representation) {
        super();
        if(representation == null){
            throw new IllegalArgumentException("NULL value ist not allowed for the Representation");
        }
        if(siteId == null || siteId.isEmpty()){
            throw new IllegalStateException("Parsed SiteId MUST NOT be NULL nor empty!");
        }
        this.signSite = siteId;
        this.representation = representation;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(", site=");
        sb.append(getSignSite());
        sb.append(", id=");
        sb.append(getId());
        sb.append(", representation=");
        sb.append(getRepresentation());
        return sb.toString();
    }
    
    @Override
    public final String getSignSite() {
        return signSite;
    }

    @Override
    public final String getId() {
        return representation.getId();
    }

    @Override
    public final Representation getRepresentation() {
        return representation;
    }
    @Override
    public final SignTypeEnum getType() {
        SignTypeEnum type = parseSignType(representation);
        return type == null?SignTypeEnum.Sign:type;
    }
    /**
     * Parses the SignType from the parsed Representation
     * @param rep the representation
     * @return the type of the sign or <code>null</code> if the representation
     * does not contain the necessary information
     * @throws IllegalArgumentException if the parsed Representation contains an
     * {@link Sign#SIGN_TYPE} value that is not an valid URI for 
     * {@link SignTypeEnum#getType(String)}.
     */
    public static SignTypeEnum parseSignType(Representation rep) throws IllegalArgumentException {
        SignTypeEnum signType;
        Reference typeRef = rep.getFirstReference(Sign.SIGN_TYPE);
        if(typeRef != null){
            signType = SignTypeEnum.getType(typeRef.getReference());
        } else {
            signType = null;
        }
        return signType;
    }
}
