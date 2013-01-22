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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class Value<T> {

    /**
     * For Values that do not have a probability we use <code>-1.0d</code>
     */
    public static final double UNKNOWN_PROBABILITY = -1.0d;
    
    /**
     * The value
     */
    private final T value;
    /**
     * The probability of the Annotation
     */
    private final double probability;
    
    /**
     * Creates an Annotation for the value with an {@link #UNKNOWN_PROBABILITY
     * unknown probability}.
     * @param value the value
     */
    public Value(T value){
        this(value,UNKNOWN_PROBABILITY);
    }
    
    public Value(T value, double probability){
        if(value == null){
            throw new IllegalArgumentException("The parsed Value MUST NOT be NULL!");
        }
        this.value = value;
        if(probability != UNKNOWN_PROBABILITY && (probability > 1 || probability < 0)){
            throw new IllegalArgumentException("Probabilities MUST BE in the range [0..1]");
        }
        this.probability = probability;
    }
    
    public final T value(){
        return value;
    }
    
    public final double probability(){
        return probability;
    }
    
    public static <T> Value<T> value(T value){
        return new Value<T>(value);
    }
    
    public static <T> Value<T> value(T value,double probability){
        return new Value<T>(value,probability);
    }
    
    public static <T> List<Value<T>> values(T...values){
        List<Value<T>> valList = new ArrayList<Value<T>>(values.length);
        for(T value : values){
            valList.add(new Value<T>(value));
        }
        return valList;
    }
    public static <T> List<Value<T>> values(T[] values, double[] probabilities){
        return values(values,probabilities,values.length);
    }
    public static <T> List<Value<T>> values(T[] values, double[] probabilities,int elements){
        List<Value<T>> valList = new ArrayList<Value<T>>(elements);
        for(int i=0;i<elements;i++){
            valList.add(new Value<T>(values[i],probabilities[i]));
        }
        return valList;
    }

    @Override
    public int hashCode() {
        //for long hash see 
        //http://docs.oracle.com/javase/6/docs/api/java/lang/Double.html#hashCode()
        long bits = Double.doubleToLongBits(probability);
        return value.hashCode() + (int)(bits^(bits>>>32));
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Value && value.equals(((Value<?>)obj).value) &&
                probability == ((Value<?>)obj).probability;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Value [");
        sb.append(value.toString()).append(']');
        if(probability != UNKNOWN_PROBABILITY){
            sb.append(".prob=").append(probability);
        }
        return sb.toString();
    }
    
    /**
     * Comparator that sorts Values ONLY based on {@link Value#probability()} -
     * DO NOT USE with {@link Set} implementations as it will only allow a 
     * single Value with the same probability.<p>
     * Values with {@link #UNKNOWN_PROBABILITY} are considered as lowest
     * probability.
     */
    public static final Comparator<Value<?>> PROBABILITY_COMPARATOR = new Comparator<Value<?>>() {

        @Override
        public int compare(Value<?> o1, Value<?> o2) {
            return Double.compare(o2.probability, o1.probability);
        }
    };
    

}
