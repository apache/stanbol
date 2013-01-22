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
package org.apache.stanbol.enhancer.nlp.model.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.enhancer.nlp.model.annotation.Annotated;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;

public class AnnotatedImpl implements Annotated{

    private Map<String,Object> annotations;
    
    @SuppressWarnings("unchecked")
    public Set<String> getKeys(){
        return annotations == null ? Collections.EMPTY_SET : annotations.keySet();
    }
    
    @Override
    public final Value<?> getValue(String key) {
        if(annotations == null){
            return null;
        }
        Object value = annotations.get(key);
        if(value instanceof Value<?>){
            return (Value<?>)value;
        } else if(value != null){
            return ((Value<?>[])value)[0];
        } else {
            return null;
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public final List<Value<?>> getValues(String key) {
        if(annotations == null){
            return Collections.emptyList();
        }
        Object value = annotations.get(key);
        if(value instanceof Value<?>){
            List<?> singleton = Collections.singletonList((Value<?>)value);
            return (List<Value<?>>)singleton;
        } else if (value != null){
            return Arrays.asList((Value<?>[])value);
        } else {
            return Collections.emptyList();
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public final <V> Value<V> getAnnotation(Annotation<V> annotation) {
        if(annotations == null){
            return null;
        }
        Object value = annotations.get(annotation.getKey());
        if(value instanceof Value<?>){
            return (Value<V>)value;
        } else if(value != null){
            return ((Value<V>[])value)[0];
        } else {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public final <V> List<Value<V>> getAnnotations(Annotation<V> annotation) {
        if(annotations == null){
            return Collections.emptyList();
        }
        Object value = annotations.get(annotation.getKey());
        if(value instanceof Value<?>){
            List<?> singleton = Collections.singletonList((Value<?>)value);
            return (List<Value<V>>)singleton;
        } else if(value != null){
            return Arrays.asList((Value<V>[])value);
        } else {
            return Collections.emptyList();
        }
    }
    
    @Override
    public <V> void addAnnotations(Annotation<V> annotation, List<Value<V>> values) {
        addValuesInternal(annotation.getKey(), values);
    }
    @Override
    public void addValues(String key, List<Value<?>> values) {
        addValuesInternal(key, values);
    }
    /**
     * Just here because of Java generics combined with Collections ...
     * @param key
     * @param values
     */
    private void addValuesInternal(String key, List<?> values) {
        if(values == null || values.isEmpty()){
            return;
        }
        Map<String, Object> map = initAnnotations();
        Object currentValue = annotations.get(key);
        Object newValues;
        if(currentValue == null){
            if(values.size() == 1){
                newValues = values.get(0);
            } else {
                newValues = values.toArray(new Value<?>[values.size()]);
                Arrays.sort((Value<?>[])newValues,Value.PROBABILITY_COMPARATOR);
            }
        } else if (currentValue instanceof Value<?>){
            newValues = new Value<?>[values.size()+1];
            ((Value<?>[])newValues)[0] = (Value<?>)currentValue;
            int index = 1;
            for(Object value : values){
                ((Value<?>[])newValues)[index] = (Value<?>)value;
                index++;
            }
            Arrays.sort((Value<?>[])newValues,Value.PROBABILITY_COMPARATOR);
        } else { //an Array
            int length = ((Value<?>[])currentValue).length;
            newValues = new Value<?>[values.size()+length];
            System.arraycopy(currentValue, 0, newValues, 0, length);
            for(Object value : values){
                ((Value<?>[])newValues)[length] = (Value<?>)value;
                length++;
            }
            Arrays.sort((Value<?>[])newValues,Value.PROBABILITY_COMPARATOR);
        }
        map.put(key, newValues);
    }
    @Override
    public <V> void setAnnotations(Annotation<V> annotation, List<Value<V>> values) {
        setValuesInternal(annotation.getKey(),values);
    }
    @Override
    public void setValues(String key, List<Value<?>> values){
        setValuesInternal(key, values);
    }
    /**
     * Just here because of Java generics combined with Collections ...
     * @param key
     * @param values
     */
    private void setValuesInternal(String key, List<?> values){
        Map<String, Object> map = initAnnotations();
        if(values == null || values.isEmpty()){
            map.remove(key);
        } else if(values.size() == 1){
            map.put(key, values.get(0));
        } else {
            //we need to copy, because users might change the parsed Array!
            Value<?>[] copy = values.toArray(new Value<?>[values.size()]);
            Arrays.sort(copy,Value.PROBABILITY_COMPARATOR);
            map.put(key,copy);
        }
        
    }
    
    private Map<String,Object> initAnnotations(){
        if(annotations == null){ //avoid sync for the typical case
            annotations = new HashMap<String,Object>();
        }
        return annotations;
    }
    @Override
    public <V> void addAnnotation(Annotation<V> annotation, Value<V> value) {
        addValue(annotation.getKey(), value);
    }
    @Override
    public void addValue(String key, Value<?> value) {
        if(value != null){
          Map<String,Object> map = initAnnotations();  
          Object currentValue = map.get(key);
          if(currentValue == null){
              map.put(key, value);
          } else if (currentValue instanceof Value<?>){
              Value<?>[] newValues =  new Value<?>[]{(Value<?>)currentValue,value};
              Arrays.sort(newValues,Value.PROBABILITY_COMPARATOR);
              map.put(key, newValues);
          } else { //array
              int length = ((Value<?>[])currentValue).length;
              Value<?>[] newValues = new Value<?>[length+1];
              System.arraycopy(currentValue, 0, newValues, 0, length);
              newValues[length] = value;
              Arrays.sort(newValues,Value.PROBABILITY_COMPARATOR);
              map.put(key, newValues);
          }
        } 
    }
    @Override
    public <V> void setAnnotation(Annotation<V> annotation, Value<V> value) {
        setValue(annotation.getKey(), value);
    }
    @Override
    public void setValue(String key, Value<?> value) {
        if(annotations == null && value == null){
            return;
        }
        Map<String,Object> map = initAnnotations();
        if(value == null){
            map.remove(key);
        } else {
            map.put(key,value);
        }
    }
}
