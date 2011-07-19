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
package org.apache.stanbol.cmsadapter.web.resources;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;

@Path("/cmsadapter")
public class RootResource extends BaseStanbolResource {

    /**
     * Simply redirects user to CMS Adapter's wiki page at IKS Wiki.
     * 
     * @return
     */
    @GET
    public Response notifyChange() {
        try {
            return Response.seeOther(new URI("http://wiki.iks-project.eu/index.php/CMSAdapterRest")).build();
        } catch (URISyntaxException e) {
            throw new WebApplicationException(e, Response.status(Status.BAD_REQUEST).build());
        }
    }
}
