/*
 * Copyright 2013 The Apache Software Foundation.
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
package org.apache.stanbol.commons.web.base.resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.NavigationLink;
import org.apache.stanbol.commons.web.base.ScriptResource;

/**
 * Provides the method the common templates expect in the data object
 */
public abstract class TemplateLayoutConfiguration implements LayoutConfiguration{
    
    public static final String SYSTEM_CONSOLE = "system/console";

    /**
     * The Apache Felix Webconsole base URL does not depend on alias configured
     * for the Stanbol JerseyEndpoint. However they is affected by the base
     * path of the Servlet when Stanbol is running as WAR within a web container.<p>
     * <i>LIMITATION</i> this does not take into account the path configured
     * for the Apache Felix Webconsole (property: <code>manager.root</code>
     * class: <code>org.apache.felix.webconsole.internal.servlet.OsgiManager</code>)
     * Because of this it will only work with the default {@link #SYSTEM_CONSOLE}.
     * @return The URI for the Apache Felix Webconsole
     */
    public URI getConsoleBaseUri() {
        String root = getRootUrl();
        UriBuilder consolePathBuilder;
        if (StringUtils.isNotBlank(root) && !"/".equals(root)) {
            String request = getUriInfo().getRequestUri().toString();
            int aliasIndex = request.lastIndexOf(root);
            if (aliasIndex > 0) {
                request = request.substring(0, request.lastIndexOf(root));
            }
            consolePathBuilder = UriBuilder.fromUri(request);
        } else {
            consolePathBuilder = getUriInfo().getRequestUriBuilder();
        }
        if (this.getClass().isAnnotationPresent(Path.class)) {
            String path = this.getClass().getAnnotation(Path.class).value();
            int levels = path.endsWith("/") ? StringUtils.countMatches(path, "/") - 1 : StringUtils.countMatches(path, "/");
            for (int i = levels; i > 0; i--) {
                consolePathBuilder = consolePathBuilder.path("../");
            }
        }
        return consolePathBuilder.path(SYSTEM_CONSOLE).build();
    }

    protected abstract LayoutConfiguration getLayoutConfiguration();

    /**
     * @return menu items with "selected" CSS class for the active link precomputed where applicable
     */
    public List<MenuItem> getMainMenuItems() {
        List<MenuItem> items = new ArrayList<MenuItem>();
        for (NavigationLink link : getNavigationLinks()) {
            items.add(new MenuItem(link.getLabel(), link.getPath(), getUriInfo()));
        }
        return items;
    }

    /**
     * @return the sorted list of navigation links data transfer objects
     */
    public List<NavigationLink> getNavigationLinks() {
        return getLayoutConfiguration().getNavigationLinks();
    }

    public URI getPublicBaseUri() {
        if (getUriInfo() == null) {
            throw new RuntimeException("UriInfo has not been injected");
        }
        return getUriInfo().getBaseUri();
    }

    @SuppressWarnings(value = "unchecked")
    public List<LinkResource> getRegisteredLinkResources() {
        return getLayoutConfiguration().getRegisteredLinkResources();
    }

    @SuppressWarnings(value = "unchecked")
    public List<ScriptResource> getRegisteredScriptResources() {
        return getLayoutConfiguration().getRegisteredScriptResources();
    }

    public URI getRequestUri() {
        if (getUriInfo() == null) {
            throw new RuntimeException("UriInfo has not been injected");
        }
        return getUriInfo().getAbsolutePath();
    }

    public String getRootUrl() {
        return getLayoutConfiguration().getRootUrl();
    }

    
    public String getStaticResourcesRootUrl() {
        return getLayoutConfiguration().getStaticResourcesRootUrl();
    }
    
    public String getStaticRootUrl() {
        String baseURI = getPublicBaseUri().toString();
        return baseURI.substring(0, baseURI.length() - 1) + getStaticResourcesRootUrl();
    }

    protected abstract UriInfo getUriInfo();
    
    
    public static class MenuItem {

        public MenuItem(String label, String link, UriInfo uriInfo) {
            this.label = label;
            if (link.startsWith("/")) {
                link = link.substring(1);
            }
            this.link = link;
            cssClass = uriInfo.getPath().startsWith(link) ? "selected" : "unselected";
        }

        protected final String label;

        protected final String link;

        protected final String cssClass;

        public String getLabel() {
            return label;
        }

        public String getLink() {
            return link;
        }

        public String getCssClass() {
            return cssClass;
        }

    }
    
}
