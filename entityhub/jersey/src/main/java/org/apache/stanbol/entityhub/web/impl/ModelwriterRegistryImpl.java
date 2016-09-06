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

package org.apache.stanbol.entityhub.web.impl;

import java.util.Collection;

import javax.ws.rs.core.MediaType;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.web.ModelWriter;
import org.apache.stanbol.entityhub.web.ModelWriterRegistry;
import org.apache.stanbol.entityhub.web.writer.ModelWriterTracker;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
@Component(immediate=true)
@Service
public class ModelwriterRegistryImpl implements ModelWriterRegistry {

    
    private ModelWriterTracker modelTracker;
    
    @Activate
    protected void activate(ComponentContext ctx){
        modelTracker = new ModelWriterTracker(ctx.getBundleContext());
        modelTracker.open();
    }
    
    @Deactivate
    protected void deactivate(ComponentContext ctx){
        if(modelTracker != null){
            modelTracker.close();
        }
        modelTracker = null;
    }
    
    @Override
    public Collection<ServiceReference> getModelWriters(MediaType mediaType,
            Class<? extends Representation> nativeType) {
        return modelTracker.getModelWriters(mediaType, nativeType);
    }

    @Override
    public ModelWriter getService(ServiceReference ref) {
        return modelTracker.getService(ref);
    }

    @Override
    public boolean isWriteable(MediaType mediaType, Class<? extends Representation> nativeType) {
        return !modelTracker.getModelWriters(mediaType, nativeType).isEmpty();
    }

}
