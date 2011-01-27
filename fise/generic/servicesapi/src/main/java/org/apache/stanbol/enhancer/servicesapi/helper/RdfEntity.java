package org.apache.stanbol.enhancer.servicesapi.helper;

import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.NonLiteral;
import org.apache.clerezza.rdf.core.UriRef;

/**
 * Super interface for all interfaces using the {@link RdfEntityFactory} to
 * create proxy objects.
 *
 * @author Rupert Westenthaler
 */
public interface RdfEntity {

    /**
     * Getter for the RDF node represented by the proxy.
     *
     * @return the node representing the proxy. Typically an {@link UriRef} but
     * could be also a {@link BNode}
     */
    NonLiteral getId();

}
