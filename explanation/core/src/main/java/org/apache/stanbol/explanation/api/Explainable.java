package org.apache.stanbol.explanation.api;

/**
 * An umbrella and wrapper type for any object that may be backed up by a user-sensitive explanation.
 * 
 * @author alexdma
 * 
 */
public interface Explainable<T> {
    
    public static final int FACT = 2;
    
    public static final int KNOWLEDGE_ITEM = 1;
    
    public static final int DISCOURSE_ITEM = 3;

    /**
     * 
     * @return the referenced object.
     */
    T getItem();

}
