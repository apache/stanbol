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
package org.apache.stanbol.commons.namespaceprefix.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.namespaceprefix.NamespaceMappingUtils;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixProvider;
import org.apache.stanbol.commons.namespaceprefix.NamespacePrefixService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate=true)
@Service(value={NamespacePrefixProvider.class,NamespacePrefixService.class})
@Properties(value={
   @Property(name=Constants.SERVICE_RANKING, intValue=Integer.MAX_VALUE)
})
public class StanbolNamespacePrefixService implements NamespacePrefixService, NamespacePrefixProvider {

    protected static final Logger log = LoggerFactory.getLogger(StanbolNamespacePrefixService.class);
    
    private static final String PREFIX_MAPPINGS = "namespaceprefix.mappings";

    private static NamespacePrefixService INSTANCE = null;
    private File mappingsFile;
    
    // OSGI service references
    private ServiceTracker providersTracker;
    protected ServiceReference[] __sortedProviderRef = null;
    //non-OSGI service references
    ServiceLoader<NamespacePrefixProvider> loader;
    private ReadWriteLock mappingsLock = new ReentrantReadWriteLock();
    private SortedMap<String,String> prefixMap = new TreeMap<String,String>();
    private SortedMap<String,List<String>> namespaceMap = new TreeMap<String,List<String>>();

    private BundleContext bundleContext;
    
    /**
     * OSGI constructor <b> DO NOT USE</b> outside of an OSGI environment as this
     * will not initialise the {@link NamespacePrefixProvider} using the
     * {@link ServiceLoader} utility!
     */
    public StanbolNamespacePrefixService(){}
    
    /**
     * Constructs an Stanbol NamespacePrefixService and initialises other
     * {@link NamespacePrefixProvider} implementations using the
     * Java {@link ServiceLoader} utility.
     * @param mappingFile the mapping file used to manage local mappings. If
     * <code>null</code> no president local mappings are supported.
     * @throws IOException
     */
    public StanbolNamespacePrefixService(File mappingFile) throws IOException {
        this.mappingsFile = mappingFile;
        if(mappingsFile != null && mappingsFile.isFile()){
            readPrefixMappings(new FileInputStream(mappingsFile));
        } //else no mappings yet ... nothing todo
        loader = ServiceLoader.load(NamespacePrefixProvider.class);
        
    }
    /**
     * Imports tab separated prefix mappings from the parsed Stream
     * @param in
     * @throws IOException
     */
    public void importPrefixMappings(InputStream in) throws IOException {
        readPrefixMappings(in);
    }
    
    public static NamespacePrefixService getInstance(){
        if(INSTANCE == null){
            try {
                INSTANCE = new StanbolNamespacePrefixService(new File(PREFIX_MAPPINGS));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read "+PREFIX_MAPPINGS,e);
            }
        }
        return INSTANCE;
    }
    
    @Activate
    protected void activate(ComponentContext ctx) throws FileNotFoundException, IOException{
        bundleContext = ctx.getBundleContext();
        //(1) read the mappings
        mappingsFile = bundleContext.getDataFile(PREFIX_MAPPINGS);
        if(mappingsFile.isFile()){
            readPrefixMappings(new FileInputStream(mappingsFile));
        } //else no mappings yet ... nothing todo
    }

