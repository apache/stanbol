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
package org.apache.stanbol.enhancer.web.topic.resource;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.apache.stanbol.commons.web.base.CorsHelper.addCORSOrigin;
import static org.apache.stanbol.commons.web.base.CorsHelper.enableCORS;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.stanbol.commons.viewable.Viewable;
import org.apache.stanbol.commons.web.base.ContextHelper;
import org.apache.stanbol.commons.web.base.resource.BaseStanbolResource;
import org.apache.stanbol.enhancer.topic.TopicClassifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Path("/topic")
public class TopicClassifierRootResource extends BaseStanbolResource {

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(TopicClassifierRootResource.class);

    @OPTIONS
    public Response handleCorsPreflight(@Context HttpHeaders headers) {
        ResponseBuilder res = Response.ok();
        enableCORS(servletContext, res, headers);
        return res.build();
    }

    @GET
    @Produces(TEXT_HTML)
    public Response get(@Context HttpHeaders headers) {
        ResponseBuilder rb = Response.ok(new Viewable("index", this));
        rb.header(HttpHeaders.CONTENT_TYPE, TEXT_HTML + "; charset=utf-8");
        addCORSOrigin(servletContext, rb, headers);
        return rb.build();
    }

    public List<TopicClassifier> getClassifiers() throws InvalidSyntaxException {
        List<TopicClassifier> classifiers = new ArrayList<TopicClassifier>();
        BundleContext bundleContext = ContextHelper.getBundleContext(servletContext);
        ServiceReference[] references = bundleContext.getServiceReferences(TopicClassifier.class.getName(),
            null);
        if (references != null) {
            for (ServiceReference ref : references) {
                classifiers.add((((TopicClassifier) bundleContext.getService(ref))));
            }
        }
        return classifiers;
    }
}
