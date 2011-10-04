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

package org.apache.stanbol.contenthub.core.utils;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * 
 * @author cihan
 * 
 */
public class ContentHubClient {

    private static final String DELIMITER = "/";
    private static final String PATH_RAW = "raw";

    private static final String PARAM_CONTENT = "content";

    public static final ContentHubClient getInstance(String contentHubEndpoint) {
        return new ContentHubClient(contentHubEndpoint);
    }

    private ContentHubClient(String contentHubEndpoint) {
        this.contentHubEndpoint = contentHubEndpoint;
        if (!this.contentHubEndpoint.endsWith(DELIMITER)) {
            this.contentHubEndpoint += DELIMITER;
        }

        this.client = Client.create();
    }

    private String contentHubEndpoint;
    private Client client;

    public URI storeContent(String content) {
        nullCheck(content, "content");
        String path = contentHubEndpoint;
        WebResource wr = client.resource(path);
        MultivaluedMap<String,String> form = new MultivaluedMapImpl();
        form.add(PARAM_CONTENT, content);
        String response = wr.accept(MediaType.APPLICATION_XML).type(MediaType.APPLICATION_FORM_URLENCODED)
                .post(String.class, form);
        try {
            response = response.substring(response.indexOf("Content Item:") + 13, response.indexOf("("))
                    .trim();
            return new URI(contentHubEndpoint + PATH_RAW + DELIMITER + response);
        } catch (ClientHandlerException e) {
            return null;
        } catch (UniformInterfaceException e) {
            return null;
        } catch (URISyntaxException e) {
            return null;
        }

    }

    private void nullCheck(Object obj, String parameterName) {
        if (obj == null) {
            throw new IllegalArgumentException("Parameter " + parameterName + "can not be null");
        }
    }
}
