/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.whiteboard.fmeschbe.miltondav.impl;

import org.apache.sling.auth.core.AuthenticationSupport;

import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.MiltonServlet;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;

/**
 * The <code>SlingAuthenticationHandler</code> is the actual
 * <code>AuthenticationHandler</code> used by the Sling
 * <code>MiltonServlet</code> implementations. This handler accounts for the
 * fact that authentication has already been handled when the
 * <code>MiltonServlet</code> gains control over the request.
 */
public class SlingAuthenticationHandler implements AuthenticationHandler {

    static final String NAME = "org.apache.sling.whiteboard.fmeschbe.miltondav.impl.SlingAuthenticationHandler";

    /**
     * Returns <code>true</code> if the current request has successfully been
     * authenticated and the Sling <code>ResourceResolver</code> is available
     * for request processing. This is generally the case, so we can assume this
     * method will always return <code>true</code>.
     */
    public boolean supports(Resource r, Request request) {
        return getResourceResolver() != null;
    }

    /**
     * Returns the Sling <code>ResourceResolver</code> created during Sling
     * request authentication.
     * <p>
     * This method will only be called if the
     * {@link #supports(Resource, Request)} method of this implementation has
     * already been called and returned <code>true</code> in which case the
     * result of this method is never <code>null</code>.
     */
    public Object authenticate(Resource resource, Request request) {
        return getResourceResolver();
    }

    /**
     * Returns <code>null</code> since authentication has already completed and
     * no more processing is to occurr.
     */
    public String getChallenge(Resource resource, Request request) {
        return null;
    }

    /**
     * Always return <code>true</code> since a resource only exists after
     * successfull authentication by the Sling authentication infrastructure.
     */
    public boolean isCompatible(Resource resource) {
        return true;
    }

    /**
     * Returns the Sling <code>ResourceResolver</code> from the request
     * attribute.
     */
    private Object getResourceResolver() {
        return MiltonServlet.request().getAttribute(
            AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
    }
}
