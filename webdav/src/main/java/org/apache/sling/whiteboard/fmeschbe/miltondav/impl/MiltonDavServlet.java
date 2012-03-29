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

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.whiteboard.fmeschbe.miltondav.impl.resources.SlingResourceFactory;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>MiltonDavServlet</code> is the {@link AbstractMiltonDavServlet}
 * extension registering with the OSGi Http Service. As such it implements an
 * {@link #activate(Map) activator} method as follows:
 * <ul>
 * <li>Prepare Servlet configuration for the MiltonServlet setup</li>
 * <li>Use a HttpContext implementation implementing the
 * <code>handleSecurity</code> method calling the Sling AuthenticationSupport
 * service</li>
 * <li>Register with the Http Service</li>
 * </ul>
 */
@Component(metatype = true, name = "org.apache.sling.miltondav.impl.MiltonDavServlet", label = "%miltondav.name", description = "%miltondav.description")
public class MiltonDavServlet extends AbstractMiltonDavServlet {

    // default location at which the service is registered
    private static final String DEFAULT_PREFIX = "/dav2";

    // name of the configuration parameter providing the servlet path
    @Property(name = "prefix", value = DEFAULT_PREFIX)
    private static final String PROP_PREFIX = "prefix";

    // whether authentication is required when using this servlet (true)
    private static final boolean DEFAULT_REQUIRE_AUTH = true;

    // name of the configuration parameter defining wether auth. is required
    @Property(name = "require.auth", boolValue = DEFAULT_REQUIRE_AUTH)
    private static final String PROP_REQUIRE_AUTH = "require.auth";

    /** default log */
    private final Logger log = LoggerFactory.getLogger(getClass().getSimpleName());


    @Reference
    private HttpService osgiHttpService;



    // the actual prefix under which the servlet is registered, only non-null
    // if the servlet is actually registered with the Http Service
    private String prefix;

    /**
     * Overwrites the base class method solely to ensure the resource resolver
     * is closed when the request terminates.
     * <p>
     * This is only required when registering the servlet with the OSGi
     * HttpService directly.
     */
    @Override
    public void service(ServletRequest servletRequest,
            ServletResponse servletResponse) throws ServletException,
            IOException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("{} {}", new Object[] {
                    ((HttpServletRequest) servletRequest).getMethod(),
                    ((HttpServletRequest) servletRequest).getRequestURI() });
            }
            super.service(servletRequest, servletResponse);
        } finally {
            
        }
    }

    // ---------- SCR integration

    @SuppressWarnings("unused")
    @Activate
    private void activate(Map<String, Object> config) {

        final Object propRequireAuth = config.get(PROP_REQUIRE_AUTH);
        final boolean requireAuth;
        if (propRequireAuth instanceof Boolean) {
            requireAuth = (Boolean) propRequireAuth;
        } else if (propRequireAuth instanceof String) {
            requireAuth = Boolean.valueOf((String) propRequireAuth);
        } else {
            requireAuth = DEFAULT_REQUIRE_AUTH;
        }

        final Object propPrefix = config.get(PROP_PREFIX);
        final String prefix;
        if (propPrefix instanceof String) {
            prefix = (String) propPrefix;
        } else {
            prefix = DEFAULT_PREFIX;
        }

        java.util.Properties props = new java.util.Properties();
        props.put("resource.factory.class", SlingResourceFactory.NAME);
        props.put("authentication.handler.classes",
            SlingAuthenticationHandler.NAME);
        props.put("response.handler.class", SlingResponseHandler.NAME);

        HttpContext context = new HttpContext() {
            private final boolean authRequired = requireAuth;

            public boolean handleSecurity(HttpServletRequest request,
                    HttpServletResponse response) throws IOException {
                /*if (authRequired) {
                    request.setAttribute(
                        AuthenticationHandler.REQUEST_LOGIN_PARAMETER, "Basic");
                }
                return slingAuthenticator.handleSecurity(request, response);*/
            	return true;
            }

            // this context provides no resources, always call the servlet
            public URL getResource(String name) {
                return null;
            }

            public String getMimeType(String name) {
                return null;
            }
        };

        try {
            osgiHttpService.registerServlet(prefix, this, props, context);
            this.prefix = prefix;
        } catch (NamespaceException ne) {
            log.error(
                "activate: Cannot register Milton based WebDAV servlet at "
                    + prefix
                    + "; another servlet is already registered at that path. Please configure a different path for this servlet",
                ne);
        } catch (ServletException se) {
            log.error(
                "activate: Error while initializing the Milton based WebDAV servlet",
                se);
        }
    }

    @SuppressWarnings("unused")
    @Deactivate
    private void deactivate() {
        if (prefix != null) {
            osgiHttpService.unregister(prefix);
        }
    }
}