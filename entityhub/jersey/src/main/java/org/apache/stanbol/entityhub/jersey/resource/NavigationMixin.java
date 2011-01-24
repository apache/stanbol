package org.apache.stanbol.entityhub.jersey.resource;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

public class NavigationMixin {

    protected NavigationMixin() {
    }

    /**
     * Some subclasses set the uriInfo property. If that is necessary, than it's
     * best to do it when calling the super constructor (rw, 20101015)
     *
     * @param uriInfo
     */
    protected NavigationMixin(UriInfo uriInfo) {
        if (uriInfo == null) {
            throw new IllegalArgumentException("Parameter uriInfo MUST NOT be NULL");
        }
        this.uriInfo = uriInfo;
    }

    @Context
    protected UriInfo uriInfo;

    public URI getPublicBaseUri() {
        return uriInfo.getBaseUri();
    }

    public List<MenuItem> getMainMenuItems() {
        return Arrays.asList(new MenuItem("Entityhub", "/entityhub", uriInfo),
                new MenuItem("Referenced Site Manager", "/sites", uriInfo));
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

}
