package org.apache.stanbol.commons.web.base.resource;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.stanbol.commons.web.base.LinkResource;
import org.apache.stanbol.commons.web.base.ScriptResource;

/**
 * Mixin class to provide the controller method for the navigation template.
 * 
 * TODO: make the list of menu items dynamically contributed by WebFragments from the OSGi runtime.
 */
public class BaseStanbolResource {

    public static final String LINK_RESOURCES = "org.apache.stanbol.commons.web.resource.links";

    public static final String SCRIPT_RESOURCES = "org.apache.stanbol.commons.web.resource.scripts";

    public static final String STATIC_RESOURCES_ROOT_URL = "org.apache.stanbol.commons.web.resource.static.url.root";

    @Context
    protected UriInfo uriInfo;

    @Context
    protected ServletContext servletContext;

    public URI getPublicBaseUri() {
        return uriInfo.getBaseUri();
    }

    public List<MenuItem> getMainMenuItems() {
        return Arrays.asList(new MenuItem("/engines", "/engines", uriInfo), new MenuItem("/store", "/store",
                uriInfo), new MenuItem("/sparql", "/sparql", uriInfo));
    }

    public static class MenuItem {

        public MenuItem(String label, String link, UriInfo uriInfo) {
            this.label = label;
            this.link = link;
            cssClass = uriInfo.getPath().startsWith(link.substring(1)) ? "selected" : "unselected";
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

    public String getStaticRootUrl() {
        return (String) servletContext.getAttribute(STATIC_RESOURCES_ROOT_URL);
    }

    @SuppressWarnings("unchecked")
    public List<LinkResource> getRegisteredLinkResources() {
        if (servletContext != null) {
            return (List<LinkResource>) servletContext.getAttribute(LINK_RESOURCES);
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    public List<ScriptResource> getRegisteredScriptResources() {
        if (servletContext != null) {
            return (List<ScriptResource>) servletContext.getAttribute(SCRIPT_RESOURCES);
        } else {
            return Collections.emptyList();
        }
    }
}
