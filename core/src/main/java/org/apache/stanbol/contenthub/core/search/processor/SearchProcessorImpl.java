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

package org.apache.stanbol.contenthub.core.search.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.contenthub.servicesapi.search.engine.EngineProperties;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngine;
import org.apache.stanbol.contenthub.servicesapi.search.engine.SearchEngineException;
import org.apache.stanbol.contenthub.servicesapi.search.execution.SearchContext;
import org.apache.stanbol.contenthub.servicesapi.search.processor.SearchProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author anil.sinaci
 * 
 */
@Component
@Service
public class SearchProcessorImpl implements SearchProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SearchProcessorImpl.class);

    private static final EngineComparator COMPARATOR = new EngineComparator();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_MULTIPLE, referenceInterface = SearchEngine.class, policy = ReferencePolicy.DYNAMIC, bind = "bindSearchEngine", unbind = "unbindSearchEngine")
    private List<SearchEngine> engines = new ArrayList<SearchEngine>();

    @Override
    public final List<SearchEngine> listEngines() {
        synchronized (engines) {
            return Collections.unmodifiableList(new ArrayList<SearchEngine>(engines));
        }
    }

    @Override
    public final void processQuery(SearchContext context) {
        Iterator<SearchEngine> enginesIterator;
        List<String> allowedEngines = context.getAllowedEngines();
        synchronized (engines) {
            enginesIterator = engines.iterator();
        }

        while (enginesIterator.hasNext()) {
            SearchEngine engine = enginesIterator.next();
            long t1 = System.currentTimeMillis();
            try {
                // TODO Find a way to uniquely identify search engines
                if (allowedEngines.contains(engine.toString())) {
                    engine.search(context);
                } else {
                    logger.info("Engine {} is not selected to process resources", engine.toString());
                }
            } catch (SearchEngineException e) {
                logger.error("Query processing error: ", e);
            } finally {
                logger.info("{} engine completed execution in {} miliseconds", engine.toString(),
                    System.currentTimeMillis() - t1);
            }
        }
    }

    protected void bindSearchEngine(SearchEngine engine) {
        synchronized (engines) {
            engines.add(engine);
            Collections.sort(engines, COMPARATOR);
        }
    }

    protected void unbindSearchEngine(SearchEngine engine) {
        synchronized (engines) {
            engines.remove(engine);
        }
    }

    private static final class EngineComparator implements Comparator<SearchEngine> {

        @Override
        public int compare(SearchEngine engine1, SearchEngine engine2) {
            Integer order1 = getOrder(engine1);
            Integer order2 = getOrder(engine2);
            return order1.compareTo(order2);
        }

        public int getOrder(SearchEngine engine) {
            if (engine == null) {
                throw new IllegalArgumentException("Engine can not be null");
            }

            if (engine instanceof EngineProperties) {
                Object value = ((EngineProperties) engine).getEngineProperties().get(
                    EngineProperties.PROCESSING_ORDER);
                if (value instanceof Integer) {
                    return (Integer) value;
                }
            }
            return EngineProperties.PROCESSING_DEFAULT;
        }
    }
}
