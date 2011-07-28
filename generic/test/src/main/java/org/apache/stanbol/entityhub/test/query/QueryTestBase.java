package org.apache.stanbol.entityhub.test.query;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.entityhub.test.it.EntityhubTestBase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;

/**
 *  <p>
 *  This tests do not depend on a specific dataset but only test
 *  </p>
 *  <ul>
 *  <li> Correct Errors on Illegal Requests
 *  <li> Missing but required parameter
 *  <li> Correct default values for optional parameters
 *  </ul>
 *  <p>
 *  This set of tests should be tested against all service endpoints that
 *  support queries. Typically this is done by extending this class
 *  and configuring it to run against the according endpoint.
 *  </p><p>
 *  Please make sure that the data set this tests are executed against does
 *  not contain any information using the "http://www.test.org/test#"
 *  namespace.
 *  </p>
 * @author Rupert Westenthaler
 *
 */
public abstract class QueryTestBase extends EntityhubTestBase {
    
    public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    protected final String endpointPath;
    /**
     * Constructs a FieldQueryServiceTest
     * @param servicePath the path to the service (e.g. /entityhub/sites/query)
     * @param referencedSiteId if the 
     * @param log
     */
    public QueryTestBase(String servicePath, String referencedSiteId,Logger log){
        super(referencedSiteId == null ? null : Collections.singleton(referencedSiteId),log);
        if(servicePath == null){
            throw new IllegalArgumentException("The path to the FieldQuery endpoint MUST NOT be NULL!");
        }
        if(referencedSiteId != null){
            if(!servicePath.contains(referencedSiteId)){
                throw new IllegalArgumentException(String.format(
                    "The parsed referenceSiteId %s is not contained within the parsed servicePath %s",
                    referencedSiteId,servicePath));
            }
        }
        //remove tailing '/'
        if(servicePath.charAt(servicePath.length()-1) == '/'){
            servicePath = servicePath.substring(0, servicePath.length()-1);
        }
        //we need a leading '/'
        if(servicePath.charAt(0) != '/'){
            servicePath = '/'+servicePath;
        }
        this.endpointPath = servicePath;
        log.info("created FieldQueryTest for Service {}",servicePath);
    }
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *  Utility Methods
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */
    /**
     * Executes a {@link QueryTestCase} by sending the 
     * {@link QueryTestCase#getContent() query} as an POST request to the 
     * <code>{@link #endpointPath}/{@link QueryTestCase#getServicePath()}</Code>.
     * @param path the path to perform the field query. "/query" is added to the
     * parsed value
     * @param test the field query test
     * @return the result executor used for the test
     * @throws IOException on any exception while connecting to the entityhub
     * @throws JSONException if the returned results are not valid JSON
     */
    protected RequestExecutor executeQuery(QueryTestCase test) throws IOException, JSONException {
        Request request = builder.buildPostRequest(endpointPath+test.getServicePath());
        for(Entry<String,String> header : test.getHeaders().entrySet()){
            request.withHeader(header.getKey(), header.getValue());
        }
        request.withContent(test.getContent());
        return executor.execute(request);
    }
    /**
     * Validates Results of a Query (/find or /query requests) based on the
     * data defined by the test case
     * @param re the {@link RequestExecutor} used for the test case
     * @param test the query test case
     * @throws JSONException in case the {@link RequestExecutor#getContent()} are
     * no valid JSON. NOTE that the contents are only parsed if the
     * {@link QueryTestCase#getExpectedStatus()} is a 2xx status code.
     */
    protected void assertQueryResults(RequestExecutor re, QueryTestCase test) throws JSONException{
        re.assertStatus(test.getExpectedStatus());
        re.assertContentType("application/json"); //currently only application/json is supported
        if(!test.expectsSuccess()){
            return; //no further checks for tests that expect failure
        }
        JSONObject jso = new JSONObject(re.getContent());
        JSONArray results = jso.getJSONArray("results");
        if(test.expectesResults()){
            assertTrue("Missing Results for Query: \n "+test,
                results.length() > 0);
        } else {
            assertTrue("Unexpected Results for Query:\n "+test,
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
        for(int i=0;i<results.length();i++){
            JSONObject result = results.getJSONObject(i);
            String id = result.optString("id", null);
            assertNotNull("ID missing for an Result", id);
            if(expectedIds != null && expectedIds.remove(id)){ //not all results must be in the list
                log.info("  > expected result: {}",id);
            } else {
                log.debug("  > result: {}",id);
            }
            if(test.getProhibitedResultIds() != null){
                assertFalse("Prohibited Result '"+id+"' found!",
                    test.getProhibitedResultIds().contains(id));
            }
            Set<String> checkRequiredFields; //copy over the required fields
            if(test.getRequiredFields() != null && !test.getRequiredFields().isEmpty()){
                checkRequiredFields = new HashSet<String>(test.getRequiredFields());
            } else {
                checkRequiredFields = null;
            }
            for(Iterator<?> keys = result.keys(); keys.hasNext();){
                Object key = keys.next();
                //process key
                assertTrue("Field "+key+" is not an expected one",
                    test.getAllowedFields().contains(key));
                if(checkRequiredFields != null && checkRequiredFields.remove(key)){
                    log.info("     - required field {}", key);
                } else {
                    log.debug("     - optional field {}", key);
                }
            }
            if(checkRequiredFields != null){
                assertTrue("Missing required Fields "+checkRequiredFields,
                    checkRequiredFields.isEmpty());
            }
        }
        if(expectedIds != null){ // if there where expected results check that all where found
            assertTrue("The following expected results where missing in the Response: \n "+expectedIds,
                expectedIds.isEmpty());
        }
    }
    /**
     * Asserts that the Query is present in the response and if so returns the
     * query
     * @param content the returned content
     * @return the query as contained in the response
     * @throws JSONException on any Error while parsing the JSON query from the
     * parsed content
     */
    protected JSONObject assertResponseQuery(String content) throws JSONException {
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
    protected JSONArray assertSelectedField(JSONObject jQuery,String...selected) throws JSONException {
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
    
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *  Find Query Test Methods:
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */
    @Test
    public void testMissingNameParameter() throws IOException, JSONException {
        FindQueryTestCase test = new FindQueryTestCase(null, 400);
        executeQuery(test);
    }
    @Test
    public void testEmptyNameParameter() throws IOException, JSONException {
        FindQueryTestCase test = new FindQueryTestCase("", 400);
        executeQuery(test);
    }
    /**
     * Tests the defaults for the text constraint used for find queries. This
     * includes: <ul>
     *   <li> limit set to an value > 0
     *   <li> offset set to 0
     *   <li> selected set to rdfs:label
     *   <li> constraint.patternType set to wildcard
     *   <li> constraint.field set to rdfs:label
     *   <li> constraint.type set to text
     * </ul>
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testDefaultsParameter() throws IOException, JSONException {
        FindQueryTestCase test = new FindQueryTestCase("non_existant_"+UUID.randomUUID().toString(), false);
        RequestExecutor re = executeQuery(test);
        JSONObject jQuery = assertResponseQuery(re.getContent());
        assertTrue("Result Query does not contain Limit property",jQuery.has("limit"));
        assertTrue("Returned limit is <= 0",jQuery.getInt("limit") > 0);
        
        assertTrue("Result Query does not contain offset property",jQuery.has("offset"));
        assertTrue("Returned offset is != 0",jQuery.getInt("offset") == 0);
        
        assertSelectedField(jQuery,RDFS_LABEL);
        
        JSONArray jConstraints = jQuery.optJSONArray("constraints");
        assertNotNull("Result Query is missing the 'constraints' property",jConstraints);
        assertEquals("Result Query is expected to have a single constraint",
            1, jConstraints.length());
        JSONObject constraint = jConstraints.optJSONObject(0);
        assertNotNull("'constraints' array does not contain a JSONObject but "+jConstraints.get(0),
            constraint);
        
        assertEquals("The 'type' of the Constraint is not 'text' but "+constraint.opt("type"), 
            "text",constraint.optString("type"));

        assertEquals("The 'patternType' of the Constraint is not 'wildcard' but "+constraint.opt("patternType"), 
            "wildcard",constraint.optString("patternType"));
        
        assertEquals("The 'field' of the Constraint is not rdfs:label but "+constraint.opt("field"), 
            RDFS_LABEL,constraint.optString("field"));
    }
    
    @Test
    public void testCustomFieldParameter() throws IOException, JSONException {
        FindQueryTestCase test = new FindQueryTestCase("non_existant_"+UUID.randomUUID().toString(), false);
        String testField = "http://www.test.org/test#test_"+UUID.randomUUID();
        test.setField(testField);
        RequestExecutor re = executeQuery(test);
        JSONObject jQuery = assertResponseQuery(re.getContent());
        assertSelectedField(jQuery, testField);
    }

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     *  Field Query Test Methods:
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     */
    @Test
    public void testIllegalJSON() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': 'Paris' " + //NOTE: here the comma is missing here!
                "'patternType' : 'none', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "'dataTypes': ['http:\\/\\/www.iks-project.eu\\/ontology\\/rick\\/model\\/text'] " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }
    @Test
    public void testMissingConstrints() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
                "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
                "'offset': '0', " +
                "'limit': '3', " +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }
    @Test
    public void testEmptyFieldProperty() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': 'Paris', " + 
                "'patternType' : 'none', " +
                "'field': '', " +
                "'dataTypes': ['http:\\/\\/www.iks-project.eu\\/ontology\\/rick\\/model\\/text'] " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }    
    @Test
    public void testUnknownConstraintType() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'unknownConstraintType', " +
                "'test': 'dummy' " + 
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }    
    @Test
    public void testMultipleConstraintsForSameField() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': 'Paris', " + 
                "'patternType' : 'none', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "'dataTypes': ['http:\\/\\/www.iks-project.eu\\/ontology\\/rick\\/model\\/text'] " +
                "},{ "+
                    "'type': 'reference', "+
                    "'field': 'http:\\/\\/www.test.org\\/test#field', "+
                    "'value': 'http:\\/\\/dbpedia.org\\/ontology\\/Person', "+
                "}]"+
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }    
    @Test
    public void testOffsetNoNumber() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
                "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
                "'offset': 'abc', " +
                "'limit': '3', " +
                "'constraints': [{ " +
                    "'type': 'reference', "+
                    "'field': 'http:\\/\\/www.test.org\\/test#field', "+
                    "'value': 'http:\\/\\/www.test.org\\/test#Test', "+
                "}]"+
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }
    @Test
    public void testOffsetNegativeNumber() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
                "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
                "'offset': '-3', " +
                "'limit': '3', " +
                "'constraints': [{ " +
                    "'type': 'reference', "+
                    "'field': 'http:\\/\\/www.test.org\\/test#non-existing-field', "+
                    "'value': 'http:\\/\\/www.test.org\\/test#NonExistingValue', "+
                "}]"+
            "}", 
            false); //success but no result
        //now execute the test
        RequestExecutor re = executeQuery(test);
        JSONObject jQuery = assertResponseQuery(re.getContent());
        assertTrue("Result Query does not contain offset property",jQuery.has("offset"));
        assertTrue("Returned offset is != 0",jQuery.getInt("offset") == 0);
    }
    @Test
    public void testLimitNoNumber() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
                "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
                "'offset': '0', " +
                "'limit': 'abc', " +
                "'constraints': [{ " +
                    "'type': 'reference', "+
                    "'field': 'http:\\/\\/www.test.org\\/test#field', "+
                    "'value': 'http:\\/\\/www.test.org\\/test#Test', "+
                "}]"+
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }
    @Test
    public void testLimitNegativeNumber() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
                "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
                "'offset': '0', " +
                "'limit': '-1', " +
                "'constraints': [{ " +
                    "'type': 'reference', "+
                    "'field': 'http:\\/\\/www.test.org\\/test#non-existing-field', "+
                    "'value': 'http:\\/\\/www.test.org\\/test#NonExistingValue', "+
                "}]"+
            "}", 
            false); //success but no result
        //now execute the test
        RequestExecutor re = executeQuery(test);
        //test the of the limit was set correctly set to the default (> 0)
        JSONObject jQuery = assertResponseQuery(re.getContent());
        assertTrue("Result Query does not contain Limit property",jQuery.has("limit"));
        assertTrue("Returned limit is <= 0",jQuery.getInt("limit") > 0);
    }
    @Test
    public void testMissingConstraintFieldProperty() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': 'Paris', " + 
                "'patternType' : 'none', " +
                //"'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "'dataTypes': ['http:\\/\\/www.iks-project.eu\\/ontology\\/rick\\/model\\/text'] " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }    
    @Test
    public void testMissingConstraintTypeProperty() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                //"'type': 'text', " +
                "'test': 'dummy' " + 
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }    
    @Test
    public void testMissingReferenceConstraintValueProperty() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'reference', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field' " +
                //"'value': 'http:\\/\\/dbpedia.org\\/ontology\\/Person', " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }
    @Test
    public void testMissinValueConstraintValueProperty() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'value'," +
                //"'value': 'Paris'," +
                "'field': 'http:\\/\\/www.test.org\\/test#field'" +
                "}]," +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }
    @Test
    public void testValueConstraintDefaultDataType() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'value'," +
                "'value': 'Paris'," +
                "'field': 'http:\\/\\/www.test.org\\/test#field'" +
                "}]," +
            "}", 
            false); //expect BadRequest
        //now execute the test
        RequestExecutor re = executeQuery(test);
        JSONObject jQuery = assertResponseQuery(re.getContent());
        JSONArray jConstraints = jQuery.optJSONArray("constraints");
        assertNotNull("Result Query does not contain the constraints Array",jConstraints);
        assertTrue("Result Query Constraint Array does not contain the expected Constraint",
            jConstraints.length() == 1);
        JSONObject jConstraint = jConstraints.optJSONObject(0);
        assertNotNull("Constraint Array does not contain JSONObjects",jConstraint);
        assertTrue("Returned Query does not contain the default data type",jConstraint.has("datatype"));
    }
    @Test
    public void testMissingTextConstraintTextProperty() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                //"'text': 'Paris', " + 
                "'patternType' : 'none', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }    
    @Test
    public void testEmptyTextConstraintTextProperty() throws IOException, JSONException {
        //1. empty string
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': '', " + 
                "'patternType' : 'none', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
        //2 empty Array
        test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': [], " + 
                "'patternType' : 'none', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
        //3 Array with empty string
        test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': [''], " + 
                "'patternType' : 'none', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }    

    @Test
    public void testDefaultTextConstraintPatternTypeProperty() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': 'Paris', " + 
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "}]" +
            "}", 
            false); //expect BadRequest
        //now execute the test
        RequestExecutor re = executeQuery(test);
        JSONObject jQuery = assertResponseQuery(re.getContent());
        JSONArray jConstraints = jQuery.optJSONArray("constraints");
        assertNotNull("Result Query does not contain the constraints Array",jConstraints);
        assertTrue("Result Query Constraint Array does not contain the expected Constraint",
            jConstraints.length() == 1);
        JSONObject jConstraint = jConstraints.optJSONObject(0);
        assertNotNull("Constraint Array does not contain JSONObjects",jConstraint);
        assertTrue("The 'patternType' property MUST BE set for returned TextConstraints",
            jConstraint.has("patternType"));
        assertEquals("Default for patternType MUST BE 'none'", 
            "none", jConstraint.getString("patternType"));
    }    
    @Test
    public void testUnknownTextConstraintPatternType() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'text', " +
                "'text': 'Paris', " + 
                "'patternType' : 'unknownPatternType', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }   
    @Test
    public void testRangeConstraintNoBoundsProperties() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'range', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field'," +
                //"'lowerBound': '1946-01-01T00:00:00.000Z'," +
                //"'upperBound': '1946-12-31T23:59:59.999Z'," +
                "'inclusive': true," +
                "'datatype': 'xsd:dateTime'" +
                "}]" +
            "}", 
            400); //expect BadRequest
        //now execute the test
        executeQuery(test);
    }   
    @Test
    public void testDefaultRangeConstraintDatatypeProperty() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{"+
            "'selected': [ 'http:\\/\\/www.test.org\\/test#field'], " +
            "'constraints': [{ " +
                "'type': 'range', " +
                "'field': 'http:\\/\\/www.test.org\\/test#field', " +
                "'lowerBound': 1000," +
                "'inclusive': true," +
                "}]" +
            "}", 
            false); //expect BadRequest
        //now execute the test
        RequestExecutor re = executeQuery(test);
        JSONObject jQuery = assertResponseQuery(re.getContent());
        JSONArray jConstraints = jQuery.optJSONArray("constraints");
        assertNotNull("Result Query does not contain the constraints Array",jConstraints);
        assertTrue("Result Query Constraint Array does not contain the expected Constraint",
            jConstraints.length() == 1);
        JSONObject jConstraint = jConstraints.optJSONObject(0);
        assertNotNull("Constraint Array does not contain JSONObjects",jConstraint);
        assertTrue("Returned Query does not contain the default data type",jConstraint.has("datatype"));    }   
}
