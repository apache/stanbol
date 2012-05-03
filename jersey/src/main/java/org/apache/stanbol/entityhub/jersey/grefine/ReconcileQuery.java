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
package org.apache.stanbol.entityhub.jersey.grefine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.servicesapi.model.ValueFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java Representation for <a href="http://code.google.com/p/google-refine/wiki/ReconciliationServiceApi#Query_Request">
 * Google Refine Reconciliation queries</a>.<p>
 * {@link #getTypes()} and {@link Value#getId()} do support 'prefix:localname'
 * syntax for prefixes defined in the {@link NamespaceEnum}.
 * Also defines methods for parsing single and multiple request strings.
 * 
 * @author Rupert Westenthaler
 *
 */
public class ReconcileQuery {

    private static final Logger log = LoggerFactory.getLogger(ReconcileQuery.class);
    
    public static final Integer DEFAULT_LIMIT = 5;
    
    public static final TYPE_STRICT DEFAULT_TYPE_STRICT = TYPE_STRICT.any;
    
    private final String query;
    
    private final Set<String> types;
    
    private Integer limit;
    
    private final Map<String,Collection<Value>> properties = new HashMap<String,Collection<Value>>();
    
    private TYPE_STRICT typeStrict;
    
    protected static final ValueFactory vf = InMemoryValueFactory.getInstance();
    
    /**
     * @return the limit
     */
    public final Integer getLimit() {
        return limit;
    }

    /**
     * @param limit the limit to set
     */
    public final void setLimit(Integer limit) {
        this.limit = limit;
    }

    /**
     * @return the typeStrict
     */
    public final TYPE_STRICT getTypeStrict() {
        return typeStrict;
    }

    /**
     * @param typeStrict the typeStrict to set
     */
    public final void setTypeStrict(TYPE_STRICT typeStrict) {
        this.typeStrict = typeStrict;
    }

    /**
     * @return the query
     */
    public final String getQuery() {
        return query;
    }

    /**
     * @return the types
     */
    public final Set<String> getTypes() {
        return types;
    }


    public static enum TYPE_STRICT {any,all,should};


    public ReconcileQuery(String query,Collection<String> types) {
        if(query == null || query.isEmpty()){
            throw new IllegalArgumentException("The parsed query string MUST NOT be NULL nor empty!");
        }
        this.query = query;
        if(types == null || types.isEmpty()){
            this.types = Collections.emptySet();
        } else {
            Set<String> t = new HashSet<String>(types.size());
            for(String type : types){
                if(type != null && !type.isEmpty()){
                    t.add(type);
                }
            }
            this.types = Collections.unmodifiableSet(t);
        }
    }
    
    public Collection<Value> putProperty(String field, Collection<Value> values){
        if(field == null || field.isEmpty()){
            throw new IllegalArgumentException("The field for an property MUST NOT be NULL!");
        }
        if(values == null || values.isEmpty()){
            return properties.remove(values);
        } else {
            return properties.put(field, values);
        }
    }
    public Collection<Value> removeProperty(String field){
        return properties.remove(field);
    }
    public Collection<Value> getProperty(String field){
        return properties.get(field);
    }
    public Iterable<Entry<String,Collection<Value>>> getProperties(){
        return properties.entrySet();
    }
    /**
     * Values can be simple JSON values or JSON objects with an 'id' and a
     * 'name'. This is mapped to {@link Value} objects with an optional 
     * {@link #getId()} and a required {@link #getValue()}.<p>
     * The 'id' supports prefix:localname syntax for prefixes defined within the
     * {@link NamespaceEnum}
     * @author Rupert Westenthaler
     *
     */
    public static class Value {
        private final String id;
        private final Object value;

        private Value(Object value){
            this(null,value);
        }
        private Value(String id, Object value){
            this.id = id == null ? null : NamespaceEnum.getFullName(id);
            if(value == null){
                throw new IllegalArgumentException("The parsed value MUST NOT be NULL!");
            }
            this.value = value;
        }

        /**
         * The getter for the value of the 'id' property of the 'v' object
         * if present. This represents the value of fields that are already
         * successfully linked (reconciled) with some entity.
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * @return the value
         */
        public Object getValue() {
            return value;
        }
        /**
         * Calls the {@link #toString()} method of the {@link #getValue()}
         */
        @Override
        public String toString() {
            return value.toString();
        }
        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : value.hashCode();
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof Value && ( //other is value
                    (id != null && id.equals(((Value) o).id)) || //ids are equals or 
                        (id == null && ((Value)o).id == null //ids are null and
                        && value.equals(((Value)o).value))); //values are equals
            
        }
    }
    
    public static Map<String,ReconcileQuery> parseQueries(String queriesString) throws WebApplicationException {
        JSONObject jQueries;
        try {
            jQueries = new JSONObject(queriesString);
        }catch (JSONException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    "The parsed query is illegal formatted! \n query: \n"+queriesString+"\n").build());
        }
        @SuppressWarnings("unchecked")
        Iterator<String> keys = jQueries.keys();
        Map<String,ReconcileQuery> queries = new HashMap<String,ReconcileQuery>();
        while(keys.hasNext()){
            String key = keys.next();
            try {
                ReconcileQuery query = parseQuery(jQueries.getJSONObject(key));
                queries.put(key, query);
            } catch (JSONException e) {
                throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST).entity(
                        "The query of key '"+key+"is illegal formatted! \n query: \n"
                                + queriesString+"\n").build());
            }
        }
        return queries;
    }
    /**
     * Parses a Google Refine Reconcile Query from the parsed String.
     * @param queryString the string representation of the reconcile query
     * @return the parsed {@link ReconcileQuery} object
     * @throws WebApplicationException {@link Response.Status#BAD_REQUEST} in
     * case of the parsed string is not a well formated query. Unsupported
     * Properties are silently ignored (warnings are still logged).
     */
    public static ReconcileQuery parseQuery(String queryString) throws WebApplicationException {
        JSONObject jQuery;
        try {
            if(queryString.charAt(0) == '{') {
                jQuery = new JSONObject(queryString);
            } else {
                jQuery = new JSONObject();
                jQuery.put("query", queryString);
                //simple string query
            }
        }catch (JSONException e) {
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    "The parsed query is illegal formatted! \n query: \n"+queryString+"\n").build());
        }
        return parseQuery(jQuery);
    }
    
    private static ReconcileQuery parseQuery(JSONObject jQuery) throws WebApplicationException {
        //query (string)
        //limit (integer), optional
        //type (string| [string]), optional
        //type_strict ("any","all","should"), optional
        //properties ([Property]), optional
        //    Property:
        //        p (string)  -> ignore
        //        pid (string) -> uri
        //        v (string/Value, [string/Value]), required
        //    Value
        //        id (uri)
        String value = jQuery.optString("query");
        if(value == null || value.isEmpty()){
            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    "The parsed query is illegal formatted! \n query: \n"+jQuery.toString()+"\n").build());
        }
        JSONArray jTypes = null;
        Collection<String> types;
        if(!jQuery.has("type")){
            types = Collections.emptySet();
        } else if((jTypes = jQuery.optJSONArray("type")) != null){
            types = new HashSet<String>(jTypes.length());
            for(int i=0;i<jTypes.length();i++){
                String type = NamespaceEnum.getFullName(jTypes.optString(i));
                if(type != null && !type.isEmpty()){
                    types.add(type);
                }
            }
        } else {
            String type = jQuery.optString("type");
            if(type != null && !type.isEmpty()){
                types = Collections.singleton(type);
            } else {
                types = Collections.emptySet();
            }
        }
        ReconcileQuery reconcileQuery = new ReconcileQuery(value,types);
        //TYPE_STRICT typeStrict = null;
        String jTypeStrict = jQuery.optString("type_strict");
        if(jTypeStrict != null){
            try {
                reconcileQuery.setTypeStrict(TYPE_STRICT.valueOf(jTypeStrict));
            } catch (RuntimeException e) {
                log.warn("Unknown \"type_strict\" value in Google Refine Reconcile" +
                        " Request (use default '{}')\n {}",DEFAULT_TYPE_STRICT,jQuery.toString());
                reconcileQuery.setTypeStrict(DEFAULT_TYPE_STRICT);
            }
        } else {
            reconcileQuery.setTypeStrict(DEFAULT_TYPE_STRICT);
        }
        reconcileQuery.setLimit(jQuery.optInt("limit", DEFAULT_LIMIT));
        JSONArray jProperties = jQuery.optJSONArray("properties");
        if(jProperties != null){
            for(int i=0;i<jProperties.length();i++){
                parseProperty(reconcileQuery, jProperties.optJSONObject(i));
            }
        }
        return reconcileQuery;
    }


    /**
     * Internally used to parse a Property of a Google Refine Reconcile Query
     * @param reconcileQuery the query to add the property
     * @param jProperty the JSON formatted property
     */
    private static void parseProperty(ReconcileQuery reconcileQuery,JSONObject jProperty) {
        if(jProperty != null){
            //parse property
            String property = NamespaceEnum.getFullName(jProperty.optString("pid"));
            if(property == null){
                log.warn("Ignore Property because of missing 'pid'! \n{}",jProperty.toString());
            } else {
                //property keys may appear multiple times in queries
                //so we need to initialise the property values with already
                //existing values
                Collection<Value> values = reconcileQuery.getProperty(property);
                if(values == null){ //if not create a new Set
                    //maybe the order is important (e.g. for similarity alg) 
                    //   ... so try to keep it
                    values = new LinkedHashSet<Value>();
                }
                //parse the value
                Object jValue = jProperty.opt("v");
                if(jValue == null){
                    log.warn("Ignore Property '{}' because it has no value! \n {}",property,jProperty.toString());
                } else if(jValue instanceof JSONObject){
                    //Reconciliation data available!
                    Value value = parseValueFromV(jValue);
                    if(value != null){
                        values.add(value);
                    } else {
                        log.warn("ignore value for property {} because no name is present (value: {})!",
                            property,jValue.toString());
                    }
                } else if(jValue instanceof JSONArray){
                    //parse value list
                    JSONArray jValueArray = (JSONArray)jValue;
                    for(int j=0;j<jValueArray.length();j++){
                        jValue = jValueArray.opt(j);
                        if(jValue instanceof JSONObject){
                            //Reconciliation data available!
                            Value value = parseValueFromV(jValue);
                            if(value != null){
                                values.add(value);
                            } else {
                                log.warn("ignore value for property {} because no name is present (value: {})!",
                                    property,jValue.toString());
                            }
                        } else if(jValue != null){
                            values.add(new Value(jValue));
                        }
                    }
                    if(values.isEmpty()){
                        log.warn("Ignore Property '{}' because it does not define a valid value! \n {}",
                            property,jProperty.toString());
                    }
                } else { //number or String
                    values.add(new Value(jValue)); //directly use the value
                }
                
                if(!values.isEmpty()){
                    reconcileQuery.putProperty(property, values);
                }
            }
        }
    }

    /**
     * Parses a Value from a JSON Object by reading the 'id' and 'name' keys
     * @param jValue
     * @return The value or <code>null</code> if the parsed json object does not
     * contain the required information.
     */
    private static Value parseValueFromV(Object jValue) {
        String id = ((JSONObject)jValue).optString("id");
        String value = ((JSONObject)jValue).optString("name");
        return value != null ? new Value(id,value) : null;
    }
    
}
