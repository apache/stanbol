package org.apache.stanbol.commons.web.base;

/**
 * Simple data transfer object for scripts (typically javascript) to be contributed by WebFragment for
 * inclusion to the HTML head of the pages by the NavigationMixin abstract JAX-RS resource.
 */
public class ScriptResource {

    private final String type;
    private final String relativePath;
    private final WebFragment fragment;

    public ScriptResource(String type, String relativePath, WebFragment fragment) {
        this.type = type;
        this.relativePath = relativePath;
        this.fragment = fragment;
    }

    public String getType() {
        return type;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getFragmentName() {
        return fragment.getName();
    }

}
