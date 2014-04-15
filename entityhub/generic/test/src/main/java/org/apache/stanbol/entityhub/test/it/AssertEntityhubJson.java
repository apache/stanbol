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
package org.apache.stanbol.entityhub.test.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.test.query.QueryTestCase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for asserting JSON responses encoded in the Entityhub JSON format.
 * The intended usage is similar to Ass
 * @author Rupert Westenthaler.
 *
 */
public final class AssertEntityhubJson {
    
    private final static Logger log = LoggerFactory.getLogger(AssertEntityhubJson.class);
    
    private AssertEntityhubJson(){/*noinstances*/}

    /**
     * Validates Results of a Query (/find or /query requests) based on the
     * data defined by the test case
     * @param re the {@link RequestExecutor} used for the test case
     * @param test the query test case
     * @throws JSONException in case the {@link RequestExecutor#getContent()} are
     * no valid JSON. NOTE that the contents are only parsed if the
     * {@link QueryTestCase#getExpectedStatus()} is a 2xx status code.
     * @return in case of success the List of result Entity IDs
     */
    public static List<String> assertQueryResults(RequestExecutor re, QueryTestCase test) throws JSONException{
    	if(log.isDebugEnabled()){
    	    log.debug("Assert Query Results for test {}",test.getContent());
    	}
        re.assertStatus(test.getExpectedStatus());
        re.assertContentType("application/json"); //currently only application/json is supported
        if(!test.expectsSuccess()){
            return null; //no further checks for tests that expect failure
        }
        JSONObject jso = new JSONObject(re.getContent());
        if(log.isDebugEnabled()){
            log.debug("Assert Results: {}",jso.toString(2));
        }
        JSONArray results = jso.getJSONArray("results");
        if(test.expectesResults()){
            assertTrue("Missing Results for Query: \n "+test+
                "\n Result:\n "+results.toString(4),
                results.length() > 0);
        } else {
            assertTrue("Unexpected Results for Query:\n "+test+
                "\n Result:\n "+results.toString(4),
                results.length() == 0);
        }
        Set<String> expectedIds;
        if(test.getExpectedResultIds() != null && !test.getExpectedResultIds().isEmpty()){
            expectedIds = new HashSet<String>(test.getExpectedResultIds());
        } else {
            expectedIds = null;
        }
        
        //iterate over the results
        //General NOTE:
        //  use opt**(..) methods to avoid JSON Exception. We want to parse
        //  everything and than do asserts!
        List<String> resultIds = new ArrayList<String>(results.length());
        for(int i=0;i<results.length();i++){
            JSONObject result = results.getJSONObject(i);
            String id = result.optString("id", null);
            log.info("({}) {}",i,id);
            assertNotNull("ID missing for an Result", id);
            resultIds.add(id);
            if(expectedIds != null){
                expectedIds.remove(id); //not all results must be in the list
            }
            if(test.getProhibitedResultIds() != null){
                assertFalse("Prohibited Result '"+id+"' found!",
                    test.getProhibitedResultIds().contains(id));
            }
            assertRepresentation(result,test.getRequiredFields(),test.getAllowedFields());
        }
        if(expectedIds != null){ // if there where expected results check that all where found
            assertTrue("The following expected results where missing in the Response: \n "+expectedIds,
                expectedIds.isEmpty());
        }
        return resultIds;
    }

    /**
     * Asserts that the Query is present in the response and if so returns the
     * query
     * @param content the returned content
     * @return the query as contained in the response
     * @throws JSONException on any Error while parsing the JSON query from the
     * parsed content
     */
    public static JSONObject assertResponseQuery(String content) throws JSONException {
        assertNotNull("The content of the Response is NULL",content);
        JSONObject jResult = new JSONObject(content);
        JSONObject jQuery = jResult.optJSONObject("query");
        assertNotNull("Result does not contain the processed Query",jQuery);
        return jQuery;
    }

    /**
     * Asserts that the selected JSONArray of the field query returned within
     * the result list contains parsed selected fields
     * @param jQuery the query e.g. as returned by 
     * {@link #assertQueryResults(RequestExecutor, QueryTestCase)}
     * @return the selected fields for further processing
     * @throws JSONException on any error while parsing the JSON
     */
    public static JSONArray assertSelectedField(JSONObject jQuery,String...selected) throws JSONException {
        Set<String> selectedSet = new HashSet<String>();
        if(selected == null || selected.length == 0) {
            selectedSet = Collections.emptySet();
        } else {
            selectedSet = new HashSet<String>(Arrays.asList(selected));
        }
        JSONArray jSelected = jQuery.optJSONArray("selected");
        assertNotNull("Result Query is missing the 'selected' property",jSelected);
        assertTrue("Result Query is expected to have at least a single selected field",
            jSelected.length() > 0);
        boolean found = false;
        for(int i=0;i<jSelected.length() && !found;i++){
            String selectedField = jSelected.optString(i,null);
            assertNotNull("Selected array contains a NULL element \n"+jSelected.toString(4),
                selectedField);
            selectedSet.remove(selectedField);
        }
        assertTrue("Fields "+selectedSet+" are not selected by\n"+jSelected.toString(4),
            selectedSet.isEmpty());
        return jSelected;
    }

