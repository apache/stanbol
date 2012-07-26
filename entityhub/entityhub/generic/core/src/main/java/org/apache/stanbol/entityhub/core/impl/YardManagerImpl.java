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
package org.apache.stanbol.entityhub.core.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.servicesapi.yard.Cache;
import org.apache.stanbol.entityhub.servicesapi.yard.Yard;
import org.apache.stanbol.entityhub.servicesapi.yard.YardManager;

@Component(immediate = true)
@Service
public class YardManagerImpl implements YardManager {

//    private static final Logger log = LoggerFactory.getLogger(YardManagerImpl.class);
    @Reference(
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            referenceInterface=Yard.class,
            strategy=ReferenceStrategy.EVENT,
            policy=ReferencePolicy.DYNAMIC,
            bind="bindYard",
            unbind="unbindYard")
    private Map<String,Yard> yards = Collections.emptyMap();


    @Reference(
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            referenceInterface=Cache.class,
            strategy=ReferenceStrategy.EVENT,
            policy=ReferencePolicy.DYNAMIC,
            bind="bindCache",
            unbind="unbindCache")
    private Map<String,Cache> caches = Collections.emptyMap();// stat with a empty map!

//    private ComponentContext context;
//    @Activate
//    protected void activate(ComponentContext context){
//        log.debug("activating "+getClass()+" with "+context);
//        //nothing to do for now!
//        this.context = context;
//    }
//    @Deactivate
//    protected void deactivate(ComponentContext context){
//        context = null;
//    }
    protected void bindYard(Yard yard){
        if(yard != null){
            Map<String, Yard> tmp = new HashMap<String, Yard>(yards);
            tmp.put(yard.getId(),yard);
            this.yards = Collections.unmodifiableMap(tmp);
        }
    }
    protected void unbindYard(Yard yard){
        if(yard != null && yards.containsKey(yard.getId())){
            Map<String, Yard> tmp = new HashMap<String, Yard>(yards);
            tmp.remove(yard.getId());
            this.yards = Collections.unmodifiableMap(tmp);
        }
    }
    protected void bindCache(Cache cache){
        if(cache != null){
            Map<String, Cache> tmp = new HashMap<String, Cache>(caches);
            tmp.put(cache.getId(),cache);
            this.caches = Collections.unmodifiableMap(tmp);
        }
    }
    protected void unbindCache(Cache cache){
        if(cache != null && caches.containsKey(cache.getId())){
            Map<String, Cache> tmp = new HashMap<String, Cache>(caches);
            tmp.remove(cache.getId());
            this.caches = Collections.unmodifiableMap(tmp);
        }
    }

    @Override
    public Yard getYard(String id) {
        return yards.get(id);
    }

    @Override
    public Collection<String> getYardIDs() {
        return yards.keySet();
    }

    @Override
    public boolean isYard(String id) {
        return yards.containsKey(id);
    }
    @Override
    public Cache getCache(String id) {
        return caches.get(id);
    }
    @Override
    public Collection<String> getCacheIDs() {
        return caches.keySet();
    }
    @Override
    public boolean isCache(String id) {
        return caches.containsKey(id);
    }
}
