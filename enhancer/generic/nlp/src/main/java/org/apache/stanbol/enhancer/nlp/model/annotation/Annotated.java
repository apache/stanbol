package org.apache.stanbol.enhancer.nlp.model.annotation;

import java.util.List;

public interface Annotated {
    
    

    /**
     * Method for requesting Values of a given Key. This allows to request
     * Values without an {@link Annotation}.
     * @param key the Key
     * @return the Value with the highest probability
     */
    Value<?> getValue(Object key);
    
    /**
     * Method for requesting the Value of an Annotation.
     * @param annotation the requested {@link Annotation}
     * @return the Value with the highest probability
     * @throws ClassCastException if values of {@link Annotation#getKey()} are
     * not of type V
     */
    <V> Value<V> getAnnotation(Annotation<?,V> annotation);

    /**
     * Method for requesting Values of a given Key. This allows to request
     * Values without an {@link Annotation}.
     * @param key the Key
     * @return all Value sorted by probability
     */
    List<Value<?>> getValues(Object key);
    
    /**
     * Method for requesting the Value of an Annotation.
     * @param annotation the requested {@link Annotation}
     * @return all Values sorted by probability
     * @throws ClassCastException if the returned value of 
     * {@link Annotation#getKey()} is not of type V
     */
    <V> List<Value<V>> getAnnotations(Annotation<?,V> annotation);
    
    /**
     * Appends an Annotation to eventually already existing values 
     * @param annotation the annotation
     * @param value the value to append
     */
    <K,V> void addAnnotation(Annotation<K,V> annotation, Value<V> value);

    /**
     * Replaces existing Annotations with the parsed one
     * @param annotation the annotation
     * @param value the value for the annotation
     */
    <K,V> void setAnnotation(Annotation<K,V> annotation, Value<V> value);
    
    /**
     * Appends an Annotation to eventually already existing values 
     * @param annotation the annotation
     * @param value the value to append
     */
    <K,V> void addAnnotations(Annotation<K,V> annotation, List<Value<V>> values);

    /**
     * Replaces existing Annotations with the parsed one
     * @param annotation the annotation
     * @param value the value for the annotation
     */
    <K,V> void setAnnotations(Annotation<K,V> annotation, List<Value<V>> values);
}
