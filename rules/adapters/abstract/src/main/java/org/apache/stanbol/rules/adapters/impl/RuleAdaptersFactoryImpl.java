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

package org.apache.stanbol.rules.adapters.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdaptersFactory;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A concrete implementation of a {@link RuleAdaptersFactory}.
 * 
 * @author anuzzolese
 * 
 */
@Component(immediate = true)
@Service(RuleAdaptersFactory.class)
public class RuleAdaptersFactoryImpl implements RuleAdaptersFactory, ServiceListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<Class<?>,RuleAdapter> ruleAdapters;

    private ComponentContext componentContext;

    public RuleAdaptersFactoryImpl() {
        ruleAdapters = new HashMap<Class<?>,RuleAdapter>();
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings({"unchecked", "unused"})
    @Activate
    protected void activate(ComponentContext context) throws IOException {

        context.getBundleContext().addServiceListener(this);

        this.componentContext = context;
        log.info("in " + RuleAdaptersFactoryImpl.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Should be called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        log.info("RuleExportServiceManager is active", this);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + RuleAdaptersFactoryImpl.class + " deactivate with context " + context);
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference serviceReference = event.getServiceReference();

        Object service = componentContext.getBundleContext().getService(serviceReference);

        if (service instanceof RuleAdapter) {
            RuleAdapter RuleAdapter = (RuleAdapter) componentContext.getBundleContext().getService(
                serviceReference);

            switch (event.getType()) {
                case ServiceEvent.MODIFIED:

                    try {
                        removeRuleAdapter(RuleAdapter);
                        log.info("Removed Rule Adapter " + RuleAdapter.getClass().getCanonicalName());

                        addRuleAdapter(RuleAdapter);
                        log.info("Added Rule Adapter " + RuleAdapter.getClass().getCanonicalName());
                    } catch (UnavailableRuleObjectException e) {
                        log.error("Unavailable Rule Object " + e.getMessage());
                    }
                    break;
                case ServiceEvent.REGISTERED:
                    try {
                        addRuleAdapter(RuleAdapter);
                        log.info("Added Rule Adapter " + RuleAdapter.getClass().getCanonicalName());
                    } catch (UnavailableRuleObjectException e) {
                        log.error("Unavailable Rule Object " + e.getMessage());
                    }

                    break;
                case ServiceEvent.UNREGISTERING:
                    try {
                        removeRuleAdapter(RuleAdapter);
                        log.info("Removed Rule Adapter " + RuleAdapter.getClass().getCanonicalName());
                    } catch (UnavailableRuleObjectException e) {
                        log.error("Unavailable Rule Object " + e.getMessage());
                    }
                    break;

                default:
                    break;
            }

            log.info(ruleAdapters.entrySet().size() + " active rule adapters ");
        }
    }

    @Override
    public List<RuleAdapter> listRuleAdapters() {

        List<RuleAdapter> ruleAdapters = 
               new LinkedList<RuleAdapter>();
                //Collections.emptyList();
        ruleAdapters.addAll(this.ruleAdapters.values());
        return ruleAdapters;

    }

    @Override
    public RuleAdapter getRuleAdapter(Class<?> type) throws UnavailableRuleObjectException {

        return ruleAdapters.get(type);
    }

    @Override
    public synchronized void addRuleAdapter(RuleAdapter ruleAdapter) throws UnavailableRuleObjectException {

        ruleAdapters.put(ruleAdapter.getExportClass(), ruleAdapter);

    }

    @Override
    public synchronized void removeRuleAdapter(RuleAdapter ruleAdapter) throws UnavailableRuleObjectException {
        ruleAdapters.remove(ruleAdapter.getExportClass());

    }

}
