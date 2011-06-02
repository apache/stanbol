package org.apache.stanbol.entityhub.yard.solr.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.stanbol.commons.stanboltools.datafileprovider.DataFileProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClassPathSolrIndexConfigProvider implements DataFileProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    /**
     * Solr Core configuration are loaded form "solr/core/{core-name}
     */
    public static final String INDEX_BASE_PATH = "solr/core/";
    
    private final String symbolicName;
    /**
     * Creates a DataFileProvider that loads SolrIndexConfigurations via the
     * classpath relative to {@value #INDEX_BASE_PATH}.
     * @param bundleSymbolicName the symbolic name of the bundle to accept
     * requests from or <code>null</code> to accept any request.
     */
    ClassPathSolrIndexConfigProvider(String bundleSymbolicName) {
        symbolicName = bundleSymbolicName;
    }
    
    @Override
    public InputStream getInputStream(String bundleSymbolicName,
            String filename, Map<String, String> comments) 
    throws IOException {
        //if the symbolicName is null accept any request
        //if not, than check if the request is from the correct bundle.
        if(symbolicName != null && !symbolicName.equals(bundleSymbolicName)) {
            log.debug("Requested bundleSymbolicName {} does not match mine ({}), request ignored",
                    bundleSymbolicName, symbolicName);
            return null;
        }
        
        // load default OpenNLP models from classpath (embedded in the defaultdata bundle)
        final String resourcePath = INDEX_BASE_PATH + filename;
        final InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        log.debug("Resource {} found: {}", (in == null ? "NOT" : ""), resourcePath);
        
        // Returning null is fine - if we don't have the data file, another
        // provider might supply it
        return in;
    }

}
