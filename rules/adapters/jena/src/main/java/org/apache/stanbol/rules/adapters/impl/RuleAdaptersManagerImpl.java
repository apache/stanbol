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
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.rules.base.api.Adaptable;
import org.apache.stanbol.rules.base.api.RuleAdapter;
import org.apache.stanbol.rules.base.api.RuleAdapterManager;
import org.apache.stanbol.rules.base.api.RuleAdaptersFactory;
import org.apache.stanbol.rules.base.api.UnavailableRuleObjectException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A concrete implementation of a {@link RuleAdapterManager}.
 * 
 * @author anuzzolese
 * 
 */

@Component(immediate = true)
@Service(RuleAdapterManager.class)
public class RuleAdaptersManagerImpl implements RuleAdapterManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference
    RuleAdaptersFactory ruleAdaptersFactory;

    public RuleAdaptersManagerImpl() {

    }

    /**
     * Constructor for non-OSGi environments.
     */
    public RuleAdaptersManagerImpl(Dictionary<String,Object> configuration,
                                   RuleAdaptersFactory ruleAdaptersFactory) {
        this.ruleAdaptersFactory = ruleAdaptersFactory;

        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access the configuration.", e);
        }
    }

    @Override
    public <AdaptedTo> RuleAdapter getAdapter(Adaptable adaptable, Class<AdaptedTo> adaptedToType) throws UnavailableRuleObjectException {

        return ruleAdaptersFactory.getRuleAdapter(adaptedToType);

    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + RuleAdaptersManagerImpl.class + " activate with context " + context);
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

        log.info("RuleAdapterManagerImpl is active", this);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + RuleAdaptersManagerImpl.class + " deactivate with context " + context);
    }

    @Override
    public List<RuleAdapter> listRuleAdapters() {

        return ruleAdaptersFactory.listRuleAdapters();
    }

}
