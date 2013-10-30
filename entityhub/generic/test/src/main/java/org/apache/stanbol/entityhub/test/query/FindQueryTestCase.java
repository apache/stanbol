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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class FindQueryTestCase extends QueryTestCase {

    /**
     * If {@link #getField()} set this field is expected to be used as default
     * search field (rdfs:label)
     */
    public static final String DEFAULT_SEARCH_FIELD = "http://www.w3.org/2000/01/rdf-schema#label";
    
    private final String text;
    private String language;
    private String field;
    private Integer offset;
    private Integer limit;
    
    /**
     * Constructs a /find test case with a text and a expected HTTP status code
     * for the Request. This constructor is typically used to construct illegal
     * requests that return status codes other that 2xx
     * @param text the text
     * @param expectedStatus the expected status
     */
    public FindQueryTestCase(String text,int expectedStatus) {
        this(text, expectedStatus, null,null);
    }
    /**
     * Constructs a valid /find request (expects an 2xx status) that may or may
     * not have results
     * @param text the text
     * @param expectsResults if results are expected or not
     */
    public FindQueryTestCase(String text, boolean expectsResults) {
        this(text, 200, expectsResults ? new ArrayList<String>(0) : null,null);
    }
    /**
     * Constructs a /find request where the response MUST contain the parsed
     * expected results. Additional other results are ok. 
     * @param text the text
     * @param expectedResultIds the ids of the required results. An empty
     * collection to accept any results or <code>null</code> to explicitly
     * check that this query MUST NOT have results. 
     */
    public FindQueryTestCase(String text, Collection<String> expectedResultIds) {
        this(text, 200, expectedResultIds,null);
    }
    /**
     * Constructs a /find request where the response MUST contain the parsed
     * expected results. Additional other results are ok. 
     * @param text the text
     * @param expectedResultIds the ids of the required results. An empty
     * collection to accept any results or <code>null</code> to explicitly
     * check that this query MUST NOT have results. 
     * @param prohibitedResultIds results that MUST NOT be returned by the query
     */
    public FindQueryTestCase(String text, Collection<String> expectedResultIds,
                             Collection<String> prohibitedResultIds) {
        this(text, 200, expectedResultIds,prohibitedResultIds);
    }
    /**
     * Internally used to construct FindTestCases. Not all combinations make
     * sense therefore this one is a private one
     * @param text the text
     * @param status the status
     * @param expectedResultIds expected results
     * @param prohibitedResultIds results that MUST NOT be returned by the query
     */
    private FindQueryTestCase(String text, int status, Collection<String> expectedResultIds, 
                              Collection<String> prohibitedResultIds){
        super(status,expectedResultIds,prohibitedResultIds);
        this.text = text;
        //For queries that should succeed
        if(expectsSuccess()){
            //add the default search field (needed to correctly init that the
            //default search field is required to be included in results
            setField(null);
        }
        //find queries use application/x-www-form-urlencoded
        this.setHeader("Content-Type", "application/x-www-form-urlencoded");
    }
    /**
     * @return the language
     */
    public final String getLanguage() {
        return language;
    }
    /**
     * @param language the language to set
     */
    public final void setLanguage(String language) {
        this.language = language;
    }
    /**
     * Getter for the field to search the {@link #getText() text}
     * @return the field
     */
    public final String getField() {
        return field;
    }
    /**
     * Setter for the field to search the {@link #getText() text}
     * @param field the field to set
     */
    public final void setField(String field) {
        this.field = field;
        //set also the new field as required for results!
        setRequiredFields(Arrays.asList(
            field == null ? DEFAULT_SEARCH_FIELD : field));
    }
    /**
     * Getter for the offset of this query
     * @return the offset
     */
    public final Integer getOffset() {
        return offset;
    }
    /**
     * Setter for the offset
     * @param offset the offset to set
     */
    public final void setOffset(Integer offset) {
        this.offset = offset;
    }
    /**
     * Getter for the maximum number of results (starting from the offset)
     * @return the limit
     */
    public final Integer getLimit() {
        return limit;
    }
    /**
     * Setter for the maximum number of results (starting from the offset)
     * @param limit the limit to set
     */
    public final void setLimit(Integer limit) {
        this.limit = limit;
    }
    /**
     * Getter for the text to search
     * @return the text
     */
    public final String getText() {
        return text;
    }
    @Override
    public String getServicePath() {
        return "/find";
    }
    /**
     * Getter for the encoded Content for the query
     * @return
     */
    public final String getContent(){
        StringBuilder sb = new StringBuilder();
        addParam(sb,"name",text);
        addParam(sb, "field", field);
        addParam(sb, "lang", language);
        addParam(sb, "offset", offset);
        addParam(sb, "limit", limit);
        return sb.toString();
    }
    /**
     * Adds a param to the {@link #getContent() content} of the query 
     * @param sb
     */
    private void addParam(StringBuilder sb,String param, Object value) {
        if(value != null){
            if(sb.length() != 0){
                sb.append('&');
            }
            //TODO: do we need to URLencode the value?
            sb.append(param).append('=').append(value);
        }
    }

}
