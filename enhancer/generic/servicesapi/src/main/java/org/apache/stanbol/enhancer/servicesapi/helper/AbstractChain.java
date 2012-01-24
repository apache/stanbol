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
package org.apache.stanbol.enhancer.servicesapi.helper;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.Chain;
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
    
    private String name;
    /**
     * The {@link ComponentContext} set in the {@link #activate(ComponentContext)}
     * and reset in the {@link #deactivate(ComponentContext)} method.
     */
    protected ComponentContext context;
    
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
    }
    protected void deactivate(ComponentContext ctx){
        this.context = null;
        name = null;
    }
    
    @Override
    public final String getName(){
        return name;
    }

}
