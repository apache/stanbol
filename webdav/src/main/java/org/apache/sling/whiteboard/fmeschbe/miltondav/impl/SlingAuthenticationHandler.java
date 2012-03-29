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

import com.bradmcevoy.http.AuthenticationHandler;
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
     * Returns <code>true</code>
     */
    public boolean supports(Resource r, Request request) {
        return true;
    }

    /**
     * Returns a constant string
     */
    public Object authenticate(Resource resource, Request request) {
        return "good user";
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

}
