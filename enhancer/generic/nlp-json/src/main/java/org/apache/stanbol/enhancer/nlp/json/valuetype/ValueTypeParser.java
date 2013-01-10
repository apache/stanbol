package org.apache.stanbol.enhancer.nlp.json.valuetype;

import org.codehaus.jackson.node.ObjectNode;
/**
 * Interface allowing to extend how Classes used as generic type for
 * {@link org.apache.stanbol.enhancer.nlp.model.annotation.Value}s are parsed from JSON
 * <p>
 * Implementation MUST register itself as OSGI services AND also provide
 * a <code>META-INF/services/org.apache.stanbol.enhancer.nlp.json.valuetype.ValueTypeParser</code> 
 * file required for the {@link java.util.ServiceLoader} utility.

 * @param <T>
 */
public interface ValueTypeParser<T> {


    String PROPERTY_TYPE = "type";

    Class<T> getType();
    
    T parse(ObjectNode jAnnotation);
    
}
