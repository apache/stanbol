package org.apache.stanbol.enhancer.nlp.model.annotation;

/**
 * Definition of an Annotation including the <ul>
 * <li>key used to store the Annotation
 * <li>generic type of Values for this Annotation
 * </ul>
 *
 * @param <K>
 * @param <V>
 */
public final class Annotation<V> {

    /**
     * The type of the used Key
     */
    final String key;
    /**
     * The type of the used Value
     */
    final Class<V> valueType;
    
    public Annotation(String key,Class<V> valueType){
        if(key == null || key == null){
            throw new IllegalArgumentException("Key and Value MUST NOT be NULL!");
        }
        this.key = key;
        this.valueType = valueType;
    }
 
    public String getKey(){
        return key;
    }
    
    public Class<V> getValueType(){
        return valueType;
    }
        
}
