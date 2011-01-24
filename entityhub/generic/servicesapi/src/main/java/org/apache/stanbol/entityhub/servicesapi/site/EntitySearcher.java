package org.apache.stanbol.entityhub.servicesapi.site;

import java.io.IOException;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;

/**
 * Interface used to provide service/technology specific implementation of the
 * search interface provided by {@link ReferencedSite}.
 * @author Rupert Westenthaler
 *
 */
public interface EntitySearcher {

    /**
     * The key used to define the baseUri of the query service used for the
     * implementation of this interface.<br>
     * This constants actually uses the value of {@link ConfiguredSite#QUERY_URI}
     */
    String QUERY_URI = ConfiguredSite.QUERY_URI;
    /**
     * Searches for Entities based on the parsed {@link FieldQuery}
     * @param query the query
     * @return the result of the query
     */
    QueryResultList<String> findEntities(FieldQuery query) throws IOException;
    /**
     * Searches for Entities based on the parsed {@link FieldQuery} and returns
     * for each entity an Representation over the selected fields and values
     * @param query the query
     * @return the found entities as representation containing only the selected
     * fields and there values.
     */
    QueryResultList<Representation> find(FieldQuery query) throws IOException;


}
