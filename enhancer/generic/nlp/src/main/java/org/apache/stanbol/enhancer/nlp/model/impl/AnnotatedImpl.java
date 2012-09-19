package org.apache.stanbol.enhancer.nlp.model.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.stanbol.enhancer.nlp.model.annotation.Annotated;
import org.apache.stanbol.enhancer.nlp.model.annotation.Annotation;
import org.apache.stanbol.enhancer.nlp.model.annotation.Value;

public class AnnotatedImpl implements Annotated{

    private Map<Object,Object> annotations;
    
    
    @Override
    public final Value<?> getValue(Object key) {
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
    public final List<Value<?>> getValues(Object key) {
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
    public final <V> Value<V> getAnnotation(Annotation<?,V> annotation) {
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
    public final <V> List<Value<V>> getAnnotations(Annotation<?,V> annotation) {
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
    public <K,V> void addAnnotations(Annotation<K,V> annotation, List<Value<V>> values) {
        if(values == null || values.isEmpty()){
            return;
        }
        Map<Object, Object> map = initAnnotations();
        Object currentValue = annotations.get(annotation.getKey());
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
            for(Value<V> value : values){
                ((Value<?>[])newValues)[index] = value;
                index++;
            }
            Arrays.sort((Value<?>[])newValues,Value.PROBABILITY_COMPARATOR);
        } else { //an Array
            int length = ((Value<?>[])currentValue).length;
            newValues = new Value<?>[values.size()+length];
            System.arraycopy(currentValue, 0, newValues, 0, length);
            for(Value<V> value : values){
                ((Value<?>[])newValues)[length] = value;
                length++;
            }
            Arrays.sort((Value<?>[])newValues,Value.PROBABILITY_COMPARATOR);
        }
        map.put(annotation.getKey(), newValues);
    }
    @Override
    public <K,V> void setAnnotations(Annotation<K,V> annotation, List<Value<V>> value) {
        Map<Object, Object> map = initAnnotations();
        if(value == null || value.isEmpty()){
            map.remove(annotation.getKey());
        } else if(value.size() == 1){
            map.put(annotation.getKey(), value.get(0));
        } else {
            //we need to copy, because users might change the parsed Array!
            Value<?>[] copy = value.toArray(new Value<?>[value.size()]);
            Arrays.sort(copy,Value.PROBABILITY_COMPARATOR);
            map.put(annotation.getKey(),copy);
        }
        
    }
    
    private Map<Object,Object> initAnnotations(){
        if(annotations == null){ //avoid sync for the typical case
            annotations = new HashMap<Object,Object>();
        }
        return annotations;
    }
    @Override
    public <K,V> void addAnnotation(Annotation<K,V> annotation, Value<V> value) {
        if(value != null){
          Map<Object,Object> map = initAnnotations();  
          Object currentValue = map.get(annotation);
          if(currentValue == null){
              map.put(annotation.getKey(), value);
          } else if (value instanceof Value<?>){
              Value<?>[] newValues =  new Value<?>[]{(Value<?>)currentValue,value};
              Arrays.sort(newValues,Value.PROBABILITY_COMPARATOR);
              map.put(annotation.getKey(), newValues);
          } else { //array
              int length = ((Value<?>[])currentValue).length;
              Value<?>[] newValues = new Value<?>[length+1];
              System.arraycopy(currentValue, 0, newValues, 0, length);
              newValues[length] = value;
              Arrays.sort(newValues,Value.PROBABILITY_COMPARATOR);
              map.put(annotation.getKey(), newValues);
          }
        } 
    }
    @Override
    public <K,V> void setAnnotation(Annotation<K,V> annotation, Value<V> value) {
        if(annotations == null && value == null){
            return;
        }
        Map<Object,Object> map = initAnnotations();
        if(value == null){
            map.remove(annotation.getKey());
        } else {
            map.put(annotation.getKey(),value);
        }
        
    }
}
