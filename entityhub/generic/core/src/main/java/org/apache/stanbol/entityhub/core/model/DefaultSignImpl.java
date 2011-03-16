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

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.Sign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSignImpl implements Sign{

    Logger log = LoggerFactory.getLogger(DefaultSignImpl.class);
    
    protected final Representation representation;
//    private final String TYPE = RdfResourceEnum.signType.getUri();
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
    public DefaultSignImpl(String siteId,Representation representation) {
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
    public String getSignSite() {
        return signSite;
    }

    @Override
    public String getId() {
        return representation.getId();
    }

    @Override
    public Representation getRepresentation() {
        return representation;
    }
//    @Override
//    public SignTypeEnum getType() {
//        Reference ref = representation.getFirstReference(TYPE);
//        if(ref == null){
//            return DEFAULT_SIGN_TYPE;
//        } else {
//            SignTypeEnum type = ModelUtils.getSignType(ref.getReference());
//            if(type == null){
//                log.warn("Sign "+getId()+" is set to an unknown SignType "+ref.getReference()+"! -> return default type (value is not reseted)");
//                return DEFAULT_SIGN_TYPE;
//            } else {
//                return type;
//            }
//        }
//    }

}
