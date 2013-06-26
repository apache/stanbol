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
package org.apache.stanbol.entityhub.test.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;

public abstract class QueryTestCase {
    
    /**
     * Fields included for each result regardless of the configuration
     */
    public static final Set<String> DEFAULT_RESULT_FIELDS = 
        Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            NamespaceEnum.entityhubQuery+"score")));

    public static final Map<? extends String,? extends String> DEFAULT_HEADER;
    static {
        Map<String,String> defaultHeader = new HashMap<String,String>();
        defaultHeader.put("Accept", "application/json");
        DEFAULT_HEADER = Collections.unmodifiableMap(defaultHeader);
    }

    private final Collection<String> expectedResultIds;
    private Collection<String> prohibitedResultIds;
    private final Collection<String> allowedFields;
    private final int expectedStatus;
    private Collection<String> requiredFields;

    private Map<String,String> headers;

    
    /**
     * Creates a Query Test Case.
     * @param expectedStatus the expected HTTP status code returned for this
     * test case. If the value is not an 2xx it is not possible to define
     * {@link #getExpectedResultIds()}, {@link #getAllowedFields()} and
     * {@link #getRequiredFields()} for an test case.
     * @param expectedResultIds Expected results for this test case. 
     * <code>null</code> indicates that there MUST BE no results. An empty
     * collection indicates that there MUST BE results but does not test for
     * specific results. Any item in the parsed collection MUST BE contained in
     * the results of the test case, but this does not mean that there might not
     * be other results within the result set of this query.
     * @param prohibitedResultIds IDs that MUST NOT be part or the result. If
     * <code>null</code> or empty this feature is deactivated
     * @throws IllegalArgumentException if the {@link #getExpectedStatus()} is 
     * not an 2xx code and the expectedResultIds is not <code>null</code>.
     */
    protected QueryTestCase(int expectedStatus,Collection<String> expectedResultIds, 
                            Collection<String> prohibitedResultIds){
        this.expectedStatus = expectedStatus;
        if(!expectsSuccess() && expectedResultIds != null){
            //note even an empty collection is not allowed, because this indicated
            //that results are expected but to test for specific ids must be made
            throw new IllegalArgumentException("Expected Results can only be parsed" +
                    "if the expected status of this test case has an 2xx code");
        }
        this.expectedResultIds = expectedResultIds;
        if(expectsSuccess()){
            this.allowedFields = new HashSet<String>();
            this.allowedFields.addAll(DEFAULT_RESULT_FIELDS);
        } else {
            this.allowedFields = null;
        }
        headers = new HashMap<String,String>();
        headers.putAll(DEFAULT_HEADER);
    }
    /**
     * Setter for the required fields
     * @param requiredFields
     * @throws IllegalArgumentException In case the 
     * {@link #getExpectedStatus()} is not an 2xx code
     */
    protected final void setRequiredFields(Collection<String> requiredFields) {
        if(!expectsSuccess()){
            throw new IllegalArgumentException("Required fields can only be set" +
            		"if the expected status of a test case has a 2xx code");
        }
        this.requiredFields = requiredFields;
    }
    /**
     * Adds additional allowed fields to the {@link #DEFAULT_RESULT_FIELDS}.
     * @param allowedFields
     * @throws IllegalArgumentException if the 
     * {@link #getExpectedStatus()} is not an 2xx code
     */
    protected final void addAllowedField(String allowedField) {
        if(expectsSuccess()){
            if(allowedField != null && !allowedField.isEmpty()){
                this.allowedFields.add(allowedField);
            }
        } else {
            throw new IllegalArgumentException("Allowed fields can only be set" +
            "if the expected status of a test case has a 2xx code");
        }
    }    
    public final boolean expectesResults(){
        return expectedResultIds != null;
    }
    public final Collection<String> getExpectedResultIds() {
        return expectedResultIds;
    }
    /**
     * @return the prohibitedResultIds
     */
    public final Collection<String> getProhibitedResultIds() {
        return prohibitedResultIds;
    }
    public final Collection<String> getRequiredFields() {
        return requiredFields;
    }
    public final Collection<String> getAllowedFields() {
        return allowedFields;
    }    
    public final int getExpectedStatus() {
        return expectedStatus;
    }
    /**
     * If the {@link #getExpectedStatus()} is a 2xx
     * @return if it is expected that this query test case returns a 2xx code
     */
    public boolean expectsSuccess(){
        return expectedStatus >= 200 && expectedStatus < 300;
    }

    /**
     * Getter for the content (query) to be sent to the server
     * @return the content (query string) to be sent to the entityhub
     */
    public abstract String getContent();
    /**
     * The relative path from the service tested endpoint ("/entityhub", "/sites" or
     * "/site/{siteId}") to the service ("/query" for field queries and "/find"
     * for label based entity searches).
     * @return the relative service path.
     */
    public abstract String getServicePath();
    /**
     * Getter for the HTTP header field needed to execute this request
     * @return
     */
    public final Map<String,String> getHeaders(){
        return headers;
    }
    /**
     * Setter for an HTTP header. This headers are used for the request to the
     * Server when execution this test case. If <code>null</code> is parsed as
     * value this header is removed. Existing headers are overriden.
     * @param key the key
     * @param value the value or <code>null</code> to remove this header
     */
    protected final void setHeader(String key, String value){
        if(value == null){
            headers.remove(key);
        } else if(key != null){
            headers.put(key, value);
        }
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(":\n");
        sb.append(" query: ").append(getContent());
        sb.append(" expected status: ").append(expectedStatus).append('\n');
        sb.append(" expected results: ");
        if(expectedResultIds == null){
            sb.append("none");
        } else if(expectedResultIds.isEmpty()){
            sb.append("any");
        } else {
            sb.append(expectedResultIds);
        }
        if(allowedFields != null){
            sb.append('\n');
            sb.append(" result fields: [");
            boolean first = true;
            for(String field : allowedFields){
                if(first){
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(field);
                if(requiredFields != null && requiredFields.contains(field)){
                    sb.append(" (required)");
                }
            }
            sb.append(']');
        }
        return sb.toString();
    }
}
