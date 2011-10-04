package org.apache.stanbol.contenthub.servicesapi.search;

import java.util.List;
import java.util.Map;

import org.apache.stanbol.contenthub.servicesapi.search.execution.DocumentResource;

/**
 * This interface defines the structure of a search result returned by
 * {@link Search#search(String[], String, List, Map)}. All results of a search operation are encapsulated.
 * 
 * @author anil.sinaci
 * 
 */
public interface SearchResult {

    /**
     * Returns a list of IDs of the documents found by the search operation in sorted order. The results are
     * sorted according to their scores assigned within the search operation.
     * 
     * @return A list of IDs
     */
    List<String> getDocumentIDs();

    /**
     * Returns a list of {@link DocumentResource}s found by the search operation in sorted order. The results
     * are sorted according to their scores assigned within the search operation.
     * 
     * @return A list of {@link DocumentResource}
     */
    List<DocumentResource> getDocuments();

    /**
     * Returns the facets generated as a result of the search operations. Each search result has its own
     * facets.
     * 
     * @return A map of <code>property:[value1,value2]</code> pairs.
     */
    Map<String,List<Object>> getFacets();

}
