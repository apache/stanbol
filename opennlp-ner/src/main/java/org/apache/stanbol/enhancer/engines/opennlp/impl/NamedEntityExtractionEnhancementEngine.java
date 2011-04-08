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
package org.apache.stanbol.enhancer.engines.opennlp.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EngineException;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.apache.stanbol.enhancer.servicesapi.ServiceProperties;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

/**
 * Apache Stanbol Enhancer Named Entity Recognition enhancement engine based on opennlp's Maximum Entropy
 * models.
 */
@Component(immediate = true, metatype = true, label = "%stanbol.NamedEntityExtractionEnhancementEngine.name", description = "%stanbol.NamedEntityExtractionEnhancementEngine.description")
@Service
public class NamedEntityExtractionEnhancementEngine implements EnhancementEngine, ServiceProperties {

    private EnhancementEngine engineCore;
    
    /**
     * The default value for the Execution of this Engine. Currently set to
     * {@link ServiceProperties#ORDERING_CONTENT_EXTRACTION}
     */
    public static final Integer defaultOrder = ORDERING_CONTENT_EXTRACTION;

    private ServiceRegistration dfpServiceRegistration;
    
    @Reference
    private DataFileProvider dataFileProvider;
    
    protected void activate(ComponentContext ctx) throws IOException {
        // Need our DataFileProvider before building the models
        dfpServiceRegistration = ctx.getBundleContext().registerService(
                DataFileProvider.class.getName(), 
                new ClasspathDataFileProvider(ctx.getBundleContext().getBundle().getSymbolicName()), null);
        
        engineCore = new NEREngineCore(dataFileProvider, ctx.getBundleContext().getBundle().getSymbolicName());
    }

    protected void deactivate(ComponentContext ce) {
        if(dfpServiceRegistration != null) {
            dfpServiceRegistration.unregister();
            dfpServiceRegistration = null;
        }
    }
    
    @Override
    public Map<String,Object> getServiceProperties() {
        return Collections.unmodifiableMap(Collections.singletonMap(ENHANCEMENT_ENGINE_ORDERING,
            (Object) defaultOrder));
    }

    @Override
    public int canEnhance(ContentItem ci) throws EngineException {
        checkCore();
        return engineCore.canEnhance(ci);
    }

    @Override
    public void computeEnhancements(ContentItem ci) throws EngineException {
        checkCore();
        engineCore.computeEnhancements(ci);
    }
    
    private void checkCore() {
        if(engineCore == null) {
            throw new IllegalStateException("EngineCore not initialized");
        }
    }
}