    /**
     * Asserts that the parsed Stirng is an valid Entity. This Method only tests
     * for the Entity properties use 
     * {@link #assertRepresentation(JSONObject, Collection, Collection)} to check for
     * required/optional fields of the representation and metadata
     * @param content the content e.g. as returned by an request to the 
     * Entityhub
     * @param to test the id or <code>null</code> to skip this test
     * @param to test the site or <code>null</code> to skip this test
     * @return the parsed Entity typically used for further tests
     */
    public static JSONObject assertEntity(String content,String id, String site) throws JSONException {
        assertNotNull("The content to parse the Entity from is NULL",content);
        JSONObject jEntity = new JSONObject(content);
        if(id != null){
            assertEquals("Entity has the wrong ID", id,jEntity.optString("id", null));
        } else {
            id = jEntity.optString("id", null);
            assertNotNull("ID of the Entity MUST NOT be NULL",id);
        }
        if(site != null){
            assertEquals("Entity has the wrong Site", site, jEntity.optString("site",null));
        }
        
        assertTrue("Representation is missing",jEntity.has("representation"));
        JSONObject jRepresentation = jEntity.getJSONObject("representation");
        assertNotNull("Representation is not an JSON Object",jRepresentation);
        assertEquals("Representation MUST have the same ID as the the Entity",
            id,jEntity.optString("id", null));
        
        assertTrue("Metadata are missing",jEntity.has("metadata"));
        JSONObject jMetadata = jEntity.getJSONObject("metadata");
        assertNotNull("Metadata is not an JSON Object",jMetadata);
        Collection<String> requiredMetadata;
        if("entityhub".equals(site)){
            requiredMetadata = Arrays.asList(
                //NamespaceEnum.entityhub+"isChached", not used by the entityhub
                NamespaceEnum.entityhub+"about",
                NamespaceEnum.rdf+"type");
        } else {
            requiredMetadata = Arrays.asList(
                NamespaceEnum.entityhub+"isChached",
                NamespaceEnum.entityhub+"about",
                NamespaceEnum.rdf+"type");
        }
        Map<String,Set<List<String>>> metadata = assertRepresentation(jMetadata, requiredMetadata ,null);
        assertTrue("The Metadata of an Entity MUST BE about the Entity",
            metadata.get(NamespaceEnum.entityhub+"about")
                .contains(Arrays.asList(id,"xsd:anyURI")));
        return jEntity;
    }

    /**
     * Asserts a JSONObject that represents a Representation for allowed and
     * required fields. Any field that is not required or optional will cause
     * an assertion to fail. Any required field that is missing will also fail
     * this test.
     * @param jRepresentation
     * @param required
     * @param optional
     * @return the values of the Representation with the fields as key and the
     * values as value. The values contain the string value at index '0' and the 
     * xsd:datatype or xml:lang or <code>null</code> (if none) at index '1'
     */
    public static Map<String,Set<List<String>>> assertRepresentation(JSONObject jRepresentation, Collection<String> required,Collection<String> optional) throws JSONException {
        Set<String> checkRequiredFields = new HashSet<String>(); //copy over the required fields
        if(required != null && !required.isEmpty()){
            checkRequiredFields.addAll(required);
        }
        checkRequiredFields.add("id"); //the "id" is required by all representations
        Map<String,Set<List<String>>> valueMap = new HashMap<String,Set<List<String>>>();
        for(Iterator<?> keys = jRepresentation.keys(); keys.hasNext();){
            Object key = keys.next();
            assertFalse("Duplicate 'field' "+key,valueMap.containsKey(key));
            if(checkRequiredFields == null || !checkRequiredFields.remove(key)){
                //process key
                if(optional != null){
                    assertTrue("Field "+key+" is not an expected one: \n" +
                    		" required: "+required+"\n"+
                    		" optional: "+optional+"\n"+
                    		" representation: "+jRepresentation.toString(4),
                        optional.contains(key));
                }
            }
            if(!"id".equals(key)){
                Set<List<String>> values = new HashSet<List<String>>();
                JSONArray jValues = jRepresentation.getJSONArray(key.toString());
                assertTrue("Fields MUST contain at least a single value!",jValues.length() > 0);
                for(int i=0;i<jValues.length();i++){
                    JSONObject fieldValue = jValues.optJSONObject(i);
                    assertNotNull("Values for field "+key+" does contain an value " +
                    		"that is not an JSONObject "+jValues.optString(i),
                    		fieldValue);
                    String[] value = new String[2];
                    value[0] = fieldValue.optString("value");
                    value[1] = null;
                    assertNotNull("All Field-values MUST have the 'value' property",value[0]);
                    assertFalse("All Field-values MUST not be empty",value[0].isEmpty());
                    if(fieldValue.has("xsd:datatype")){
                        value[1] = fieldValue.getString("xsd:datatype");
                        assertFalse("The 'xsd:datatype' (if present) MUST NOT be empty",value[1].isEmpty());
                    }
                    if(fieldValue.has("xml:lang")){
                        assertNull("Field-values MUST NOT have both 'xsd:datatype' and 'xml:lang' defined!",
                            value[1]);
                        value[1] = fieldValue.getString("xml:lang");
                        assertFalse("The 'xml:lang' (if present) MUST NOT be empty",value[1].isEmpty());
                    }
                    assertFalse("Duplicate value "+value+" for field "+key,
                        values.contains(value));
                    values.add(Arrays.asList(value));
                    //both xsd:datatype and xml:lang are optional depending on the
                    //type of the value. Therefore it can not be tested
                    //the 'type' property is deprecated
                }
                valueMap.put(key.toString(), values);
            }
        }
        if(checkRequiredFields != null){
            assertTrue("Missing required Fields "+checkRequiredFields+" present: "+valueMap,
                checkRequiredFields.isEmpty());
        }
        return valueMap;
    }

}
