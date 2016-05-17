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
package org.apache.stanbol.commons.stanboltools.datafileprovider.impl.tracking;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
//DO NOT REMOVE - workaround for FELIX-2906 
import java.lang.Integer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileListener;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileTracker;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the {@link DataFileTracker} interface. This implementation
 * can work within and outside of an OSGI Environment.<p>
 * When within an OSGI Environment tracking in started by 
 * {@link #activate(ComponentContext)}and stopped by 
 * {@link #deactivate(ComponentContext)} the required {@link DataFileProvider} is
 * injected.<p>
 * Outside an OSGI Environment the {@link DataFileProvider} needs to be
 * provided by the constructor (DO NOT use the default constructor).
 * Tracking is started and stopped by calls to the {@link #startTracking()} and
 * {@link #stopTracking()} methods.
 * @author Rupert Westenthaler.
 *
 */
@Component(immediate=true, metatype=true)
@Service
@Property(name=Constants.SERVICE_RANKING, intValue=Integer.MAX_VALUE)
public final class DataFileTrackerImpl implements DataFileTracker {
    
    protected final Logger log = LoggerFactory.getLogger(DataFileTrackerImpl.class);
    /**
     * The default interval set to five seconds.
     */
    public static final long DEFAULT_INTERVAL = 5000;
    /**
     * The minimum interval between two polls is set to 0.5 seconds.
     */
    private static final long MIN_INTERVAL = 500;

    @Property(longValue=DEFAULT_INTERVAL)
    public static final String PROPERTY_TRACKING_INTERVAL = "data.files.tracking.interval";
    
    /**
     * The {@link DataFileProvider} used for tracking. Injected by OSGI or
     * paresed by the constructor (outside OSGI). <p>
     * Do not directly access (use {@link #getDataFileProvider()}) and store
     * a local reference in case of multiple requests because within OSGI this 
     * uses {@link ReferencePolicy#DYNAMIC} and
     * {@link ReferenceStrategy#EVENT} to avoid a service restart
     * on a change of the {@link DataFileProvider} with the highest
     * {@link Constants#SERVICE_RANKING}. Service restarts cause all existing 
     * registrations to track data files to be lost!.
     */
    @Reference(
        cardinality=ReferenceCardinality.MANDATORY_UNARY,
        policy=ReferencePolicy.DYNAMIC,
        strategy=ReferenceStrategy.EVENT,
        bind="bindDataFileProvider",
        unbind="unbindDataFileProvider")
    protected DataFileProvider _dataFileProvider;
    
    /**
     * Getter for the current {@link DataFileProvider} instance used to track
     * data files. This value may change dynamically without a restart of this
     * Service.
     * @return The current {@link DataFileProvider} instance
     * @see #_dataFileProvider
     */
    protected DataFileProvider getDataFileProvider(){
        return _dataFileProvider;
    }
    /**
     * Bind and Update method for {@link #_dataFileProvider}
     * @param dfp
     */
    protected void bindDataFileProvider(DataFileProvider dfp){
        _dataFileProvider = dfp;
    }
    /**
     * Unbind method for {@link #_dataFileProvider}
     * @param dfp
     */
    protected void unbindDataFileProvider(DataFileProvider dfp){
        if(dfp != null && dfp.equals(_dataFileProvider)){
            _dataFileProvider = null;
        } //else ignore
    }
    
    /**
     * The current interval inbetween the end of the previous and the start of
     * the next poll for tracked resources. 
     */
    protected long interval;
    /**
     * Internally used to shutdown the {@link TrackingDaemon}
     */
    protected boolean active = false;

    /**
     * Default constructor used by OSGI. This expects that the
     * {@link #_dataFileProvider} is injected during activation
     */
    public DataFileTrackerImpl() {
    }
    /**
     * Constructor to be used outside an OSGI environment to manually
     * instantiate a {@link DataFileTracker} for a given provider
     * @param provider the provider used to track data files
     */
    public DataFileTrackerImpl(DataFileProvider provider){
        if(provider == null){
            throw new IllegalArgumentException("The parsed DataFileProvider MUST NOT be NULL!");
        }
        bindDataFileProvider(provider);
    }
    
    @Activate
    protected void activate(ComponentContext context){
        Object value = context.getProperties().get(PROPERTY_TRACKING_INTERVAL);
        if(value instanceof Number){
            setInterval(((Number)value).longValue());
        } else if(value != null){
            try {
                setInterval(new BigDecimal(value.toString()).longValue());
            } catch (NumberFormatException e){ 
                log.warn("Value of property '"+value+"' can not be converted into a LONG",e);
            } catch (ArithmeticException e) { 
                log.warn("Value of property '"+value+"' can not be converted into a LONG",e);
            }
        }
        //do not start here ... only if the first resource is added
        //startTracking();
    }
    @Deactivate
    protected void deactivate(ComponentContext context){
        stopTracking();
        setInterval(DEFAULT_INTERVAL);
        trackedResources.clear();
    }
    /**
     * @return the interval
     */
    public final long getInterval() {
        return interval;
    }
    
    /**
     * Setter for the interval between two polls. If the parsed interval is
     * smaller equals zero the interval is set to the {@link #DEFAULT_INTERVAL}.
     * If the parsed interval bigger than zero but lower than the 
     * {@link #MIN_INTERVAL} the value is set to MIN_INTERVAL.
     * @param interval the interval to set
     */
    public final void setInterval(long interval) {
        if(interval <= 0){
            this.interval = DEFAULT_INTERVAL;
        } else if(interval < MIN_INTERVAL){
            this.interval = MIN_INTERVAL;
        } else {
            this.interval = interval;
        }
    }
    private final Map<DataFileReference,TrackingState> trackedResources = Collections.synchronizedMap(
        new HashMap<DataFileReference,TrackingState>());
        
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTracker#add(org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTrackerImpl.ResourceListener, java.lang.String)
     */
    @Override
    public void add(DataFileListener resourceListener, String name,Map<String,String> properties){
        add(resourceListener,null,name,properties);
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTracker#add(org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTrackerImpl.ResourceListener, java.lang.String, java.lang.String)
     */
    @Override
    public void add(DataFileListener resourceListener, String bundleSymbolicName, String name, Map<String,String> properties){
        if(resourceListener == null){
            throw new IllegalArgumentException("The parsed ResourceListener MUST NOT be NULL!");
        }
        DataFileReference r = new DataFileReference(bundleSymbolicName, name, properties);
        synchronized (trackedResources) {
            TrackingState trackingState = trackedResources.get(r);
            if(trackingState == null){ //add new
                trackingState = new TrackingState();
                trackedResources.put(r, trackingState);
            }
            trackingState.addListener(resourceListener);
            if(!trackedResources.isEmpty()){ //maybe this was the first added RDFTerm
                startTracking(); //so me might want to start tracking
            }
        }
    }
    @Override
    public boolean isTracked(String bundleSymbolicName,String resourceName) {
        return trackedResources.containsKey(new DataFileReference(bundleSymbolicName, resourceName));
    }
    @Override
    public boolean isTracked(DataFileListener resourceListener, String bundleSymbolicName,String resourceName) {
        TrackingState state = trackedResources.get(new DataFileReference(bundleSymbolicName, resourceName));
        return state != null ? state.isListener(resourceListener) : false;
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTracker#remove(org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTrackerImpl.ResourceListener, java.lang.String)
     */
    public void remove(DataFileListener resourceListener, String resource){
        remove(resourceListener,null,resource);
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTracker#remove(org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTrackerImpl.ResourceListener, java.lang.String, java.lang.String)
     */
    public void remove(DataFileListener resourceListener, String bundleSymbolicName, String name){
        if(resourceListener != null && name != null && !name.isEmpty()){
            DataFileReference r = new DataFileReference(bundleSymbolicName,name);
            synchronized (trackedResources) {
                TrackingState state = trackedResources.get(r);
                if(state != null && state.removeListener(resourceListener) != null && state.isEmpty()){
                    trackedResources.remove(r);
                }
                if(trackedResources.isEmpty()){ //no tracked resources left?
                    stopTracking(); //stop tracking until others are added
                }
            }
        } //else ignore
    }

    /* (non-Javadoc)
     * @see org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTracker#removeAll(org.apache.stanbol.commons.stanboltools.datafileprovider.impl.ResourceTrackerImpl.ResourceListener)
     */
    public void removeAll(DataFileListener resourceListener){
        if(resourceListener != null){
            synchronized (trackedResources) {
                //try to remove the listener for all tracked resources
                Iterator<Entry<DataFileReference,TrackingState>> entries = trackedResources.entrySet().iterator();
                while(entries.hasNext()){
                    Entry<DataFileReference,TrackingState> entry = entries.next();
                    if(entry.getValue().removeListener(resourceListener) != null && entry.getValue().isEmpty()){
                        //and remove tracked resources if no others are listening
                        entries.remove();
                    }
                }
                if(trackedResources.isEmpty()){ //no tracked resources left?
                    stopTracking(); //stop tracking until others are added
                }
            }
        }
    }
    /**
     * used to start a new {@link TrackingDaemon} e.g. if the first resource
     * to be tracked was added to {@link #trackedResources}.
     * If tracking is already active this method has no effect
     */
    private void startTracking(){
        if(!active){
            log.info("start Tracking ...");
            active=true;
            Thread t = new Thread(new TrackingDaemon(), "DataFileTrackingDaemon");
            t.setDaemon(true);
            t.start();
        } //else already active
    }
    /**
     * Used to stop the {@link TrackingDaemon} e.g. if the last tracked
     * resource was removed, or this component is deactivated
     * @see #deactivate(ComponentContext)
     * @see #close()
     */
    private void stopTracking(){
        active = false;
    }
    /**
     * Can be used outside an OSGI environment to close this instance and
     * stop tracking for all currently registered sources
     */
    public void close(){
        deactivate(null);
    }
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    protected class TrackingDaemon implements Runnable {

        public void run(){
            while(active){
                //a clone of the tracked resource
                Set<DataFileReference> resources;
                synchronized (trackedResources) {
                    resources = new HashSet<DataFileReference>(trackedResources.keySet());
                }
                DataFileProvider dataFileProvider = getDataFileProvider();
                if(dataFileProvider == null){
                    log.info("Currently no DataFileProvider available");
                } else {
                    log.debug("Track {} resources",resources.size());
                    for(DataFileReference resoruce : resources){
                        TrackingState resourceState;
                        synchronized (trackedResources) {
                            resourceState = trackedResources.get(resoruce);
                        }
                        if(log.isDebugEnabled()){
                            log.debug(" > {} (state:{})",resoruce.getName(),
                                resourceState != null ? 
                                        resourceState.getTrackingState() != null ?
                                                resourceState.getTrackingState() : "none" :
                                                    null);
                        }
                        if(resourceState != null){ //might be null if removed in the meantime
                            STATE state;
                            if(dataFileProvider.isAvailable(resoruce.getBundleSymbolicName(), 
                                    resoruce.getName(), resoruce.getProperties())){
                                state = STATE.AVAILABLE;
                            } else {
                                state = STATE.UNAVAILABLE;
                            }
                            fire(dataFileProvider,resoruce,resourceState,state);
                        }
                    }
                }
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    log.debug("interrupped",e);
                }
            }
            log.info(" ... tracking stopped!");
        }
        /**
         * Processes the the tracking event and fires events to the {@link DataFileListener}
         * registered for the parsed resource
         * @param dataFileProvider the {@link DataFileProvider} used to get an
         * {@link InputStream} per {@link DataFileListener} that needs to be notified
         * @param resoruce the resource
         * @param trackinState the {@link DataFileListener} and {@link STATE}s for that
         * resource
         * @param state the updated state of the parsed resource
         * @param is the {@link InputStream} already available from the tracking
         * process or <code>null</code> in case the parsed state is {@link STATE#UNAVAILABLE}.
         * If parsed the stream will be {@link InputStream#close() closed} even if
         * not used.
         */
        private void fire(DataFileProvider dataFileProvider, DataFileReference resoruce, TrackingState trackinState, STATE state) {
            InputStream is = null;
            //iterate over clone to avoid concurrent modification exceptions
            //by external and/or this calls to remove(..)
            for(Entry<DataFileListener,STATE> listenerState : trackinState){
                if(listenerState.getValue() != state){
                    boolean remove = false;
                    switch (state) {
                        case UNAVAILABLE:
                            try {
                                remove = listenerState.getKey().unavailable(resoruce.getName());
                                trackinState.updateListener(listenerState.getKey(), state);
                            } catch (Exception e) {
                                log.warn("Exception from Listener '"+listenerState.getKey()+
                                    "' while calling unavailable for resource '"+
                                    resoruce.getName()+"'!",e);
                                //the state to ERROR ... will try again on the next check
                                trackinState.updateListener(listenerState.getKey(), STATE.ERROR);
                            }
                            break;
                        case AVAILABLE:
                            if(is == null){ //get a new InputStream
                                try {
                                    is = dataFileProvider.getInputStream(resoruce.getBundleSymbolicName(), 
                                        resoruce.getName(), resoruce.getProperties());
                                } catch (IOException e) {
                                    //now again unavailable ?! 
                                    //Send anyway with null as stream
                                    is = null;
                                }
                            }
                            try {
                                remove = listenerState.getKey().available(resoruce.getName(), is);
                                trackinState.updateListener(listenerState.getKey(), state);
                            } catch (Exception e) {
                                log.warn("Exception from Listener '"+listenerState.getKey()+
                                    "' while calling available for resource '"+
                                    resoruce.getName()+"'!",e);
                                //the state is no changed ... will try again on the next check
                                trackinState.updateListener(listenerState.getKey(), STATE.ERROR);
                            }
                            is = null; //we need a new InputStream getName() each resource
                            break;
                        // ignore  other values
                    }
                    if(remove){ //remove if requested
                        remove(listenerState.getKey(), resoruce.getBundleSymbolicName(), resoruce.getName());
                    }
                } //else this listener is already in the notified state ... ignore
            }
//            if(is != null) { //the created InputStream was not sent to a Listener
//                try {
//                    is.close(); //clean up
//                } catch (IOException e) {
//                    //ignore
//                }
//            }
        }
    }
}
