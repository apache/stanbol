package org.apache.stanbol.cmsadapter.servicesapi.processor;

/**
 * Base class for which can be extended by any {@link Processor} implementation. This class contains common
 * functions that can be used in {@link Processor} implementations.
 * 
 */
public class BaseProcessor {
    /**
     * Detects whether the path of a CMS object specified in <i>path</i> parameter is included in the
     * <i>query</i> parameter.
     * 
     * @param path
     * @param query
     * @return
     */
    protected boolean matches(String path, String query) {
        if (path != null) {
            if (query.endsWith("%")) {
                return path.startsWith(query.substring(0, query.length() - 1))
                       || path.contentEquals(query.substring(0, query.length() - 2));
            } else {
                return path.equals(query);
            }
        } else {
            return false;
        }
    }
}
