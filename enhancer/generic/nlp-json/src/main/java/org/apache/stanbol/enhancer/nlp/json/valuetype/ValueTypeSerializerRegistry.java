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
package org.apache.stanbol.enhancer.nlp.json.valuetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks {@link ValueTypeSerializer} implementations by using a 
 * {@link ServiceTracker} when running within OSIG and the {@link ServiceLoader}
 * utility when used outside of OSGI. 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value=ValueTypeSerializerRegistry.class)
public class ValueTypeSerializerRegistry {
    
    private final Logger log = LoggerFactory.getLogger(ValueTypeSerializerRegistry.class);

    private static ValueTypeSerializerRegistry instance;
    /**
     * Should be used when running outside of OSGI to create the singleton
     * instance of this factory.
     * @return the singleton instance
     */
    public static final ValueTypeSerializerRegistry getInstance(){
        if(instance == null){
            instance = new ValueTypeSerializerRegistry();
        }
        return instance;
    }
    
    ReadWriteLock serializerLock = new ReentrantReadWriteLock();
    private boolean inOsgi;
    /**
     * Used outside OSGI
     */
    Map<Class<?>,ValueTypeSerializer<?>> valueTypeSerializers;
    /**
     * Used if running within OSGI in combination with the {@link #serializerTracker}
     */
    Map<Class<?>, List<ServiceReference>> valueTypeSerializerRefs;
    ServiceTracker serializerTracker;
    
    @SuppressWarnings("unchecked")
    public <T> ValueTypeSerializer<T> getSerializer(Class<T> type){
        if(!inOsgi && valueTypeSerializers == null){
            initValueTypeSerializer(); //running outside OSGI
        }
        if(!inOsgi){
            serializerLock.readLock().lock();
            try {
                return (ValueTypeSerializer<T>)valueTypeSerializers.get(type);
            } finally {
                serializerLock.readLock().unlock();
            }
        } else {
            ServiceTracker serializerTracker = this.serializerTracker;
            //check if we need to lazily open the ServiceTracker
            if(valueTypeSerializerRefs == null && serializerTracker != null){
                synchronized (serializerTracker) {
                    if(valueTypeSerializerRefs == null){
                        valueTypeSerializerRefs = new HashMap<Class<?>,List<ServiceReference>>();
                        //NOTE: do not open within activate(..) because of
                        //  org.apache.stanbol.enhancer.nlp.json FrameworkEvent 
                        //  ERROR (org.osgi.framework.ServiceException: 
                        //  ServiceFactory.getService() resulted in a cycle.) 
                        serializerTracker.open();
                    }
                }
            }
            serializerLock.readLock().lock();
            try {
                List<ServiceReference> refs = valueTypeSerializerRefs.get(type);
                return refs == null || refs.isEmpty() ? null :
                    (ValueTypeSerializer<T>) serializerTracker.getService(
                        refs.get(refs.size()-1));
            } finally {
                serializerLock.readLock().unlock();
            }
        }
    }
    
    @Activate
    protected void activate(ComponentContext ctx){
        inOsgi = true;
        final BundleContext bc = ctx.getBundleContext();
        serializerTracker = new ServiceTracker(bc, ValueTypeSerializer.class.getName(), 
            new SerializerTracker(bc));
        //NOTE: do not open within activate(..) because of
        //  org.apache.stanbol.enhancer.nlp.json FrameworkEvent 
        //  ERROR (org.osgi.framework.ServiceException: 
        //  ServiceFactory.getService() resulted in a cycle.) 
        //serializerTracker.open();
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        inOsgi = false;
        serializerTracker.close();
        serializerTracker = null;
        serializerLock.writeLock().lock();
        try {
            if(valueTypeSerializers != null){
                valueTypeSerializers.clear();
                valueTypeSerializers = null;
            }
            if(valueTypeSerializerRefs != null){
                valueTypeSerializerRefs.clear();
                valueTypeSerializerRefs = null;
            }
        } finally {
            serializerLock.writeLock().unlock();
        }
    }
    
    /**
     * Only used when running outside an OSGI environment
     */
    @SuppressWarnings("rawtypes")
    private void initValueTypeSerializer() {
        serializerLock.writeLock().lock();
        try {
            if(valueTypeSerializers == null){
                valueTypeSerializers = new HashMap<Class<?>,ValueTypeSerializer<?>>();
                ServiceLoader<ValueTypeSerializer> loader = ServiceLoader.load(ValueTypeSerializer.class);
                for(Iterator<ValueTypeSerializer> it = loader.iterator();it.hasNext();){
                    ValueTypeSerializer vts = it.next();
                    ValueTypeSerializer<?> serializer = valueTypeSerializers.get(vts.getType());
                    if(serializer != null){
                        log.warn("Multiple Serializers for type {} (keep: {}, ignoreing: {}",
                            new Object[]{vts.getType(),serializer,vts});
                    } else {
                        valueTypeSerializers.put(vts.getType(), vts);
                    }
                }
            }
        } finally {
            serializerLock.writeLock().unlock();
        }
    }
    /**
     * {@link ServiceTrackerCustomizer} writing services to the
     * {@link ValueTypeSerializerRegistry#valueTypeSerializerRefs} map. It also
     * sorts multiple Serializer for a single type based on sorting
     * {@link ServiceReference}s. The reference with the highest ranking will be
     * last in the list.
     *
     */
    private final class SerializerTracker implements ServiceTrackerCustomizer {
        private final BundleContext bc;

        private SerializerTracker(BundleContext bc) {
            this.bc = bc;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            ValueTypeSerializer<?> service = (ValueTypeSerializer<?>)bc.getService(reference);
            if(service != null){
                serializerLock.writeLock().lock();
                try {
                    List<ServiceReference> refs = valueTypeSerializerRefs.get(service.getType());
                    if(refs == null){
                        refs = new ArrayList<ServiceReference>(2);
                        valueTypeSerializerRefs.put(service.getType(), refs);
                    }
                    refs.add(reference);
                    if(refs.size() > 1){
                        Collections.sort(refs);
                    }
                } finally {
                    serializerLock.writeLock().unlock();
                }
            }
            return service;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            ValueTypeSerializer<?> vts = (ValueTypeSerializer<?>) service;
            serializerLock.writeLock().lock();
            try {
                List<ServiceReference> refs = valueTypeSerializerRefs.get(vts.getType());
                if(refs != null && refs.remove(reference) && refs.isEmpty()
                        && valueTypeSerializers != null){ //not yet deactivated
                    valueTypeSerializers.remove(vts.getType());
                }
            } finally {
                serializerLock.writeLock().unlock();
            }
            
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
            ValueTypeSerializer<?> vts = (ValueTypeSerializer<?>) service;
            try {
                List<ServiceReference> refs = valueTypeSerializerRefs.get(vts.getType());
                if(refs != null) {
                    refs.remove(reference);
                } else {
                    refs = new ArrayList<ServiceReference>(2);
                    valueTypeSerializerRefs.put(vts.getType(), refs);
                }
                refs.add(reference);
                if(refs.size() > 1){
                    Collections.sort(refs);
                }
            } finally {
                serializerLock.writeLock().unlock();
            }
            
        }
    }

}
