/**
 *
 */
package org.apache.stanbol.entityhub.yard.solr.query;

import org.apache.stanbol.entityhub.yard.solr.model.IndexDataType;


/**
 * Constraint Types defined for IndexFields<p>
 * This could be replaced by a more flexible way to register supported
 * constraint types in future versions
 * @author Rupert Westenthaler
 */
public enum IndexConstraintTypeEnum{
    /**
     * constraints the DataType of values
     * @see IndexDataType
     */
    DATATYPE,
    /**
     * Constraints the language of values
     */
    LANG,
    /**
     * Constraints the field
     */
    FIELD,
    /**
     * Constraints the Value
     */
    EQ,
    /**
     * REGEX based filter on values
     */
    REGEX,
    /**
     * Wildcard based filter on values
     */
    WILDCARD,
    /**
     * Greater than constraint
     */
    GT,
    /**
     * Lower than constraint
     */
    LT,
    /**
     * Greater else constraint
     */
    GE,
    /**
     * Lower else constraint
     */
    LE,
}
