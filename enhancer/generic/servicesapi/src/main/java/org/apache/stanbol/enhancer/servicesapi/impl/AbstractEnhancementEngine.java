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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngine;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * Abstract base implementation that reads the of the 
 * {@link EnhancementEngine#PROPERTY_NAME} from configuration parsed in 
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
 * 
 * @author Rupert Westenthaler
 * 
 * @param <A> Exception thrown by the {@link #activate(ComponentContext)} method
 * @param <D> Exception thrown by the {@link #deactivate(ComponentContext)} method
 */
@Component(immediate = true, metatype = true)
@Properties(value={
    @Property(name=EnhancementEngine.PROPERTY_NAME),
    @Property(name=Constants.SERVICE_RANKING)
})
@Service
public abstract class AbstractEnhancementEngine<A extends Exception,D extends Exception> implements EnhancementEngine {

    private String name;
    /**
     * activator that reads and validates the {@link EnhancementEngine#PROPERTY_NAME}
     * property from the {@link ComponentContext#getProperties()}.
     * @param ctx the component context
     * @throws ConfigurationException if the required property 
     * {@link EnhancementEngine#PROPERTY_NAME} is missing or empty
     * @throws A to allow sub classes to throw any kind of exception
     */
    @Activate
    protected void activate(ComponentContext ctx) throws ConfigurationException, A {
        Object value = ctx.getProperties().get(PROPERTY_NAME);
        if(value instanceof String){
            name = (String)value;
            if(name.isEmpty()){
                name = null;
                throw new ConfigurationException(PROPERTY_NAME, "The configured" +
                		"name of an EnhancementEngine MUST NOT be empty!");
            }
        } else {
            throw new ConfigurationException(PROPERTY_NAME, value == null ?
                    "The name is a required property!" :
                    "The name of an EnhancementEngine MUST be an non empty String " +
                    "(type: "+value.getClass()+" value: "+value+")");
        }
    }
    /**
     * Deactivates this component and re-sets the name to <code>null</code>
     * @param ctx not used
     * @throws D to allow sub classes to throw any Exception
     */
    @Deactivate
    protected void deactivate(ComponentContext ctx) throws D {
        name = null;
    }
    @Override
    public final String getName(){
        return name;
    }
    /**
     * Prints the simple name of the Class and the configured Name.
     */
    @Override
    public String toString() {
        return String.format("%s(name=%s)", getClass().getSimpleName(),name);
    }

}
