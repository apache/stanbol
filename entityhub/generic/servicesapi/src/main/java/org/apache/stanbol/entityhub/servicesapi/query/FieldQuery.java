package org.apache.stanbol.entityhub.servicesapi.query;

import java.util.Collection;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Simple query interface that allows to search for representations based on
 * fields and there values.<p>
 * Currently it is only possible to set a single constraint per field. Therefore
 * it is not possible to combine an range constraint with an language constraint.
 * e.g. searching for all labels form a-f in a list of given languages.
 * TODO: This shortcoming needs to be reevaluated. The intension was to ease the
 * implementation and the usage of this interface.
 * TODO: Implementation need to be able to throw UnsupportedConstraintExceptions
 *       for specific combinations of Constraints e.g. Regex or case insensitive ...
 * TODO: Would be nice if an Implementation could also announce the list of supported
 *       constraints (e.g. via Capability levels ...)
 * @author Rupert Westenthaler
 */
public interface FieldQuery extends Query,Iterable<Entry<String, Constraint>>{

    /**
     * The value used as result for {@link Query#getQueryType()} of this query
     * type.
     */
    String TYPE = "fieldQuery";

    /**
     * Adds Fields to be selected by this Query
     * @param fields the fields to be selected by this query
     */
    public abstract void addSelectedField(String field);

    /**
     * Adds Fields to be selected by this Query
     * @param fields the fields to be selected by this query
     */
    public abstract void addSelectedFields(Collection<String> fields);

    /**
     * Removes Fields to be selected by this Query
     * @param fields the fields to be selected by this query
     */
    public abstract void removeSelectedField(String fields);

    /**
     * Removes Fields to be selected by this Query
     * @param fields the fields to be selected by this query
     */
    public abstract void removeSelectedFields(Collection<String> fields);

    /**
     * Unmodifiable set with all the fields to be selected by this query
     * @return the fields to be selected by this query
     */
    public abstract Set<String> getSelectedFields();

    /**
     * Sets/replaces the constraint for a field of the representation. If
     * <code>null</code> is parsed as constraint this method removes any existing
     * constraint for the field
     * @param field the field
     * @param constraint the Constraint
     */
    public abstract void setConstraint(String field, Constraint constraint);

    /**
     * Removes the constraint for the parse field
     * @param field
     */
    public abstract void removeConstraint(String field);

    /**
     * Checks if there is a constraint for the given field
     * @param field the field
     * @return the state
     */
    public abstract boolean isConstraint(String field);

    /**
     * Getter for the Constraint of a field
     * @param field the field
     * @return the constraint or <code>null</code> if none is defined.
     */
    public abstract Constraint getConstraint(String field);

    /**
     * Getter for the unmodifiable list of query elements for the given Path. Use
     * the add/remove constraint methods to change query elements for an path
     * @param path the path
     * @return the list of query elements for a path
     */
    public abstract Set<Entry<String, Constraint>> getConstraints();
    /**
     * Removes all constraints form the query
     */
    void removeAllConstraints();
    /**
     * Removes all selected fields
     */
    void removeAllSelectedFields();

    /**
     * Copies the state of this instance to the parsed one
     * @param <T> the {@link FieldQuery} implementation
     * @param copyTo the instance to copy the state
     * @return the parsed instance with the exact same state as this one
     */
    <T extends FieldQuery> T copyTo(T copyTo);

}
