package org.apache.stanbol.enhancer.nlp.json.valuetype;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

/**
 * Interface allowing to extend how Classes used as generic type for
 * {@link org.apache.stanbol.enhancer.nlp.model.annotation.Value}s are serialised to JSON
 * <p>
 * Implementation MUST register itself as OSGI services AND also provide
 * a <code>META-INF/services/org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeSerializer</code> 
 * file required for the {@link java.util.ServiceLoader} utility.
 * 
 * @param <T>
 */
public interface ValueTypeSerializer<T> {

    String PROPERTY_TYPE = ValueTypeParser.PROPERTY_TYPE;
    
    Class<T> getType();
    
    ObjectNode serialize(ObjectMapper mapper, T value);
}
