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

import static org.apache.stanbol.enhancer.servicesapi.Chain.PROPERTY_NAME;
import static org.osgi.framework.Constants.OBJECTCLASS;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility similar to {@link ServiceTracker} that allows to track one/some/all
 * {@link Chain}s. As convenience this also implements the
 * {@link ChainManager} interface however the intended usage scenarios 
 * for this utility are considerable different to the using the 
 * ChainManager interface as a service.<p>
 * This utility especially allows to <ul>
 * <li> track only {@link Chains} with names as parsed in the
 * constructor.
 * <li> A {@link ServiceTrackerCustomizer} can be parsed to this utility. The
 * methods of this interface will be called on changes to any service tracked
 * by this instance. This allows users of this utility to update there internal
 * state on any change of the state of tracked chains.
 * </ul>
 * Please not that calls to {@link #open()} and {@link #close()} are required
 * to start and stop the tracking of this class. In general the same rules
 * as for the {@link ServiceTracker} apply also to this utility.
 * 
 * @author Rupert Westenthaler
 *
 */
public class ChainsTracker implements ChainManager{

    private static final Logger log = LoggerFactory.getLogger(ChainsTracker.class);
    
    private Set<String> trackedChains;

    private NameBasedServiceTrackingState nameTracker;
    /**
     * Protected constructor intended to be used by subclasses that do not want
     * to compete the initialisation as part of construction(e.g.
     * implementations of the {@link ChainManager} interface the follow the
     * OSGI component model).<p>
     * Users that use this constructor MUST make sure to call
     * {@link #initChainTracker(BundleContext, Set, ServiceTrackerCustomizer)}. 
     * Note that {@link #initChainTracker()} does NOT call {@link #open()}. <p>
     * Access to the internal state is provided by the protected getters for the
     * {@link ServiceTracker} and the {@link NameBasedServiceTrackingState} and
     * the public {@link #getTrackedChains()} method.
     */
    protected ChainsTracker(){/* nothing to do here */ }
    /**
     * Creates a new {@link ChainsTracker} for the parsed {@link BundleContext}
     * and chain names.
     * Examples:
     * <code><pre>
     *     //Track all active chains
     *     new ChainsTracker(context);
     *     
     *     //Track only the chain with the name "dbpediaLinking"
     *     new ChainsTracker(context,"dbpediaLinking");
     * </pre></code>
     * @param context The bundle context used to track chains
     * @param chainNames the name of the chains to track. If empty 
     * all chains are tracked.
     */
    public ChainsTracker(BundleContext context, String...chainNames){
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        final Set<String> names;
        if(chainNames == null){
            names = Collections.emptySet();
        } else {
            names = new HashSet<String>(Arrays.asList(chainNames));
        }
        initChainTracker(context,names,null);
   }
    /**
     * Creates a new {@link ChainsTracker} for the parsed {@link BundleContext}
     * and chain names.
     * Examples:
     * <code><pre>
     *     //Track all active chains with a customiser
     *     new ChainsTracker(context,null,customiser);
     *     
     *     //Track all chains with the names and use the customiser
     *     //to react on changes
     *     new ChainsTracker(context,chainNames,customiser);
     * </pre></code>
     * @param context the bundle context used to track chains
     * @param chainNames the names of the chains to track. Parse <code>null</code>
     * or an {@link Collections#emptySet()} to track all chains
     * @param customizer the {@link ServiceTrackerCustomizer} used with this tracker.
     */
    public ChainsTracker(BundleContext context, Set<String> chainNames, ServiceTrackerCustomizer customizer){
        if(context == null){
            throw new IllegalArgumentException("The parsed BundleContext MUST NOT be NULL!");
        }
        initChainTracker(context,chainNames,customizer);
    }
    /**
     * Initialises the {@link ChainsTracker} by using the parsed parameter.<p>
     * This will create a copy of the parsed chainNames to avoid changes to the
     * internal state due to external changes.
     * @param context the {@link BundleContext}. MUST NOT be <code>null</code>.
     * @param chainNames the chains to track. <code>null</code> or an empty Set
     * to track all chains
     * This Method can also be used to re-initialise an existing instance. Any
     * existing {@link ServiceTracker} will be closed and the current state
     * will be lost.
     * @param customiser an optional service tracker customiser.
     * @throws IllegalStateException it the parsed {@link BundleContext} is <code>null</code>
     * @throws IllegalArgumentException if the parsed chainNames do only contain
     * invalid Chain names. Even through null values and empty values are removed
     * without failing it is assumed as error if the parsed set only contains
     * such values.
     */
    protected void initChainTracker(BundleContext context, Set<String> chainNames, ServiceTrackerCustomizer customiser) {
        if(nameTracker != null){ //if we re-initialise
            nameTracker.close(); //try to close the current ServiceTracker
        }
        if(context == null){
            throw new IllegalStateException("Unable to initialise tracking if NULL is parsed as Bundle Context!");
        }
        final Set<String> trackedChains;
        if(chainNames == null || chainNames.isEmpty()){
            trackedChains = Collections.emptySet();
            this.trackedChains = Collections.emptySet();
        } else {
            //copy to not modify the parsed set and to avoid internal state
            //to be modified by changes of the parsed set.
            trackedChains = new HashSet<String>(chainNames);
            if(trackedChains.remove(null)){
                log.warn("NULL element was removed from the parsed chain names");
            }
            if(trackedChains.remove("")){
                log.warn("empty String element was removed from the parsed chain names");
            }
            if(trackedChains.isEmpty()){
                throw new IllegalArgumentException("The parsed set with the chainNames " +
                        "contained only invalid chain names. Parse NULL or an empty set" +
                        "if you want to track all chains (parsed: '"+chainNames+"')!");
            }
            this.trackedChains = Collections.unmodifiableSet(chainNames);
        }
        log.info("configured tracking for {}",trackedChains.isEmpty()? "all Chains" : ("the Chains "+trackedChains));
        if(trackedChains.isEmpty()){
            this.nameTracker = new NameBasedServiceTrackingState(context, 
                Chain.class.getName(), PROPERTY_NAME,customiser);
        } else {
            StringBuilder filterString = new StringBuilder();
            filterString.append("(&");
            filterString.append('(').append(OBJECTCLASS).append('=');
            filterString.append(Chain.class.getName()).append(')');
            filterString.append("(|");
            for(String name : trackedChains){
                filterString.append('(').append(PROPERTY_NAME);
                filterString.append('=').append(name).append(')');
            }
            filterString.append("))");
            try {
                this.nameTracker = new NameBasedServiceTrackingState(context,
                    context.createFilter(filterString.toString()), PROPERTY_NAME, customiser);
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Unable to build Filter for the" +
                        "parsed chain names "+trackedChains,e);
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
     * Getter for the list of tracked chain names. This set represents the
     * names of chains tracked by this instance. It does not provide any
     * indication if an {@link Chain} with that name is available
     * or not.<p>
     * If all chains are tracked by this ChainTracker instance this is
     * Indicated by returning an empty Set.
     * @return the tracked chains or an empty set if all chains are tracked.
     */
    public final Set<String> getTrackedChains() {
        return trackedChains;
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.ChainManager#getReference(java.lang.String)
     */
    @Override
    public ServiceReference getReference(String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL or empty");
        }
        if(trackedChains.isEmpty() || trackedChains.contains(name)){
            return nameTracker.getReference(name);
        } else {
            throw new IllegalArgumentException("The Chain with the parsed name '"+
                name+"' is not tracked (tracked: "+trackedChains+")!");
        }
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.ChainManager#isChain(java.lang.String)
     */
    @Override
    public boolean isChain(String name) {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL or empty");
        }
        return nameTracker.getReference(name) != null;
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.ChainManager#getReferences(java.lang.String)
     */
    @Override
    public List<ServiceReference> getReferences(String name) throws IllegalArgumentException {
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("The parsed name MUST NOT be NULL or empty");
        }
        if(trackedChains.isEmpty() || trackedChains.contains(name)){
            List<ServiceReference> refs = nameTracker.getReferences(name);
            if(refs == null){
                refs = Collections.emptyList();
            }
            return refs;
        } else {
            throw new IllegalArgumentException("The chain with the parsed name '"+
                name+"' is not tracked (tracked: "+trackedChains+")!");
        }
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.ChainManager#getActiveChainNames()
     */
    @Override
    public Set<String> getActiveChainNames(){
        return nameTracker.getNames();
    }
    /**
     * Getter for the map with the names and the {@link ServiceReference} of the 
     * chain with the highest priority for that name.
     * @return the map with the names and {@link ServiceReference}s of all
     * currently active and tracked chains
     */
    public Map<String,ServiceReference> getActiveChainReferences(){
        return nameTracker.getActive();
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.ChainManager#getChain(java.lang.String)
     */
    @Override
    public Chain getChain(String name){
        ServiceReference ref = getReference(name);
        return ref == null ? null : (Chain)nameTracker.getService(ref);
    }
    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.ChainManager#getChain(org.osgi.framework.ServiceReference)
     */
    public Chain getChain(ServiceReference chainReference){
        return (Chain)nameTracker.getService(chainReference);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.stanbol.enhancer.servicesapi.ChainManager#getDefault()
     */
    @Override
    public Chain getDefault() {
        Chain chain = getChain(DEFAULT_CHAIN_NAME);
        if(chain == null){
            chain = (Chain)nameTracker.getService();
        }
        return chain;
    }
    /**
     * Getter for the name based service tracker. {@link ServiceReference}s
     * returned by this instance are guaranteed to refer to {@link Chain}
     * services with names that are {@link #getTrackedChains() tracked} by this
     * instance
     * @return the chain tracking state
     */
    protected final NameBasedServiceTrackingState getChainTrackingState() {
        return nameTracker;
    }
}
