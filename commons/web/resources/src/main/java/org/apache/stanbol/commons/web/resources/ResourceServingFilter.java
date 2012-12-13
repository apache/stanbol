/*
 * Copyright 2012 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.stanbol.commons.web.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.component.ComponentContext;

/**
 * Serves resource in META-INF/services of any active bundle
 */
@Component
@Service(Filter.class)
@Properties(value = {
    @Property(name = "pattern", value = ".*"),
    @Property(name = "service.ranking", intValue = 500)
})
public class ResourceServingFilter implements Filter, BundleListener {

    public static final String RESOURCE_PREFIX = "META-INF/resources";
    public static final int RESOURCE_PREFIX_LENGTH = RESOURCE_PREFIX.length();
    private Set<Bundle> resourceProvidingBundles;
    private Map<String, Bundle> path2Bundle;

    @Activate
    protected void activate(final ComponentContext context) {
        resourceProvidingBundles = new HashSet<Bundle>();
        path2Bundle = new HashMap<String, Bundle>();
        final Bundle[] registeredBundles = context.getBundleContext().getBundles();
        for (int i = 0; i < registeredBundles.length; i++) {
            if (registeredBundles[i].getState() == Bundle.ACTIVE) {
                registerResources(registeredBundles[i]);
            }
        }
        context.getBundleContext().addBundleListener(this);
    }

    @Deactivate
    protected void deactivate(final ComponentContext context) {
        context.getBundleContext().removeBundleListener(this);
        resourceProvidingBundles = null;
        path2Bundle = null;

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        doFilterHttp((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    @Override
    public void destroy() {
    }

    private synchronized void registerResources(Bundle bundle) {
        //TODO maybe bundle.getLastModified() could be used for modification data in http-headers, and for if-modified since negotiation
        registerResourcesWithPathPrefix(bundle, RESOURCE_PREFIX);
    }

    private void registerResourcesWithPathPrefix(Bundle bundle, String prefix) {
        final Enumeration<String> resourceEnum = bundle.getEntryPaths(prefix);
        if (resourceEnum != null && resourceEnum.hasMoreElements()) {
            resourceProvidingBundles.add(bundle);
            while (resourceEnum.hasMoreElements()) {
                String resourcePath = resourceEnum.nextElement();
                if (resourcePath.endsWith("/")) {
                    registerResourcesWithPathPrefix(bundle, resourcePath);
                } else {
                    path2Bundle.put(resourcePath.substring(RESOURCE_PREFIX_LENGTH), bundle);
                }
            }
        }
    }
       
    private void doFilterHttp(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        final String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        String resourcePatch = requestPath.substring(contextPath.length());
        Bundle resourceBundle = path2Bundle.get(resourcePatch);
        if (resourceBundle != null) {
            if (request.getMethod().equals("GET") || request.getMethod().equals("HEAD")) {
                URL url = resourceBundle.getEntry(RESOURCE_PREFIX + resourcePatch);
                String mediaType = URLConnection.guessContentTypeFromName(url.getFile());
                response.setContentType(mediaType);
                //TODO can we get the length of a resource without 
                //TODO handle caching related headers
                if (!request.getMethod().equals("HEAD")) {
                    OutputStream os = response.getOutputStream();
                    byte[] ba = new byte[1024];
                    InputStream is = url.openStream();
                    int i = is.read(ba);
                    while (i != -1) {
                        os.write(ba, 0, i);
                        i = is.read(ba);
                    }
                    os.flush();
                }
            } else {
                //TODO handle OPTIONS
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        final Bundle bundle = event.getBundle();
        if (event.getType() == BundleEvent.STARTED) {
            registerResources(bundle);
        } else {
            if (resourceProvidingBundles.contains(bundle)) {
                synchronized (this) {
                    Iterator<Map.Entry<String, Bundle>> entryIter = path2Bundle.entrySet().iterator();
                    while (entryIter.hasNext()) {
                        Map.Entry<String, Bundle> entry = entryIter.next();
                        if (entry.getValue().equals(bundle)) {
                            entryIter.remove();
                        }
                    }
                }
            }
        }
    }

}
