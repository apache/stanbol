package org.apache.stanbol.commons.viewable;

import org.apache.clerezza.rdf.utils.GraphNode;

/**
 * An RdfViewable is a GraphNode associated with a template path. The template 
 * path will be attempted to be resolved based on the accepted target formats
 * to create a representation of the GraphNode. 
 * @deprecated Moved to {@link org.apache.stanbol.commons.web.viewable.RdfViewable}
 */
public class RdfViewable extends org.apache.stanbol.commons.web.viewable.RdfViewable {

    /**
     * 
     * @param templatePath the templatePath
     * @param graphNode the graphNode with the actual content
     */
    public RdfViewable(final String templatePath, final GraphNode graphNode) {
        super(templatePath,graphNode);
    }
    
    /**
     * With this version of the constructor the templatePath is prefixed with
     * the slash-separated package name of the given Class.
     * 
     */
    public RdfViewable(final String templatePath, final GraphNode graphNode, final Class<?> clazz) {
        super(templatePath,graphNode,clazz);
    }
}
