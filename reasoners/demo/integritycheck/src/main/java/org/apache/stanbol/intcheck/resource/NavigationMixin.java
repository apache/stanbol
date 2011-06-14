package org.apache.stanbol.intcheck.resource;

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
        return Arrays.asList(//new MenuItem("Home", "/intcheck", "homeMenu", uriInfo, null),
        		//new MenuItem("ONM", "javascript:expandMenu('onmanagerMenu')", "onmanagerMenu", uriInfo,Arrays.asList(
                    new MenuItem("Scopes", "javascript:listScopes()", "scopesMenu", uriInfo, null),
                    //new MenuItem("Sessions", "/intcheck/session", "sessionsMenu", uriInfo, null))),
        		//new MenuItem("R&I", "javascript:expandMenu('reasoningMenu')", "reasoningMenu", uriInfo,Arrays.asList(
                    new MenuItem("Recipes&Rules", "/intcheck/recipe", "rulesMenu", uriInfo, null),
                    //new MenuItem("Reasoning services", "/intcheck/reasoning", "reasoningServicesMenu", uriInfo, null))),
        		//new MenuItem("Semion", "javascript:expandMenu('semionMenu')", "semionMenu", uriInfo,Arrays.asList(
                    //new MenuItem("Reenginner", "/intcheck/reengineer", "reengineerMenu", uriInfo, null),
                    //new MenuItem("Refactorer", "/intcheck/refactorer", "refactorerMenu", uriInfo, null))),
        		//new MenuItem("Storage", "/intcheck/graphs", "storageMenu", uriInfo, null),
        		//new MenuItem("Usage", "javascript:expandMenu('usageMenu')", "usageMenu", uriInfo, Arrays.asList(
                    //new MenuItem("Documentation", "/intcheck/documentation", "documentationMenu", uriInfo, null),
                    //new MenuItem("RESTful services", "/intcheck/documentation/restful", "documentationMenu", uriInfo, null),
                    new MenuItem("Demo", "/intcheck/demo", "demoWikinewsMenu", uriInfo, null));
        
        
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
