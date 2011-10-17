package org.apache.stanbol.explanation.api;

import java.util.Collection;

/**
 * A knowledge object that incorporates a description, or justification, for another knowledge object, be it a
 * content item, fact, event or collection thereof.
 * 
 * @author alexdma
 * 
 */
public interface Explanation {

    /**
     * 
     * @return the object being explained.
     */
    Explainable<?> getObject();

    /**
     * 
     * @return the items that can be assembled for rendering the explanation.
     */
    Collection<?> getGrounding();
    
    /**
     * 
     * @return the nature of this explanation object.
     */
    ExplanationTypes getType();

}
