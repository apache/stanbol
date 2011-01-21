package org.apache.stanbol.entityhub.core.query;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the FieldQuery interface.
 * Note that the getter methods are defined as final. So implementations that
 * need to overwrite some functionality need to use the sets provided by this
 * implementation to store selected fields and field constraints.
 * @author Rupert Westenthaler
 *
 */
public class FieldQueryImpl implements Cloneable, FieldQuery{

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(FieldQueryImpl.class);

    protected final Map<String,Constraint> queryConstraint = new HashMap<String, Constraint>();
    private final Map<String,Constraint> unmodQueryElements = Collections.unmodifiableMap(queryConstraint);

    protected final Set<String> selected = new HashSet<String>();
    private final Set<String> unmodSelected = Collections.unmodifiableSet(selected);

    private Integer limit;

    private int offset;

    public FieldQueryImpl(){
        super();
    }

    public void addSelectedField(String field){
        if(field != null){
            selected.add(field);
        }
    }

    public void addSelectedFields(Collection<String> fields){
        if(fields != null){
            selected.addAll(fields);
        }
    }

    public void removeSelectedField(String field){
        if(field != null){
            selected.remove(field);
        }
    }

    public void removeSelectedFields(Collection<String> fields){
        if(fields != null){
            selected.removeAll(fields);
        }
    }

    public final Set<String> getSelectedFields(){
        return unmodSelected;
    }

    public void setConstraint(String field,Constraint constraint){
        if(field != null && !field.isEmpty()){
            if(constraint == null){
                queryConstraint.remove(field);
            } else {
                queryConstraint.put(field, constraint);
            }
        } else {
            throw new IllegalArgumentException("Parameter Field MUST NOT be NULL nor empty!");
        }
    }
    /**
     * Calls {@link #setConstraint(String, Constraint)} with <code>null</code>
     * as {@link Constraint}. So overwrite the setConstraint Method if needed.
     * @see org.apache.stanbol.entityhub.core.query.FieldConstraint#removeConstraint(java.lang.String)
     */
    public final void removeConstraint(String field){
        setConstraint(field,null);
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.core.query.FieldQuery#isConstraint(java.lang.String)
     */
    public final boolean isConstraint(String field){
        return queryConstraint.containsKey(field);
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.core.query.FieldQuery#getConstraint(java.lang.String)
     */
    public final Constraint getConstraint(String field){
        return queryConstraint.get(field);
    }
    /* (non-Javadoc)
     * @see org.apache.stanbol.entityhub.core.query.FieldQuery#getConstraints()
     */
    @Override
    public Set<Entry<String,Constraint>> getConstraints(){
        return unmodQueryElements.entrySet();
    }

    @Override
    public final Iterator<Entry<String, Constraint>> iterator() {
        return unmodQueryElements.entrySet().iterator();
    }

    @Override
    public String toString() {
        return "Query constraints:"+queryConstraint+" selectedFields:"+selected;
    }
    @Override
    public FieldQuery clone() {
        return copyTo(new FieldQueryImpl());
    }
    /**
     * Uses the public API to clone the state of this instance to the instance
     * provided as parameter.
     * @param <C> An implementation of the FieldQuery interface
     * @param copyTo An instance to copy the state of this on.
     * @return The parsed instance
     */
    public <C extends FieldQuery> C copyTo(C copyTo){
        copyTo.removeAllConstraints();
        copyTo.removeAllSelectedFields();
        for(Entry<String,Constraint> entry : queryConstraint.entrySet()){
            //we need not to copy keys or values, because everything is immutable
            copyTo.setConstraint(entry.getKey(), entry.getValue());
        }
        copyTo.addSelectedFields(selected);
        return copyTo;
    }
    @Override
    public void removeAllConstraints() {
        selected.clear();
    }
    @Override
    public void removeAllSelectedFields() {
        queryConstraint.clear();
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
        if(limit != null && limit.intValue()<1){
            limit = null;
        }
        this.limit = limit;
    }
    @Override
    public final void setOffset(int offset) {
        if(offset < 0){
            offset = 0;
        }
        this.offset = offset;
    }
}
