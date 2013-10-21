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
package org.apache.stanbol.reasoners.web.input.impl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.stanbol.ontologymanager.servicesapi.scope.ScopeManager;
import org.apache.stanbol.ontologymanager.servicesapi.session.SessionManager;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputFactory;
import org.apache.stanbol.reasoners.servicesapi.ReasoningServiceInputManager;
import org.apache.stanbol.reasoners.web.input.provider.impl.FileInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.OntologyManagerInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.RecipeInputProvider;
import org.apache.stanbol.reasoners.web.input.provider.impl.UrlInputProvider;
import org.apache.stanbol.rules.base.api.RuleAdapterManager;
import org.apache.stanbol.rules.base.api.RuleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for a {@see ReasoningServiceInputManager}. It creates a {@see SimpleInputManager} collecting all
 * known {@see ReasoningServiceInputProvider}s.
 * 
 * TODO In the future we may want to move this out of the /web module into a dedicated /input module.
 * 
 * @author enridaga
 * 
 */
public class RESTInputFactory implements ReasoningServiceInputFactory {
    ScopeManager onm;
    SessionManager sessionManager;
    RuleStore rStore;
    RuleAdapterManager adapterManager;

    private Logger log = LoggerFactory.getLogger(getClass());

    public RESTInputFactory(ScopeManager onm,
                            SessionManager sm,
                            RuleStore rStore,
                            RuleAdapterManager adapterManager) {
        this.onm = onm;
        this.sessionManager = sm;
        this.rStore = rStore;
        this.adapterManager = adapterManager;
    }

    @Override
    public ReasoningServiceInputManager createInputManager(Map<String,List<String>> parameters) {
        ReasoningServiceInputManager inmgr = new SimpleInputManager();
        String scope = null;
        String session = null;
        for (Entry<String,List<String>> entry : parameters.entrySet()) {
            if (entry.getKey().equals("url")) {
                if (!entry.getValue().isEmpty()) {
                    // We keep only the first value
                    // XXX (make sense support multiple values?)
                    inmgr.addInputProvider(new UrlInputProvider(entry.getValue().iterator().next()));
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'url' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            } else if (entry.getKey().equals("file")) {
                if (!entry.getValue().isEmpty()) {
                    // We keep only the first value
                    // FIXME We create the file once again...
                    inmgr.addInputProvider(new FileInputProvider(new File(entry.getValue().iterator().next())));
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'url' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }
            } else if (entry.getKey().equals("scope")) {
                if (!entry.getValue().isEmpty()) {
                    scope = entry.getValue().iterator().next();
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'scope' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }

            } else if (entry.getKey().equals("session")) {
                if (!entry.getValue().isEmpty()) {
                    session = entry.getValue().iterator().next();
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'session' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }

            } else if (entry.getKey().equals("recipe")) {
                if (!entry.getValue().isEmpty()) {
                    inmgr.addInputProvider(new RecipeInputProvider(rStore, adapterManager, entry.getValue()
                            .iterator().next()));
                } else {
                    // Parameter exists with no value
                    log.error("Parameter 'recipe' must have a value!");
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                }

            }
        }
        if (scope != null) {
            inmgr.addInputProvider(new OntologyManagerInputProvider(onm, sessionManager, scope, session));
        }
        return inmgr;
    }

}
