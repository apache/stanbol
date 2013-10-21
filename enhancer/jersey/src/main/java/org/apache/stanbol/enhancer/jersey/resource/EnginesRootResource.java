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
package org.apache.stanbol.enhancer.jersey.resource;

import javax.ws.rs.Path;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.ContentItemFactory;
import org.apache.stanbol.enhancer.servicesapi.EnhancementEngineManager;
import org.apache.stanbol.enhancer.servicesapi.EnhancementJobManager;
/**
 * This provides backward compatibility for the "/engines" endpoint that was
 * used to enhance content parsed to the Stanbol Enhancer before the 
 * implementation of the "/enhancer" RESTful interface defined by
 * STANBOL-431.<p>
 * This provides the same interface as for "/enhancer" by sub-classing the
 * {@link EnhancerRootResource}.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("/engines")
public final class EnginesRootResource extends BaseStanbolResource  {
    
    @Reference
    private EnhancementJobManager jobManager;
    @Reference
    private EnhancementEngineManager engineManager;
    @Reference
    private ChainManager chainManager;
    @Reference
    private ContentItemFactory ciFactory;
    @Reference
    private Serializer serializer;
    
    @Path("")
    public GenericEnhancerUiResource get() {
        return new GenericEnhancerUiResource(null, jobManager, 
                engineManager, chainManager, ciFactory, serializer,
                getLayoutConfiguration(), getUriInfo());
    }
    
}
