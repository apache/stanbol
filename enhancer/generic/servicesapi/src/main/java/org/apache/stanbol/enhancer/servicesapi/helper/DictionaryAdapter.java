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

package org.apache.stanbol.enhancer.servicesapi.helper;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Adapter for {@link Map} to {@link Dictionary}. Primarily implemented for the use in 
 * {@link EnhancementEngineHelper#getEnhancementPropertyDict(org.apache.stanbol.enhancer.servicesapi.EnhancementEngine, org.apache.stanbol.enhancer.servicesapi.ContentItem)}.
 * <p>
 * This Adapter will hide {@link Entry Entries} in the {@link Map} that do use
 * <code>null</code> as key or value. It also throws {@link NullPointerException}
 * in case <code>null</code> is parsed to {@link #put(Object, Object)},
 * {@link #get(Object)} or {@link #remove(Object)}.
 * 
 * @author Rupert Westenthaler
 *
 * @param <K>
 * @param <V>
 */
public class DictionaryAdapter<K,V> extends Dictionary<K,V> {
    
    private final Map<K,V> map;

    DictionaryAdapter(Map<K,V> map){
        this.map = map;
    }
    
    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Enumeration<K> keys() {
        final Iterator<Entry<K,V>> it = map.entrySet().iterator();
        return new Enumeration<K>(){

            K next;
            
            @Override
            public boolean hasMoreElements() {
                return retrieveNext();
            }

            private boolean retrieveNext(){
                if(next != null){
                    return true;
                } else {
                    while(next == null && it.hasNext()){
                        Entry<K,V> e = it.next();
                        if(e.getKey() != null && e.getValue() != null){
                            next = e.getKey();
                        } //we need to ignore NULL key or NULL value mappings
                    }
                    return next != null;
                }
            }        
            
            @Override
            public K nextElement() {
                K cur = next;
                next = null;
                return cur;
            }
            
        };
    }

    @Override
    public Enumeration<V> elements() {
        final Iterator<V> it = map.values().iterator();
        return new Enumeration<V>(){

            V next = null;
            
            @Override
            public boolean hasMoreElements() {
                return retrieveNext();
            }
            
            private boolean retrieveNext(){
                if(next != null){
                    return true;
                } else {
                    while(next == null && it.hasNext()){
                        next = it.next();
                    }
                    return next != null;
                }
            }

            @Override
            public V nextElement() {
                V cur = next;
                next = null;
                return cur;
            }
            
        };
    }

    @Override
    public V get(Object key) {
        if(key == null){
            throw new NullPointerException("The key MUST NOT be NULL!");
        }
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        if(key == null || value == null){
            throw new NullPointerException("Key '"+key+"' and Value '"+value+"' MUST NOT be NULL!");
        }
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        if(key == null){
            throw new NullPointerException("The key MUST NOT be NULL!");
        }
        return map.remove(key);
    }

}
