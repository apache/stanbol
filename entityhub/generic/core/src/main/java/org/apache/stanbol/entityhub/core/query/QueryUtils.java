package org.apache.stanbol.entityhub.core.query;

import org.apache.stanbol.entityhub.servicesapi.query.Query;


/**
 * Utility class for queries.
 * @author Rupert Westenthaler
 *
 */
public class QueryUtils {

    /**
     * Getter for the Limit calculated bye on the limit defined by the query
     * and the configuration of the default results (for queries that do not
     * define a limit) and the maximum number of Results.<p>
     * Configurations for defaultResults and maxResults <= 0 are ignored. Return
     * values < = 0 should be interpreted as no constraints.
     * @param query the query
     * @param defaultResults the default number of results
     * @param maxResults the maximum number of queries
     * @return if > 0, than the value represents the number of results for the
     * query. Otherwise no constraint.
     */
    public static int getLimit(Query query,int defaultResults, int maxResults){
        int limit = query.getLimit() != null?query.getLimit():-1;
        if(defaultResults > 0){
            limit = Math.min(limit, defaultResults);
        }
        if(maxResults > 0){
            limit = Math.min(limit,maxResults);
        }
        return limit;
    }

}
