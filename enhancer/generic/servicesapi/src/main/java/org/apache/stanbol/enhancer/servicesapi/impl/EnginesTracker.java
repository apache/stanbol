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

import static org.apache.stanbol.enhancer.servicesapi.EnhancementEngine.PROPERTY_NAME;
import static org.osgi.framework.Constants.OBJECTCLASS;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility similar to {@link ServiceTracker} that allows to track one/some/all
 * {@link EnhancementEngine}s. As convenience this also implements the
 * {@link EnhancementEngineManager} interface however the intended usage scenarios 
 * for this utility are considerable different to the using the 
 * EnhancementEngineManager interface as a service.<p>
 * This utility especially allows to <ul>
 * <li> track only {@link EnhancementEngine} with names as parsed in the
 * constructor. This allows e.g. a {@link Chain} implementation to keep only
 * track of {@link EnhancementEngine} that are actually referenced by this
 * chain.
 * <li> A {@link ServiceTrackerCustomizer} can be parsed to this utility. The
 * methods of this interface will be called on changes to any service tracked
 * by this instance. This allows users of this utility to update there internal
 * state on any change of the state of tracked engines. This might be especially
 * useful for {@link Chain} implementations that need to update there execution
 * plan on such changes. However this can also be used by 
 * {@link EnhancementJobManager} implementations that want to get notified about
 * enhancement engines referenced by the {@link Chain} the are currently using
 * to enhance an {@link ContentItem}.
 * </ul>
 * Please not that calls to {@link #open()} and {@link #close()} are required
 * to start and stop the tracking of this class. In general the same rules
 * as for the {@link ServiceTracker} apply also to this utility.
 * 
 * @author Rupert Westenthaler
 *
 */
public class EnginesTracker implements EnhancementEngineManager{

    private static final Logger log = LoggerFactory.getLogger(EnginesTracker.class);
    
    private Set<String> trackedEngines;

    private NameBasedServiceTrackingState nameTracker;
    /**
     * Protected constructor intended to be used by subclasses that do not want
     * to compete the initialisation as part of construction(e.g.
     * implementations of the {@link EnhancementEngineManager} interface the 
     * follow the OSGI component model).<p>
     * Users that use this constructor MUST make sure to call
     * {@link #initEngineTracker(BundleContext, Set, ServiceTrackerCustomizer). 
     * Note that initEngineTracker method does NOT call {@link #open()}. <p>
     * Access to the internal state is provided by the protected getters for the
     * {@link ServiceTracker} and the {@link NameBasedServiceTrackingState} and
     * the public {@link #getTrackedEngines()} method.
     */
    protected EnginesTracker(){/* nothing to do here */ }    /**
     * Creates a new {@link EnginesTracker} for the parsed {@link BundleContext}
     * and engine names.
     * Examples:
     * <code><pre>
     *     //Track all active engines
     *     new EnginesTracker(context);
     *     
     *     //Track only the langId engine
     *     new EnginesTracker(context,langId);
     * </pre></code>
     * @param context The bundle context used to track engines
     * @param engineNames the name of the engines to track. If empty 
     * all engines are tracked.
     */
    public EnginesTracker(BundleContext context, String...engineNames){
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        final Set<String> names;
        if(engineNames == null){
            names = Collections.emptySet();
        } else {
            names = new HashSet<String>(Arrays.asList(engineNames));
        }
        initEngineTracker(context,names,null);
    }
    /**
     * Creates a new {@link EnginesTracker} for the parsed {@link BundleContext}
     * and engine names.
     * Examples:
     * <code><pre>
     *     //Track all active engines with a customiser
     *     new EnginesTracker(context,null,customiser);
     *     
     *     //Track all engines referenced by a Chain and use the customiser
     *     //to react on changes
     *     new EnginesTracker(context,chain.getEngineNames(),customiser);
     * </pre></code>
     * @param context the bundle context used to track engines
     * @param engineNames the names of the engines to track. Parse <code>null</code>
     * or an {@link Collections#emptySet()} to track all engines
     * @param customizer the {@link ServiceTrackerCustomizer} used with this tracker.
     */
    public EnginesTracker(BundleContext context, Set<String> engineNames, ServiceTrackerCustomizer customizer){
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        initEngineTracker(context,engineNames,customizer);
    }
    
    /**
     * Initialises the {@link EnginesTracker} by using the parsed parameter.<p>
     * This will create a copy of the parsed engineNames to avoid changes to the
     * internal state due to external changes.<p>
     * This Method can also be used to re-initialise an existing instance. Any
     * existing {@link ServiceTracker} will be closed and the current state
     * will be lost.
     * @param context the {@link BundleContext}. MUST NOT be <code>null</code>.
     * @param engineNames the engines to track. <code>null</code> or an empty Set
     * to track all engines
     * @param customiser an optional service tracker customiser.
     * @throws IllegalStateException it the parsed {@link BundleContext} is <code>null</code>
     * @throws IllegalArgumentException if the parsed engineNames do only contain
     * invalid engine names. Even through null values and empty values are removed
     * without failing it is assumed as error if the parsed set only contains
     * such values.
     */
    protected void initEngineTracker(BundleContext context, Set<String> engineNames, ServiceTrackerCustomizer customiser) {
        if(nameTracker != null){ //if this is a re-initialisation
            nameTracker.close(); //try to close the existing service tracker instance
        }
        if(context == null){
            throw new IllegalStateException("Unable to initialise tracking if NULL is parsed as Bundle Context!");
        }
        final Set<String> trackedEngines;
        if(engineNames == null || engineNames.isEmpty()){
            trackedEngines = Collections.emptySet();
            this.trackedEngines = Collections.emptySet();
        } else {
            //copy to not modify the parsed set and to avoid internal state
            //to be modified by changes of the parsed set.
            trackedEngines = new HashSet<String>(engineNames);
            if(trackedEngines.remove(null)){
                log.warn("NULL element was removed from the parsed engine names");
            }
            if(trackedEngines.remove("")){
                log.warn("empty String element was removed from the parsed engine names");
            }
            if(trackedEngines.isEmpty()){
                throw new IllegalArgumentException("The parsed set with the engineNames " +
                        "contained only invalid Engine names. Parse NULL or an empty set" +
                        "if you want to track all engines (parsed: '"+engineNames+"')!");
            }
            this.trackedEngines = Collections.unmodifiableSet(trackedEngines);
        }
        log.info("configured tracking for {}",trackedEngines.isEmpty()? "all Engines" : ("the Engines "+trackedEngines));
        if(trackedEngines.isEmpty()){
            this.nameTracker = new NameBasedServiceTrackingState(context, EnhancementEngine.class.getName(), PROPERTY_NAME, customiser);
        } else {
            StringBuilder filterString = new StringBuilder();
            filterString.append("(&");
            filterString.append('(').append(OBJECTCLASS).append('=');
            filterString.append(EnhancementEngine.class.getName()).append(')');
            filterString.append("(|");
            for(String name : trackedEngines){
                filterString.append('(').append(EnhancementEngine.PROPERTY_NAME);
                filterString.append('=').append(name).append(')');
            }
            filterString.append("))");
            try {
                this.nameTracker = new NameBasedServiceTrackingState(context,
                    context.createFilter(filterString.toString()), PROPERTY_NAME, customiser);
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Unable to build Filter for the" +
                        "parsed Engine names "+trackedEngines,e);
            }
        }
    }
    /**
     * Starts tracking based on the configuration parsed in the constructor
     */
    public void open(){
        nameTracker.open();
    }
    /**
     * Closes this tracker
     */
    public void close(){
        nameTracker.close();
        nameTracker = null;
    }
    /**
     * Getter for the list of tracked engine names. This set represents the
     * names of engines tracked by this instance. It does not provide any
     * indication if an {@link EnhancementEngine} with that name is available
     * or not.<p>
     * If all engines are tracked by this EngineTracker instance this is
     * Indicated by returning an empty Set.
     * @return the tracked engines or an empty set if all engines are tracked.
     */
    public final Set<String> getTrackedEngines() {
        return trackedEngines;
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager#getReference(java.lang.String)
     */
    @Override
    public ServiceReference getReference(String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL or empty");
        }
        if(trackedEngines.isEmpty() || trackedEngines.contains(name)){
            return nameTracker.getReference(name);
        } else {
            throw new IllegalArgumentException("The Engine with the parsed name '"+
                name+"' is not tracked (tracked: "+trackedEngines+")!");
        }
    }
    @Override
    public boolean isEngine(String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL or empty");
        }
        return nameTracker.getReference(name) != null;
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager#getReferences(java.lang.String)
     */
    @Override
    public List<ServiceReference> getReferences(String name) throws IllegalArgumentException {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL or empty");
        }
        if(trackedEngines.isEmpty() || trackedEngines.contains(name)){
            List<ServiceReference> refs = nameTracker.getReferences(name);
            if(refs == null){
                refs = Collections.emptyList();
            }
            return refs;
        } else {
            throw new IllegalArgumentException("The Engine with the parsed name '"+
                name+"' is not tracked (tracked: "+trackedEngines+")!");
        }
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager#getActiveEngineNames()
     */
    @Override
    public Set<String> getActiveEngineNames(){
        return nameTracker.getNames();
    }
    /**
     * Getter for the map with the names and the {@link ServiceReference} of the 
     * engine with the highest priority for that name.
     * @return the map with the names and {@link ServiceReference}s of all
     * currently active and tracked engines
     */
    public Map<String,ServiceReference> getActiveEngineReferences(){
        return nameTracker.getActive();
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager#getEngine(java.lang.String)
     */
    @Override
    public EnhancementEngine getEngine(String name){
        ServiceReference ref = getReference(name);
        return ref == null ? null : (EnhancementEngine)nameTracker.getService(ref);
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager#getEngine(org.osgi.framework.ServiceReference)
     */
    public EnhancementEngine getEngine(ServiceReference engineReference){
        return (EnhancementEngine)nameTracker.getService(engineReference);
    }
    /**
     * Getter for the name based service tracker. {@link ServiceReference}s
     * returned by this instance are guaranteed to refer to {@link EnhancementEngine}
     * services with names that are {@link #getTrackedEngines() tracked} by this
     * instance
     * @return the engine tracking state
     */
    protected final NameBasedServiceTrackingState getEngineTrackingState() {
        return nameTracker;
    }
}
