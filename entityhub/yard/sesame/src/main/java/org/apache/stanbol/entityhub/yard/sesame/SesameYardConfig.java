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
package org.apache.stanbol.entityhub.yard.sesame;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.apache.stanbol.entityhub.core.yard.AbstractYard.YardConfig;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SesameYardConfig extends YardConfig {

    private final Logger log = LoggerFactory.getLogger(SesameYardConfig.class);
    
    
    public SesameYardConfig(String id) throws IllegalArgumentException {
        super(id);
    }
    public SesameYardConfig(Dictionary<String,Object> config) throws ConfigurationException, IllegalArgumentException {
        super(config);
    }
    /**
     * Getter for the {@link SesameYard#CONTEXT_ENABLED} state
     * @return the state or the {@link SesameYard#DEFAULT_CONTEXT_ENABLED default}
     * if not present in the config. 
     */
    public boolean isContextEnabled(){
        Object value = config.get(SesameYard.CONTEXT_ENABLED);
        if(value instanceof Boolean){
            return ((Boolean)value).booleanValue();
        } else if(value != null){
            return Boolean.parseBoolean(value.toString());
        } else {
            return SesameYard.DEFAULT_CONTEXT_ENABLED;
        }
    }
    /**
     * Setter for the {@link SesameYard#CONTEXT_ENABLED} state
     * @param state the state or <code>null</code> to remove the config (reset to 
     * the {@link SesameYard#DEFAULT_CONTEXT_ENABLED default})
     */
    public void setContextEnabled(Boolean state){
        if(state != null){
            config.put(SesameYard.CONTEXT_ENABLED, state);
        } else {
            config.remove(SesameYard.CONTEXT_ENABLED);
        }
    }
    
    /**
     * Setter for the Contexts
     * @param contexts
     */
    public void setContexts(String[] contexts){
        if(contexts == null){
            config.remove(SesameYard.CONTEXT_URI);
        } else {
            config.put(SesameYard.CONTEXT_URI, contexts);
        }
    }
    
    /**
     * Getter for the {@link SesameYard#CONTEXT_URI} property.
     * @return the contexts or an empty array if none
     */
    public String[] getContexts(){
        Object value = config.get(SesameYard.CONTEXT_URI);
        Set<String> values = null;
        if(value instanceof String){
            String str = ((String)value).trim();
            return new String[]{str.isEmpty() ? null : str};
        } else if (value == null){
            return new String[]{};
        } else if(value instanceof String[]){
            values = new HashSet<String>(((String[])value).length);
            for(String str : (String[])value){
                str = str != null ? str.trim() : str;
                values.add(str.isEmpty() ? null : str);
            }
        } else if(value instanceof Iterable<?>){
            values = new HashSet<String>(((String[])value).length);
            for(Object o : (String[])value){
                if(o == null){
                    values.add(null);
                } else {
                    String str = o.toString().trim();
                    values.add(str.isEmpty() ? null : str);
                }
            }
        } else {
            log.warn("Illegal '{}' value '{}' (type: '{}')! Supported: String, String[] and Iterables",
                new Object[]{SesameYard.CONTEXT_URI, value, value.getClass()});
            log.warn("   ... return empty context array as fallback!");
            return new String[]{};
        }
        return values.toArray(new String[values.size()]);
    }
    /**
     * Setter for the {@link SesameYard#INCLUDE_INFERRED} state
     * @param state the state or <code>null</code> to remove the config (reset to 
     * the {@link SesameYard#DEFAULT_INCLUDE_INFERRED default})
     */
    public void setIncludeInferred(Boolean state){
        if(state == null){
            config.remove(SesameYard.INCLUDE_INFERRED);
        } else {
            config.put(SesameYard.INCLUDE_INFERRED, state);
        }
    }
    /**
     * Getter for the {@link SesameYard#INCLUDE_INFERRED} state.
     * @return the state or {@link SesameYard#DEFAULT_INCLUDE_INFERRED} if not
     * present in the configuration.
     */
    public boolean isIncludeInferred(){
        Object value = config.get(SesameYard.INCLUDE_INFERRED);
        if(value instanceof Boolean){
            return ((Boolean)value).booleanValue();
        } else if(value != null){
            return Boolean.parseBoolean(value.toString());
        } else {
            return SesameYard.DEFAULT_INCLUDE_INFERRED;
        } 
    }
    
    @Override
    protected void validateConfig() throws ConfigurationException {
        Object value = config.get(SesameYard.CONTEXT_URI);
        if(!(value == null || value instanceof String || value instanceof String[]
                || value instanceof Iterable<?>)){
            throw new ConfigurationException(SesameYard.CONTEXT_URI, String.format(
                "Illegal '%s' value '%s' (type: '%s')! Supported: String, String[] and Iterables",
                SesameYard.CONTEXT_URI, value, value.getClass()));
        }
    }

}
