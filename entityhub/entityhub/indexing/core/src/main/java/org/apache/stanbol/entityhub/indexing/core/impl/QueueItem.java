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
package org.apache.stanbol.entityhub.indexing.core.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Item internally used for Queues. It holds an item and can store
 * additional properties. The Item can be only set while constructing an
 * instance. The properties can be changed at any time.<p>
 * Notes:<ul>
 * <li> {@link #hashCode()} uses the hashCode of the item and 
 * {@link #equals(Object)} also checks if the item is equal to the other
 * item. The properties are not used!
 * <li>This Class is not synchronised.
 * </ul>
 * @author Rupert Westenthaler
 *
 * @param <T>
 */
public class QueueItem<T> {
    private Map<String,Object> properties;
    private final T item;
    /**
     * Creates a new QueueItem
     * @param item the payload
     */
    public QueueItem(T item){
        this.item = item;
    }
    /**
     * Creates a QueueItem and copies the properties of an other one.
     * NOTE that components with an reference to the other QueueItem will be
     * able to change the properties of the new one. Use <br>
     * <pre><code>
     *   QueueItem&lt;String&gt; item = new QueueItem&lt;String&gt;("demo");
     *   for(String key : other.getProperties()){
     *       item.setProperties(key,other.getProperty(key));
     *   }
     * </code></pre><br>
     * if you need to ensure that the properties within the QueueItem are not
     * shared with others.
     * @param item the payload
     * @param properties the properties
     */
    public QueueItem(T item,QueueItem<?> other){
        this(item);
        if(other != null){
            this.properties = other.properties;
        }
    }
    public T getItem(){
        return item;
    }
    public void setProperty(String key,Object value){
        if(properties == null){
            properties = new HashMap<String,Object>();
        }
        properties.put(key, value);
    }
    public Object getProperty(String key){
        return properties != null ?
            properties.get(key): null;
    }
    public Object removeProperty(String key){
        return properties != null ?
            properties.remove(key): null;
    }
    public Set<String> properties(){
        if(properties != null){
            return Collections.unmodifiableSet(properties.keySet());
        } else {
            return Collections.emptySet();
        }
    }
    @Override
    public int hashCode() {
        return item.hashCode();
    }
    @Override
    public boolean equals(Object other) {
        return (other instanceof QueueItem<?>) &&
                item.equals(((QueueItem<?>)other).item);
    }
}