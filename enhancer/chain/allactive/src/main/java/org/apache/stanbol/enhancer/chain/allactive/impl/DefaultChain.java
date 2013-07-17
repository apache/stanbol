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
package org.apache.stanbol.enhancer.chain.allactive.impl;


import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * This is actually no {@link Chain} implementation, but an component that - 
 * if {@link #PROPERTY_ENABLED} is set to <code>true</code> - ensures that an
 * instance of the {@link AllActiveEnginesChain} with the 
 * {@link EnhancementEngine#PROPERTY_NAME} = "default" and
 * the {@link Constants#SERVICE_RANKING} = {@link Integer#MIN_VALUE} is 
 * registered as service.<p>
 * This ensures that the behaviour of the Stanbol Enhancer - to enhance parsed 
 * {@link ContentItem}s by using all currently active 
 * {@link EnhancementEngine}s - is still the default after the introduction of 
 * {@link Chain}s. See the documentation of the Stanbol Enhancer for details.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,metatype=true)
public class DefaultChain {
    
    @Property(boolValue=DefaultChain.DEFAULT_STATE)
    public static final String PROPERTY_ENABLED = "stanbol.enhancer.chain.default.enabled";
    
    @Property(value=DefaultChain.DEFAULT_NAME)
    public static final String PROPERTY_NAME = "stanbol.enhancer.chain.default.name";
    
    
    public static final boolean DEFAULT_STATE = true;
    public static final String DEFAULT_NAME = "default";
    
    private ServiceRegistration defaultChainReg;
    private AllActiveEnginesChain defaultChain;
    
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        boolean enabled = DEFAULT_STATE;
        Object value = ctx.getProperties().get(PROPERTY_ENABLED);
        if(value != null){
            enabled = Boolean.parseBoolean(value.toString());
        }
        value = ctx.getProperties().get(PROPERTY_NAME);
        String name = value == null ? DEFAULT_NAME : value.toString();
        if(name.isEmpty()){
            throw new ConfigurationException(PROPERTY_NAME, "The parsed name for the default chain MUST NOT be empty!");
        }
        int ranking;
        value = ctx.getProperties().get(Constants.SERVICE_RANKING);
        if(value instanceof Number){
            ranking = ((Number)value).intValue();
        } else if(value != null){
            try {
                ranking = Integer.parseInt(value.toString());
            }catch (NumberFormatException e) {
                throw new ConfigurationException(Constants.SERVICE_RANKING, "Unable to pase Integer service.ranking value",e);
            }
        } else {
            ranking = Integer.MIN_VALUE;
        }
        if(enabled){
            defaultChain = new AllActiveEnginesChain(ctx.getBundleContext(),name);
            Dictionary<String,Object> properties = new Hashtable<String,Object>();
            properties.put(Chain.PROPERTY_NAME, defaultChain.getName());
            properties.put(Constants.SERVICE_RANKING, ranking);
            defaultChainReg = ctx.getBundleContext().registerService(
                Chain.class.getName(), defaultChain, properties);
        }
    }
    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        if(defaultChainReg != null){
            defaultChainReg.unregister();
            defaultChainReg = null;
        }
        if(defaultChain != null){
            defaultChain.close();
            defaultChain = null;
        }
    }
    
}
