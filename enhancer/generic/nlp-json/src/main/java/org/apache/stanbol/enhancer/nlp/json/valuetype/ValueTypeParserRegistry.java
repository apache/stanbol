package org.apache.stanbol.enhancer.nlp.json.valuetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
 * Tracks {@link ValueTypeParser} implementations by using a 
 * {@link ServiceTracker} when running within OSIG and the {@link ServiceLoader}
 * utility when used outside of OSGI. 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,policy=ConfigurationPolicy.IGNORE)
@Service(value=ValueTypeParserRegistry.class)
public class ValueTypeParserRegistry {
    private final Logger log = LoggerFactory.getLogger(ValueTypeParserRegistry.class);

    private static ValueTypeParserRegistry instance;
    /**
     * Should be used when running outside of OSGI to create the singleton
     * instance of this factory.
     * @return the singleton instance
     */
    public static final ValueTypeParserRegistry getInstance(){
        if(instance == null){
            instance = new ValueTypeParserRegistry();
        }
        return instance;
    }

    ReadWriteLock parserLock = new ReentrantReadWriteLock();
    private boolean inOsgi;
    /**
     * Used outside OSGI
     */
    Map<Class<?>,ValueTypeParser<?>> valueTypeParsers;
    /**
     * Used if running within OSGI in combination with the {@link #parserTracker}
     */
    Map<Class<?>, List<ServiceReference>> valueTypeParserRefs;
    ServiceTracker parserTracker;
    
    @SuppressWarnings("unchecked")
    public <T> ValueTypeParser<T> getParser(Class<T> type){
        if(!inOsgi && valueTypeParsers == null){
            initValueTypeParser(); //running outside OSGI
        }
        parserLock.readLock().lock();
        try {
            if(!inOsgi){
                return (ValueTypeParser<T>)valueTypeParsers.get(type);
            } else {
                List<ServiceReference> refs = valueTypeParserRefs.get(type);
                return refs == null || refs.isEmpty() ? null :
                    (ValueTypeParser<T>) parserTracker.getService(
                        refs.get(refs.size()-1));
            }
        } finally {
            parserLock.readLock().unlock();
        }
    }
    
    @Activate
    protected void activate(ComponentContext ctx){
        inOsgi = true;
        final BundleContext bc = ctx.getBundleContext();
        valueTypeParserRefs = new HashMap<Class<?>,List<ServiceReference>>();
        parserTracker = new ServiceTracker(bc, ValueTypeParser.class.getName(), 
            new ParserTracker(bc));
        parserTracker.open();
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        inOsgi = false;
        parserTracker.close();
        parserTracker = null;
        parserLock.writeLock().lock();
        try {
            if(valueTypeParsers != null){
                valueTypeParsers.clear();
                valueTypeParsers = null;
            }
        } finally {
            parserLock.writeLock().unlock();
        }
    }
    
    /**
     * Only used when running outside an OSGI environment
     */
    @SuppressWarnings("rawtypes")
    private void initValueTypeParser() {
        parserLock.writeLock().lock();
        try {
            if(valueTypeParsers == null){
                valueTypeParsers = new HashMap<Class<?>,ValueTypeParser<?>>();
                ServiceLoader<ValueTypeParser> loader = ServiceLoader.load(ValueTypeParser.class);
                for(Iterator<ValueTypeParser> it = loader.iterator();it.hasNext();){
                    ValueTypeParser vts = it.next();
                    ValueTypeParser<?> serializer = valueTypeParsers.get(vts.getType());
                    if(serializer != null){
                        log.warn("Multiple Parsers for type {} (keep: {}, ignoreing: {}",
                            new Object[]{vts.getType(),serializer,vts});
                    } else {
                        valueTypeParsers.put(vts.getType(), vts);
                    }
                }
            }
        } finally {
            parserLock.writeLock().unlock();
        }
    }
    /**
     * {@link ServiceTrackerCustomizer} writing services to the
     * {@link ValueTypeParserRegistry#valueTypeParserRefs} map. It also
     * sorts multiple Parser for a single type based on sorting 
     * {@link ServiceReferences}. The service reference with the highest rank
     * will be the last in the list. 
     *
     */
    private final class ParserTracker implements ServiceTrackerCustomizer {
        private final BundleContext bc;

        private ParserTracker(BundleContext bc) {
            this.bc = bc;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            ValueTypeParser<?> service = (ValueTypeParser<?>)bc.getService(reference);
            parserLock.writeLock().lock();
            try {
                List<ServiceReference> refs = valueTypeParserRefs.get(service.getType());
                if(refs == null){
                    refs = new ArrayList<ServiceReference>(2);
                    valueTypeParserRefs.put(service.getType(), refs);
                }
                refs.add(reference);
                if(refs.size() > 1){
                    Collections.sort(refs);
                }
            } finally {
                parserLock.writeLock().unlock();
            }
            return service;
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            ValueTypeParser<?> vts = (ValueTypeParser<?>) service;
            parserLock.writeLock().lock();
            try {
                List<ServiceReference> refs = valueTypeParserRefs.get(vts.getType());
                if(refs != null && refs.remove(reference) && refs.isEmpty() &&
                        valueTypeParsers != null){ //not yet deactivated
                    valueTypeParsers.remove(vts.getType());
                }
            } finally {
                parserLock.writeLock().unlock();
            }
            
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
            ValueTypeParser<?> vts = (ValueTypeParser<?>) service;
            try {
                List<ServiceReference> refs = valueTypeParserRefs.get(vts.getType());
                if(refs != null) {
                    refs.remove(reference);
                } else {
                    refs = new ArrayList<ServiceReference>(2);
                    valueTypeParserRefs.put(vts.getType(), refs);
                }
                refs.add(reference);
                if(refs.size() > 1){
                    Collections.sort(refs);
                }
            } finally {
                parserLock.writeLock().unlock();
            }
            
        }
    }
}
