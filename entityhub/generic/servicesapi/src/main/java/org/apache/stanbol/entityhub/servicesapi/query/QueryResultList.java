package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.Iterator;
import java.util.Set;


public interface QueryResultList<T> extends Iterable<T> {

    /**
     * Getter for the query of this result set.
     * TODO: Change return Value to {@link Query}
     * @return the query used to create this result set
     */
    FieldQuery getQuery();
    /**
     * The selected fields of this query
     * @return
     */
    Set<String> getSelectedFields();

    /**
     * Iterator over the results of this query
     */
    Iterator<T> iterator();
    /**
     * <code>true</code> if the result set is empty
     * @return <code>true</code> if the result set is empty. Otherwise <code>false</code>
     */
    boolean isEmpty();

    /**
     * The size of this result set
     * @return
     */
    int size();
    /**
     * The type of the results in the list
     * @return the type
     */
    Class<T> getType();
}
