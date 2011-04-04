package org.apache.stanbol.commons.web.base;

/**
 * Simple data transfer object for stylesheet (CSS) and fiveicon list to be contributed by WebFragment for
 * inclusion to the HTML head of the pages by the NavigationMixin abstract JAX-RS resource.
 */
public class LinkResource implements Comparable<LinkResource> {

    private final String rel;

    private final String relativePath;

    private final WebFragment fragment;

    protected final int order;

    public LinkResource(String rel, String relativePath, WebFragment fragment, int order) {
        this.rel = rel;
        this.relativePath = relativePath;
        this.fragment = fragment;
        this.order = order;
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

    @Override
    public int compareTo(LinkResource o) {
        return order - o.order;
    }
}
