package org.apache.stanbol.commons.web.base;

/**
 * Data transfer object to define an entry in the main navigation menu.
 */
public class NavigationLink implements Comparable<NavigationLink> {

    public final String label;

    public final String path;

    public final String descriptionTemplate;

    public final int order;

    public NavigationLink(String path, String label, String descriptionTemplate, int order) {
        this.path = path;
        this.label = label;
        this.descriptionTemplate = descriptionTemplate;
        this.order = order;
    }

    @Override
    public int compareTo(NavigationLink other) {
        return order - other.order;
    }

    public String getLabel() {
        return label;
    }

    public String getPath() {
        return path;
    }

    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }

    public int getOrder() {
        return order;
    }

}
