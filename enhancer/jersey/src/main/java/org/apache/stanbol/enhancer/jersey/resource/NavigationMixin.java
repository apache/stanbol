package org.apache.stanbol.enhancer.jersey.resource;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

public class NavigationMixin {

    @Context
    protected UriInfo uriInfo;

    public URI getPublicBaseUri() {
        return uriInfo.getBaseUri();
    }

    public List<MenuItem> getMainMenuItems() {
        return Arrays.asList(
                new MenuItem("/engines", "/engines", uriInfo),
                new MenuItem("/store", "/store", uriInfo),
                new MenuItem("/sparql", "/sparql", uriInfo));
    }

    public static class MenuItem {

        public MenuItem(String label, String link, UriInfo uriInfo) {
            this.label = label;
            this.link = link;
            cssClass = uriInfo.getPath().startsWith(link.substring(1)) ? "selected" : "unselected";
        }

        protected final String label;

        protected final  String link;

        protected final  String cssClass;

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
