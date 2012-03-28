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
package org.apache.sling.whiteboard.fmeschbe.miltondav.impl.resources;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.auth.core.AuthenticationSupport;
import org.apache.sling.whiteboard.fmeschbe.miltondav.impl.MiltonDavServlet;

import com.bradmcevoy.http.MiltonServlet;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;

public class SlingResourceFactory implements ResourceFactory {

    public static final String NAME = "org.apache.sling.whiteboard.fmeschbe.miltondav.impl.resources.SlingResourceFactory";

    private String prefix;

    public SlingResourceFactory() {
    }

    public Resource getResource(final String host, String path) {

        // cut off prefix
        final String prefixPath = getPrefix();
        if (path.startsWith(prefixPath)) {
            path = path.substring(prefixPath.length());
        }

        // ensure non-empty path
        if (path.length() == 0) {
            path = "/";
        }

        org.apache.sling.api.resource.Resource slingResource = getResourceResolver().getResource(
            path);
        return createResource(slingResource);
    }

    static ResourceResolver getResourceResolver() {
        return (ResourceResolver) MiltonServlet.request().getAttribute(
            AuthenticationSupport.REQUEST_ATTRIBUTE_RESOLVER);
    }

    static Resource createResource(
            final org.apache.sling.api.resource.Resource slingResource) {
        if (slingResource == null) {
            return null;
        }

        Node node = slingResource.adaptTo(javax.jcr.Node.class);
        if (node != null) {
            try {
                if (node.isNodeType("nt:file")) {
                    return new JcrFileResource(slingResource);
                }
            } catch (RepositoryException re) {
                // TODO: log
            }

            return new JcrFolderResource(slingResource);
        }

        File file = slingResource.adaptTo(File.class);
        if (file != null) {
            if (file.isFile()) {
                return new PlatformFileResource(slingResource);
            }
            return new PlatformFolderResource(slingResource);
        }

        if ("nt:file".equals(slingResource.getResourceType())) {
            return new ReadOnlyFileResource(slingResource);
        }

        return new ReadOnlyFolderResource(slingResource);
    }

    private String getPrefix() {
        if (prefix == null) {
            StringBuilder b = new StringBuilder();
            HttpServletRequest request = MiltonDavServlet.request();

            // start with context path if not null (may be empty)
            if (request.getContextPath() != null) {
                b.append(request.getContextPath());
            }

            // append servlet path if not null (may be empty)
            if (request.getServletPath() != null) {
                b.append(request.getServletPath());
            }

            // cut off trailing slash
            if (b.length() > 1 && b.charAt(b.length() -1) == '/') {
                b.setLength(b.length()-1);
            }

            prefix = b.toString();
        }
        return prefix;
    }
}
