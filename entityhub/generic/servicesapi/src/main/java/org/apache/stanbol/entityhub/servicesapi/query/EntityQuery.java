package org.apache.stanbol.entityhub.servicesapi.query;

import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.model.rdf.RdfResourceEnum;

/**
 * A simple Search service that is intended for Entity lookup based on
 * <ul>
 * <li> Tags/Keywords by parsing the exact name
 * <li> Prefix to provide suggestions for users that type some characters into
 * a search field.
 * </ul><p>
 * Results of such queries will be {@link Representation} with the
 * following fields
 * <ul>
 * <li> id (required)
 * <li> name (required) by using field {@link RdfResourceEnum#name()}
 * <li> rank (required) by using field
 * <li> description (optional)
 * <li> image (optional)
 * <li> type (optional)
 * </ul><p>
 * Specific implementations may add additional fields. All fields MUST BE part
 * of the list returned by {@link QueryResultList#getSelectedFields()}.
 * TODO: define the URIs of the results
 * @author Rupert Westenthaler
 *
 */
public interface EntityQuery extends Query{
    /**
     * Getter for the name/name fragment of the entity to search
     * @return the name
     */
    String getName();
    /**
     * If Prefix is enabled, the parsed name is interpreted as prefix.
     * The regex representation would be <code>"^"+getName()+".*$"</code>
     * @return
     */
    boolean isPrefix();
    /**
     * The type of the entity
     * TODO: currently symbols define only a required name and description, but
     * often it is also the type that is used to filter individuals. Therefore
     * it could make sense to also add the type to the properties of symbols
     * @return the type of the entities (full name, no wildcard support)
     */
    String getEntityType();
}
