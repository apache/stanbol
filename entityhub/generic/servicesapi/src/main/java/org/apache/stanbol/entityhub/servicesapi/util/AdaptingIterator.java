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
package org.apache.stanbol.entityhub.servicesapi.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Uses the parsed Adapter to convert values of type T to values of type
 * A. If an instance of T can not be converted to A, than such values are
 * filtered. This means that this implementation can be used for both filtering
 * and converting of values of the base iterator. In fact the 
 * FilteringIterator is implemented based on this class.<p>
 * Note that {@link Iterator#remove()} only works as long as 
 * {@link Iterator#hasNext()} was not called to determine if there are
 * further elements. The reason for that is, that in order to filter elements
 * of the parent iterator {@link Iterator#next()} has to be called to check
 * weather any further element is valid against the used Filter.
 * This call to {@link Iterator#next()} causes the parent Iterator to switch
 * to the next element, meaning that after that the <code>remove()</code>
 * method would delete a different element. To avoid that this Iterator
 * throws an {@link IllegalStateException} in such cases. If the parent
 * Iterator does not support <code>remove()</code> at all an
 * {@link UnsupportedOperationException} is thrown. <p>
 * 
 * @author Rupert Westenthaler
 *
 * @param <T> The type of the incoming elements
 * @param <A> The type of the elements returned by this iterator
 */
public class AdaptingIterator<T,A> implements Iterator<A> {

    /**
     * Adapts values of type T to values of type A. <code>null</code> indicated
     * that the adaption is not possible for the current value of T
     * 
     * @author Rupert Westenthaler
     *
     * @param <T>
     * @param <A>
     */
    public interface Adapter<T,A> {
        /**
         * Converts the value of type T to a value of type A. If an instance of
         * T can not be converted to A, than <code>null</code> is returned
         * @param value the incoming value
         * @param type the target type
         * @return the converted value or <code>null</code> if the parsed value
         * is <code>null</code> or the parsed value can not be converted
         */
        A adapt(T value, Class<A> type);
    }
    
    private final Adapter<T, A> adapter;
    private final Iterator<T> it;
    private final Class<A> type;
    private A next;
    private Boolean hasNext;
    /**
     * Constructs an instance based on an iterator of type T, an adapter and the
     * target type
     * @param it the base iterator
     * @param adapter the adapter
     * @param type the target type
     */
    public AdaptingIterator(Iterator<T> it,Adapter<T,A> adapter,Class<A> type){
        if(it == null){
            throw new IllegalArgumentException("Parsed iterator MUST NOT be NULL!");
        }
        if(adapter == null){
            throw new IllegalArgumentException("Parsed adapter MUST NOT be NULL!");
        }
        if(type == null){
            throw new IllegalArgumentException("Parsed type MUST NOT be NULL!");
        }
        this.it = it;
        this.adapter = adapter;
        this.type = type;
    }
    @Override
    public final boolean hasNext() {
        if(hasNext == null){ // only once even with multiple calls
            next = prepareNext();
            hasNext = next != null;
        }
        return hasNext;
    }

    @Override
    public final A next() {
        hasNext(); //call hasNext (to init next Element if not already done)
        if(!hasNext){
            throw new NoSuchElementException();
        } else {
            A current = next;
            next = null;
            hasNext = null;
            return current;
        }
    }
    /**
     * This implementation of remove does have an additional restriction. It
     * is only able to remove the current element of the parent Iterator (parsed
     * in the constructor) if {@link #hasNext()} was not yet called. This is
     * because {@link #hasNext()} needs to call {@link Iterator#next()} on the
     * parent iterator to check if there are further elements that can be
     * adapted successfully. This causes that the current element of this
     * Iterator (stored in an local variable) is no longer the current element
     * of the parent iterator and therefore calls to {@link #remove()} would
     * delete an other object within the collection holding the elements used
     * for this iteration. To prevent this this method throws an
     * {@link IllegalStateException} ins such cases. Users of this method need
     * therefore to ensure, that there are no calls to remove between a call
     * to {@link #hasNext()} and {@link #next()} (what is not the case in
     * typical use cases).
     * @see Iterator#remove()
     */
    @Override
    public final void remove() {
        /*
         * TODO: See java doc for a detailed description! 
         * If someone has a better Idea how to solve this please let me know!
         * all the best 
         * Rupert Westenthaler
         */
        if(hasNext!= null){
            throw new IllegalStateException("Remove can not be called after calling hasNext()! See java doc for more information.");
        }
        it.remove();
    }
    protected A prepareNext(){
        T check;
        A converted;
        while(it.hasNext()){
            check = it.next();
            converted = adapter.adapt(check,type);
            if(converted != null){
                return converted;
            }
        }
        return null;
    }

}
