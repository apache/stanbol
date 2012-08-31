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
package org.apache.stanbol.contenthub.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.stanbol.commons.testing.http.RequestExecutor;
import org.apache.stanbol.commons.testing.stanbol.StanbolTestBase;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;

/**
 * Tests of the Store RESTful service of the Contenthub.
 * 
 * @author meric
 * 
 */
public class StoreResourceTest extends StanbolTestBase {

    private final String defaultPath = "/contenthub/store/";

    @Test
    public void testGetView() throws ClientProtocolException, IOException {
        RequestExecutor test = executor.execute(builder.buildGetRequest(defaultPath));
        test.assertContentType(MediaType.TEXT_HTML);
        test.assertContentContains("</div>");
    }

    @Test
    public void testGetsWithNonExistUri() throws ClientProtocolException, IOException {
        RequestExecutor test;

        String uri = "non_exist_test_uri";
        // GetRawContent
        test = executor.execute(builder.buildGetRequest(defaultPath + "raw/" + uri));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());

        // GetContentItemMetaData
        test = executor.execute(builder.buildGetRequest(defaultPath + "metadata/" + uri));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());

        // DownloadContentItem Raw
        test = executor.execute(builder.buildGetRequest(defaultPath + "raw/" + uri).withHeader("Accept",
            MediaType.APPLICATION_OCTET_STREAM));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());

        // DownloadContentItem Metadata
        test = executor.execute(builder.buildGetRequest(defaultPath + "metadata/" + uri).withHeader("Accept",
            MediaType.APPLICATION_OCTET_STREAM));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());

        // GetContent
        test = executor.execute(builder.buildGetRequest(defaultPath + "content/" + uri));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());

        // GetContentItemView
        test = executor.execute(builder.buildGetRequest(defaultPath + "page/" + uri));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());

        // DeleteContentItem
        String path = builder.buildUrl(defaultPath + uri);
        test = executor.execute(builder.buildOtherRequest(new HttpDelete(path)));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());

    }

    @Test
    public void testCRUD() throws ClientProtocolException, IOException {
        RequestExecutor test;
        final String content = "Paris is the capital city of France.";
        final String title = "Paris";
        final String uri = "urn:content-item-sha1-1de18ff0566f179c31aea7ff28de6bbf574ee0da";

        // CreateContentItemFromForm
        test = executor.execute(builder.buildPostRequest(defaultPath)
                .withHeader("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
                .withFormContent("content", content, "title", title));
        test.assertStatus(Status.OK.getStatusCode());
        test.assertContentContains(content);

        // GetRawContent
        test = executor.execute(builder.buildGetRequest(defaultPath + "raw/" + uri));
        test.assertStatus(Status.OK.getStatusCode());
        assertEquals(content, test.getContent());

        // GetContentItemMetaData
        test = executor.execute(builder.buildGetRequest(defaultPath + "metadata/" + uri));
        test.assertStatus(Status.OK.getStatusCode());
        test.assertContentType(MediaType.TEXT_PLAIN);
        test.assertContentContains("<rdf:RDF");

        // DownloadContentItem Raw
        test = executor.execute(builder.buildGetRequest(defaultPath + "raw/" + uri).withHeader("Accept",
            MediaType.APPLICATION_OCTET_STREAM));
        test.assertStatus(Status.OK.getStatusCode());
        test.assertContentType(MediaType.TEXT_PLAIN);
        assertEquals(content, test.getContent());

        // DownloadContentItem Metadata in RDF/XML format
        test = executor.execute(builder.buildGetRequest(
            defaultPath + "metadata/" + uri + "?format=application%2Frdf%2Bxml").withHeader("Accept",
            MediaType.APPLICATION_OCTET_STREAM));
        test.assertStatus(Status.OK.getStatusCode());
        test.assertContentType(MediaType.TEXT_PLAIN);
        test.assertContentContains("<rdf:RDF");

        // DownloadContentItem Metadata in N-Triples
        test = executor.execute(builder.buildGetRequest(
            defaultPath + "metadata/" + uri + "?format=text%2Frdf%2Bnt").withHeader("Accept",
            MediaType.APPLICATION_OCTET_STREAM));
        test.assertStatus(Status.OK.getStatusCode());
        test.assertContentType(MediaType.TEXT_PLAIN);
        test.assertContentContains("<" + uri + ">");

        // DeleteContentItem
        String path = builder.buildUrl(defaultPath + uri);
        test = executor.execute(builder.buildOtherRequest(new HttpDelete(path)));
        test.assertStatus(Status.OK.getStatusCode());

        // GetContent with deleted item uri
        test = executor.execute(builder.buildGetRequest(defaultPath + "content/" + uri));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testCreate() throws ClientProtocolException, UnsupportedEncodingException, IOException {
        RequestExecutor test;
        final String content = "Paris is the capital city of France.";
        final String uri = "urn:content-item-sha1-1de18ff0566f179c31aea7ff28de6bbf574ee0da";
        // create ContentItem
        test = executor.execute(builder.buildPostRequest(defaultPath).withContent(content));
        test.assertStatus(Status.CREATED.getStatusCode());

        // test whether it is created
        test = executor.execute(builder.buildGetRequest(defaultPath + "content/" + uri));
        test.assertStatus(Status.OK.getStatusCode());

        // create ContentItem with id
        test = executor.execute(builder.buildPostRequest(defaultPath + "test_item_uri").withContent(content));
        test.assertStatus(Status.CREATED.getStatusCode());

        // test whether it is created
        test = executor.execute(builder.buildGetRequest(defaultPath + "content/test_item_uri"));
        test.assertStatus(Status.OK.getStatusCode());

        // delete created items
        String path = builder.buildUrl(defaultPath + uri);
        test = executor.execute(builder.buildOtherRequest(new HttpDelete(path)));
        test.assertStatus(Status.OK.getStatusCode());
        path = builder.buildUrl(defaultPath + "test_item_uri");
        test = executor.execute(builder.buildOtherRequest(new HttpDelete(path)));
        test.assertStatus(Status.OK.getStatusCode());

        // test whether they are deleted
        test = executor.execute(builder.buildGetRequest(defaultPath + "content/" + uri));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());

        test = executor.execute(builder.buildGetRequest(defaultPath + "content/test_item_uri"));
        test.assertStatus(Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void testPostFile() throws IOException {
        final String content = "I live in Istanbul.";
        final String uri = "urn:content-item-sha1-cc34b9186ed006d9b0462adabbf290145ba6948d";
        // create file to submit
        String fileName = "test.file";
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        boolean success = file.createNewFile();
        if (success) {
            // fill content of file
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName));
            bufferedWriter.write(content);
            bufferedWriter.close();

            // createContentItemFromForm with file
            FormDataMultiPart part = new FormDataMultiPart().field("file", file, MediaType.TEXT_PLAIN_TYPE);
            WebResource resource = Client.create().resource(serverBaseUrl + defaultPath);
            ClientResponse response = resource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(
                ClientResponse.class, part);
            assertEquals(Status.OK.getStatusCode(), response.getStatus());

            RequestExecutor test;

            // test whether it is created
            test = executor.execute(builder.buildGetRequest(defaultPath + "content/" + uri));
            test.assertStatus(Status.OK.getStatusCode());

            // delete created ContentItem and files
            String path = builder.buildUrl(defaultPath + uri);
            test = executor.execute(builder.buildOtherRequest(new HttpDelete(path)));
            test.assertStatus(Status.OK.getStatusCode());
            file.delete();
        } else {
            assertTrue("Failed to create file", false);
        }
    }
}
