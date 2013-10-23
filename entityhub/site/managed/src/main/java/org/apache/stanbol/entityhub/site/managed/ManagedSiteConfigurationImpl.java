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
package org.apache.stanbol.entityhub.site.managed;

import java.util.Dictionary;

import org.apache.stanbol.entityhub.core.site.SiteConfigurationImpl;
import org.apache.stanbol.entityhub.servicesapi.model.ManagedEntityState;
import org.apache.stanbol.entityhub.servicesapi.model.MappingState;
import org.apache.stanbol.entityhub.servicesapi.site.ManagedSiteConfiguration;
import org.osgi.service.cm.ConfigurationException;

public class ManagedSiteConfigurationImpl extends SiteConfigurationImpl implements ManagedSiteConfiguration {

    public ManagedSiteConfigurationImpl(Dictionary<String,Object> config) throws ConfigurationException {
        super(config);
        //set the defaults for managed sites to active and confirmed
        if(getDefaultManagedEntityState() == null){
            setDefaultManagedEntityState(ManagedEntityState.active);
        }
        if(getDefaultMappedEntityState() == null){
            setDefaultMappedEntityState(MappingState.confirmed);
        }
    }
    
    @Override
    public final String getYardId() {
        Object id = config.get(YARD_ID);
        return id == null || id.toString().isEmpty() ? 
                getId() : id.toString();
    }
    
    /**
     * 
     * @param id
     * @throws UnsupportedOperationException in case this configuration is {@link #readonly}
     * @see #getCacheId()
     */
    public final void getYardId(String id) throws UnsupportedOperationException {
        if(id == null || id.isEmpty()){
            config.remove(YARD_ID);
        } else {
            config.put(YARD_ID, id);
        }
    }
    
}
