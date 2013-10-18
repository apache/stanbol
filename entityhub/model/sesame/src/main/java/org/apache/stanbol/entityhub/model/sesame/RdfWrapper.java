package org.apache.stanbol.entityhub.model.sesame;

import org.openrdf.model.Value;

/**
 * Interface that allows access to the wrapped Sesame {@link Value}
 * @author Rupert Westenthaler
 *
 */
public interface RdfWrapper {
    /**
     * Getter for the wrapped Sesame {@link Value}
     * @return the value
     */
    Value getValue();
}