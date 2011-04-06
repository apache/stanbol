package org.apache.stanbol.ontologymanager.web.resource;

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
        return Arrays.asList(new MenuItem("Home", "/kres", "homeMenu", uriInfo, null),
        		new MenuItem("Usage", "javascript:expandMenu('usageMenu')", "usageMenu", uriInfo, 
        				Arrays.asList(new MenuItem("Documentation", "/ontomgr/documentation", "documentationMenu", uriInfo, null),
        							  new MenuItem("RESTful services", "/ontomgr/documentation/restful", "restfulMenu", uriInfo, null))));
        
        
    }

    public static class MenuItem {

        public MenuItem(String label, String link, String id, UriInfo uriInfo, List<MenuItem> subMenu) {
            this.label = label;
            this.link = link;
            this.id = id;
            this.subMenu = subMenu;
            cssClass = uriInfo.getPath().startsWith(link.substring(1)) ? "current" : "unselected";
        }

        protected final String label;
        
        protected final String id;

        protected final  String link;
        
        protected final  List<MenuItem> subMenu;

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
        
        public String getId() {
			return id;
		}
        
        public List<MenuItem> getSubMenu() {
			return subMenu;
		}

    }

}
