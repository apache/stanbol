package org.apache.stanbol.entityhub.servicesapi.query;


/**
 * Factory interface for creating instances of FieldQueries
 * @author Rupert Westenthaler
 *
 */
public interface FieldQueryFactory {
    /**
     * Creates a new field query instance without any constraints or selected
     * fields
     * @return a new and empty field query instance
     */
    FieldQuery createFieldQuery();

}
