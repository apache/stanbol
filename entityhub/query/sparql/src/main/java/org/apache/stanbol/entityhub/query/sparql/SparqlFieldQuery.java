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
package org.apache.stanbol.entityhub.query.sparql;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.stanbol.entityhub.core.query.FieldQueryImpl;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;

/**
 * Adds the "selected field" to "SPARQL variable name" mapping
 * 
 * @author Rupert Westenthaler
 * 
 */
public class SparqlFieldQuery extends FieldQueryImpl implements FieldQuery, Cloneable {
    /**
     * String used as prefix for variables generated for fields
     */
    protected static final String FIELD_VAR_PREFIX = "v_";
    protected static final String ROOT_VAR_NAME = "id";
    protected int varNum;
    protected final Map<String,String> field2VarMappings;
    protected final Map<String,String> unmodField2VarMappings;
    protected SparqlEndpointTypeEnum endpointType;

    protected SparqlFieldQuery() {
        this(null);
    }

    protected SparqlFieldQuery(SparqlEndpointTypeEnum endpointType) {
        super();
        this.endpointType = endpointType != null ? endpointType : SparqlEndpointTypeEnum.Standard;
        varNum = 0;
        field2VarMappings = new HashMap<String,String>();
        unmodField2VarMappings = Collections.unmodifiableMap(field2VarMappings);
    }

    public final SparqlEndpointTypeEnum getSparqlEndpointType() {
        return endpointType;
    }

    public final void setSparqlEndpointType(SparqlEndpointTypeEnum endpointType) {
        this.endpointType = endpointType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldQueryImpl#addSelectedField (java.lang.String)
     */
    @Override
    public void addSelectedField(String field) {
        super.addSelectedField(field);
        field2VarMappings.put(field, getFieldVar());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldQueryImpl#addSelectedFields (java.util.Collection)
     */
    @Override
    public void addSelectedFields(Collection<String> fields) {
        super.addSelectedFields(fields);
        for (String field : fields) {
            field2VarMappings.put(field, getFieldVar());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldQueryImpl#removeSelectedField (java.lang.String)
     */
    @Override
    public void removeSelectedField(String field) {
        super.removeSelectedField(field);
        field2VarMappings.remove(field);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldQueryImpl#removeSelectedFields (java.util.Collection)
     */
    @Override
    public void removeSelectedFields(Collection<String> fields) {
        super.removeSelectedFields(fields);
        for (String field : fields) {
            field2VarMappings.remove(field);
        }
    }

    /**
     * Getter for the variable name for a selected field
     * 
     * @param field
     *            the selected field
     * @return the variable name or <code>null</code> if the parsed field is not selected.
     */
    public String getVariableName(String field) {
        return field2VarMappings.get(field);
    }

    /**
     * Getter for the unmodifiable field name to variable name mapping.
     * 
     * @return
     */
    public Map<String,String> getFieldVariableMappings() {
        return unmodField2VarMappings;
    }

    private String getFieldVar() {
        varNum++;
        return FIELD_VAR_PREFIX + varNum;
    }

    public String getRootVariableName() {
        return ROOT_VAR_NAME;
    }

    /**
     * Clones the query (including the field to var name mapping)
     */
    @Override
    public SparqlFieldQuery clone() {
        return clone(new SparqlFieldQuery());
    }
    /**
     * can be used by sub-classes to implement {@link #clone()}   
     * @param emptyClone an empty instance of the sub class
     * @return the parsed instance set will the state of this class. NOTE:
     * state of sub-classes need still to be set
     */
    protected final <T extends SparqlFieldQuery> T clone(T emptyClone){
        T clone = super.copyTo(emptyClone);
        // Note: this uses the public API. However the field->ar mapping might
        // still
        // be different if any removeSelectedField(..) method was used on this
        // instance. Because of that manually set the map and the value of the
        // int.
        // clone.field2VarMappings.clear(); //clear is not necessary, because
        // the keys are equals!
        clone.field2VarMappings.putAll(field2VarMappings);
        clone.varNum = varNum;
        return clone;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + field2VarMappings.hashCode() + varNum + endpointType.ordinal();
    }

    /**
     * Removes also the field to var name mappings
     * 
     * @see org.apache.stanbol.entityhub.core.query.FieldQueryImpl#removeAllSelectedFields()
     */
    @Override
    public void removeAllSelectedFields() {
        super.removeAllSelectedFields();
        field2VarMappings.clear();
        varNum = 0;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof SparqlFieldQuery
               && ((SparqlFieldQuery) obj).field2VarMappings.equals(field2VarMappings)
               && ((SparqlFieldQuery) obj).varNum == varNum
               && ((SparqlFieldQuery) obj).endpointType == endpointType;
    }

    /**
     * Getter for the SPARQL SELECT representation of this FieldQuery
     * 
     * @return the SPARQL SELECT query
     */
    public String toSparqlSelect(boolean includeFields) {
        return SparqlQueryUtils.createSparqlSelectQuery(this, includeFields, endpointType);
    }

    /**
     * Getter for the SPARQL CONSTRUCT representation of this FieldQuery
     * 
     * @return the SPARQL CONSTRUCT query
     */
    public String toSparqlConstruct() {
        return SparqlQueryUtils.createSparqlConstructQuery(this, endpointType);
    }

    @Override
    public String toString() {
        return super.toString() + " field->variable mappings: " + field2VarMappings;
    }

    public SparqlFieldQuery createFieldQuery() {
        // TODO Auto-generated method stub
        return null;
    }
}
