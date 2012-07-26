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
package org.apache.stanbol.entityhub.core.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the FieldQuery interface. Note that the getter methods are defined as final. So
 * implementations that need to overwrite some functionality need to use the sets provided by this
 * implementation to store selected fields and field constraints.
 * 
 * @author Rupert Westenthaler
 * 
 */
public class FieldQueryImpl implements Cloneable, FieldQuery {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(FieldQueryImpl.class);

    private final Map<String,Constraint> queryConstraint = new HashMap<String,Constraint>();
    private final Map<String,Constraint> unmodQueryElements = Collections.unmodifiableMap(queryConstraint);

    private final Set<String> selected = new HashSet<String>();
    private final Set<String> unmodSelected = Collections.unmodifiableSet(selected);

    private Integer limit;

    private int offset;

    public FieldQueryImpl() {
        super();
    }

    public void addSelectedField(String field) {
        if (field != null) {
            selected.add(field);
        }
    }

    public void addSelectedFields(Collection<String> fields) {
        if (fields != null) {
            selected.addAll(fields);
        }
    }

    public void removeSelectedField(String field) {
        if (field != null) {
            selected.remove(field);
        }
    }

    public void removeSelectedFields(Collection<String> fields) {
        if (fields != null) {
            selected.removeAll(fields);
        }
    }

    public final Set<String> getSelectedFields() {
        return unmodSelected;
    }

    public void setConstraint(String field, Constraint constraint) {
        if (null == field || field.isEmpty()) throw new IllegalArgumentException(
                "Parameter Field MUST NOT be NULL nor empty!");

        if (constraint == null) {
            queryConstraint.remove(field);
        } else {
            queryConstraint.put(field, constraint);
        }
    }

    /**
     * Calls {@link #setConstraint(String, Constraint)} with <code>null</code> as {@link Constraint}. So
     * overwrite the setConstraint Method if needed.
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldConstraint#removeConstraint(java.lang.String)
     */
    public final void removeConstraint(String field) {
        setConstraint(field, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldQuery#isConstraint(java. lang.String)
     */
    public final boolean isConstrained(String field) {
        return queryConstraint.containsKey(field);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldQuery#getConstraint(java .lang.String)
     */
    public final Constraint getConstraint(String field) {
        return queryConstraint.get(field);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldQuery#getConstraints()
     */
    @Override
    public Set<Entry<String,Constraint>> getConstraints() {
        return unmodQueryElements.entrySet();
    }

    @Override
    public final Iterator<Entry<String,Constraint>> iterator() {
        return unmodQueryElements.entrySet().iterator();
    }

    @Override
    public String toString() {
        StringBuilder query = new StringBuilder();
        query.append(String.format("Query Constraints (%d)\n", this.queryConstraint.size()));
        for (Entry<String,Constraint> entry : this.queryConstraint.entrySet()) {
            query.append(String.format("[key:: %s][%s]\n", entry.getKey(), entry.getValue().toString()));
        }
        query.append(String.format("unmod Query Elements (%d)\n", this.unmodQueryElements.size()));
        for (Entry<String,Constraint> entry : this.unmodQueryElements.entrySet()) {
            query.append(String.format("[key:: %s][%s]\n", entry.getKey(), entry.getValue().toString()));
        }
        query.append(String.format("Selected (%d)\n", this.selected.size()));
        for (String entry : this.selected) {
            query.append(String.format("[%s]", entry));
        }
        query.append(String.format("unmod Selected (%d)\n", this.unmodSelected.size()));
        for (String entry : this.unmodSelected) {
            query.append(String.format("[%s]", entry));
        }
        query.append(String.format("[limit :: %d]\n", this.limit));
        query.append(String.format("[offset :: %d]\n", this.offset));

        return query.toString();
    }

    @Override
    public FieldQuery clone() {
        return copyTo(new FieldQueryImpl());
    }

    /**
     * Uses the public API to clone the state of this instance to the instance provided as parameter.
     * 
     * @param <C>
     *            An implementation of the FieldQuery interface
     * @param copyTo
     *            An instance to copy the state of this on.
     * @return The parsed instance
     */
    public <C extends FieldQuery> C copyTo(C copyTo) {
        copyTo.removeAllConstraints();
        copyTo.removeAllSelectedFields();
        for (Entry<String,Constraint> entry : queryConstraint.entrySet()) {
            // we need not to copy keys or values, because everything is
            // immutable
            copyTo.setConstraint(entry.getKey(), entry.getValue());
        }
        copyTo.addSelectedFields(selected);
        copyTo.setLimit(limit);
        copyTo.setOffset(offset);
        return copyTo;
    }

    @Override
    public void removeAllConstraints() {
        queryConstraint.clear();
    }

    @Override
    public void removeAllSelectedFields() {
        selected.clear();
    }

    @Override
    public final String getQueryType() {
        return FieldQuery.TYPE;
    }

    @Override
    public final Integer getLimit() {
        return limit;
    }

    @Override
    public final int getOffset() {
        return offset;
    }

    @Override
    public final void setLimit(Integer limit) {
        if (limit != null && limit.intValue() < 1) {
            limit = null;
        }
        this.limit = limit;
    }

    @Override
    public final void setOffset(int offset) {
        if (offset < 0) {
            offset = 0;
        }
        this.offset = offset;
    }

    @Override
    public int hashCode() {
        return queryConstraint.hashCode() + selected.hashCode() + offset + (limit != null ? limit : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FieldQuery && ((FieldQuery) obj).getConstraints().equals(getConstraints())
            && ((FieldQuery) obj).getSelectedFields().equals(getSelectedFields())
            && ((FieldQuery) obj).getOffset() == getOffset()) {
            if (limit != null) {
                return limit.equals(((FieldQuery) obj).getLimit());
            } else {
                return ((FieldQuery) obj).getLimit() == null;
            }
        } else {
            return false;
        }
    }

}
