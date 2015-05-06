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

import static org.apache.clerezza.rdf.core.serializedform.SupportedFormat.RDF_XML;
import static org.apache.stanbol.entityhub.test.it.AssertEntityhubJson.assertEntity;
import static org.apache.stanbol.entityhub.test.it.AssertEntityhubJson.assertQueryResults;
import static org.apache.stanbol.entityhub.test.it.AssertEntityhubJson.assertRepresentation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.stanbol.commons.testing.http.Request;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.entityhub.servicesapi.defaults.NamespaceEnum;
import org.apache.stanbol.entityhub.test.query.FieldQueryTestCase;
import org.apache.stanbol.entityhub.test.query.FindQueryTestCase;
import org.apache.stanbol.entityhub.test.query.QueryTestBase;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
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


    private static final Collection<String> EXPECTED_DOAP_FIELDS;
    static {
        Collection<String> fields = new ArrayList<String>();
        fields.add("http://usefulinc.com/ns/doap#created");
        fields.add("http://usefulinc.com/ns/doap#license");
        fields.add("http://usefulinc.com/ns/doap#name");
        fields.add("http://usefulinc.com/ns/doap#homepage");
        fields.add("http://usefulinc.com/ns/doap#shortdesc");
        fields.add("http://usefulinc.com/ns/doap#description");
        fields.add("http://usefulinc.com/ns/doap#bug-database");
        fields.add("http://usefulinc.com/ns/doap#mailing-list");
        fields.add("http://usefulinc.com/ns/doap#download-page");
        fields.add("http://usefulinc.com/ns/doap#programming-language");
        fields.add("http://usefulinc.com/ns/doap#category");
        fields.add("http://projects.apache.org/ns/asfext#pmc");
        EXPECTED_DOAP_FIELDS = Collections.unmodifiableCollection(fields);
    }
    @Override
    protected String getDefaultFindQueryField() {
        return NamespaceEnum.entityhub+"label";
    }
    /*
     * Tests the CRUD interface
     * 
     */
    @Test
    public void testEntityCrud() throws IOException, JSONException {
        //execution order is important
        testEntityCreation();
        testEntityCreated();
        testEntityUpdate();
        testEntityUpdated();
        testEntityDelete();
        testEntityDeleted();
        testEntityDeleteAll();
        testAllEntitiesDeleted();
    }
    private void testEntityCreation() throws IOException {
        InputStream in = EntityhubTest.class.getClassLoader().getResourceAsStream("doap_Stanbol.rdf");
        Assert.assertNotNull("Unable to load test resource 'doap_Stanbol.rdf'", in);
        String stanbolProjectUri = "http://stanbol.apache.org";
        //create a POST request with a test RDF file
        RequestExecutor test = executor.execute(
            buildImportRdfData(in ,RDF_XML, true, stanbolProjectUri));
        //assert that the entity was created
        test.assertStatus(201);
    }

    private void testEntityCreated() throws IOException, JSONException {
        String id = "http://stanbol.apache.org";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest("/entityhub/entity","id",id)
            .withHeader("Accept", "application/json"));
        re.assertStatus(200);

        JSONObject jEntity = assertEntity(re.getContent(), id, "entityhub");
        Map<String,Set<List<String>>> data = assertRepresentation(jEntity.getJSONObject("representation"), 
            EXPECTED_DOAP_FIELDS, null);
        //test values of two properties we will update in a following test
        Set<List<String>> pmcValues = data.get("http://projects.apache.org/ns/asfext#pmc");
        Assert.assertNotNull(pmcValues);
        Assert.assertEquals(1, pmcValues.size());
        Assert.assertEquals("http://incubator.apache.org", pmcValues.iterator().next().get(0));
        Set<List<String>> downloadValues = data.get("http://usefulinc.com/ns/doap#download-page");
        Assert.assertNotNull(downloadValues);
        Assert.assertEquals(1, downloadValues.size());
        Assert.assertEquals("http://stanbol.apache.org", downloadValues.iterator().next().get(0));
    }

    private void testEntityUpdate() throws IOException, JSONException {
        InputStream in = EntityhubTest.class.getClassLoader().getResourceAsStream("mod_doap_Stanbol.rdf");
        Assert.assertNotNull("Unable to load test resource 'mod_doap_Stanbol.rdf'", in);
        String stanbolProjectUri = "http://stanbol.apache.org";
        //create a POST request with a test RDF file
        RequestExecutor test = executor.execute(
            buildImportRdfData(in ,RDF_XML, false, stanbolProjectUri));
        //assert that the entity was created
        test.assertStatus(200);
        //check that the updated entity was returned
        assertEntity(test.getContent(), stanbolProjectUri, "entityhub");
    }

    private void testEntityUpdated() throws IOException, JSONException {
        String id = "http://stanbol.apache.org";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest("/entityhub/entity","id",id)
            .withHeader("Accept", "application/json"));
        re.assertStatus(200);

        JSONObject jEntity = assertEntity(re.getContent(), id, "entityhub");
        Map<String,Set<List<String>>> data = assertRepresentation(jEntity.getJSONObject("representation"), 
            EXPECTED_DOAP_FIELDS, null);
        Set<List<String>> pmcValues = data.get("http://projects.apache.org/ns/asfext#pmc");
        Assert.assertNotNull(pmcValues);
        Assert.assertEquals(1, pmcValues.size());
        Assert.assertEquals("http://stanbol.apache.org", pmcValues.iterator().next().get(0));
        Set<List<String>> downloadValues = data.get("http://usefulinc.com/ns/doap#download-page");
        Assert.assertNotNull(downloadValues);
        Assert.assertEquals(1, downloadValues.size());
        Assert.assertEquals("http://stanbol.apache.org/downloads/", downloadValues.iterator().next().get(0));
    }
    
    private void testEntityDelete() throws IOException {
        String stanbolProjectUri = "http://stanbol.apache.org";
        Request request = builder.buildOtherRequest(new HttpDelete(
            builder.buildUrl("/entityhub/entity", "id", stanbolProjectUri)));
        RequestExecutor re = executor.execute(request);
        re.assertStatus(200);
    }

    private void testEntityDeleted() throws IOException {
        String id = "http://stanbol.apache.org";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest("/entityhub/entity","id",id)
            .withHeader("Accept", "application/json"));
        re.assertStatus(404);
    }
    private void testEntityDeleteAll() throws IOException {
        Request request = builder.buildOtherRequest(new HttpDelete(
            builder.buildUrl("/entityhub/entity", "id", "*")));
        RequestExecutor re = executor.execute(request);
        re.assertStatus(200);
    }
    private void testAllEntitiesDeleted() throws IOException {
        String id = "http://xml.apache.org/xerces-c/";
        RequestExecutor re = executor.execute(
            builder.buildGetRequest("/entityhub/entity","id",id)
            .withHeader("Accept", "application/json"));
        re.assertStatus(404);
    }
    @Test
    public void testEntityLookup() throws IOException, JSONException {
        String uri = "http://dbpedia.org/resource/Paris";
        //first check that lookup without create returns 404
        RequestExecutor re = executor.execute(builder.buildGetRequest(
            "/entityhub/lookup", "id",uri));
        re.assertStatus(404);
        //Now check that lookup with create does work
        re = executor.execute(builder.buildGetRequest(
            "/entityhub/lookup", "id",uri,"create","true"));
        re.assertStatus(200);
        JSONObject entity = assertEntity(re.getContent(), null, "entityhub");
        String ehUri = entity.optString("id", null);
        
        //try to retrieve the entity with the generated id
        re = executor.execute(builder.buildGetRequest(
            "/entityhub/entity", "id",ehUri));
        re.assertStatus(200);
        assertEntity(re.getContent(), ehUri, "entityhub");
        
        //no try again to lookup the entity without create
        re = executor.execute(builder.buildGetRequest(
            "/entityhub/lookup", "id",uri));
        re.assertStatus(200);
        assertEntity(re.getContent(), ehUri, "entityhub");
        
        //finally delete the entity
        re = executor.execute(builder.buildOtherRequest(new HttpDelete(
            builder.buildUrl("/entityhub/entity", "id", ehUri))));
        re.assertStatus(200);

    }
    
    @Test
    public void testQueries() throws IOException, JSONException {
        //first load the data for the rquery test
        URL url = EntityhubTest.class.getClassLoader().getResource("apache-project-doap-files.zip");
        Assert.assertNotNull(url);
        File f;
        try {
          f = new File(url.toURI());
        } catch(URISyntaxException e) {
          f = new File(url.getPath());
        }
        Assert.assertNotNull(f.isFile());
        ZipFile archive = new ZipFile(f);
        try {
            for(Enumeration<? extends ZipEntry> e = archive.entries();e.hasMoreElements();){
                ZipEntry entry = e.nextElement();
                log.debug(" - uploading {} to entityhub",entry);
                RequestExecutor re = executor.execute(
                    buildImportRdfData(archive.getInputStream(entry) ,RDF_XML, false, null));
                //assert that the entity was created (or already existed)
                //some projects seams to have more than a single doap file
                int status = re.getResponse().getStatusLine().getStatusCode();
                Assert.assertTrue("Unable to add '"+entry.getName()+"'! Status:" 
                        + re.getResponse().getStatusLine(), status == 200 || status == 304);
            }
        } finally {
            archive.close();
        }

        testFindNameQuery();
        testFindWildcards();
        testFindLimitAndOffsetQuery();
        testFieldQueryTextConstraints();
        //finally delete all added entity
        RequestExecutor re = executor.execute(builder.buildOtherRequest(new HttpDelete(
            builder.buildUrl("/entityhub/entity", "id", "*"))));
        re.assertStatus(200);

    }
    
    private void testFindNameQuery() throws IOException, JSONException {
        FindQueryTestCase test = new FindQueryTestCase("Apache Stanbol",
            Arrays.asList(
                "http://stanbol.apache.org"));//,
                //"http://dbpedia.org/resource/Paris_Hilton"));
        test.setField("http://usefulinc.com/ns/doap#name");
        test.setLanguage(null);
        executeQuery(test);
    }
    private void testFindWildcards() throws IOException, JSONException {
        FindQueryTestCase test = new FindQueryTestCase("Hiv*",
            Arrays.asList(
                "http://hive.apache.org",
                "http://jakarta.apache.org/hivemind/"),
            Arrays.asList(
                "http://beehive.apache.org"));
        test.setField("http://usefulinc.com/ns/doap#name");
        test.setLanguage(null);
        executeQuery(test);
    }

    private void testFindLimitAndOffsetQuery() throws IOException, JSONException {
    	//With Solr4 we need a test that produces different scores for results,
    	//to ensure consistant odering
        FindQueryTestCase test = new FindQueryTestCase("XML XSL*",
            Arrays.asList(
                    "http://velocity.apache.org/anakia/",
                    "http://xalan.apache.org/xalan-c/",
                    "http://xalan.apache.org/xalan-j/",
                    "http://velocity.apache.org/dvsl/devel/",
                    "http://xmlgraphics.apache.org/commons/",
                    "http://xmlgraphics.apache.org/fop"),
            null);
        test.setField("http://usefulinc.com/ns/doap#description");
        test.setLimit(10);
        test.setLanguage(null);
        RequestExecutor re = executeQuery(test);
        //get the list of results (will assert the response twice)
        //to check the expected limit and offset results
        List<String> resultList = assertQueryResults(re,test);
        List<String> expected = resultList.subList(2, 4); //3rd and 4th element
        List<String> excluded = new ArrayList<String>(); //all other
        excluded.addAll(resultList.subList(0, 2));
        excluded.addAll(resultList.subList(4, resultList.size()));
        //repeat the test with offset 2 and limit 2 to only retrieve the 3-4 result
        test = new FindQueryTestCase("XML XSL*", expected, excluded);
        test.setField("http://usefulinc.com/ns/doap#description");
        test.setOffset(2);
        test.setLimit(2);
        test.setLanguage(null);
        executeQuery(test);
        
    }

    private void testFieldQueryTextConstraints() throws IOException, JSONException {
        FieldQueryTestCase test = new FieldQueryTestCase(
            "{ "+
                "'selected': ["+
                    "'http:\\/\\/usefulinc.com\\/ns\\/doap#name'],"+
                "'offset': '0',"+
                "'limit': '3',"+
                "'constraints': [{ "+
                    "'type': 'text', "+
                    "'patternType': 'wildcard', "+
                    "'text': 'Stanbol', "+
                    "'field': 'http:\\/\\/usefulinc.com\\/ns\\/doap#name' "+
                "},{ "+
                    "'type': 'text', "+
                    "'patternType': 'wildcard', "+
                    "'text': 'Java', "+
                    "'field': 'http:\\/\\/usefulinc.com\\/ns\\/doap#programming-language' "+
                "}]"+
             "}",
             Arrays.asList( //list of expected results
                 "http://stanbol.apache.org"),
             null);
        //now execute the test
        executeQuery(test);
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
    protected Request buildImportRdfData(InputStream in, String contentType, boolean create, String uri){
        Assert.assertNotNull(in);
        Assert.assertNotNull(contentType);
        Request request;
        String path;
        if(uri != null){
            path = builder.buildUrl("/entityhub/entity", "id",uri);
        } else {
            path = builder.buildUrl("/entityhub/entity");
        }
        if(create){
            request = builder.buildOtherRequest(new HttpPost(path));
        } else {
            request = builder.buildOtherRequest(new HttpPut(path));
        }
        //set the HttpEntity (both PUT and POST are HttpEntityEnclosingRequests)
        ((HttpEntityEnclosingRequest)request.getRequest()).setEntity(
            new InputStreamEntity(in, -1));
        //finally set the correct content-type of the provided data
        //currently fixed to "application/rdf+xml"
        request.getRequest().setHeader("Content-Type", contentType);
        return request;
    }
    
}
