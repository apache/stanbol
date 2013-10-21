package org.apache.stanbol.commons.viewable;
/**
 * This is a replacement for the jersey Vieable that allows rendering an 
 * arbitrary object using a Freemarker template specified by path.
 * 
 * Usage of this class promotes a bad programming style where the 
 * application logic is not clearly separated from the presentation but 
 * where backend method are called by the presentation layer.
 * 
 * Users should consider migrate to RdfViewable instead where instead of
 * an arbitrary Object a GraphNode representing a node in a graph is passed,
 * this approach also allows the response to be rendered as RDF.
 * 
 * @deprecated Moved to {@link org.apache.stanbol.commons.web.viewable.Viewable}
 */
@Deprecated
public class Viewable extends org.apache.stanbol.commons.web.viewable.Viewable {
    
    /**
     * This uses the class name of Pojo to prefix the template
     * 
     * @param templatePath the templatePath
     * @param graphNode the graphNode with the actual content
     */
    public Viewable(String templatePath, Object pojo) {
        super(templatePath, pojo);
    }

    /**
     * With this version of the constructor the templatePath is prefixed with
     * the slash-separated class name of clazz.
     * 
     */
    public Viewable(final String templatePath, final Object pojo, final Class<?> clazz) {
        super(templatePath, pojo, clazz);
    }
}
