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
package org.apache.stanbol.enhancer.nlp.model.annotation;

import java.util.List;
import java.util.Set;

public interface Annotated {
    
    /**
     * Getter for all keys used by Annotations
     * @return the Set with all keys. An empty Set if none
     */
    Set<String> getKeys();

    /**
     * Method for requesting Values of a given Key. This allows to request
     * Values without an {@link Annotation}.
     * @param key the Key
     * @return the Value with the highest probability
     */
    Value<?> getValue(String key);
    
    /**
     * Method for requesting the Value of an Annotation.
     * @param annotation the requested {@link Annotation}
     * @return the Value with the highest probability
     * @throws ClassCastException if values of {@link Annotation#getKey()} are
     * not of type V
     */
    <V> Value<V> getAnnotation(Annotation<V> annotation);

    /**
     * Method for requesting Values of a given Key. This allows to request
     * Values without an {@link Annotation}.
     * @param key the Key
     * @return all Value sorted by probability
     */
    List<Value<?>> getValues(String key);
    
    /**
     * Method for requesting the Value of an Annotation.
     * @param annotation the requested {@link Annotation}
     * @return all Values sorted by probability
     * @throws ClassCastException if the returned value of 
     * {@link Annotation#getKey()} is not of type V
     */
    <V> List<Value<V>> getAnnotations(Annotation<V> annotation);
    
    /**
     * Appends an Annotation to eventually already existing values 
     * @param annotation the annotation
     * @param value the value to append
     */
    <V> void addAnnotation(Annotation<V> annotation, Value<V> value);

    /**
     * Replaces existing Annotations with the parsed one
     * @param annotation the annotation
     * @param value the value for the annotation
     */
    <V> void setAnnotation(Annotation<V> annotation, Value<V> value);
    
    /**
     * Appends an Value to the key. This method is intended for internal use (
     * e.g. parsers). Users are encouraged to define type save
     * {@link Annotation} objects and use {@link #addAnnotation(Annotation, Value)}
     * instead. 
     * @param key the key
     * @param value the value
     */
    void addValue(String key, Value<?> value);
    /**
     * Replaces existing Values for a key with the parsed one. This method is 
     * intended for internal use (e.g. parsers). Users are encouraged to define 
     * type save {@link Annotation} objects and use 
     * {@link #setAnnotation(Annotation, Value)} instead. 
     * @param key the key
     * @param value the value
     */
    void setValue(String key, Value<?> value);
    /**
     * Appends an Annotation to eventually already existing values 
     * @param annotation the annotation
     * @param value the value to append
     */
    <V> void addAnnotations(Annotation<V> annotation, List<Value<V>> values);

    /**
     * Replaces existing Annotations with the parsed one
     * @param annotation the annotation
     * @param value the value for the annotation
     */
    <V> void setAnnotations(Annotation<V> annotation, List<Value<V>> values);
    
    /**
     * Appends the parsed values to the key. This method is intended for internal use (
     * e.g. parsers). Users are encouraged to define type save
     * {@link Annotation} objects and use {@link #addAnnotations(Annotation, List)
     * instead. 
     * @param key the key
     * @param value the value
     */
    void addValues(String key, List<Value<?>> values);
    
    /**
     * Replaces existing Values for a key with the parsed one. This method is 
     * intended for internal use (e.g. parsers). Users are encouraged to define 
     * type save {@link Annotation} objects and use 
     * {@link #setAnnotations(Annotation, List) instead. 
     * @param key the key
     * @param value the value
     */
    void setValues(String key, List<Value<?>> values);
}
