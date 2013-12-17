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
package org.apache.stanbol.enhancer.jobmanager.impl;

import static org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper.EXECUTION_ORDER_COMPARATOR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.EnhancementException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.apache.stanbol.enhancer.servicesapi.helper.EnhancementEngineHelper;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Naive EnhancementJobManager implementation that keeps its request queue in
 * memory.
 *
 * @deprecated Deprecated as it does not support Enhancement Chains. Use the 
 * EventJobmanager instead.
 * @scr.component immediate="true"
 * @scr.service
 * @scr.reference name="EnhancementEngine"
 *                interface="org.apache.stanbol.enhancer.servicesapi.EnhancementEngine"
 *                cardinality="0..n" policy="dynamic"
 * @scr.property name="service.ranking" 
 *               value="-1000"
 *               type="Integer"
 */
@Deprecated
public class WeightedJobManager implements EnhancementJobManager {
    private static final Logger log = LoggerFactory.getLogger(WeightedJobManager.class);
    // handle thread safety efficiently when traversals (e.g. when calling
    // #enhanceContent) are expected to be much more frequent than mutable
    // operations (binding or unbinding engines).
    //NITE CopyOnWriteArrayList can no longer be used, becuase,
    //     Iterators over CopyOnWriteArrayList do not support add/remove ...
    //     Therefore this implementation can not be used for Collections.sort
    //     Therefore a new ArrayList is generated each time an add/reomve
    //     operation is performed
    private List<EnhancementEngine> sortedEngineList = new ArrayList<EnhancementEngine>();

    @Override
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
        long start = System.currentTimeMillis();
        while (engines.hasNext()) {
            EnhancementEngine engine = engines.next();
            long startEngine = System.currentTimeMillis();
            if (engine.canEnhance(ci) == EnhancementEngine.CANNOT_ENHANCE) {
                log.debug("[{}] cannot be enhanced by engine [{}], skipping",
                        ci.getUri().getUnicodeString(), engine);
            } else {
                // TODO should handle sync/async enhancing. All sync for now.
                engine.computeEnhancements(ci);
                log.debug("ContentItem [{}] enhanced by engine [{}] in {}ms",
                        new Object[]{ci.getUri().getUnicodeString(), engine,System.currentTimeMillis()-startEngine});
            }
        }
        log.debug("ContentItem [{}] enhanced in {}ms",ci.getUri().getUnicodeString(),(System.currentTimeMillis()-start));
    }
    
    @Override
	public void enhanceContent(ContentItem ci, Chain chain) throws EngineException {
        if(chain != null){
            log.error("This EnhancementJobManager implementation does not yet" +
            		"support Enhancement Chains");
        }
		//This implementation don't take "chain" in account.
    	enhanceContent(ci);
	}
    
    public void bindEnhancementEngine(EnhancementEngine e) {
        synchronized (sortedEngineList) {
            List<EnhancementEngine> newList = new ArrayList<EnhancementEngine>(sortedEngineList);
            newList.add(e);
            Collections.sort(newList,EXECUTION_ORDER_COMPARATOR);
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
    
}
