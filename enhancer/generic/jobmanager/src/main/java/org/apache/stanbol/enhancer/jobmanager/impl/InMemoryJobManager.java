package org.apache.stanbol.enhancer.jobmanager.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Naive EnhancementJobManager implementation that keeps its request queue in
 * memory.
 *
 * @scr.component immediate="true"
 * @scr.service
 * @scr.reference name="EnhancementEngine"
 *                interface="org.apache.stanbol.enhancer.servicesapi.EnhancementEngine"
 *                cardinality="0..n" policy="dynamic"
 *
 */
public class InMemoryJobManager implements EnhancementJobManager {
    private static final Logger log = LoggerFactory.getLogger(InMemoryJobManager.class);

    // handle thread safety efficiently when traversals (e.g. when calling
    // #enhanceContent) are expected to be much more frequent than mutable
    // operations (binding or unbinding engines).
    //NITE CopyOnWriteArrayList can no longer be used, becuase,
    //     Iterators over CopyOnWriteArrayList do not support add/remove ...
    //     Therefore this implementation can not be used for Collections.sort
    //     Therefore a new ArrayList is generated each time an add/reomve
    //     operation is performed
    private List<EnhancementEngine> sortedEngineList = new ArrayList<EnhancementEngine>();

    private static final ExecutionOrderComparator executionOrderComparator = new ExecutionOrderComparator();

    public void enhanceContent(ContentItem ci) throws EngineException {
        log.debug("enhanceContent({}), {} engines available", ci, sortedEngineList.size());
        Iterator<EnhancementEngine> engines;
        //changes in the sortedEngineList do creates new Lists. Therefore we need
        //only sync the creation of the Iterator. Calls to the iterator will
        //not trigger ConcurrentModificationExceptions
        //however the remove Method will not have any affect if the list was
        //changed.
        synchronized (sortedEngineList) {
            engines = sortedEngineList.iterator();
        }
        while (engines.hasNext()) {
            EnhancementEngine engine = engines.next();
            if (engine.canEnhance(ci) == EnhancementEngine.CANNOT_ENHANCE) {
                log.debug("[{}] cannot be enhanced by engine [{}], skipping",
                        ci.getId(), engine);
            } else {
                // TODO should handle sync/async enhancing. All sync for now.
                engine.computeEnhancements(ci);
                log.debug("ContentItem [{}] enhanced by engine [{}]",
                        ci.getId(), engine);
            }
        }
    }

    public void bindEnhancementEngine(EnhancementEngine e) {
        synchronized (sortedEngineList) {
            List<EnhancementEngine> newList = new ArrayList<EnhancementEngine>(sortedEngineList);
            newList.add(e);
            Collections.sort(newList,executionOrderComparator);
            sortedEngineList = newList;
        }
        log.info("EnhancementEngine {} added to our list: {}", e, sortedEngineList);
    }

    public void unbindEnhancementEngine(EnhancementEngine e) {
        synchronized (sortedEngineList) {
            List<EnhancementEngine> newList = new ArrayList<EnhancementEngine>(sortedEngineList);
            newList.remove(e);
            sortedEngineList = newList;
        }
        log.info("EnhancementEngine {} removed from our list: {}", e, sortedEngineList);
    }

    public List<EnhancementEngine> getActiveEngines() {
        return Collections.unmodifiableList(sortedEngineList);
    }

    private static class ExecutionOrderComparator implements Comparator<EnhancementEngine> {

        @Override
        public int compare(EnhancementEngine engine1, EnhancementEngine engine2) {
            Integer order1 = getOrder(engine1);
            Integer order2 = getOrder(engine2);
            //start with the highest number finish with the lowest ...
            return order1 == order2?0:order1<order2?1:-1;
        }

        public Integer getOrder(EnhancementEngine engine){
            log.info("getOrder "+engine);
            if (engine instanceof ServiceProperties){
                log.info(" ... implements ServiceProperties");
                Object value = ((ServiceProperties)engine).getServiceProperties().get(ServiceProperties.ENHANCEMENT_ENGINE_ORDERING);
                log.info("   > value = "+value +" "+value.getClass());
                if (value !=null && value instanceof Integer){
                    return (Integer)value;
                }
            }
            return ServiceProperties.ORDERING_DEFAULT;
        }
    }
}
