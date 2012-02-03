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
package org.apache.stanbol.explanation.impl;

import java.io.IOException;
import java.util.Dictionary;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.explanation.api.Configuration;
import org.apache.stanbol.ontologymanager.ontonet.api.ONManager;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the explanation environment configuration.
 * 
 */
@Component(immediate = true, metatype = true)
@Service(Configuration.class)
public class ExplanationEnvironmentConfiguration implements Configuration {

    public static final String _SCOPE_SHORT_ID_DEFAULT = "Explanation";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(policy = ReferencePolicy.DYNAMIC)
    private ONManager onm;

    @Property(name = Configuration.SCOPE_SHORT_ID, value = _SCOPE_SHORT_ID_DEFAULT)
    private String scopeShortID;

    /**
     * This default constructor is <b>only</b> intended to be used by the OSGI environment with Service
     * Component Runtime support.
     * <p>
     * DO NOT USE to manually create instances - the ExplanationGeneratorImpl instances do need to be
     * configured! YOU NEED TO USE {@link #ExplanationEnvironmentConfiguration(ONManager, Dictionary)} or its
     * overloads, to parse the configuration and then initialise the rule store if running outside an OSGI
     * environment.
     */
    public ExplanationEnvironmentConfiguration() {
        super();
    }

    /**
     * To be invoked by non-OSGi environments.
     * 
     * @param onm
     * @param configuration
     */
    public ExplanationEnvironmentConfiguration(ONManager onm, Dictionary<String,Object> configuration) {
        this();
        this.onm = onm;
        try {
            activate(configuration);
        } catch (IOException e) {
            log.error("Unable to access servlet context.", e);
        }
    }

    /**
     * Used to configure an instance within an OSGi container.
     * 
     * @throws IOException
     *             if there is no valid component context.
     */
    @SuppressWarnings("unchecked")
    @Activate
    protected void activate(ComponentContext context) throws IOException {
        log.info("in " + Configuration.class + " activate with context " + context);
        if (context == null) {
            throw new IllegalStateException("No valid" + ComponentContext.class + " parsed in activate!");
        }
        activate((Dictionary<String,Object>) context.getProperties());
    }

    /**
     * Called within both OSGi and non-OSGi environments.
     * 
     * @param configuration
     * @throws IOException
     */
    protected void activate(Dictionary<String,Object> configuration) throws IOException {

        this.scopeShortID = (String) configuration.get(Configuration.SCOPE_SHORT_ID);
        if (this.scopeShortID == null) this.scopeShortID = _SCOPE_SHORT_ID_DEFAULT;
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("in " + Configuration.class + " deactivate with context " + context);
    }

    @Override
    public ONManager getOntologyNetworkManager() {
        return onm;
    }

    @Override
    public String getScopeID() {
        return scopeShortID;
    }

    @Override
    public String getScopeShortId() {
        return scopeShortID;
    }

}
