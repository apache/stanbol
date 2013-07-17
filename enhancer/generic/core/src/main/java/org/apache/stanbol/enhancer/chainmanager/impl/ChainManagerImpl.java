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
package org.apache.stanbol.enhancer.chainmanager.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.enhancer.servicesapi.ChainManager;
import org.apache.stanbol.enhancer.servicesapi.impl.ChainsTracker;
import org.osgi.service.component.ComponentContext;

/**
 * Implementation of the ChainManager interface as OSGI component based
 * on {@link ChainsTracker}.
 * 
 * @author Rupert Westenthaler
 *
 */
@Component(immediate=true,enabled=true)
@Service(value=ChainManager.class)
public class ChainManagerImpl extends ChainsTracker implements ChainManager {

    public ChainManagerImpl(){
        super();
    }
    
    @Activate
    public void activate(ComponentContext ctx){
        initChainTracker(ctx.getBundleContext(), null, null);
        open();
    }
    @Deactivate
    public void deactivate(ComponentContext ctx){
        close();
    }
}