    /**
     * 
     */
    private void openTracker() {
        providersTracker = new ServiceTracker(bundleContext, NamespacePrefixProvider.class.getName(),
            new ServiceTrackerCustomizer() {
            
            @Override
            public void removedService(ServiceReference reference, Object service) {
                bundleContext.ungetService(reference);
                __sortedProviderRef = null;
            }
            
            @Override
            public void modifiedService(ServiceReference reference, Object service) {
                __sortedProviderRef = null;
            }
            
            @Override
            public Object addingService(ServiceReference reference) {
                Object service = bundleContext.getService(reference);
                if(StanbolNamespacePrefixService.this.equals(service)){//we need not to track this instance
                    bundleContext.ungetService(reference);
                    return null;
                }
                __sortedProviderRef = null;
                return service;
            }
        });
        providersTracker.open();
    }
    /**
     * Expected to be called only during activation
     * @param in
     * @throws IOException
     */
    private void readPrefixMappings(InputStream in) throws IOException {
        LineIterator it = IOUtils.lineIterator(in, "UTF-8");
        while(it.hasNext()){
            String mapping = it.nextLine();
            if(mapping.charAt(0) != '#'){
                int sep = mapping.indexOf('\t');
                if(sep < 0 || mapping.length() <= sep+1){
                    log.warn("Illegal prefix mapping '{}'",mapping);
                } else {
                    String old = addMapping(mapping.substring(0, sep),mapping.substring(sep+1),false);
                    if(old != null){
                        log.info("Duplicate mention of prefix {}. Override mapping from {} to {}",
                            new Object[]{mapping.substring(0, sep), old, mapping.substring(sep+1)});
                    }
                }
            } else { //comment
                log.debug(mapping);
            }
        }
    }
    private void writePrefixMappings(OutputStream os) throws IOException {
        mappingsLock.readLock().lock();
        try {
            Collection<String> lines = new ArrayList<String>(prefixMap.size()+1);
            lines.add(String.format("# %d mappings written at %2$tY-%2$tm-%2$teT%2$TH:%2$TM:%2$TS",
                prefixMap.size(),new GregorianCalendar(TimeZone.getTimeZone("GMT"))));
            for(Entry<String,String> mapping : prefixMap.entrySet()){
                lines.add(String.format("%s/t%s", mapping.getKey(),mapping.getValue()));
            }
            IOUtils.writeLines(lines, null, os, "UTF-8");
        } finally {
            mappingsLock.readLock().unlock();
        }
    }
    /**
     * Internally used to add an mapping
     * @param prefix the prefix
     * @param namespace the namespace
     * @param store if the added mapping should be stored to the {@link #mappingsFile}
     * @return the previouse mapping or <code>null</code> if none.
     */
    private String addMapping(String prefix, String namespace, boolean store) throws IOException {
        mappingsLock.writeLock().lock();
        try {
            String old = prefixMap.put(prefix, namespace);
            if(!namespace.equals(old)){ //if the mapping changed
                boolean failed = false; //used for rollback in case of an exception
                try {
                    //(1) persist the mapping
                    if(store){
                        if(mappingsFile != null){
                            if(!mappingsFile.isFile()){
                                if(!mappingsFile.createNewFile()){
                                    throw new IOException("Unable to create mapping file "+mappingsFile);
                                }
                            }
                            writePrefixMappings(new FileOutputStream(mappingsFile, false));
                        } //else do not persist mappings
                    }
                    //(2) update the inverse mappings (ensure read only lists!)
                    List<String> prefixes = namespaceMap.get(namespace);
                    if(prefixes == null){
                        namespaceMap.put(namespace, Collections.singletonList(prefix));
                    } else {
                        String[] ps = new String[prefixes.size()+1];
                        int i=0;
                        for(;i<prefixes.size();i++){
                            ps[i] = prefixes.get(i);
                        }
                        ps[i] = prefix;
                        namespaceMap.put(namespace, Arrays.asList(ps));
                    }
                } catch (IOException e) {
                    failed = true;
                    throw e;
                } catch (RuntimeException e) {
                    failed = true;
                    throw e;
                } finally {
                    if(failed){ //rollback
                        if(old == null){
                            prefixMap.remove(prefix);
                        } else {
                            prefixMap.put(prefix, old);
                        }
                    }
                }
            }
            return old;
        } finally {
            mappingsLock.writeLock().unlock();
        }
    }
    private ServiceReference[] getSortedProviderReferences(){
        ServiceReference[] refs = __sortedProviderRef;
        if(bundleContext != null){ //OSGI variant
            if(providersTracker == null){ //lazy initialisation of the service tracker
                synchronized (this) {
                    if(providersTracker == null && bundleContext != null){
                        openTracker();
                    }
                }
            }
            //the check for the size ensures that registered/unregistered services
            //are not overlooked when that happens during this method is executed
            //by an other thread.
            if(refs == null || refs.length != providersTracker.size()){
                ServiceReference[] r = providersTracker.getServiceReferences();
                refs = Arrays.copyOf(r,r.length); //copy
                Arrays.sort(refs);
                this.__sortedProviderRef = refs;
            }
        } else if(refs == null){ //non OSGI variant
            List<ServiceReference> refList = new ArrayList<ServiceReference>();
            Iterator<NamespacePrefixProvider> it = loader.iterator();
            while(it.hasNext()){
                refList.add(new NonOsgiServiceRef<NamespacePrefixProvider>(it.next()));
            }
            refs = refList.toArray(new ServiceReference[refList.size()]);
            this.__sortedProviderRef = refs;
        }
        return refs;
    }
    
