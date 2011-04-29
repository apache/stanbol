package org.apache.stanbol.entityhub.indexing.core.source;

import java.io.IOException;
import java.io.InputStream;
/**
 * The processor used by the resource loader to load registered resources
 * @author Rupert Westenthaler
 *
 */
public interface ResourceImporter {
    /**
     * Processes an resource and returns the new state for that resource
     * @param is the stream to read the resource from
     * @param resourceName the name of the resource
     * @return the State of the resource after the processing
     * @throws IOException On any error while reading the resource. Throwing
     * an IOException will set the state or the resource to
     * {@link ResourceState#ERROR}
     */
    ResourceState importResource(InputStream is,String resourceName) throws IOException;
}
