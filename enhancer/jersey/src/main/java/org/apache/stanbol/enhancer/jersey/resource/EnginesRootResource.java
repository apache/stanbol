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

import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
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
@Path("/engines")
public final class EnginesRootResource extends AbstractEnhancerUiResource {
    
    public EnginesRootResource(@Context ServletContext context) {
        super(null,context);
    }
}
