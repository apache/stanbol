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

package org.apache.stanbol.entityhub.web.writer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.ws.rs.core.MediaType;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.web.ModelWriter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelWriterTracker extends ServiceTracker {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Holds the config
     */
    private final Map<String, Map<MediaType,List<ServiceReference>>> writers = new HashMap<String,Map<MediaType,List<ServiceReference>>>();
    /**
     * Caches requests for MediaTypes and types
     */
    private final Map<CacheKey, Collection<ServiceReference>> cache = new HashMap<CacheKey,Collection<ServiceReference>>();
    /**
     * lock for {@link #writers} and {@link #cache}
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public Object addingService(ServiceReference reference) {
        Object service = super.addingService(reference);
        Set<MediaType> mediaTypes = parseMediaTypes(((ModelWriter)service).supportedMediaTypes());
        Class<? extends Representation> nativeType = ((ModelWriter)service).getNativeType();
        if(!mediaTypes.isEmpty()){
            lock.writeLock().lock();
            try {
                for(MediaType mediaType : mediaTypes){
                    addModelWriter(nativeType, mediaType, reference);
                }
            } finally {
                lock.writeLock().unlock();
            }
            return service;
        } else { //else no MediaTypes registered
            return null; //ignore this service
        }
    }
    
    @Override
    public void removedService(ServiceReference reference, Object service) {
        if(service != null){
            Set<MediaType> mediaTypes = parseMediaTypes(((ModelWriter)service).supportedMediaTypes());
            Class<? extends Representation> nativeType = ((ModelWriter)service).getNativeType();
            if(!mediaTypes.isEmpty()){
                lock.writeLock().lock();
                try {
                    for(MediaType mediaType : mediaTypes){
                        removeModelWriter(nativeType, mediaType, reference);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
        super.removedService(reference, service);
    }

    @Override
    public final void modifiedService(ServiceReference reference, Object service) {
        super.modifiedService(reference, service);
        if(service != null){
            Set<MediaType> mediaTypes = parseMediaTypes(((ModelWriter)service).supportedMediaTypes());
            Class<? extends Representation> nativeType = ((ModelWriter)service).getNativeType();
            if(!mediaTypes.isEmpty()){
                lock.writeLock().lock();
                try {
                    for(MediaType mediaType : mediaTypes){
                        updateModelWriter(nativeType, mediaType, reference);
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
    }

    /**
     * @param reference
     * @param key
     */
    private void addModelWriter(Class<? extends Representation> nativeType, 
            MediaType mediaType, ServiceReference reference) {
        //we want to have all ModelWriters under the null key
        log.debug(" > add ModelWriter format: {}, bundle: {}, nativeType: {}",
            new Object[]{mediaType, reference.getBundle(),
                nativeType != null ? nativeType.getName() : "none"});
        Map<MediaType,List<ServiceReference>> typeWriters = writers.get(null);
        addTypeWriter(typeWriters, mediaType, reference);
        if(nativeType != null){ //register also as native type writers
            typeWriters = writers.get(nativeType.getName());
            if(typeWriters == null){
                typeWriters = new HashMap<MediaType,List<ServiceReference>>();
                writers.put(nativeType.getName(), typeWriters);
            }
            addTypeWriter(typeWriters, mediaType, reference);
        }
        cache.clear(); //clear the cache after a change
   }

    /**
     * @param typeWriters
     * @param mediaType
     * @param reference
     */
    private void addTypeWriter(Map<MediaType,List<ServiceReference>> typeWriters,
            MediaType mediaType,
            ServiceReference reference) {
        List<ServiceReference> l;
        l = typeWriters.get(mediaType);
        if(l == null){
            l = new ArrayList<ServiceReference>();
            typeWriters.put(mediaType, l);
        }
        l.add(reference);
        Collections.sort(l); //service ranking based sorting
    }
    /**
     * @param key
     * @param reference
     */
    private void removeModelWriter(Class<? extends Representation> nativeType, 
            MediaType mediaType, ServiceReference reference) {
        log.debug(" > remove ModelWriter format: {}, service: {}, nativeType: {}",
            new Object[]{mediaType, reference,
                nativeType != null ? nativeType.getClass().getName() : "none"});
        Map<MediaType,List<ServiceReference>> typeWriters = writers.get(null);
        removeTypeWriter(typeWriters, mediaType, reference);
        if(nativeType != null){
            typeWriters = writers.get(nativeType.getName());
            if(typeWriters != null){
                removeTypeWriter(typeWriters, mediaType, reference);
                if(typeWriters.isEmpty()){
                    writers.remove(nativeType.getName());
                }
            }
        }
        cache.clear(); //clear the cache after a change
    }

    /**
     * @param typeWriters
     * @param mediaType
     * @param reference
     */
    private void removeTypeWriter(Map<MediaType,List<ServiceReference>> typeWriters,
            MediaType mediaType,
            ServiceReference reference) {
        List<ServiceReference> l = typeWriters.get(mediaType);
        if(l != null && l.remove(reference) && l.isEmpty()){
            writers.remove(mediaType); //remove empty mediaTypes
        }
    }

    /**
     * @param key
     * @param reference
     */
    private void updateModelWriter(Class<? extends Representation> nativeType, 
            MediaType mediaType, ServiceReference reference) {
        log.debug(" > update ModelWriter format: {}, service: {}, nativeType: {}",
            new Object[]{mediaType, reference,
                nativeType != null ? nativeType.getClass().getName() : "none"});
        Map<MediaType,List<ServiceReference>> typeWriters = writers.get(null);
        updateTypeWriter(typeWriters, mediaType, reference);
        if(nativeType != null){
            typeWriters = writers.get(nativeType.getName());
            if(typeWriters != null){
                updateTypeWriter(typeWriters, mediaType, reference);
            }
        }
        cache.clear(); //clear the cache after a change
    }

    /**
     * @param typeWriters
     * @param mediaType
     * @param reference
     */
    private void updateTypeWriter(Map<MediaType,List<ServiceReference>> typeWriters,
            MediaType mediaType,
            ServiceReference reference) {
        List<ServiceReference> l = typeWriters.get(mediaType);
        if(l != null && l.contains(reference)){
            Collections.sort(l); //maybe service.ranking has changed
        }
    }
    
    public ModelWriterTracker(BundleContext context) {
        super(context, ModelWriter.class.getName(), null);
        //add the union key value mapping
        writers.put(null, new HashMap<MediaType,List<ServiceReference>>());
    }

    /**
     * @param mts
     * @return
     */
    private Set<MediaType> parseMediaTypes(Collection<MediaType> mts) {
        if(mts == null || mts.isEmpty()){
            return Collections.emptySet();
        }
        Set<MediaType> mediaTypes = new HashSet<MediaType>(mts.size());
        for(MediaType mt : mts){
            if(mt != null){
                //strip all parameters
                MediaType mediaType = mt.getParameters().isEmpty() ? mt :
                    new MediaType(mt.getType(),mt.getSubtype());
                mediaTypes.add(mediaType);
            }
        }
        return mediaTypes;
    }
    /**
     * Getter for a sorted list of References to {@link ModelWriter} that can
     * serialise Representations to the parsed {@link MediaType}. If a
     * nativeType of the Representation is given {@link ModelWriter} for that
     * specific type will be preferred.
     * @param mediaType The {@link MediaType}. Wildcards are supported
     * @param nativeType optionally the native type of the {@link Representation}
     * @return A sorted collection of references to compatible {@link ModelWriter}.
     * Use {@link #getService()} to receive the actual service. However note that
     * such calls may return <code>null</code> if the service was deactivated in
     * the meantime.
     */
    public Collection<ServiceReference> getModelWriters(MediaType mediaType, Class<? extends Representation> nativeType){
        Collection<ServiceReference> refs;
        String nativeTypeName = nativeType == null ? null : nativeType.getName();
        CacheKey key = new CacheKey(mediaType, nativeTypeName);
        lock.readLock().lock();
        try {
            refs = cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
        if(refs == null){ //not found in cache
            refs = new ArrayList<ServiceReference>();
            Map<MediaType, List<ServiceReference>> typeWriters = writers.get(
                nativeTypeName);
            if(typeWriters != null){ //there are some native writers for this type
                refs.addAll(getTypeWriters(typeWriters, mediaType));
            }
            if(nativeType != null){ //if we have a native type
                //also add writers for the generic type to the end
                
                refs.addAll(getTypeWriters(writers.get(null), mediaType));
            }
            refs = Collections.unmodifiableCollection(refs);
            lock.writeLock().lock();
            try {
                cache.put(key, refs);
            } finally {
                lock.writeLock().unlock();
            }
        }
        return refs;
    }
    
    
    private Collection<ServiceReference> getTypeWriters(
        Map<MediaType,List<ServiceReference>> typeWriters, MediaType mediaType) {
        //use a linked has set to keep order but filter duplicates
        Collection<ServiceReference> refs = new LinkedHashSet<ServiceReference>();
        boolean wildcard = mediaType.isWildcardSubtype() || mediaType.isWildcardType();
        lock.readLock().lock();
        try {
            if(!wildcard){
                //add writer that explicitly mention this type first
                List<ServiceReference> l = typeWriters.get(mediaType);
                if(l != null){
                    refs.addAll(l);
                }
            }
            List<ServiceReference> wildcardMatches = null;
            int count = 0;
            for(Entry<MediaType,List<ServiceReference>> entry : typeWriters.entrySet()){
                MediaType mt = entry.getKey();
                if(mt.isCompatible(mediaType) &&
                        //ignore exact matches already treated above
                        (wildcard || !mt.equals(mediaType))){
                    if(count == 0){
                        wildcardMatches = entry.getValue();
                    } else {
                        if(count == 1){
                            wildcardMatches = new ArrayList<ServiceReference>(wildcardMatches); 
                        }
                        wildcardMatches.addAll(entry.getValue());
                    }
                }
            }
            if(count > 1){ //sort matches for different media types
                Collections.sort(wildcardMatches);
            }
            //add wildcard matches to the linked has set
            if(count > 0){
                refs.addAll(wildcardMatches);
            }
        } finally {
            lock.readLock().unlock();
        }
        return refs;
    }

    @Override
    public ModelWriter getService() {
        return (ModelWriter)super.getService();
    }
    
    @Override
    public ModelWriter getService(ServiceReference reference) {
        return (ModelWriter)super.getService(reference);
    }

    /**
     * Used as key for {@link ModelWriterTracker#cache}
     */
    private static class CacheKey {
        
        final String nativeType;
        final MediaType mediaType;
        
        CacheKey(MediaType mediaType, String nativeType){
            this.nativeType = nativeType;
            this.mediaType = mediaType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + mediaType.hashCode();
            result = prime * result + ((nativeType == null) ? 0 : nativeType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            CacheKey other = (CacheKey) obj;
            if (!mediaType.equals(other.mediaType)) return false;
            if (nativeType == null) {
                if (other.nativeType != null) return false;
            } else if (!nativeType.equals(other.nativeType)) return false;
            return true;
        }
        
        
    }
    
    
}
