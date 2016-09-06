/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.commons.stanboltools.datafileprovider.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
// DO NOT REMOVE - workaround for FELIX-2906 
import java.lang.Integer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProviderEvent;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProviderLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The main DatafileProvider, delegates to other DataFileProvider if 
 *  the requested file is not found in its datafiles folder.
 *  
 *  Must have the highest service ranking of all DatafileProvider, so
 *  that this is the default one which delegates to others.
 */
@Component(immediate=true, metatype=true)
@Service
@Property(name=Constants.SERVICE_RANKING, intValue=Integer.MAX_VALUE)
public class MainDataFileProvider implements DataFileProvider, DataFileProviderLog {
    /**
     * Relative to the "sling.home" or if not present the working directory.
     */
    @Property(value="datafiles")
    public static final String DATA_FILES_FOLDER_PROP = "data.files.folder";

    @Property(intValue=100)
    public static final String MAX_EVENTS_PROP = "max.events";

    private static final Logger log = LoggerFactory.getLogger(MainDataFileProvider.class);

    private File dataFilesFolder;

    private int maxEvents;
    
    /** List of past events, up to maxEvents in size */
    private final List<DataFileProviderEvent> events = new LinkedList<DataFileProviderEvent>();
    
    /** 
     * Tracks providers to which we can delegate. <i>NOTE:</i> this tracker is
     * lazily opened by {@link #getSortedServiceRefs()} as this can not be
     * done during {@link #activate(ComponentContext) activation} as it would
     * result in <code>org.osgi.framework.ServiceException: 
     * ServiceFactory.getService() resulted in a cycle.</code>
     * @see #providersTrackerOpen
     * @see #getSortedServiceRefs()
     */
    private ServiceTracker providersTracker;
    /**
     * Used to track if {@link ServiceTracker#open()} was already called on
     * {@link #providersTracker}
     * @see #providersTracker
     * @see #getSortedServiceRefs()
     */
    private boolean providersTrackerOpen = false; //used for lazily open the tracker
    
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        String folderName = requireProperty(ctx.getProperties(), DATA_FILES_FOLDER_PROP, String.class);
        String slingHome = ctx.getBundleContext().getProperty("sling.home");
        if(slingHome != null){
            dataFilesFolder = new File(slingHome,folderName);
        } else {
            dataFilesFolder = new File(folderName);
        }
        if(!dataFilesFolder.exists()){
            if(!dataFilesFolder.mkdirs()){
                throw new ConfigurationException(DATA_FILES_FOLDER_PROP, "Unable to create the configured Directory "+dataFilesFolder);
            }
        } else if(!dataFilesFolder.isDirectory()){
            throw new ConfigurationException(DATA_FILES_FOLDER_PROP, "The configured DataFile directory "+dataFilesFolder+" does already exists but is not a directory!");
        } //else exists and is a directory!
        maxEvents = requireProperty(ctx.getProperties(), MAX_EVENTS_PROP, Integer.class).intValue();
        providersTracker = new ServiceTracker(ctx.getBundleContext(), DataFileProvider.class.getName(), null);
        providersTrackerOpen = false;
        //NOTE: do not call apen in activate as this do cause a 
        //org.osgi.framework.ServiceException: ServiceFactory.getService() resulted in a cycle.
        //providersTracker.open();
        log.info("Activated, max.events {}, data files folder {}", 
            maxEvents, dataFilesFolder.getAbsolutePath());
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        if(providersTracker != null) {
            synchronized (providersTracker) {
                providersTrackerOpen = false;
                providersTracker.close();
            }
            providersTracker = null;
        }
    }
    
    @SuppressWarnings("unchecked")
    static <ResultType> ResultType requireProperty(Dictionary<?, ?> props, String name, Class<ResultType> clazz) 
    throws ConfigurationException {
        final Object o = props.get(name);
        if(o == null) {
            throw new ConfigurationException(name, "Missing required configuration property: " + name);
        }
        if( !( clazz.isAssignableFrom(o.getClass()))) {
            throw new ConfigurationException(name, "Property is not a " + clazz.getName());
        }
        return (ResultType)o;
    }

    @Override
    public Iterator<DataFileProviderEvent> iterator() {
        // Iterate on a copy of our list to avoid concurrency issues
        final List<DataFileProviderEvent> copy = new LinkedList<DataFileProviderEvent>();
        synchronized (events) {
            copy.addAll(events);
        }
        return copy.iterator();
    }

    @Override
    public int maxEventsCount() {
        return maxEvents;
    }

    @Override
    public int size() {
        return events.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public InputStream getInputStream(String bundleSymbolicName,
            String filename, Map<String, String> comments) throws IOException {
        InputStream result = null;
        String fileUrl = null;
        final File dataFile = getDataFile(bundleSymbolicName, filename);
        // Then, if not found, query other DataFileProviders,
        // ordered by service ranking
        if(dataFile == null) {
            // Sort providers by service ranking
            final List<ServiceReference> refs = getSortedServiceRefs();
            for(ServiceReference ref: refs) {
                final Object o = providersTracker.getService(ref);
                if(o == this) {
                    continue;
                }
                final DataFileProvider dfp = (DataFileProvider)o;
                try {
                    result = dfp.getInputStream(bundleSymbolicName, filename, comments);
                } catch (Exception e) {
                    //Exceptions thrown by an implementation should never
                    //affect the MainDataFileProvider
                    log.debug(String.format("Eception while searching DataFile %s by using provider %s (ignore)",
                        filename,dfp),e);
                }
                if(result == null) {
                    log.debug("{} does not provide file {}", dfp, filename);
                } else {
                    fileUrl = dfp.getClass().getName() + "://" + filename;
                    break; //break as soon as a resource was found
                }
            }
        } else {
            try {
                result =  AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                    @Override
                    public InputStream run() throws IOException {
                        return new FileInputStream(dataFile);
                    }
                });
            } catch (PrivilegedActionException pae) {
                Exception e = pae.getException();
                if(e instanceof IOException){
                    throw (IOException)e;
                } else {
                    throw RuntimeException.class.cast(e);
                }
            }
            fileUrl = dataFile.toURI().toASCIIString();
        }
        
        // Add event
        final DataFileProviderEvent event = new DataFileProviderEvent(
                bundleSymbolicName, filename, 
                comments, fileUrl);
        
        synchronized (events) {
            if(events.size() >= maxEvents) {
                events.remove(0);
            }
            events.add(event);
        }

        if(result == null) {
            throw new IOException("File not found: " + filename);
        }
        
        log.debug("Successfully loaded file {}", event);
        return result;
    }

    /**
     * Getter for the sorted list of service References. THis also lazily
     * opens the {@link ServiceTracker} on the first call.
     * @return the sorted list of service references
     */
    private List<ServiceReference> getSortedServiceRefs() {
        ServiceTracker providersTracker = this.providersTracker;
        if(providersTracker == null){ //already deactivated
            return Collections.emptyList();
        }
        if(!providersTrackerOpen){ //check if we need to open the service tracker
            synchronized (providersTracker) { //sync
                if(!providersTrackerOpen){ //and check again
                    providersTracker.open(); //we need to open it
                }
            }
        }
        final List<ServiceReference> refs = Arrays.asList(providersTracker.getServiceReferences());
        Collections.sort(refs);
        return refs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isAvailable(String bundleSymbolicName, String filename, Map<String,String> comments) {
        String fileUrl = null;
        File dataFile = getDataFile(bundleSymbolicName, filename);
        // Then, if not found, query other DataFileProviders,
        // ordered by service ranking
        if(dataFile == null) {
            // Sort providers by service ranking
            final List<ServiceReference> refs = getSortedServiceRefs();
            Collections.sort(refs);
            for(ServiceReference ref: refs) {
                final Object o = providersTracker.getService(ref);
                if(o == this) {
                    continue;
                }
                final DataFileProvider dfp = (DataFileProvider)o;
                try {
                    if(dfp.isAvailable(bundleSymbolicName, filename, comments)){
                        log.debug("{} does provide file {}", dfp, filename);
                        fileUrl = dfp.getClass().getName() + "://" + filename;
                        break;
                    }
                } catch (RuntimeException e) {
                    log.warn("Exception while checking availability of Datafile " +
                    		"'{}' on DataFileProvider {}",filename,dfp);
                }
            }
        } else {
            log.debug("{} does provide file {}", this, filename);
            fileUrl = dataFile.toURI().toASCIIString();
        }
        
        // Add event
        final DataFileProviderEvent event = new DataFileProviderEvent(
                bundleSymbolicName, filename, 
                comments, fileUrl);
        
        synchronized (events) {
            if(events.size() >= maxEvents) {
                events.remove(0);
            }
            events.add(event);
        }

        return fileUrl != null;
    }
    /**
     * @param bundleSymbolicName
     * @param filename
     * @return
     */
    private File getDataFile(String bundleSymbolicName, final String filename) {
        // First look for the file in our data folder,
        // with and without bundle symbolic name prefix
        final String [] candidateNames = bundleSymbolicName == null ? 
                new String[]{filename} : 
                    new String[]{
                        bundleSymbolicName + "-" + filename,
                        filename
                    };
        return AccessController.doPrivileged(new PrivilegedAction<File>() {
            @Override
            public File run() {
                File dataFile = null;
                for(String name : candidateNames) {
                    dataFile = new File(dataFilesFolder, name);
                    log.debug("Looking for file {}", dataFile.getAbsolutePath());
                    if(dataFile.exists() && dataFile.canRead()) {
                        log.debug("File found in data files folder: {}", filename);
                        break;
                    } else {
                        dataFile = null;
                    }
                }
                return dataFile;
            }
        });
    }
    
    File getDataFilesFolder() {
        return dataFilesFolder;
    }

}
