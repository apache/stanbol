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
