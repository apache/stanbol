/**
 * 
 */
package org.apache.stanbol.contenthub.servicesapi;

import org.apache.stanbol.contenthub.servicesapi.enhancements.vocabulary.EnhancementGraphVocabulary;

/**
 * @author anil.sinaci
 *
 */
public class Constants {
    
    public static final String DEFAULT_ENCODING = "UTF-8";
    
    public static final String ENHANCER_ENTITIY_CACHE_GRAPH_URI = "enhancerEntityCache";
    public static final String[] RESERVED_GRAPH_URIs = {ENHANCER_ENTITIY_CACHE_GRAPH_URI,
                                                        EnhancementGraphVocabulary.ENHANCEMENTS_GRAPH_URI};

    public static boolean isGraphReserved(String graphURI) {
        for (String uri : RESERVED_GRAPH_URIs) {
            if (uri.equals(graphURI)) return true;
        }
        return false;
    }
}