    protected NamespacePrefixProvider getService(ServiceReference ref){
        if(ref instanceof NonOsgiServiceRef<?>){
            return ((NonOsgiServiceRef<NamespacePrefixProvider>)ref).getService();
        } else if(providersTracker != null){
            return (NamespacePrefixProvider)providersTracker.getService(ref);
        } else {
            return null;
        }
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        if(providersTracker != null) {
            providersTracker.close();
            providersTracker = null;
        }
        mappingsFile = null;
        bundleContext = null;
    }

    @Override
    public String getNamespace(String prefix) {
        String namespace;
        mappingsLock.readLock().lock();
        try {
            namespace = prefixMap.get(prefix);
        } finally {
            mappingsLock.readLock().unlock();
        }
        if(namespace == null){
            ServiceReference[] refs = getSortedProviderReferences();
            for(int i=0;namespace == null && i<refs.length;i++){
                NamespacePrefixProvider provider = getService(refs[i]);
                if(provider != null){
                    namespace = provider.getNamespace(prefix);
                }
            }
        }
        return namespace;
    }

    @Override
    public String getPrefix(String namespace) {
        List<String> prefixes = getPrefixes(namespace);
        return prefixes.isEmpty() ? null : prefixes.get(0);
    }

    @Override
    public List<String> getPrefixes(String namespace) {
        List<String> prefixes;
        mappingsLock.readLock().lock();
        try {
            prefixes = namespaceMap.get(namespace);
        } finally {
            mappingsLock.readLock().unlock();
        }
        if(prefixes == null){
            ServiceReference[] refs = getSortedProviderReferences();
            for(int i=0;prefixes == null && i<refs.length;i++){
                NamespacePrefixProvider provider = getService(refs[i]);
                if(provider != null){
                    prefixes = provider.getPrefixes(namespace);
                }
            }
        }
        return prefixes == null ? Collections.EMPTY_LIST:prefixes;
    }

    @Override
    public String setPrefix(String prefix, String namespace) {
        boolean validPrefix = NamespaceMappingUtils.checkPrefix(prefix);
        boolean validNamespace = NamespaceMappingUtils.checkNamespace(namespace);
        if(validPrefix && validNamespace){
            try {
                return addMapping(prefix, namespace,true);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to add Mapping because of an "
                        + e.getClass().getSimpleName()+ "while setting the parsed mapping",e);
            }
        } else {
            StringBuilder sb = new StringBuilder("The parsed Mapping is not Valid: ");
            if(!validPrefix){
                sb.append(String.format("The parsed prefix '%s' MUST only contain "
                        + "alpha numeric chars including '_' and '-'",prefix));
            }
            if(!validNamespace){
                if(!validPrefix) {
                    sb.append("| ");
                }
                sb.append(String.format("The parsed namespace '%s' MUST end with"
                        + "'/' or '#' in case of an URI or ':' in case of an URN",namespace));
           }
            throw new IllegalArgumentException(sb.toString());
        }
    }

    @Override
    public String getFullName(String shortNameOrUri) {
        String prefix = NamespaceMappingUtils.getPrefix(shortNameOrUri);
        if(prefix != null){
            String namespace = getNamespace(prefix);
            if(namespace != null){
                return namespace+shortNameOrUri.substring(prefix.length()+1);
            } else { //no mapping return null
                return null;
            }
        } else { //not a shortName ... return the parsed
            return shortNameOrUri;
        }
    }

    @Override
    public String getShortName(String uri) {
        String namespace = NamespaceMappingUtils.getNamespace(uri);
        if(namespace != null){
            String prefix = getPrefix(namespace);
            if(prefix != null){
                return prefix+uri.substring(namespace.length());
            } //else no mapping -> return the full URI
        } //no namespace -> return the full URI
        return uri;
    }

    /**
     * Internally used to mimic ServiceReferences when used outside OSGI
     * @param <T>
     */
    private final class NonOsgiServiceRef<T> implements ServiceReference {
        
        private T service;

        private NonOsgiServiceRef(T service){
            this.service = service;
        }
        
        public T getService(){
            return service;
        }
        
        @Override
        public Object getProperty(String key) {
            return null;
        }

        @Override
        public String[] getPropertyKeys() {
            return new String[]{};
        }

        @Override
        public Bundle getBundle() {
            return null;
        }

        @Override
        public Bundle[] getUsingBundles() {
            return new Bundle[]{};
        }

        @Override
        public boolean isAssignableTo(Bundle bundle, String className) {
            return true;
        }

        @Override
        public int compareTo(Object reference) {
            return 0;
        }
        
    }
    
}
