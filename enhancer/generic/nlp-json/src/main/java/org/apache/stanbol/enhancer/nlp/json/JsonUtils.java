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
package org.apache.stanbol.enhancer.nlp.json;

import java.util.EnumSet;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JsonUtils {

    private JsonUtils(){};
    
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    
    /**
     * @param jValue
     * @param categories
     */
    public static <T extends Enum<T>> EnumSet<T> parseEnum(ObjectNode jValue, String key, Class<T> type) {
        final EnumSet<T> categories = EnumSet.noneOf(type);
        JsonNode node = jValue.path(key);
        if(node.isMissingNode()) {
            return categories; //no values nothing to do
        }
        if(node.isArray()){
            ArrayNode jLcs = (ArrayNode)node;
            for(int i=0;i < jLcs.size();i++){
                JsonNode jLc =  jLcs.get(i);
                if(jLc.isTextual()){
                    try {
                        categories.add(Enum.valueOf(type,jLc.getTextValue()));
                    } catch (IllegalArgumentException e) {
                        log.warn("unknown "+type.getSimpleName()+" '"+jLc+"'",e);
                    }
                } else if(jLc.isInt()) {
                        categories.add(type.getEnumConstants()[jLc.asInt()]);
                } else {
                    log.warn("unknow value in '{}' Array at index [{}]: {}",
                        new Object[]{key,i,jLc});
                }
            }
        } else if(node.isTextual()){
            try {
                categories.add(Enum.valueOf(type,node.getTextValue()));
            } catch (IllegalArgumentException e) {
                log.warn("unknown "+type.getSimpleName()+" '"+node+"'",e);
            }
        } else if(node.isInt()) {
            categories.add(type.getEnumConstants()[node.asInt()]);
        } else {
            log.warn("unknow value for key '{}': {}",key,node);
        }
        return categories;
    }
}
