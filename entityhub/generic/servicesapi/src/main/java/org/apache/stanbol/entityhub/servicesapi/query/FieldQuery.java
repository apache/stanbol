/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public interface FieldQuery extends Query,Iterable<Entry<String, Constraint>>,Cloneable{

    /**
     * The value used as result for {@link Query#getQueryType()} of this query
     * type.
     */
    String TYPE = "fieldQuery";

    /**
     * Adds Fields to be selected by this Query
     * @param fields the fields to be selected by this query
     */
    void addSelectedField(String field);

    /**
     * Adds Fields to be selected by this Query
     * @param fields the fields to be selected by this query
     */
    void addSelectedFields(Collection<String> fields);

    /**
     * Removes Fields to be selected by this Query
     * @param fields the fields to be selected by this query
     */
    void removeSelectedField(String fields);

    /**
     * Removes Fields to be selected by this Query
     * @param fields the fields to be selected by this query
     */
    void removeSelectedFields(Collection<String> fields);

    /**
     * Unmodifiable set with all the fields to be selected by this query
     * @return the fields to be selected by this query
     */
    Set<String> getSelectedFields();

    /**
     * Sets/replaces the constraint for a field of the representation. If
     * <code>null</code> is parsed as constraint this method removes any existing
     * constraint for the field
     * @param field the field
     * @param constraint the Constraint
     */
    void setConstraint(String field, Constraint constraint);

    /**
     * Removes the constraint for the parse field
     * @param field
     */
    void removeConstraint(String field);

    /**
     * Checks if there is a constraint for the given field
     * @param field the field
     * @return the state
     */
    boolean isConstrained(String field);

    /**
     * Getter for the Constraint of a field
     * @param field the field
     * @return the constraint or <code>null</code> if none is defined.
     */
    Constraint getConstraint(String field);
    
    /**
     * Getter for the unmodifiable list of query elements for the given Path. Use
     * the add/remove constraint methods to change query elements for an path
     * @param path the path
     * @return the list of query elements for a path
     */
    Set<Entry<String, Constraint>> getConstraints();
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
    /**
     * Clones the FieldQuery
     * @return the clone
     */
    FieldQuery clone();
}
