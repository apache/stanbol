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
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.stanbol.enhancer.servicesapi.Chain;

/**
 * RESTful interface for enhancement {@link Chain}s. This is basically the
 * same as the {@link EnhancerRootResource} but sets the used
 * {@link EnhancerRootResource#chain} to the one parsed by the {@link PathParam}. 
 *
 */
@Path("/enhancer/chain/{chain}")
public final class EnhancementChainResource extends AbstractEnhancerUiResource {

    public EnhancementChainResource(@PathParam(value = "chain") String chain,
                                    @Context ServletContext context) {
        super(chain,context);
        if(chain == null || chain.isEmpty()){
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

}
