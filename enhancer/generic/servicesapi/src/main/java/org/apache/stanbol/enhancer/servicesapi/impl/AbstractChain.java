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
package org.apache.stanbol.enhancer.servicesapi.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Chain;
import org.apache.stanbol.enhancer.servicesapi.helper.ConfigUtils;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
/**
 * Abstract base implementation that reads the of the 
 * {@link Chain#PROPERTY_NAME} from configuration parsed in 
 * the activate methods and implements the {@link #getName()} method. If the 
 * name is missing or empty a {@link ConfigurationException} is thrown.<p>
 * In addition this Class defines a {@link Service}, {@link Component} and 
 * {@link Property} annotations for <ul>
 * <li> EnhancementEngine#PROPERTY_NAME
 * <li> {@link Constants#SERVICE_RANKING}
 * </ul>
 * This annotations can be {@link Component#inherit()} to sub classes and would
 * allow users to specify the name and the ranking of an engine by using e.g. the
 * Apache Felix Webconsole.<p>
 * @author Rupert Westenthaler
 *
 */
@Component(immediate = true, metatype = true)
@Properties(value={
    @Property(name=Chain.PROPERTY_NAME),
    @Property(name=Constants.SERVICE_RANKING)
})
@Service
public abstract class AbstractChain implements Chain {
    
    /**
     * Property used to configure chain scoped enhancement properties as described
     * by <a herf="https://issues.apache.org/jira/browse/STANBOL-488">STANBOL-488</a></p>
     * Properties defined by this will get parsed to all enhancement engines in the
     * chain.
     */
    public static final String PROPERTY_CHAIN_PROPERTIES = "stanbol.enhancer.chain.chainproperties";

    private String name;
    /**
     * The {@link ComponentContext} set in the {@link #activate(ComponentContext)}
     * and reset in the {@link #deactivate(ComponentContext)} method.
     */
    protected ComponentContext context;

    private Map<String,Object> chainProperties;
    
    protected void activate(ComponentContext ctx) throws ConfigurationException {
        this.context = ctx;
        Object value = ctx.getProperties().get(PROPERTY_NAME);
        if(value instanceof String){
            name = (String)value;
            if(name.isEmpty()){
                name = null;
                throw new ConfigurationException(PROPERTY_NAME, "The configured" +
                        "name of a Chain MUST NOT be empty!");
            }
        } else {
            throw new ConfigurationException(PROPERTY_NAME, value == null ?
                    "The name is a required property!" :
                    "The name of a Chain MUST be an non empty String " +
                    "(type: "+value.getClass()+" value: "+value+")");
        }
        value = ctx.getProperties().get(PROPERTY_CHAIN_PROPERTIES);
        Collection<String> chainPropsConfig;
        if(value instanceof String[]){
            chainPropsConfig = Arrays.asList((String[])value);
        } else if(value instanceof Collection<?>){
            chainPropsConfig = new ArrayList<String>(((Collection<?>)value).size());
            for(Object o : (Collection<?>)value){
                if(o != null){
                    chainPropsConfig.add(o.toString());
                }
            }
        } else if(value instanceof String){
            chainPropsConfig = Collections.singleton((String)value);
        } else if (value != null){
            throw new ConfigurationException(PROPERTY_CHAIN_PROPERTIES, 
                "Chain level EnhancementProperties can be parsed as String[],"
                + "Collection<String> or String (single value). The actually "
                + "parsed type was "+value.getClass().getName());
        } else {
            chainPropsConfig = Collections.emptyList();
        }
        chainProperties = ConfigUtils.getEnhancementProperties(chainPropsConfig);
    }
    protected void deactivate(ComponentContext ctx){
        this.context = null;
        name = null;
    }
    
    @Override
    public final String getName(){
        return name;
    }
    
    protected Map<String,Object> getChainProperties(){
        return chainProperties;
    }

}
