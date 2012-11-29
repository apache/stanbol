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
package org.apache.stanbol.factstore.web.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.stanbol.commons.ldviewable.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/factstore")
public class FactStoreRootResource extends BaseFactStoreResource {
    
    @SuppressWarnings("unused")
    private static Logger logger = LoggerFactory.getLogger(FactStoreRootResource.class);
    
    @GET
    @Produces(TEXT_HTML)
    public Response get() {
        return Response.ok(new Viewable("index", this), TEXT_HTML).build();
    }
    
}
