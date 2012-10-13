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

/**
 * Filters elements of the base Iterator base on the generic type of this one
 * @author Rupert Westenthaler
 *
 * @param <T> the type of elements returned by this iterator
 */
public class TypeSafeIterator<T> extends AdaptingIterator<Object,T> implements Iterator<T> {

    /**
     * Constructs an iterator that selects only elements of the parsed iterator
     * that are assignable to the parse type
     * @param it the base iterator
     * @param type the type all elements of this Iterator need to be assignable to.
     */
    @SuppressWarnings("unchecked")
    public TypeSafeIterator(Iterator<?> it,Class<T> type){
        super((Iterator<Object>)it,new AssignableFormAdapter<T>(),type);
    }

    /**
     * Adapter implementation that uses {@link Class#isAssignableFrom(Class)}
     * to check whether a value can be casted to the requested type
     */
    private static class AssignableFormAdapter<T> implements Adapter<Object,T> {

        @SuppressWarnings("unchecked")
        @Override
        public T adapt(Object value, Class<T> type) {
            if(type.isAssignableFrom(value.getClass())){
              return (T)value;
            } else {
                return null;
            }
        }
        
    }
}

