package org.apache.stanbol.entityhub.ldpath.query;

/**
 * Interface that allows to get the LDPath program to select values for
 * QueryResults.
 * @author Rupert Westenthaler
 *
 */
public interface LDPathSelect {
    /**
     * The LDPath program used to select values for query results or
     * <code>null</code> if none
     * @return the LDPath program or <code>null</code> if none.
     */
    public String getLDPathSelect();

}
