package org.apache.stanbol.commons.web;

/**
 * Simple data transfer object for stylesheet (CSS) and fiveicon list to be contributed by WebFragment for
 * inclusion to the HTML head of the pages by the NavigationMixin abstract JAX-RS resource.
 */
public class LinkResource {

    private final String rel;
    private final String relativePath;
    private final WebFragment fragment;

    public LinkResource(String rel, String relativePath, WebFragment fragment) {
        this.rel = rel;
        this.relativePath = relativePath;
        this.fragment = fragment;
    }

    public String getRel() {
        return rel;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getFragmentName() {
        return fragment.getName();
    }
}
