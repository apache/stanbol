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
package org.apache.stanbol.webdav;

import com.bradmcevoy.http.AuthenticationService;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.http11.Http11ResponseHandler;
import com.bradmcevoy.http.values.ValueWriters;
import com.bradmcevoy.http.webdav.DefaultWebDavResponseHandler;
import com.bradmcevoy.http.webdav.PropFindXmlGenerator;
import com.bradmcevoy.http.webdav.ResourceTypeHelper;

/**
 * The <code>SlingResponseHandler</code> is basically a
 * <code>DefaultWebDavResponseHandler</code> but overwriting the
 * {@link #respondCreated(Resource, Response, Request)} method to actually call
 * <code>respondNoContent</code>.
 */
public class SlingResponseHandler extends DefaultWebDavResponseHandler {

    static final String NAME = "org.apache.stanbol.webdav.SlingResponseHandler";

    public SlingResponseHandler() {
        super(new AuthenticationService());
    }

    public SlingResponseHandler(AuthenticationService authenticationService) {
        super(authenticationService);
    }

    public SlingResponseHandler(AuthenticationService authenticationService,
            ResourceTypeHelper resourceTypeHelper) {
        super(authenticationService, resourceTypeHelper);
    }

    public SlingResponseHandler(ValueWriters valueWriters,
            AuthenticationService authenticationService) {
        super(valueWriters, authenticationService);
    }

    public SlingResponseHandler(ValueWriters valueWriters,
            AuthenticationService authenticationService,
            ResourceTypeHelper resourceTypeHelper) {
        super(valueWriters, authenticationService, resourceTypeHelper);
    }

    public SlingResponseHandler(Http11ResponseHandler wrapped,
            ResourceTypeHelper resourceTypeHelper,
            PropFindXmlGenerator propFindXmlGenerator) {
        super(wrapped, resourceTypeHelper, propFindXmlGenerator);
    }

    @Override
    public void respondCreated(Resource resource, Response response,
            Request request) {
        respondNoContent(resource, response, request);
    }
}
