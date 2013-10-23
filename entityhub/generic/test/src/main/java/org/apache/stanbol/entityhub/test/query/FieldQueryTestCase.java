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
/**
 * 
 */
package org.apache.stanbol.entityhub.test.query;

import java.util.Collection;
import java.util.HashSet;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class FieldQueryTestCase extends QueryTestCase {
    
    private String query;
    
    /**
     * Typically used to test invalid formatted queries to return an
     * status other than 200
     * @param query the query
     * @param expectedStatus the expected status
     */
    public FieldQueryTestCase(String query, int expectedStatus){
        this(query, expectedStatus,null,null);
    }
    /**
     * Creates a FieldQueryTest for the parsed field query and in addition says
     * if the query is expected to provide results or not. This is typically used
     * to test queries that do not have results or that do expect results but
     * one does not want/need to check the ids of the returned entities. <p>
     * NOTE:<ul>
     * <li> This assumes that the server needs to return "200 ok" on the query request.
     * <li> The constructor parses the parsed query to get the selected fields.
     * This is used during test execution to check if the returned results do
     * only contain fields selected by the query.
     * </ul>
     * @param query the query
     * @param expectesResults If results are expected or not
     */
    public FieldQueryTestCase(String query, boolean expectesResults){
        this(query,expectesResults?new HashSet<String>():null);
    }
    /**
     * Creates a FieldQueryTest for the parsed field query that tests the results 
     * of the query for the expected results
     * NOTE:<ul>
     * <li> This assumes that the server needs to return "200 ok" on the query request.
     * <li> The constructor parses the parsed query to get the selected fields.
     * This is used during test execution to check if the returned results do
     * only contain fields selected by the query.
     * </ul>
     * @param query the query
     * @param expectedResultIds Entities that MUST BE in the returned results.
     * However there might be additional results that are not in this collection
     */
    public FieldQueryTestCase(String query, Collection<String> expectedResultIds){
        this(query,expectedResultIds,null);
    }
    /**
     * Creates a FieldQueryTest for the parsed field query that tests the results 
     * of the query for the expected results and each result to provide 
     * information for the required fields.<p>
     * NOTE:<ul>
     * <li> This assumes that the server needs to return "200 ok" on the query request.
     * <li> The constructor parses the parsed query to get the selected fields.
     * This is used during test execution to check if the returned results do
     * only contain fields selected by the query.
     * </ul>
     * @param query the query
     * @param expectedResultIds Entities that MUST BE in the returned results.
     * However there might be additional results that are not in this collection
     * @param requiredFields Fields the MUST BE present for each result of the
     * query.
     */
    public FieldQueryTestCase(String query, Collection<String> expectedResultIds,Collection<String> requiredFields){
        this(query,200,expectedResultIds,requiredFields);
    }
    private FieldQueryTestCase(String query, int expectedStatus, 
                               Collection<String> expectedResultIds,
                               Collection<String> requiredFields){
        super(expectedStatus,expectedResultIds,null); //TODO: add support for prohibitedResultIds
        if(query == null || query.isEmpty()){
            throw new IllegalStateException("The parsed Query MUST NOT be NULL");
        }
        this.query = query;
        if(expectsSuccess()){
            //parse the selected fields from the query and add them to the allowed fields
            try {
                JSONObject jQuery = new JSONObject(query);
                //reset the parsed query for better debugging
                this.query = jQuery.toString(4);
                JSONArray selected = jQuery.optJSONArray("selected");
                if(selected != null){
                    for(int i=0;i<selected.length();i++){
                        addAllowedField(selected.getString(i));
                    }
                }
            } catch (JSONException e) {
                throw new IllegalArgumentException("Tests that expect Results MUST parse a valid FieldQuery!",e);
            }
            //now set the required fields
            setRequiredFields(requiredFields);
        } // else do not parse the query, because it might be invalid JSON
        
        setHeader("Content-Type", "application/json");
    }
    @Override
    public final String getContent() {
        return query;
    }
    @Override
    public String getServicePath() {
        return "/query";
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(FieldQueryTestCase.class.getSimpleName());
        sb.append(":\n");
        sb.append(" field query:\n").append(query).append('\n');
        sb.append(super.toString());
        return sb.toString();
    }
}