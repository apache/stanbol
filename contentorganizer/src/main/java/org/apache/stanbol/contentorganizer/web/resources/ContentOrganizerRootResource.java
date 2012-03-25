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
package org.apache.stanbol.contentorganizer.web.resources;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.contentorganizer.servicesapi.ContentOrganizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.view.ImplicitProduces;
import com.sun.jersey.api.view.Viewable;

/**
 * 
 * @author alexdma
 * 
 */

@Path("/virtualdir")
@ImplicitProduces(MediaType.TEXT_HTML + ";qs=2")
public class ContentOrganizerRootResource extends BaseStanbolResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    private final ContentOrganizer organizer;

    public ContentOrganizerRootResource(@Context ServletContext servletContext) {
        super();
        this.organizer = ContextHelper.getServiceFromContext(ContentOrganizer.class, servletContext);
        if (this.organizer == null) System.err.println("CHEZZI AMERI");
        else System.out.println("OLE");
    }

    @GET
    @Produces(TEXT_HTML)
    public Response getHtmlInfo(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    @POST
    public Response requestClassification(@Context HttpHeaders headers) {
        organizer.classifyContent();
        ResponseBuilder rb = Response.ok();
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

}
