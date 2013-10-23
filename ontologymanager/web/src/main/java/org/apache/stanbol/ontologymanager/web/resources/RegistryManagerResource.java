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
package org.apache.stanbol.ontologymanager.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
//import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.web.viewable.Viewable;
//import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.ontologymanager.registry.api.RegistryManager;
import org.apache.stanbol.ontologymanager.registry.api.model.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@Service(Object.class)
@Property(name="javax.ws.rs", boolValue=true)
@Path("/ontonet/registry")
public class RegistryManagerResource extends BaseStanbolResource {

    private final Logger log = LoggerFactory.getLogger(getClass());
    @Reference
    protected RegistryManager regMgr;

    public RegistryManagerResource() {
        super();
    }

    @GET
    @Produces(value = MediaType.TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
//        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public List<Library> getLibraries() {
        if (regMgr != null) {
            log.debug("There are {} ontology libraries registered.", regMgr.getLibraries().size());
            return new ArrayList<Library>(regMgr.getLibraries());
        } else {
            log.debug("There are no ontology libraries registered.");
            return Collections.emptyList();
        }
    }

    public String getPath() {
        return uriInfo.getPath().replaceAll("[\\/]*$", "");
    }

}
