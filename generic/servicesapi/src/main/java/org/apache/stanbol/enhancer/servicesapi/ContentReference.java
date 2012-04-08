package org.apache.stanbol.enhancer.servicesapi;

import java.io.IOException;


/**
 * A reference to the content. This allows to {@link #dereference()} the
 * content when it is used.
 */
public interface ContentReference {
    /**
     * The String representation of this reference.
     * @return the reference string
     */
    String getReference();
    /**
     * Dereferences this content reference
     * @return the referenced {@link ContentSource}
     * @throws IOException on any error while dereferencing the source
     */
    ContentSource dereference() throws IOException;
}