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
package org.apache.stanbol.enhancer.servicesapi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to manage the state of ServiceReferences for services that are 
 * accessed based on the value of a specific property. <p>
 * This can be used to track both {@link EnhancementEngine}s as well as
 * {@link Chain}s.<p>
 * This implementation supports the use of {@link #readLock()} on returned
 * values. Also the <code>null</code> as value for the parsed name property.
 * 
 * @author Rupert Westenthaler
 *
 */
public class NameBasedServiceTrackingState extends ServiceTracker implements ServiceTrackerCustomizer {
    
    private final Logger log = LoggerFactory.getLogger(NameBasedServiceTrackingState.class);
    
//    /**
//     * Allows to forward to an other customiser after this class has finished
//     * his work
//     */
    private final ServiceTrackerCustomizer customizer;
    /**
     * Lock used to protect acces to {@link #state} and {@link #tracked}
     */
    ReadWriteLock lock = new ReentrantReadWriteLock();
    /**
     * This member uses lazzy initialisation and is set back to <code>null</code>
     * after every change of a tracked service. Use {@link #getState()}
     * to access this the internal state.<p>
     * Mapping from the names to the {@link ServiceReference}s of the
     * Engines. ServiveReferences are sorted by {@link Constants#SERVICE_RANKING}.<p>
     * Note that values of Entries are not modified on changes but replaced
     * by new instances. Because of this one needs only sync the retrieval
     * but not further accesses to the list.
     */
    private Map<String, List<ServiceReference>> state_;
    /**
     * Property used to retrieve the name of the component via the properties
     * provided by the {@link ServiceReference}
     */
    private final String property;
    /**
     * Creates a trackingState with an optional customiser
     * @param context the {@link BundleContext} used for tracking. MUST NOT
     * be <code>null</code>.
     * @param filter The filter used for the ServiceTracker
     * @param nameProperty the property used to lookup the name of the tracked
     * services. This MUST NOT be <code>null</code> nor empty.
     * @param customizer optionally a customiser used with this tracker
     * @throws IllegalArgumentException it the parsed nameProperty is 
     * <code>null</code> or an empty string.
     */
    public NameBasedServiceTrackingState(BundleContext context, Filter filter, String nameProperty, ServiceTrackerCustomizer customizer){
        super(context, filter, null);
        if(nameProperty == null || nameProperty.isEmpty()){
            throw new IllegalArgumentException("the property to lookup the " +
            		"name of tracked services MUST NOT be NULL nor emtoy!");
        }
        this.property = nameProperty;
        this.customizer = customizer;
    }
    /**
     * Creates a trackingState with an optional customiser
     * @param context the {@link BundleContext} used for tracking. MUST NOT
     * be <code>null</code>.
     * @param clazz The type of the tracked services
     * @param nameProperty the property used to lookup the name of the tracked
     * services. This MUST NOT be <code>null</code> nor empty.
     * @param customizer optionally a customiser used with this tracker
     * @throws IllegalArgumentException it the parsed nameProperty is 
     * <code>null</code> or an empty string.
     */
    public NameBasedServiceTrackingState(BundleContext context, String clazz, String nameProperty, ServiceTrackerCustomizer customizer){
        super(context, clazz, null);
        if(nameProperty == null || nameProperty.isEmpty()){
            throw new IllegalArgumentException("the property to lookup the " +
                    "name of tracked services MUST NOT be NULL nor emtoy!");
        }
        this.property = nameProperty;
        this.customizer = customizer;
    }
    /**
     * Getter for the read only set of names of all currently active and 
     * tracked engines
     * @return the names of all currently active and tracked engines
     */
    public Set<String> getNames() {
        Map<String, List<ServiceReference>> state = getState();
        lock.writeLock().lock();
        try {
            return Collections.unmodifiableSet(state.keySet());
        } finally {
            lock.writeLock().unlock();
        }
    }
    /**
     * Getter for the read only names -&gt; {@link ServiceReference} of the 
     * currently active and tracked engines.
     * @return the name -&gt; {@link ServiceReference} mapping of all active
     * engines.
     */
    public Map<String,ServiceReference> getActive() {
        Map<String,List<ServiceReference>> state = getState();
        Map<String,ServiceReference> active = new HashMap<String,ServiceReference>(state.size());
        for(Entry<String,List<ServiceReference>> entry : state.entrySet()){
            active.put(entry.getKey(), entry.getValue().get(0));
        }
        return Collections.unmodifiableMap(active);
    }
    @Override
    public Object addingService(ServiceReference reference) {
        lock.writeLock().lock();
        try {
            this.state_ = null;
        } finally {
            lock.writeLock().unlock();
        }
        Object service;
        log.debug(" ... adding service {}",reference);
        //try { //NOT sure if we should catch exceptions here
            if(customizer != null){
                service =  customizer.addingService(reference);
            } else {
                service =  context.getService(reference);
            }
        //} catch(RuntimeException e){
        //    log.warn(" ... unable to get Service for Reference: " + reference 
        //        + " because a " + e.getClass().getSimpleName() + " with message: "
        //        + e.getMessage());
        //    return null;
        //}
        return service;
    }
    @Override
    public void modifiedService(ServiceReference reference, Object service) {
        lock.writeLock().lock();
        try {
            this.state_ = null;
        } finally {
            lock.writeLock().unlock();
        }
        if(customizer != null){
            customizer.modifiedService(reference, service);
        } //else nothing to do
    }
    /**
     * Looks like {@link #modifiedService(ServiceReference, Object)} is not 
     * called on property changes (add is used instead). Parsed
     * {@link ServiceReference} objects are not equals even if they are for the
     * same {@link ServiceRegistration}. So the only way to keep the state in
     * sync is to rebuild it after every change to a tracked service.
     */
    private Map<String,List<ServiceReference>> getState() {
        lock.writeLock().lock();
        try {
            if(this.state_ == null){
            //Temporary map to collect the values
                Map<String,List<ServiceReference>> tmp = new HashMap<String,List<ServiceReference>>();
                ServiceReference[] serviceRefs = getServiceReferences();
                if(serviceRefs != null){
                    for(ServiceReference ref : serviceRefs){
                        String name = (String)ref.getProperty(property);
                        List<ServiceReference> refs = tmp.get(name);
                        if(refs == null){
                            refs = new ArrayList<ServiceReference>(3);
                            tmp.put(name, refs);
                        }
                        refs.add(ref);
                    }
                }
                //now iterate a second time to sort and make values immutable an
                Map<String,List<ServiceReference>> state = new HashMap<String,List<ServiceReference>>(tmp.size());
                for(Entry<String,List<ServiceReference>> entry : tmp.entrySet()){
                    if(entry.getValue().size() > 1){
                        Collections.sort(entry.getValue(),ServiceReferenceRankingComparator.INSTANCE);
                    }
                    state.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
                }
                this.state_ = Collections.unmodifiableMap(state);
            }
            return state_;
        }finally{
            lock.writeLock().unlock();
        }
    }
    @Override
    public void removedService(ServiceReference reference, Object service) {
        lock.writeLock().lock();
        try {
            this.state_ = null;
        } finally {
            lock.writeLock().unlock();
        }
        if(customizer != null){
            customizer.removedService(reference, service);
        } else {
            context.ungetService(reference);
        }
    }
    
    public List<ServiceReference> getReferences(String name){
        Map<String,List<ServiceReference>> state = getState();
        List<ServiceReference> refs = state.get(name);
        return refs == null ? null : Collections.unmodifiableList(refs);
    }
    public ServiceReference getReference(String name){
        Map<String,List<ServiceReference>> state = getState();
        List<ServiceReference> refs = state.get(name);
        return refs == null ? null : refs.get(0);
    }
}