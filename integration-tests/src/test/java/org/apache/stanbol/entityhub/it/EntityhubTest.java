/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.stanbol.entityhub.it;

import static junit.framework.Assert.assertNotSame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.entityhub.test.it.EntityhubTestBase;
import org.apache.stanbol.entityhub.test.query.FieldQueryTestCase;
import org.apache.stanbol.entityhub.test.query.FindQueryTestCase;
import org.apache.stanbol.entityhub.test.query.QueryTestBase;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the RESTful service provided by the Entityhub. Note that this 
 * extends the QueryTestBase. By that generic tests for the query interface
 * (e.g. illegal requests, usage of default values ...) are already covered.
 * @author Rupert Westenthaler
 *
 */
public final class EntityhubTest extends QueryTestBase {
    
    public EntityhubTest() {
        super("/entityhub", null);
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * TODO: define
     */
    public static final String SINGLE_IMPORT_ENTITY_ID = "";
    
    @Override
    protected String getDefaultFindQueryField() {
        return "http://www.iks-project.eu/ontology/rick/model/label";
    }
    /*
     * First the CRUD interface
     */
//    @Test
    public void testEntityCreation() throws IOException {
        String singleImportResource = "";
        //create a POST request with a test RDF file
        RequestExecutor test = executor.execute(
            buildImportRdfData(singleImportResource, true, SINGLE_IMPORT_ENTITY_ID));
        //assert that the entity was created
        test.assertStatus(201);
        
        //NOTE: the check for the imported ID(s) is part of the 
        // testEntityRetrieval test Method
        
    }
//    @Test
    public void testMultipleEntityCreation() throws IOException {
        String multipleImportResource = "";
        //create a POST request with a test RDF file
        RequestExecutor test = executor.execute(
            buildImportRdfData(multipleImportResource, true, null));
        //assert that the entity was created
        test.assertStatus(201);
        
        //NOTE: the check for the imported ID(s) is part of the 
        // testEntityRetrieval test Method
        
    }
//    @Test
    public void testEntityRetrieval(){
        //make a lookup for ID(s) of entities imported by the 
        //testEntityCreation and testMultipleEntityCreation test method
        
    }
//    @Test
    public void testEntityUpdates() throws IOException {
        String singleUpdateResource = "";
        //create a POST request with a test RDF file that contains updated data
        //of the one used for testEntityCreation
        //create a POST request with a test RDF file
        RequestExecutor test = executor.execute(
            buildImportRdfData(singleUpdateResource, false, SINGLE_IMPORT_ENTITY_ID));
        test.assertStatus(200);
        //TODO: validate that the entity was updated
        
    }
    
//    @Test
    public void testMultipleEntityUpdates() throws IOException {
        String nultipleUpdateResource = "";
        //create a POST request with a test RDF file that contains updated data
        //of the one used for testMultipleEntityCreation 
        RequestExecutor test = executor.execute(
            buildImportRdfData(nultipleUpdateResource, false, null));
        test.assertStatus(200);
        //TODO: validate that the entity was updated
        
        //check for the updates to be applied
        //check the metadata to be updated (creation date != modification date)
    }

//    @Test
    public void testEntityDeletion(){
        // delete an entity previously imported
        // by making a retrieval for this ID check that the removal was successfull 
    }
//    @Test
    public void testEntityLookup(){
        //lookup is importing entities from referenced sites
        //lookup entities from the dbpedia site also used by the other integration
        //tests
    }
//    @Test
    public void testEntityImport(){
        //import some entities from referenced sites
        //here we shall also use some entities from the dbpedia referenced site
    }
    
//    @Test
    public void testFindNameQuery() throws IOException, JSONException {
        //typical find by name query tests bases on created and imported entities 
    }
//    @Test
    public void testFindLimitAndOffsetQuery() throws IOException, JSONException {
        //typical tests based on created and imported entities
    }
//    @Test
    public void testFindLanguageQuery() throws IOException, JSONException {
        //typical tests based on created and imported entities
    }
//    @Test
    public void testFindWildcards() throws IOException, JSONException {
        //typical tests based on created and imported entities
    }
//    @Test
    public void testFindSpecificFieldQuery() throws IOException, JSONException {
        //typical tests based on created and imported entities
    }
    
//    @Test
    public void testFieldQueryRangeConstraints() throws IOException, JSONException {
        //typical tests based on created and imported entities
    }
    
//    @Test
    public void testFieldQueryTextConstraints() throws IOException, JSONException {
        //typical tests based on created and imported entities
    }
//    @Test
    public void testFieldQueryValueConstraints() throws IOException, JSONException {
        //typical tests based on created and imported entities
    }    
    

    /**
     * Imports/updates RDF data of the file to the entityhub with the possibility
     * to restrict imports/updates to the parsed uri
     * @param file the file with the RDF data (needs to be in the classpath)
     * @param create if <code>true</code> the data are created (POST) otherwise
     * updated (PUT). 
     * @param uri if not <code>null</code> only data of this URI are imported by
     * specifying the id parameter
     */
    protected Request buildImportRdfData(String file, boolean create, String uri){
        Request request;
        String path;
        if(uri != null){
            path = builder.buildUrl("/entity", "id",uri);
        } else {
            path = builder.buildUrl("/entity");
        }
        if(create){
            request = builder.buildPostRequest(path);
        } else {
            request = builder.buildOtherRequest(new HttpPut(path));
        }
        //set the HttpEntity (both PUT and POST are HttpEntityEnclosingRequests)
        ((HttpEntityEnclosingRequest)request.getRequest()).setEntity(
            new InputStreamEntity(
                EntityhubTest.class.getClassLoader().getResourceAsStream(file),
                -1));
        //finally set the correct content-type of the provided data
        //currently fixed to "application/rdf+xml"
        request.getRequest().setHeader("Content-Type", "application/rdf+xml");
        
        return request;
    }
    
}
