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
 * Implementation of an Iterator over {@link String} values, that uses the 
 * {@link Object#toString()} on elements of the parent Iterator for the 
 * conversion.<p>
 * This Implementation does not use {@link AdaptingIterator}s implementation, 
 * because the {@link Object#toString()} method can be used to create a string
 * representation for every object and therefore there is no need for the
 * filtering functionality provided by the {@link AdaptingIterator}.
 * 
 * @author Rupert Westenthaler
 */
public class ToStringIterator implements Iterator<String> {

    private final Iterator<?> it;
    /**
     * Creates an string iterator over parsed parent
     * @param it the parent iterator
     * @throws NullPointerException if <code>null</code> is parsed as parent
     * iterator
     */
    public ToStringIterator(Iterator<?> it) throws NullPointerException{
        if(it == null){
            throw new IllegalArgumentException("Parsed iterator MUST NOT be NULL!");
        }
        this.it = it;
    }
    @Override
    public final void remove() {
        it.remove();
    }

    @Override
    public final String next() {
        Object next =  it.next();
        return next != null?next.toString():null;
    }

    @Override
    public final boolean hasNext() {
        return it.hasNext();
    }
}
